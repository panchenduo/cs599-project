package com.wut.shortlink.admin.common.constant;

public class RedisCacheConstant {
    /**
     * 用户注册
     */
    public static final String LOCK_USER_REGISTER_KEY = "short-link:lock:user_register:";
    /**
     * 用户登录缓存
     */
    public static final String USER_LOGIN_KEY = "short-link:login:";

    /**
     * 分组创建分布式锁
     */
    public static final String LOCK_GROUP_CREATE_KEY = "short-link:lock_group-create:%s";
}
