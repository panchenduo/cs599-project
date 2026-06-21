package com.wut.shortlink.aggregation.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wut.shortlink.admin.common.biz.user.UserContext;
import com.wut.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.wut.shortlink.admin.service.GroupService;
import com.wut.shortlink.aggregation.dto.req.ShortLinkAgentChatReqDTO;
import com.wut.shortlink.aggregation.dto.resp.AgentToolCallRespDTO;
import com.wut.shortlink.aggregation.dto.resp.ShortLinkAgentChatRespDTO;
import com.wut.shortlink.aggregation.memory.AgentMemoryService;
import com.wut.shortlink.aggregation.memory.AgentMemorySnapshot;
import com.wut.shortlink.aggregation.runtime.AgentRoutingExecutor;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Java 原生智能短链 Agent 编排实现。
 * <p>
 * 借鉴 RAG 项目的“模型路由 + 熔断 + 摘要记忆”模式，但保持当前短链项目轻量可演示。
 */
@Service
@RequiredArgsConstructor
public class ShortLinkAgentServiceImpl implements ShortLinkAgentService {

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s，。；,;]+)");
    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)\\s*(天|日|day|days)");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String PRIMARY_AGENT = "rule-planner-v2";
    private static final String FALLBACK_AGENT = "safe-fallback";

    private final GroupService groupService;
    private final ShortLinkService shortLinkService;
    private final ShortLinkStatsService shortLinkStatsService;
    private final AgentMemoryService agentMemoryService;
    private final AgentRoutingExecutor agentRoutingExecutor;

    @Override
    public ShortLinkAgentChatRespDTO chat(ShortLinkAgentChatReqDTO requestParam) {
        ShortLinkAgentChatReqDTO actualRequest = requestParam == null ? new ShortLinkAgentChatReqDTO() : requestParam;
        String conversationId = firstNotBlank(actualRequest.getConversationId(), UUID.randomUUID().toString());
        String userId = firstNotBlank(UserContext.getUsername(), "anonymous");
        String message = Optional.ofNullable(actualRequest.getMessage()).orElse("").trim();
        List<AgentToolCallRespDTO> toolCalls = new ArrayList<>();
        AgentMemorySnapshot memory = agentMemoryService.load(conversationId, userId);

        if (notBlank(message)) {
            agentMemoryService.append(conversationId, userId, "user", message);
        }

        AgentRoutingExecutor.RoutedResult<ShortLinkAgentChatRespDTO> routed = agentRoutingExecutor.executeWithFallback(
                List.of(PRIMARY_AGENT, FALLBACK_AGENT),
                candidate -> {
                    if (FALLBACK_AGENT.equals(candidate)) {
                        return buildSafeFallbackResp(toolCalls);
                    }
                    return dispatch(actualRequest, message, memory, toolCalls);
                }
        );
        ShortLinkAgentChatRespDTO response = routed.result();
        response.setConversationId(conversationId);
        response.setMemorySummary(firstNotBlank(memory.getSummary(), "暂无压缩记忆"));
        response.setDispatchStatus(routed.status()
                + "；主策略熔断状态：" + agentRoutingExecutor.health(PRIMARY_AGENT)
                + "；兜底策略熔断状态：" + agentRoutingExecutor.health(FALLBACK_AGENT));

        agentMemoryService.append(conversationId, userId, "assistant", response.getAnswer());
        return response;
    }

    private ShortLinkAgentChatRespDTO dispatch(ShortLinkAgentChatReqDTO requestParam,
                                               String message,
                                               AgentMemorySnapshot memory,
                                               List<AgentToolCallRespDTO> toolCalls) {
        String intent = resolveIntent(message, requestParam, memory);
        return switch (intent) {
            case "CREATE_SHORT_LINK" -> createShortLink(requestParam, message, toolCalls);
            case "BATCH_CREATE_SHORT_LINK" -> batchCreateShortLink(requestParam, message, toolCalls);
            case "ANALYZE_GROUP_STATS" -> analyzeGroupStats(requestParam, message, toolCalls);
            case "QUERY_LINKS" -> queryLinks(requestParam, message, toolCalls);
            case "QUERY_GROUPS" -> queryGroups(toolCalls);
            default -> buildHelpResp(toolCalls);
        };
    }

    private String resolveIntent(String message, ShortLinkAgentChatReqDTO requestParam, AgentMemorySnapshot memory) {
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
        if (containsAny(message, "链接列表", "短链列表", "有哪些链接", "查看链接", "查询链接", "这个链接")) {
            return "QUERY_LINKS";
        }
        if (containsAny(message, "分组", "组列表", "有哪些组")) {
            return "QUERY_GROUPS";
        }
        if (notBlank(memory.getSummary()) && containsAny(message, "继续", "刚才", "上面")) {
            return "QUERY_LINKS";
        }
        return "HELP";
    }

    private ShortLinkAgentChatRespDTO createShortLink(ShortLinkAgentChatReqDTO requestParam, String message, List<AgentToolCallRespDTO> toolCalls) {
        String originUrl = firstNotBlank(requestParam.getOriginUrl(), extractUrls(message).stream().findFirst().orElse(null));
        if (!notBlank(originUrl)) {
            return buildNeedMoreInfoResp("CREATE_SHORT_LINK", "我还缺少原始链接。请把需要生成短链的 http 或 https 地址发给我。");
        }
        ShortLinkGroupRespDTO group = resolveGroup(requestParam.getGid(), message, toolCalls);
        if (group == null) {
            return buildNoGroupResp("CREATE_SHORT_LINK", toolCalls);
        }
        ShortLinkCreateReqDTO createReq = ShortLinkCreateReqDTO.builder()
                .originUrl(originUrl)
                .gid(group.getGid())
                .createdType(0)
                .validDateType(resolveValidDateType(requestParam, message))
                .validDate(resolveValidDate(requestParam, message))
                .describe(firstNotBlank(requestParam.getDescribe(), buildDescribe(message, originUrl)))
                .build();
        try {
            ShortLinkCreateRespDTO createResp = callTool("createShortLink", createReq, () -> shortLinkService.createShortLink(createReq), toolCalls);
            return ShortLinkAgentChatRespDTO.builder()
                    .intent("CREATE_SHORT_LINK")
                    .answer("已创建短链：" + createResp.getFullShortUrl() + "\n原始链接：" + createResp.getOriginUrl() + "\n分组：" + group.getName())
                    .suggestions(List.of("查看当前分组短链列表", "继续批量创建短链", "生成当前分组运营报告"))
                    .toolCalls(toolCalls)
                    .build();
        } catch (RuntimeException ex) {
            return buildToolFailureResp("CREATE_SHORT_LINK", "短链创建失败：" + simplifyError(ex), toolCalls);
        }
    }

    private ShortLinkAgentChatRespDTO batchCreateShortLink(ShortLinkAgentChatReqDTO requestParam, String message, List<AgentToolCallRespDTO> toolCalls) {
        List<String> urls = extractUrls(message);
        if (urls.isEmpty() && notBlank(requestParam.getOriginUrl())) {
            urls = List.of(requestParam.getOriginUrl());
        }
        if (urls.isEmpty()) {
            return buildNeedMoreInfoResp("BATCH_CREATE_SHORT_LINK", "我没有识别到可批量创建的链接。请一次提供多个 http 或 https 地址。");
        }
        ShortLinkGroupRespDTO group = resolveGroup(requestParam.getGid(), message, toolCalls);
        if (group == null) {
            return buildNoGroupResp("BATCH_CREATE_SHORT_LINK", toolCalls);
        }
        ShortLinkBatchCreateReqDTO batchReq = new ShortLinkBatchCreateReqDTO();
        batchReq.setOriginUrls(urls);
        batchReq.setDescribes(urls.stream().map(each -> firstNotBlank(requestParam.getDescribe(), "Agent 批量创建：" + each)).toList());
        batchReq.setGid(group.getGid());
        batchReq.setCreatedType(0);
        batchReq.setValidDateType(resolveValidDateType(requestParam, message));
        batchReq.setValidDate(resolveValidDate(requestParam, message));
        try {
            ShortLinkBatchCreateRespDTO batchResp = callTool("batchCreateShortLink", batchReq, () -> shortLinkService.batchCreateShortLink(batchReq), toolCalls);
            String links = Optional.ofNullable(batchResp.getBaseLinkInfos()).orElse(List.of()).stream()
                    .map(each -> "- " + each.getFullShortUrl() + " -> " + each.getOriginUrl())
                    .collect(Collectors.joining("\n"));
            return ShortLinkAgentChatRespDTO.builder()
                    .intent("BATCH_CREATE_SHORT_LINK")
                    .answer("批量创建完成，共成功 " + Optional.ofNullable(batchResp.getTotal()).orElse(0) + " 条，分组：" + group.getName() + "\n" + links)
                    .suggestions(List.of("分析这个分组最近 7 天访问效果", "查看当前分组短链列表"))
                    .toolCalls(toolCalls)
                    .build();
        } catch (RuntimeException ex) {
            return buildToolFailureResp("BATCH_CREATE_SHORT_LINK", "批量创建失败：" + simplifyError(ex), toolCalls);
        }
    }

    private ShortLinkAgentChatRespDTO analyzeGroupStats(ShortLinkAgentChatReqDTO requestParam, String message, List<AgentToolCallRespDTO> toolCalls) {
        ShortLinkGroupRespDTO group = resolveGroup(requestParam.getGid(), message, toolCalls);
        if (group == null) {
            return buildNoGroupResp("ANALYZE_GROUP_STATS", toolCalls);
        }
        LocalDate endDate = parseDate(requestParam.getEndDate(), LocalDate.now());
        LocalDate startDate = parseDate(requestParam.getStartDate(), endDate.minusDays(resolveDaysFromMessage(message, 7) - 1L));
        ShortLinkGroupStatsReqDTO statsReq = new ShortLinkGroupStatsReqDTO();
        statsReq.setGid(group.getGid());
        statsReq.setStartDate(startDate.format(DATE_FORMATTER));
        statsReq.setEndDate(endDate.format(DATE_FORMATTER));
        try {
            ShortLinkStatsRespDTO statsResp = callTool("groupShortLinkStats", statsReq, () -> shortLinkStatsService.groupShortLinkStats(statsReq), toolCalls);
            return ShortLinkAgentChatRespDTO.builder()
                    .intent("ANALYZE_GROUP_STATS")
                    .answer(buildStatsAnswer(group, statsReq, Optional.ofNullable(statsResp).orElse(new ShortLinkStatsRespDTO())))
                    .suggestions(List.of("查看当前分组短链列表", "创建一个新的活动短链", "继续分析访问设备和地域分布"))
                    .toolCalls(toolCalls)
                    .build();
        } catch (RuntimeException ex) {
            return buildToolFailureResp("ANALYZE_GROUP_STATS", "统计分析失败：" + simplifyError(ex), toolCalls);
        }
    }

    private ShortLinkAgentChatRespDTO queryLinks(ShortLinkAgentChatReqDTO requestParam, String message, List<AgentToolCallRespDTO> toolCalls) {
        ShortLinkGroupRespDTO group = resolveGroup(requestParam.getGid(), message, toolCalls);
        if (group == null) {
            return buildNoGroupResp("QUERY_LINKS", toolCalls);
        }
        ShortLinkPageReqDTO pageReq = new ShortLinkPageReqDTO();
        pageReq.setGid(group.getGid());
        pageReq.setCurrent(1);
        pageReq.setSize(5);
        try {
            IPage<ShortLinkPageRespDTO> pageResp = callTool("pageShortLink", pageReq, () -> shortLinkService.pageShortLink(pageReq), toolCalls);
            List<ShortLinkPageRespDTO> records = Optional.ofNullable(pageResp.getRecords()).orElse(List.of());
            String rows = records.stream()
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
        } catch (RuntimeException ex) {
            return buildToolFailureResp("QUERY_LINKS", "查询短链列表失败：" + simplifyError(ex), toolCalls);
        }
    }

    private ShortLinkAgentChatRespDTO queryGroups(List<AgentToolCallRespDTO> toolCalls) {
        List<ShortLinkGroupRespDTO> groups = listGroups(toolCalls);
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

    private <T> T callTool(String toolName, Object request, Supplier<T> supplier, List<AgentToolCallRespDTO> toolCalls) {
        long start = System.currentTimeMillis();
        try {
            T response = supplier.get();
            toolCalls.add(AgentToolCallRespDTO.builder()
                    .toolName(toolName)
                    .request(request)
                    .response(compactResponse(response))
                    .success(Boolean.TRUE)
                    .durationMs(System.currentTimeMillis() - start)
                    .message("工具调用成功")
                    .build());
            return response;
        } catch (RuntimeException ex) {
            toolCalls.add(AgentToolCallRespDTO.builder()
                    .toolName(toolName)
                    .request(request)
                    .response(Map.of("error", simplifyError(ex)))
                    .success(Boolean.FALSE)
                    .durationMs(System.currentTimeMillis() - start)
                    .message("工具调用失败")
                    .build());
            throw ex;
        }
    }

    private ShortLinkGroupRespDTO resolveGroup(String gid, String message, List<AgentToolCallRespDTO> toolCalls) {
        List<ShortLinkGroupRespDTO> groups = listGroups(toolCalls);
        if (groups.isEmpty()) {
            return null;
        }
        if (notBlank(gid)) {
            Optional<ShortLinkGroupRespDTO> matchedGroup = groups.stream()
                    .filter(each -> Objects.equals(each.getGid(), gid))
                    .findFirst();
            if (matchedGroup.isPresent()) {
                return matchedGroup.get();
            }
        }
        if (containsAny(message, "默认")) {
            Optional<ShortLinkGroupRespDTO> defaultGroup = groups.stream()
                    .filter(each -> Optional.ofNullable(each.getName()).orElse("").contains("默认"))
                    .findFirst();
            if (defaultGroup.isPresent()) {
                return defaultGroup.get();
            }
        }
        Optional<ShortLinkGroupRespDTO> namedGroup = groups.stream()
                .filter(each -> notBlank(each.getName()) && notBlank(message) && message.contains(each.getName()))
                .findFirst();
        return namedGroup.orElse(groups.get(0));
    }

    private List<ShortLinkGroupRespDTO> listGroups(List<AgentToolCallRespDTO> toolCalls) {
        try {
            return callTool("listGroups", Map.of(), groupService::groupList, toolCalls);
        } catch (RuntimeException ex) {
            return List.of();
        }
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

    private ShortLinkAgentChatRespDTO buildSafeFallbackResp(List<AgentToolCallRespDTO> toolCalls) {
        return ShortLinkAgentChatRespDTO.builder()
                .intent("SAFE_FALLBACK")
                .answer("主调度策略暂时不可用，我已切换到安全兜底模式。你可以先执行查询类任务，或稍后再尝试创建/分析操作。")
                .suggestions(List.of("查看当前分组列表", "查看当前分组短链列表", "分析默认分组最近 7 天访问情况"))
                .toolCalls(toolCalls)
                .build();
    }

    private ShortLinkAgentChatRespDTO buildNeedMoreInfoResp(String intent, String answer) {
        return ShortLinkAgentChatRespDTO.builder()
                .intent(intent)
                .answer(answer)
                .suggestions(List.of("查看当前分组列表", "帮我给 https://www.zhihu.com 创建一个 7 天有效的短链"))
                .toolCalls(List.of())
                .build();
    }

    private ShortLinkAgentChatRespDTO buildNoGroupResp(String intent, List<AgentToolCallRespDTO> toolCalls) {
        return ShortLinkAgentChatRespDTO.builder()
                .intent(intent)
                .answer("当前账户还没有可用分组，或者分组查询失败。请先创建短链分组后再使用智能助手。")
                .suggestions(List.of("查看当前分组列表"))
                .toolCalls(toolCalls)
                .build();
    }

    private ShortLinkAgentChatRespDTO buildToolFailureResp(String intent, String answer, List<AgentToolCallRespDTO> toolCalls) {
        return ShortLinkAgentChatRespDTO.builder()
                .intent(intent)
                .answer(answer)
                .suggestions(List.of("查看当前分组列表", "查看当前分组短链列表", "换一个链接重试"))
                .toolCalls(toolCalls)
                .build();
    }

    private String buildStatsAnswer(ShortLinkGroupRespDTO group, ShortLinkGroupStatsReqDTO request, ShortLinkStatsRespDTO stats) {
        int pv = Optional.ofNullable(stats.getPv()).orElse(0);
        int uv = Optional.ofNullable(stats.getUv()).orElse(0);
        int uip = Optional.ofNullable(stats.getUip()).orElse(0);
        return "分组「" + group.getName() + "」在 " + request.getStartDate() + " 至 " + request.getEndDate() + " 的运营分析：\n"
                + "1. 总访问 PV " + pv + "，独立访客 UV " + uv + "，独立 IP " + uip + "。\n"
                + "2. " + buildConversionInsight(pv, uv, uip) + "\n"
                + "3. " + buildDistributionInsight(stats) + "\n"
                + "4. " + buildRiskInsight(stats);
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

    private Object compactResponse(Object response) {
        if (response instanceof ShortLinkStatsRespDTO stats) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("pv", Optional.ofNullable(stats.getPv()).orElse(0));
            summary.put("uv", Optional.ofNullable(stats.getUv()).orElse(0));
            summary.put("uip", Optional.ofNullable(stats.getUip()).orElse(0));
            return summary;
        }
        if (response instanceof IPage<?> page) {
            return Map.of("total", page.getTotal(), "size", page.getRecords().size());
        }
        return response;
    }

    private List<String> extractUrls(String message) {
        Matcher matcher = URL_PATTERN.matcher(Optional.ofNullable(message).orElse(""));
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
        if (DAYS_PATTERN.matcher(Optional.ofNullable(message).orElse("")).find()) {
            return 1;
        }
        return 0;
    }

    private Date resolveValidDate(ShortLinkAgentChatReqDTO requestParam, String message) {
        Integer validDays = requestParam.getValidDays();
        if (validDays == null) {
            Matcher matcher = DAYS_PATTERN.matcher(Optional.ofNullable(message).orElse(""));
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

    private int resolveDaysFromMessage(String message, int defaultDays) {
        Matcher matcher = DAYS_PATTERN.matcher(Optional.ofNullable(message).orElse(""));
        if (matcher.find()) {
            return Math.max(Integer.parseInt(matcher.group(1)), 1);
        }
        return defaultDays;
    }

    private String buildDescribe(String message, String originUrl) {
        String cleaned = Optional.ofNullable(message).orElse("").replace(originUrl, "")
                .replaceAll("帮我|创建|生成|新增|短链|短链接|有效|永久|长期|\\d+\\s*(天|日|day|days)", "")
                .replaceAll("\\s+", "");
        if (!notBlank(cleaned) || cleaned.length() > 40) {
            return "Agent 创建短链";
        }
        return cleaned;
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        if (!notBlank(value)) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return defaultValue;
        }
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

    private String simplifyError(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        if (!notBlank(message)) {
            return "未知错误";
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private boolean notBlank(String source) {
        return source != null && !source.trim().isEmpty();
    }
}
