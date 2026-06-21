package com.wut.shortlink.aggregation.dto.req;

import lombok.Data;

/**
 * 智能短链 Agent 对话请求参数
 */
@Data
public class ShortLinkAgentChatReqDTO {

    /**
     * 用户自然语言指令
     */
    private String message;

    /**
     * 当前分组标识，前端可传入当前选中的分组
     */
    private String gid;

    /**
     * 统计开始日期，格式 yyyy-MM-dd
     */
    private String startDate;

    /**
     * 统计结束日期，格式 yyyy-MM-dd
     */
    private String endDate;

    /**
     * 显式传入的原始链接
     */
    private String originUrl;

    /**
     * 显式传入的短链描述
     */
    private String describe;

    /**
     * 有效天数，为空时按永久有效处理
     */
    private Integer validDays;
}
