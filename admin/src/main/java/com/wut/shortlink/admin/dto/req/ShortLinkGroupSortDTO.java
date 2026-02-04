package com.wut.shortlink.admin.dto.req;

import lombok.Data;

@Data
public class ShortLinkGroupSortDTO {
    /**
     * 分组id
     */
    private String gid;
    /**
     * 排序顺序
     */
    private Integer sortOrder;
}
