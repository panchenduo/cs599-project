package com.wut.shortlink.aggregation.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wut.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.wut.shortlink.admin.service.GroupService;
import com.wut.shortlink.aggregation.dto.req.ShortLinkAgentChatReqDTO;
import com.wut.shortlink.aggregation.dto.resp.AgentToolCallRespDTO;
import com.wut.shortlink.aggregation.dto.resp.ShortLinkAgentChatRespDTO;
import com.wut.shortlink.aggregation.service.ShortLinkAgentService;
import com.wut.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.wut.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.wut.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import com.wut.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkStatsRespDTO;
import com.wut.shortlink.project.service.ShortLinkService;
import com.wut.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Java 原生智能短链 Agent 编排实现。
 * <p>
 * 当前版本采用规则意图识别 + 短链业务工具调用，避免演示强依赖外部 LLM Key；
 * 后续可以在 intent 识别和 answer 生成阶段接入 LangChain4j 或大模型 Function Calling。
 */
@Service
@RequiredArgsConstructor
public class ShortLinkAgentServiceImpl implements ShortLinkAgentService {

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s，。；,;]+)");
    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)\\s*(天|日|day|days)");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final GroupService groupService;
    private final ShortLinkService shortLinkService;
    private final ShortLinkStatsService shortLinkStatsService;

    @Override
    public ShortLinkAgentChatRespDTO chat(ShortLinkAgentChatReqDTO requestParam) {
        String message = Optional.ofNullable(requestParam.getMessage()).orElse("").trim();
        String intent = resolveIntent(message, requestParam);
        List<AgentToolCallRespDTO> toolCalls = new ArrayList<>();
        return switch (intent) {
            case "CREATE_SHORT_LINK" -> createShortLink(requestParam, message, toolCalls);
            case "BATCH_CREATE_SHORT_LINK" -> batchCreateShortLink(requestParam, message, toolCalls);
            case "ANALYZE_GROUP_STATS" -> analyzeGroupStats(requestParam, toolCalls);
            case "QUERY_LINKS" -> queryLinks(requestParam, toolCalls);
            case "QUERY_GROUPS" -> queryGroups(toolCalls);
            default -> buildHelpResp(toolCalls);
        };
    }

    private String resolveIntent(String message, ShortLinkAgentChatReqDTO requestParam) {
        List<String> urls = extractUrls(message);
        if (urls.size() > 1 || containsAny(message, "批量", "多个", "一批")) {
            return "BATCH_CREATE_SHORT_LINK";
        }
        if (notBlank(requestParam.getOriginUrl()) || (!urls.isEmpty() && containsAny(message, "创建", "生成", "新增", "短链", "短链接"))) {
            return "CREATE_SHORT_LINK";
        }
        if (containsAny(message, "统计", "分析", "报告", "访问", "异常", "趋势", "运营", "优化")) {
            return "ANALYZE_GROUP_STATS";
        }
        if (containsAny(message, "分组", "组列表", "有哪些组")) {
            return "QUERY_GROUPS";
        }
        if (containsAny(message, "链接列表", "短链列表", "有哪些链接", "查看链接", "查询链接")) {
            return "QUERY_LINKS";
        }
        return "HELP";
    }

    private ShortLinkAgentChatRespDTO createShortLink(ShortLinkAgentChatReqDTO requestParam, String message, List<AgentToolCallRespDTO> toolCalls) {
        String originUrl = firstNotBlank(requestParam.getOriginUrl(), extractUrls(message).stream().findFirst().orElse(null));
        if (!notBlank(originUrl)) {
            return ShortLinkAgentChatRespDTO.builder()
                    .intent("CREATE_SHORT_LINK")
                    .answer("我还缺少原始链接。请把需要生成短链的 http 或 https 地址发给我。")
                    .suggestions(List.of("例如：帮我给 https://www.zhihu.com 创建一个 7 天有效的短链"))
                    .toolCalls(toolCalls)
                    .build();
        }
        ShortLinkGroupRespDTO group = resolveGroup(requestParam.getGid(), toolCalls);
        ShortLinkCreateReqDTO createReq = ShortLinkCreateReqDTO.builder()
                .originUrl(originUrl)
                .gid(group.getGid())
                .createdType(0)
                .validDateType(resolveValidDateType(requestParam, message))
                .validDate(resolveValidDate(requestParam, message))
                .describe(firstNotBlank(requestParam.getDescribe(), buildDescribe(message, originUrl)))
                .build();
        ShortLinkCreateRespDTO createResp = shortLinkService.createShortLink(createReq);
        toolCalls.add(AgentToolCallRespDTO.builder()
                .toolName("createShortLink")
                .request(createReq)
                .response(createResp)
                .success(Boolean.TRUE)
                .message("已调用短链接创建工具")
                .build());
        return ShortLinkAgentChatRespDTO.builder()
                .intent("CREATE_SHORT_LINK")
                .answer("已创建短链：" + createResp.getFullShortUrl() + "\n原始链接：" + createResp.getOriginUrl() + "\n分组：" + group.getName())
                .suggestions(List.of("查看这个短链的访问统计", "继续批量创建短链", "生成当前分组运营报告"))
                .toolCalls(toolCalls)
                .build();
    }

    private ShortLinkAgentChatRespDTO batchCreateShortLink(ShortLinkAgentChatReqDTO requestParam, String message, List<AgentToolCallRespDTO> toolCalls) {
        List<String> urls = extractUrls(message);
        if (urls.isEmpty() && notBlank(requestParam.getOriginUrl())) {
            urls = List.of(requestParam.getOriginUrl());
        }
        if (urls.isEmpty()) {
            return ShortLinkAgentChatRespDTO.builder()
                    .intent("BATCH_CREATE_SHORT_LINK")
                    .answer("我没有识别到可批量创建的链接。请一次提供多个 http 或 https 地址。")
                    .suggestions(List.of("例如：批量创建 https://a.com 和 https://b.com 的短链"))
                    .toolCalls(toolCalls)
                    .build();
        }
        ShortLinkGroupRespDTO group = resolveGroup(requestParam.getGid(), toolCalls);
        ShortLinkBatchCreateReqDTO batchReq = new ShortLinkBatchCreateReqDTO();
        batchReq.setOriginUrls(urls);
        batchReq.setDescribes(urls.stream().map(each -> firstNotBlank(requestParam.getDescribe(), "Agent 批量创建：" + each)).toList());
        batchReq.setGid(group.getGid());
        batchReq.setCreatedType(0);
        batchReq.setValidDateType(resolveValidDateType(requestParam, message));
        batchReq.setValidDate(resolveValidDate(requestParam, message));
        ShortLinkBatchCreateRespDTO batchResp = shortLinkService.batchCreateShortLink(batchReq);
        toolCalls.add(AgentToolCallRespDTO.builder()
                .toolName("batchCreateShortLink")
                .request(batchReq)
                .response(batchResp)
                .success(Boolean.TRUE)
                .message("已调用批量创建工具")
                .build());
        String links = batchResp.getBaseLinkInfos().stream()
                .map(each -> "- " + each.getFullShortUrl() + " -> " + each.getOriginUrl())
                .collect(Collectors.joining("\n"));
        return ShortLinkAgentChatRespDTO.builder()
                .intent("BATCH_CREATE_SHORT_LINK")
                .answer("批量创建完成，共成功 " + batchResp.getTotal() + " 条，分组：" + group.getName() + "\n" + links)
                .suggestions(List.of("分析这个分组最近 7 天访问效果", "查看短链列表"))
                .toolCalls(toolCalls)
                .build();
    }

    private ShortLinkAgentChatRespDTO analyzeGroupStats(ShortLinkAgentChatReqDTO requestParam, List<AgentToolCallRespDTO> toolCalls) {
        ShortLinkGroupRespDTO group = resolveGroup(requestParam.getGid(), toolCalls);
        LocalDate endDate = parseDate(requestParam.getEndDate(), LocalDate.now());
        LocalDate startDate = parseDate(requestParam.getStartDate(), endDate.minusDays(6));
        ShortLinkGroupStatsReqDTO statsReq = new ShortLinkGroupStatsReqDTO();
        statsReq.setGid(group.getGid());
        statsReq.setStartDate(startDate.format(DATE_FORMATTER));
        statsReq.setEndDate(endDate.format(DATE_FORMATTER));
        ShortLinkStatsRespDTO statsResp = shortLinkStatsService.groupShortLinkStats(statsReq);
        toolCalls.add(AgentToolCallRespDTO.builder()
                .toolName("groupShortLinkStats")
                .request(statsReq)
                .response(statsSummary(statsResp))
                .success(Boolean.TRUE)
                .message("已调用分组访问统计工具")
                .build());
        return ShortLinkAgentChatRespDTO.builder()
                .intent("ANALYZE_GROUP_STATS")
                .answer(buildStatsAnswer(group, statsReq, statsResp))
                .suggestions(List.of("查看当前分组短链列表", "创建一个新的活动短链", "继续分析访问设备和地域分布"))
                .toolCalls(toolCalls)
                .build();
    }

    private ShortLinkAgentChatRespDTO queryLinks(ShortLinkAgentChatReqDTO requestParam, List<AgentToolCallRespDTO> toolCalls) {
        ShortLinkGroupRespDTO group = resolveGroup(requestParam.getGid(), toolCalls);
        ShortLinkPageReqDTO pageReq = new ShortLinkPageReqDTO();
        pageReq.setGid(group.getGid());
        pageReq.setCurrent(1);
        pageReq.setSize(5);
        IPage<ShortLinkPageRespDTO> pageResp = shortLinkService.pageShortLink(pageReq);
        toolCalls.add(AgentToolCallRespDTO.builder()
                .toolName("pageShortLink")
                .request(pageReq)
                .response(Map.of("total", pageResp.getTotal(), "size", pageResp.getRecords().size()))
                .success(Boolean.TRUE)
                .message("已调用短链分页查询工具")
                .build());
        String rows = pageResp.getRecords().stream()
                .map(each -> "- " + firstNotBlank(each.getDescribe(), "未命名短链") + "：" + each.getFullShortUrl()
                        + "，PV " + Optional.ofNullable(each.getTotalPv()).orElse(0))
                .collect(Collectors.joining("\n"));
        if (!notBlank(rows)) {
            rows = "当前分组还没有短链。";
        }
        return ShortLinkAgentChatRespDTO.builder()
                .intent("QUERY_LINKS")
                .answer("分组「" + group.getName() + "」的短链概览：\n" + rows)
                .suggestions(List.of("创建一个短链", "生成当前分组运营报告"))
                .toolCalls(toolCalls)
                .build();
    }

    private ShortLinkAgentChatRespDTO queryGroups(List<AgentToolCallRespDTO> toolCalls) {
        List<ShortLinkGroupRespDTO> groups = groupService.groupList();
        toolCalls.add(AgentToolCallRespDTO.builder()
                .toolName("listGroups")
                .request(Map.of())
                .response(groups)
                .success(Boolean.TRUE)
                .message("已调用分组列表工具")
                .build());
        String content = groups.stream()
                .map(each -> "- " + each.getName() + "：" + each.getGid() + "，短链数 " + firstNotBlank(each.getShortLinkCount(), "0"))
                .collect(Collectors.joining("\n"));
        if (!notBlank(content)) {
            content = "当前账户还没有短链分组。";
        }
        return ShortLinkAgentChatRespDTO.builder()
                .intent("QUERY_GROUPS")
                .answer("当前可用分组：\n" + content)
                .suggestions(List.of("查看默认分组短链列表", "分析某个分组最近 7 天访问数据"))
                .toolCalls(toolCalls)
                .build();
    }

    private ShortLinkAgentChatRespDTO buildHelpResp(List<AgentToolCallRespDTO> toolCalls) {
        return ShortLinkAgentChatRespDTO.builder()
                .intent("HELP")
                .answer("我是智能短链运营 Agent，可以帮你创建短链、批量创建短链、查询分组和短链列表，也可以生成分组访问统计分析。")
                .suggestions(List.of(
                        "帮我给 https://www.zhihu.com 创建一个 7 天有效的短链",
                        "分析默认分组最近 7 天访问情况",
                        "查看当前分组短链列表"
                ))
                .toolCalls(toolCalls)
                .build();
    }

    private ShortLinkGroupRespDTO resolveGroup(String gid, List<AgentToolCallRespDTO> toolCalls) {
        List<ShortLinkGroupRespDTO> groups = groupService.groupList();
        toolCalls.add(AgentToolCallRespDTO.builder()
                .toolName("listGroups")
                .request(Map.of("gid", firstNotBlank(gid, "")))
                .response(groups)
                .success(Boolean.TRUE)
                .message("已获取当前用户短链分组")
                .build());
        if (notBlank(gid)) {
            Optional<ShortLinkGroupRespDTO> matchedGroup = groups.stream()
                    .filter(each -> Objects.equals(each.getGid(), gid))
                    .findFirst();
            if (matchedGroup.isPresent()) {
                return matchedGroup.get();
            }
        }
        return groups.stream().findFirst().orElseThrow(() -> new IllegalStateException("请先创建短链接分组"));
    }

    private String buildStatsAnswer(ShortLinkGroupRespDTO group, ShortLinkGroupStatsReqDTO request, ShortLinkStatsRespDTO stats) {
        int pv = Optional.ofNullable(stats.getPv()).orElse(0);
        int uv = Optional.ofNullable(stats.getUv()).orElse(0);
        int uip = Optional.ofNullable(stats.getUip()).orElse(0);
        StringBuilder builder = new StringBuilder();
        builder.append("分组「").append(group.getName()).append("」在 ")
                .append(request.getStartDate()).append(" 至 ").append(request.getEndDate()).append(" 的运营分析：\n");
        builder.append("1. 总访问 PV ").append(pv).append("，独立访客 UV ").append(uv).append("，独立 IP ").append(uip).append("。\n");
        builder.append("2. ").append(buildConversionInsight(pv, uv, uip)).append("\n");
        builder.append("3. ").append(buildDistributionInsight(stats)).append("\n");
        builder.append("4. ").append(buildRiskInsight(stats));
        return builder.toString();
    }

    private String buildConversionInsight(int pv, int uv, int uip) {
        if (pv == 0) {
            return "当前周期暂无访问数据，建议先投放测试链接或检查短链是否已正确分发。";
        }
        double pvPerUv = uv == 0 ? pv : (double) pv / uv;
        if (pvPerUv >= 3) {
            return "单用户重复访问较高，说明链接有复访或重复打开行为，可进一步检查是否来自内部测试或自动化流量。";
        }
        if (uip > uv * 1.5) {
            return "独立 IP 明显高于 UV，可能存在 Cookie 丢失、跨设备访问或异常流量，需要关注访问日志。";
        }
        return "PV、UV、UIP 比例整体平稳，短链访问质量暂未发现明显异常。";
    }

    private String buildDistributionInsight(ShortLinkStatsRespDTO stats) {
        String locale = Optional.ofNullable(stats.getLocaleCnStats()).orElse(List.of()).stream()
                .findFirst()
                .map(each -> each.getLocale() + "占比最高")
                .orElse("暂无地域分布数据");
        String device = Optional.ofNullable(stats.getDeviceStats()).orElse(List.of()).stream()
                .findFirst()
                .map(each -> each.getDevice() + "设备占比最高")
                .orElse("暂无设备分布数据");
        String browser = Optional.ofNullable(stats.getBrowserStats()).orElse(List.of()).stream()
                .findFirst()
                .map(each -> each.getBrowser() + "浏览器占比最高")
                .orElse("暂无浏览器分布数据");
        return locale + "，" + device + "，" + browser + "。";
    }

    private String buildRiskInsight(ShortLinkStatsRespDTO stats) {
        boolean hasHighFrequencyIp = Optional.ofNullable(stats.getTopIpStats()).orElse(List.of()).stream()
                .anyMatch(each -> Optional.ofNullable(each.getCnt()).orElse(0) >= 20);
        if (hasHighFrequencyIp) {
            return "发现高频访问 IP，建议结合访问日志判断是否存在刷量或爬虫访问。";
        }
        return "建议后续开启定时分析任务，对访问突增、地域集中和高频 IP 做自动告警。";
    }

    private Map<String, Object> statsSummary(ShortLinkStatsRespDTO stats) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pv", Optional.ofNullable(stats.getPv()).orElse(0));
        summary.put("uv", Optional.ofNullable(stats.getUv()).orElse(0));
        summary.put("uip", Optional.ofNullable(stats.getUip()).orElse(0));
        summary.put("localeCount", Optional.ofNullable(stats.getLocaleCnStats()).orElse(List.of()).size());
        summary.put("browserCount", Optional.ofNullable(stats.getBrowserStats()).orElse(List.of()).size());
        summary.put("deviceCount", Optional.ofNullable(stats.getDeviceStats()).orElse(List.of()).size());
        return summary;
    }

    private List<String> extractUrls(String message) {
        Matcher matcher = URL_PATTERN.matcher(message);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private int resolveValidDateType(ShortLinkAgentChatReqDTO requestParam, String message) {
        if (requestParam.getValidDays() != null && requestParam.getValidDays() > 0) {
            return 1;
        }
        if (DAYS_PATTERN.matcher(message).find()) {
            return 1;
        }
        if (containsAny(message, "永久", "长期")) {
            return 0;
        }
        return 0;
    }

    private Date resolveValidDate(ShortLinkAgentChatReqDTO requestParam, String message) {
        Integer validDays = requestParam.getValidDays();
        if (validDays == null) {
            Matcher matcher = DAYS_PATTERN.matcher(message);
            if (matcher.find()) {
                validDays = Integer.parseInt(matcher.group(1));
            }
        }
        if (validDays == null || validDays <= 0) {
            return null;
        }
        LocalDateTime expireTime = LocalDateTime.of(LocalDate.now().plusDays(validDays), LocalTime.MAX.withNano(0));
        return Date.from(expireTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private String buildDescribe(String message, String originUrl) {
        String cleaned = message.replace(originUrl, "").replaceAll("\\s+", "");
        if (!notBlank(cleaned) || cleaned.length() > 40) {
            return "Agent 创建短链";
        }
        return cleaned;
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        if (!notBlank(value)) {
            return defaultValue;
        }
        return LocalDate.parse(value, DATE_FORMATTER);
    }

    private boolean containsAny(String source, String... keywords) {
        if (!notBlank(source)) {
            return false;
        }
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String firstNotBlank(String first, String second) {
        return notBlank(first) ? first : second;
    }

    private boolean notBlank(String source) {
        return source != null && !source.trim().isEmpty();
    }
}
