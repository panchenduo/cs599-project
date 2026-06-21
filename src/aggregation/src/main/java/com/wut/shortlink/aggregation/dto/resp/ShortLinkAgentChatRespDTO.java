package com.wut.shortlink.aggregation.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能短链 Agent 对话响应参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkAgentChatRespDTO {

    /**
     * 会话标识
     */
    private String conversationId;

    /**
     * 本次识别出的意图
     */
    private String intent;

    /**
     * Agent 面向用户的自然语言回复
     */
    private String answer;

    /**
     * 压缩后的对话记忆摘要
     */
    private String memorySummary;

    /**
     * Agent 调度状态，例如使用的候选模型和降级原因
     */
    private String dispatchStatus;

    /**
     * 建议用户继续追问或执行的动作
     */
    private List<String> suggestions;

    /**
     * 本次 Agent 使用的系统工具
     */
    private List<AgentToolCallRespDTO> toolCalls;
}
