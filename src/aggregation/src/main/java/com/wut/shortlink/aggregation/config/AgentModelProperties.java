package com.wut.shortlink.aggregation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "short-link.agent.model")
public class AgentModelProperties {

    private Map<String, ProviderConfig> providers = new HashMap<>();

    private ModelGroup chat = new ModelGroup();

    private Selection selection = new Selection();

    private Stream stream = new Stream();

    @Data
    public static class ProviderConfig {
        private String url;
        private String apiKey;
        private Map<String, String> endpoints = new HashMap<>();
    }

    @Data
    public static class ModelGroup {
        private String defaultModel;
        private String deepThinkingModel;
        private List<ModelCandidate> candidates = new ArrayList<>();
    }

    @Data
    public static class ModelCandidate {
        private String id;
        private String provider;
        private String model;
        private String url;
        private Integer priority = 100;
        private Boolean enabled = true;
        private Boolean supportsThinking = false;
    }

    @Data
    public static class Selection {
        private Integer failureThreshold = 2;
        private Long openDurationMs = 30000L;
    }

    @Data
    public static class Stream {
        private Integer messageChunkSize = 5;
    }
}
