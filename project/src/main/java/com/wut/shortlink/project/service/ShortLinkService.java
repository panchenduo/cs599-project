package com.wut.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wut.shortlink.project.dao.entity.LinkDO;
import com.wut.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.wut.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.wut.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.wut.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.wut.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.List;

/**
 * 短链接服务接口层
 */
public interface ShortLinkService extends IService<LinkDO> {
    /**
     * 创建短链接
     *
     * @param reqDTO 创建短链接请求参数
     * @return 创建短链接结果
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO reqDTO);

    /**
     * 分页查询短链接
     *
     * @param shortLinkPageReqDTO 分页查询参数
     * @return 分页查询结果
     */
    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO shortLinkPageReqDTO);

    /**
     * 查询短链接分组内数量
     *
     * @param requestParam 查询参数
     * @return 短链接分组内数量
     */
    List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam);

    /**
     * 修改短链接
     *
     * @param requestParam 修改短链接请求参数
     */
    void updateShortLink(ShortLinkUpdateReqDTO requestParam);

    /**
     * 短链接跳转长链接
     *
     * @param shortUri 短链接
     * @param request  请求对象
     * @param response 响应对象
     */
    void restoreUrl(String shortUri, ServletRequest request, ServletResponse response);

    /**
     * 批量创建短链接
     *
     * @param requestParam 批量创建短链接请求参数
     * @return 批量创建短链接返回参数
     */
    ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam);

    /**
     * 短链接统计
     *
     * @param fullShortUrl         完整短链接
     * @param gid                  分组标识
     * @param shortLinkStatsRecord 短链接统计实体参数
     */
    void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO shortLinkStatsRecord);
}
