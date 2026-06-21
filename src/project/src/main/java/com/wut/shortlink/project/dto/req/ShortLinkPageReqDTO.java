package com.wut.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wut.shortlink.project.dao.entity.LinkDO;
import lombok.Data;

@Data
public class ShortLinkPageReqDTO extends Page<LinkDO> {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 排序标识
     */
    private String orderTag;
}
