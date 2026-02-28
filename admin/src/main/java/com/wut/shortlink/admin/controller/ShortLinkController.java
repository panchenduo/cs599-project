package com.wut.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.common.convention.result.Results;
import com.wut.shortlink.admin.remote.ShortLinkRemoteService;
import com.wut.shortlink.admin.remote.dto.req.ShortLinkBatchCreateReqDTO;
import com.wut.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.wut.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.wut.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import com.wut.shortlink.admin.remote.dto.resp.ShortLinkBaseInfoRespDTO;
import com.wut.shortlink.admin.remote.dto.resp.ShortLinkBatchCreateRespDTO;
import com.wut.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.wut.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.wut.shortlink.admin.toolkit.EasyExcelWebUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    /**
     * 批量创建短链接
     */
    @SneakyThrows
    @PostMapping("/api/short-link/admin/v1/create/batch")
    public void batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam, HttpServletResponse response) {
        Result<ShortLinkBatchCreateRespDTO> shortLinkBatchCreateRespDTOResult = shortLinkRemoteService.batchCreateShortLink(requestParam);
        if (shortLinkBatchCreateRespDTOResult.isSuccess()) {
            List<ShortLinkBaseInfoRespDTO> baseLinkInfos = shortLinkBatchCreateRespDTOResult.getData().getBaseLinkInfos();
            EasyExcelWebUtil.write(response, "批量创建短链接-SaaS短链接系统", ShortLinkBaseInfoRespDTO.class, baseLinkInfos);
        }
    }

}
