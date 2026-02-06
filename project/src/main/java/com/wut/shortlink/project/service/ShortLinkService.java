package com.wut.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wut.shortlink.project.dao.entity.LinkDO;
import com.wut.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.wut.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkPageRespDTO;

/**
 * 短链接服务接口层
 */
public interface ShortLinkService extends IService<LinkDO> {
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO reqDTO);

    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO shortLinkPageReqDTO);
}
