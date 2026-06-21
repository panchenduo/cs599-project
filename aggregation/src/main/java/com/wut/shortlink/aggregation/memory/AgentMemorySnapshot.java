package com.wut.shortlink.aggregation.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent 会话记忆快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMemorySnapshot {

    private String summary;

    private List<AgentMemoryMessage> recentMessages;
}
