package com.wut.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wut.shortlink.admin.common.constant.RedisCacheConstant;
import com.wut.shortlink.admin.common.convention.exception.ClientException;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.common.convention.result.Results;
import com.wut.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.wut.shortlink.admin.dao.entity.UserDO;
import com.wut.shortlink.admin.dao.mapper.UserMapper;
import com.wut.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.wut.shortlink.admin.dto.resp.UserRespDTO;
import com.wut.shortlink.admin.service.UserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 用户接口实现层
 */
@RequiredArgsConstructor
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    @Resource
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    @Resource
    private final RedissonClient redissonClient;

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
        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO userRegisterReqDTO) {
        if (hasUsername(userRegisterReqDTO.getUsername())) {
            throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(RedisCacheConstant.LOCK_USER_REGISTER_KEY+userRegisterReqDTO.getUsername());
        try {
            if (lock.tryLock()) {
                int inserted = baseMapper.insert(BeanUtil.toBean(userRegisterReqDTO, UserDO.class));
                if (inserted < 1) {
                    throw new ClientException(UserErrorCodeEnum.USER_SAVE_ERROR);
                }
                userRegisterCachePenetrationBloomFilter.add(userRegisterReqDTO.getUsername());
                return;
            }
            throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        } finally {
            lock.unlock();
        }

    }
}
