package com.wut.shortlink.aggregation.controller;

import com.wut.shortlink.aggregation.config.AgentModelProperties;
import com.wut.shortlink.aggregation.config.ShortLinkAgentProperties;
import com.wut.shortlink.aggregation.dto.req.ShortLinkAgentChatReqDTO;
import com.wut.shortlink.aggregation.dto.resp.AgentSettingsRespDTO;
import com.wut.shortlink.aggregation.dto.resp.ShortLinkAgentChatRespDTO;
import com.wut.shortlink.aggregation.runtime.AgentRoutingExecutor;
import com.wut.shortlink.aggregation.service.ShortLinkAgentService;
import com.wut.shortlink.project.common.convention.result.Result;
import com.wut.shortlink.project.common.convention.result.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能短链 Agent 控制层
 */
@RestController
@RequiredArgsConstructor
public class ShortLinkAgentController {

    private final ShortLinkAgentService shortLinkAgentService;
    private final AgentModelProperties agentModelProperties;
    private final ShortLinkAgentProperties shortLinkAgentProperties;
    private final AgentRoutingExecutor agentRoutingExecutor;

    /**
     * 智能短链 Agent 对话
     */
    @PostMapping("/api/short-link/admin/v1/agent/chat")
    public Result<ShortLinkAgentChatRespDTO> chat(@RequestBody ShortLinkAgentChatReqDTO requestParam) {
        return Results.success(shortLinkAgentService.chat(requestParam));
    }

    @GetMapping("/api/short-link/admin/v1/agent/settings")
    public Result<AgentSettingsRespDTO> settings() {
        return Results.success(AgentSettingsRespDTO.builder()
                .model(toModelSettings())
                .runtime(toRuntimeSettings())
                .build());
    }

    private AgentSettingsRespDTO.ModelSettings toModelSettings() {
        Map<String, AgentSettingsRespDTO.ProviderConfig> providers = new HashMap<>();
        agentModelProperties.getProviders().forEach((key, value) -> providers.put(key, AgentSettingsRespDTO.ProviderConfig.builder()
                .url(value.getUrl())
                .apiKey(maskApiKey(value.getApiKey()))
                .endpoints(value.getEndpoints())
                .build()));
        return AgentSettingsRespDTO.ModelSettings.builder()
                .providers(providers)
                .chat(AgentSettingsRespDTO.ModelGroup.builder()
                        .defaultModel(agentModelProperties.getChat().getDefaultModel())
                        .deepThinkingModel(agentModelProperties.getChat().getDeepThinkingModel())
                        .candidates(agentModelProperties.getChat().getCandidates().stream()
                                .map(each -> AgentSettingsRespDTO.ModelCandidate.builder()
                                        .id(each.getId())
                                        .provider(each.getProvider())
                                        .model(each.getModel())
                                        .url(each.getUrl())
                                        .priority(each.getPriority())
                                        .enabled(each.getEnabled())
                                        .supportsThinking(each.getSupportsThinking())
                                        .health(agentRoutingExecutor.health(each.getId()))
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .selection(AgentSettingsRespDTO.Selection.builder()
                        .failureThreshold(agentModelProperties.getSelection().getFailureThreshold())
                        .openDurationMs(agentModelProperties.getSelection().getOpenDurationMs())
                        .build())
                .stream(AgentSettingsRespDTO.Stream.builder()
                        .messageChunkSize(agentModelProperties.getStream().getMessageChunkSize())
                        .build())
                .build();
    }

    private AgentSettingsRespDTO.RuntimeSettings toRuntimeSettings() {
        ShortLinkAgentProperties.Memory memory = shortLinkAgentProperties.getMemory();
        return AgentSettingsRespDTO.RuntimeSettings.builder()
                .memoryEnabled(memory.getEnabled())
                .historyKeepTurns(memory.getHistoryKeepTurns())
                .summaryStartTurns(memory.getSummaryStartTurns())
                .summaryMaxChars(memory.getSummaryMaxChars())
                .build();
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 10) {
            return "******";
        }
        return trimmed.substring(0, 6) + "***" + trimmed.substring(trimmed.length() - 4);
    }
}
