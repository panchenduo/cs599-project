package com.wut.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.dao.entity.UserDO;
import com.wut.shortlink.admin.dto.resp.UserRespDTO;

/**
 * 用户接口层
 */
public interface UserService extends IService<UserDO>{
    Result<UserRespDTO> getUserByUsername(String username);
}
