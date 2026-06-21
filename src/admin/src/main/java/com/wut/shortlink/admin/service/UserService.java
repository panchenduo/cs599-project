package com.wut.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.dao.entity.UserDO;
import com.wut.shortlink.admin.dto.req.UserLoginReqDTO;
import com.wut.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.wut.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.wut.shortlink.admin.dto.resp.UserLoginRespDTO;
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
     *
     * @param username 用户名
     * @return 用户信息 存在返回true，不存在返回false
     */
    boolean hasUsername(String username);

    /**
     * 添加用户
     *
     * @param userRegisterReqDTO 用户注册信息
     */
    void register(UserRegisterReqDTO userRegisterReqDTO);

    /**
     * 修改用户信息
     *
     * @param userUpdateReqDTO 用户信息
     */
    void update(UserUpdateReqDTO userUpdateReqDTO);

    /**
     * 用户登录
     *
     * @param userLoginReqDTO 用户登录信息
     * @return Token
     */

    UserLoginRespDTO login(UserLoginReqDTO userLoginReqDTO);

    /**
     * 检查用户登录状态
     *
     * @param username 用户名
     * @param token    Token
     * @return True: 用户已经登录 False: 用户没有登录
     */

    Boolean checkLogin(String username, String token);

    /**
     * 用户登出
     *
     * @param username 用户名
     * @param token    Token
     */
    void logout(String username, String token);
}
