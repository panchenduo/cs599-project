package com.wut.shortlink.project.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ValiDateTypeEnum {
    /**
     * 永久有效期
     */
    PERMANENT(0),
    /**
     * 用户自定义有效期
     */
    CUSTOM(1);
    @Getter
    private final int type;
}
