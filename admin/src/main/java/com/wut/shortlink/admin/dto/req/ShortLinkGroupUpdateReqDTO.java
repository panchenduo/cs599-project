package com.wut.shortlink.admin.dto.req;

import lombok.Data;

@Data
public class ShortLinkGroupUpdateReqDTO {
    /**
     * 分组id
     */
    private String gid;
    /**
     * 分组名称
     */
    private String name;
}
