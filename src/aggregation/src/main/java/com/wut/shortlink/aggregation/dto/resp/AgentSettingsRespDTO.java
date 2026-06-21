package com.wut.shortlink.aggregation.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AgentSettingsRespDTO {

    private ModelSettings model;

    private RuntimeSettings runtime;

    @Data
    @Builder
    public static class ModelSettings {
        private Map<String, ProviderConfig> providers;
        private ModelGroup chat;
        private Selection selection;
        private Stream stream;
    }

    @Data
    @Builder
    public static class ProviderConfig {
        private String url;
        private String apiKey;
        private Map<String, String> endpoints;
    }

    @Data
    @Builder
    public static class ModelGroup {
        private String defaultModel;
        private String deepThinkingModel;
        private List<ModelCandidate> candidates;
    }

    @Data
    @Builder
    public static class ModelCandidate {
        private String id;
        private String provider;
        private String model;
        private String url;
        private Integer priority;
        private Boolean enabled;
        private Boolean supportsThinking;
        private String health;
    }

    @Data
    @Builder
    public static class Selection {
        private Integer failureThreshold;
        private Long openDurationMs;
    }

    @Data
    @Builder
    public static class Stream {
        private Integer messageChunkSize;
    }

    @Data
    @Builder
    public static class RuntimeSettings {
        private Boolean memoryEnabled;
        private Integer historyKeepTurns;
        private Integer summaryStartTurns;
        private Integer summaryMaxChars;
    }
}
