package com.wut.shortlink.aggregation.memory;

/**
 * Agent 会话记忆服务。
 */
public interface AgentMemoryService {

    AgentMemorySnapshot load(String conversationId, String userId);

    void append(String conversationId, String userId, String role, String content);
}
