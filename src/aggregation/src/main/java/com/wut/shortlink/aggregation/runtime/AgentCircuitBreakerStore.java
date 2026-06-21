package com.wut.shortlink.aggregation.runtime;

import com.wut.shortlink.aggregation.config.ShortLinkAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent 候选模型/工具熔断状态存储。
 */
@Component
@RequiredArgsConstructor
public class AgentCircuitBreakerStore {

    private final ShortLinkAgentProperties properties;

    private final Map<String, Health> healthById = new ConcurrentHashMap<>();

    public boolean allowCall(String id) {
        if (id == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        AtomicBoolean allowed = new AtomicBoolean(false);
        healthById.compute(id, (key, value) -> {
            Health health = value == null ? new Health() : value;
            if (health.state == State.OPEN) {
                if (health.openUntil > now) {
                    return health;
                }
                health.state = State.HALF_OPEN;
                health.halfOpenInFlight = true;
                allowed.set(true);
                return health;
            }
            if (health.state == State.HALF_OPEN && health.halfOpenInFlight) {
                return health;
            }
            health.halfOpenInFlight = true;
            allowed.set(true);
            return health;
        });
        return allowed.get();
    }

    public void markSuccess(String id) {
        if (id == null) {
            return;
        }
        healthById.compute(id, (key, value) -> {
            Health health = value == null ? new Health() : value;
            health.consecutiveFailures = 0;
            health.openUntil = 0L;
            health.halfOpenInFlight = false;
            health.state = State.CLOSED;
            return health;
        });
    }

    public void markFailure(String id) {
        if (id == null) {
            return;
        }
        long now = System.currentTimeMillis();
        healthById.compute(id, (key, value) -> {
            Health health = value == null ? new Health() : value;
            if (health.state == State.HALF_OPEN) {
                open(health, now);
                return health;
            }
            health.consecutiveFailures++;
            if (health.consecutiveFailures >= properties.getCircuitBreaker().getFailureThreshold()) {
                open(health, now);
            } else {
                health.halfOpenInFlight = false;
            }
            return health;
        });
    }

    public String describe(String id) {
        Health health = healthById.get(id);
        if (health == null) {
            return "CLOSED";
        }
        return health.state.name();
    }

    private void open(Health health, long now) {
        health.state = State.OPEN;
        health.openUntil = now + properties.getCircuitBreaker().getOpenDurationMs();
        health.consecutiveFailures = 0;
        health.halfOpenInFlight = false;
    }

    private static class Health {

        private int consecutiveFailures;

        private long openUntil;

        private boolean halfOpenInFlight;

        private State state = State.CLOSED;
    }

    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
