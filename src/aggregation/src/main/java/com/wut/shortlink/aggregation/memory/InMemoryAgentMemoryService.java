package com.wut.shortlink.aggregation.memory;

import com.wut.shortlink.aggregation.config.ShortLinkAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量会话记忆实现，参考 RAG 项目的“摘要 + 最近消息”模式。
 */
@Service
@RequiredArgsConstructor
public class InMemoryAgentMemoryService implements AgentMemoryService {

    private final ShortLinkAgentProperties properties;

    private final Map<String, ConversationMemory> memoryByConversation = new ConcurrentHashMap<>();

    @Override
    public AgentMemorySnapshot load(String conversationId, String userId) {
        ConversationMemory memory = memoryByConversation.get(buildKey(conversationId, userId));
        if (memory == null) {
            return AgentMemorySnapshot.builder()
                    .summary("")
                    .recentMessages(List.of())
                    .build();
        }
        synchronized (memory) {
            return AgentMemorySnapshot.builder()
                    .summary(memory.summary)
                    .recentMessages(new ArrayList<>(memory.messages))
                    .build();
        }
    }

    @Override
    public void append(String conversationId, String userId, String role, String content) {
        if (isBlank(conversationId) || isBlank(userId) || isBlank(content)) {
            return;
        }
        ConversationMemory memory = memoryByConversation.computeIfAbsent(buildKey(conversationId, userId), key -> new ConversationMemory());
        synchronized (memory) {
            memory.messages.add(AgentMemoryMessage.builder()
                    .role(role)
                    .content(content)
                    .createTime(System.currentTimeMillis())
                    .build());
            memory.messages.sort(Comparator.comparing(AgentMemoryMessage::getCreateTime));
            compressIfNeeded(memory);
        }
    }

    private void compressIfNeeded(ConversationMemory memory) {
        ShortLinkAgentProperties.Memory config = properties.getMemory();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        int keepMessages = Math.max(config.getHistoryKeepTurns(), 1) * 2;
        int triggerMessages = Math.max(config.getSummaryStartTurns(), config.getHistoryKeepTurns() + 1) * 2;
        if (memory.messages.size() < triggerMessages) {
            return;
        }
        int compressEnd = Math.max(memory.messages.size() - keepMessages, 0);
        if (compressEnd <= 0) {
            return;
        }
        List<AgentMemoryMessage> toCompress = new ArrayList<>(memory.messages.subList(0, compressEnd));
        String merged = mergeSummary(memory.summary, toCompress, config.getSummaryMaxChars());
        memory.summary = merged;
        memory.messages = new ArrayList<>(memory.messages.subList(compressEnd, memory.messages.size()));
    }

    private String mergeSummary(String oldSummary, List<AgentMemoryMessage> messages, int maxChars) {
        StringBuilder builder = new StringBuilder();
        if (!isBlank(oldSummary)) {
            builder.append(oldSummary.trim()).append("；");
        }
        for (AgentMemoryMessage message : messages) {
            String role = "user".equals(message.getRole()) ? "用户" : "助手";
            builder.append(role).append("：").append(message.getContent().replaceAll("\\s+", " ").trim()).append("；");
        }
        String result = builder.toString();
        int limit = Math.max(maxChars, 80);
        if (result.length() <= limit) {
            return result;
        }
        return result.substring(Math.max(result.length() - limit, 0));
    }

    private String buildKey(String conversationId, String userId) {
        return userId + ":" + conversationId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class ConversationMemory {

        private String summary = "";

        private List<AgentMemoryMessage> messages = new ArrayList<>();
    }
}
