package com.wut.shortlink.aggregation.service;

import com.wut.shortlink.aggregation.dto.req.ShortLinkAgentChatReqDTO;
import com.wut.shortlink.aggregation.dto.resp.ShortLinkAgentChatRespDTO;

/**
 * 智能短链 Agent 服务
 */
public interface ShortLinkAgentService {

    /**
     * 执行一次智能短链 Agent 对话
     *
     * @param requestParam 对话请求参数
     * @return Agent 回复
     */
    ShortLinkAgentChatRespDTO chat(ShortLinkAgentChatReqDTO requestParam);
}
