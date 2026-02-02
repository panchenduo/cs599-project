package com.wut.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wut.shortlink.admin.common.convention.exception.ClientException;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.common.convention.result.Results;
import com.wut.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.wut.shortlink.admin.dao.entity.UserDO;
import com.wut.shortlink.admin.dao.mapper.UserMapper;
import com.wut.shortlink.admin.dto.resp.UserRespDTO;
import com.wut.shortlink.admin.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 用户接口实现层
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    @Override
    public Result<UserRespDTO> getUserByUsername(String username) {
        UserDO userDO = baseMapper.selectOne(new QueryWrapper<UserDO>().eq("username", username));
        if (userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NOT_EXIST);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        return Results.success(result);
    }

    @Override
    public boolean hasUsername(String username) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(wrapper);
        return userDO == null;
    }
}
