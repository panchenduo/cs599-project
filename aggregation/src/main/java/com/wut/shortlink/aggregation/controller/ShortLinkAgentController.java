package com.wut.shortlink.aggregation.controller;

import com.wut.shortlink.aggregation.dto.req.ShortLinkAgentChatReqDTO;
import com.wut.shortlink.aggregation.dto.resp.ShortLinkAgentChatRespDTO;
import com.wut.shortlink.aggregation.service.ShortLinkAgentService;
import com.wut.shortlink.project.common.convention.result.Result;
import com.wut.shortlink.project.common.convention.result.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能短链 Agent 控制层
 */
@RestController
@RequiredArgsConstructor
public class ShortLinkAgentController {

    private final ShortLinkAgentService shortLinkAgentService;

    /**
     * 智能短链 Agent 对话
     */
    @PostMapping("/api/short-link/admin/v1/agent/chat")
    public Result<ShortLinkAgentChatRespDTO> chat(@RequestBody ShortLinkAgentChatReqDTO requestParam) {
        return Results.success(shortLinkAgentService.chat(requestParam));
    }
}
