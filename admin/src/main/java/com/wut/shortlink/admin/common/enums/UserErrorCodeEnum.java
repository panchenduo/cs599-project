package com.wut.shortlink.admin.common.enums;

import com.wut.shortlink.admin.common.convention.errorcode.IErrorCode;

public enum UserErrorCodeEnum implements IErrorCode {
    USER_NOT_EXIST("A000101", "用户不存在"),
    USER_NAME_EXIST("A000102", "用户名已存在"),
    USER_EXIST("A000103", "用户已存在"),
    USER_SAVE_ERROR("A000104", "用户保存失败"),
    USER_UPDATE_ERROR("A000103", "用户更新失败");

    private final String code;

    private final String message;

    UserErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
