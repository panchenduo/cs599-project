package com.wut.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.dao.entity.UserDO;
import com.wut.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.wut.shortlink.admin.dto.req.UserUpdateReqDTO;
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
    /**
     * 添加用户
     * @param userRegisterReqDTO 用户注册信息
     */
    void register(UserRegisterReqDTO userRegisterReqDTO);
    /**
     * 修改用户信息
     * @param userUpdateReqDTO 用户信息
     */

    void update(UserUpdateReqDTO userUpdateReqDTO);
}
