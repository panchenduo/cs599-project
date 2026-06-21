package com.wut.shortlink.aggregation.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Agent 调度执行器，支持候选模型/策略失败后的 fallback。
 */
@Component
@RequiredArgsConstructor
public class AgentRoutingExecutor {

    private final AgentCircuitBreakerStore circuitBreakerStore;

    public <T> RoutedResult<T> executeWithFallback(List<String> candidates, Function<String, T> caller) {
        RuntimeException last = null;
        for (String candidate : candidates) {
            if (!circuitBreakerStore.allowCall(candidate)) {
                continue;
            }
            try {
                T result = caller.apply(candidate);
                circuitBreakerStore.markSuccess(candidate);
                return new RoutedResult<>(candidate, "使用候选策略：" + candidate, result);
            } catch (RuntimeException ex) {
                last = ex;
                circuitBreakerStore.markFailure(candidate);
            }
        }
        throw last == null ? new IllegalStateException("没有可用的 Agent 调度候选") : last;
    }

    public String health(String candidate) {
        return circuitBreakerStore.describe(candidate);
    }

    public record RoutedResult<T>(String candidate, String status, T result) {
    }
}
