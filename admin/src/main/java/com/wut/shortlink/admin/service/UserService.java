package com.wut.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.dao.entity.UserDO;
import com.wut.shortlink.admin.dto.resp.UserRespDTO;

/**
 * 用户接口层
 */
public interface UserService extends IService<UserDO> {
    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    Result<UserRespDTO> getUserByUsername(String username);
    /**
     * 查询用户是否存在
     * @param username 用户名
     * @return 用户信息 存在返回true，不存在返回false
     */
    boolean hasUsername(String username);
}
