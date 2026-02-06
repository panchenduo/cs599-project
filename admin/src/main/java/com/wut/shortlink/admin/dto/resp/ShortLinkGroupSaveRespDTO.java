package com.wut.shortlink.admin.dto.resp;

import lombok.Data;

@Data
public class ShortLinkGroupSaveRespDTO {
    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组排序
     */
    private Integer sortOrder;
}
