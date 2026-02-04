package com.wut.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wut.shortlink.admin.dao.entity.GroupDO;
import com.wut.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.wut.shortlink.admin.dto.req.ShortLinkGroupSortDTO;
import com.wut.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
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

    /**
     * 修改短链接分组
     */
    void updateGroup(ShortLinkGroupUpdateReqDTO shortLinkGroupUpdateReqDTO);

    /**
     * 删除短链接分组
     */
    void deleteGroup(String gid);

    /**
     * 排序短链接分组
     */
    void sortGroup(List<ShortLinkGroupSortDTO> shortLinkGroupSortDTO);
}
