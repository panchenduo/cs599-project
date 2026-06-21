package com.wut.shortlink.aggregation.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 会话记忆消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMemoryMessage {

    private String role;

    private String content;

    private Long createTime;
}
