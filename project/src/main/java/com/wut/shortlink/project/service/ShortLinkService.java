package com.wut.shortlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wut.shortlink.project.dao.entity.LinkDO;
import com.wut.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkCreateRespDTO;

/**
 * 短链接服务接口层
 */
public interface ShortLinkService extends IService<LinkDO> {
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO reqDTO);
}
