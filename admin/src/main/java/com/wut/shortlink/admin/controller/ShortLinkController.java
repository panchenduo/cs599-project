package com.wut.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.common.convention.result.Results;
import com.wut.shortlink.admin.remote.ShortLinkRemoteService;
import com.wut.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.wut.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.wut.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.wut.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.wut.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.springframework.web.bind.annotation.*;

@RestController
public class ShortLinkController {
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };
    /**
     * 创建短链接
     *
     * @param reqDTO
     * @return 创建结果
     */
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO reqDTO) {
        return shortLinkRemoteService.createShortLink(reqDTO);
    }

    /**
     * 分页查询短链接
     *
     * @param shortLinkPageReqDTO
     * @return 查询分页结果
     */
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO shortLinkPageReqDTO) {
        return shortLinkRemoteService.pageShortLink(shortLinkPageReqDTO);
    }
    /**
     * 修改短链接
     */
    @PostMapping("/api/short-link/admin/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        shortLinkRemoteService.updateShortLink(requestParam);
        return Results.success();
    }


}
