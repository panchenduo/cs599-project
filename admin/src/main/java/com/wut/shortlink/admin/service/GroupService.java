package com.wut.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wut.shortlink.admin.dao.entity.GroupDO;
import com.wut.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.wut.shortlink.admin.dto.resp.ShortLinkGroupSaveRespDTO;

import java.util.List;

/**
 * 短链接分组服务接口
 */
public interface GroupService extends IService<GroupDO> {
    /**
     * 新增短链接分组
     */
    void saveGroup(ShortLinkGroupSaveReqDTO shortLinkGroupSaveReqDTO);

    /**
     * 获取分组列表
     */
    List<ShortLinkGroupSaveRespDTO> groupList();
}
