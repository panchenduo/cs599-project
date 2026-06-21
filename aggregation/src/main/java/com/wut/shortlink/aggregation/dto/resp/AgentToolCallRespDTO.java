package com.wut.shortlink.aggregation.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 工具调用记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolCallRespDTO {

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具入参摘要
     */
    private Object request;

    /**
     * 工具返回摘要
     */
    private Object response;

    /**
     * 是否调用成功
     */
    private Boolean success;

    /**
     * 调用说明
     */
    private String message;
}
