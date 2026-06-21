package com.wut.shortlink.aggregation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 智能短链 Agent 运行配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "short-link.agent")
public class ShortLinkAgentProperties {

    private Memory memory = new Memory();

    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Data
    public static class Memory {

        /**
         * 是否启用会话记忆压缩
         */
        private Boolean enabled = true;

        /**
         * 保留最近多少轮原始对话
         */
        private Integer historyKeepTurns = 4;

        /**
         * 超过多少轮开始压缩摘要
         */
        private Integer summaryStartTurns = 5;

        /**
         * 摘要最大长度
         */
        private Integer summaryMaxChars = 240;
    }

    @Data
    public static class CircuitBreaker {

        /**
         * 连续失败阈值
         */
        private Integer failureThreshold = 2;

        /**
         * 熔断打开时长
         */
        private Long openDurationMs = 30000L;
    }
}
