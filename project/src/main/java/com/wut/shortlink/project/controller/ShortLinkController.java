package com.wut.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wut.shortlink.project.common.convention.result.Result;
import com.wut.shortlink.project.common.convention.result.Results;
import com.wut.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.wut.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.wut.shortlink.project.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {
    private final ShortLinkService shortLinkService;

    @PostMapping("/api/short-link/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO reqDTO) {
        return Results.success(shortLinkService.createShortLink(reqDTO));
    }

    @GetMapping("/api/short-link/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO shortLinkPageReqDTO) {
        return Results.success(shortLinkService.pageShortLink(shortLinkPageReqDTO));
    }
}
