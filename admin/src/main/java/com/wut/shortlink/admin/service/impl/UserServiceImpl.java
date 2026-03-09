package com.wut.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wut.shortlink.admin.common.biz.user.UserContext;
import com.wut.shortlink.admin.common.constant.RedisCacheConstant;
import com.wut.shortlink.admin.common.convention.exception.ClientException;
import com.wut.shortlink.admin.common.convention.exception.ServiceException;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.common.convention.result.Results;
import com.wut.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.wut.shortlink.admin.dao.entity.UserDO;
import com.wut.shortlink.admin.dao.mapper.UserMapper;
import com.wut.shortlink.admin.dto.req.UserLoginReqDTO;
import com.wut.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.wut.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.wut.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.wut.shortlink.admin.dto.resp.UserRespDTO;
import com.wut.shortlink.admin.service.GroupService;
import com.wut.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.wut.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.wut.shortlink.admin.common.enums.UserErrorCodeEnum.USER_EXIST;

/**
 * 用户接口实现层
 */
@RequiredArgsConstructor
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupService groupService;

    @Override
    public Result<UserRespDTO> getUserByUsername(String username) {
        UserDO userDO = baseMapper.selectOne(new QueryWrapper<UserDO>().eq("username", username));
        if (userDO == null) {
            throw new ServiceException(UserErrorCodeEnum.USER_NOT_EXIST);
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
        if (!hasUsername(userRegisterReqDTO.getUsername())) {
            throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(RedisCacheConstant.LOCK_USER_REGISTER_KEY + userRegisterReqDTO.getUsername());
        if (!lock.tryLock()){
            throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        }
        try {
            int inserted = baseMapper.insert(BeanUtil.toBean(userRegisterReqDTO, UserDO.class));
            if (inserted < 1) {
                throw new ClientException(UserErrorCodeEnum.USER_SAVE_ERROR);
            }
            userRegisterCachePenetrationBloomFilter.add(userRegisterReqDTO.getUsername());
            groupService.saveGroup(userRegisterReqDTO.getUsername(),"默认分组");
        }catch (DuplicateKeyException e){
            throw new ClientException(USER_EXIST);
        }
        finally {
            lock.unlock();
        }

    }

    @Override
    public void update(UserUpdateReqDTO userUpdateReqDTO) {
        if (!Objects.equals(userUpdateReqDTO.getUsername(), UserContext.getUsername())) {
            throw new ClientException("当前登录用户修改请求异常");
        }
        LambdaUpdateWrapper<UserDO> lambdaUpdateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, userUpdateReqDTO.getUsername());
        int updated = baseMapper.update(BeanUtil.toBean(userUpdateReqDTO, UserDO.class), lambdaUpdateWrapper);
        if (updated < 1) {
            throw new ClientException(UserErrorCodeEnum.USER_UPDATE_ERROR);
        }
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO userLoginReqDTO) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, userLoginReqDTO.getUsername())
                .eq(UserDO::getPassword, userLoginReqDTO.getPassword())
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(wrapper);
        if (userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NOT_EXIST);
        }
        //判断用户是否已经登录过了
        /*Boolean isLogged = stringRedisTemplate.hasKey(USER_LOGIN_KEY + userLoginReqDTO.getUsername());
        if (isLogged!= null && isLogged) {
            throw new ClientException(UserErrorCodeEnum.USER_ALREADY_LOGIN);
        }*/
        //多端登录
        Map<Object ,Object> hasLoginMap = stringRedisTemplate.opsForHash().entries(USER_LOGIN_KEY + userLoginReqDTO.getUsername());
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            stringRedisTemplate.expire(USER_LOGIN_KEY + userLoginReqDTO.getUsername(), 30L, TimeUnit.MINUTES);
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("用户登录错误"));
            return new UserLoginRespDTO(token);
        }
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put(USER_LOGIN_KEY + userLoginReqDTO.getUsername(), uuid, JSONUtil.toJsonStr(userDO));
        stringRedisTemplate.expire(USER_LOGIN_KEY + userLoginReqDTO.getUsername(), 30, TimeUnit.MINUTES);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get(USER_LOGIN_KEY + username, token)!= null;
    }

    @Override
    public void logout(String username, String token) {
        if (checkLogin(username, token)){
            stringRedisTemplate.opsForHash().delete(USER_LOGIN_KEY + username, token);
            return;
        }
        throw new ClientException(UserErrorCodeEnum.USER_TOKEN_NOT_EXIST_OR_USER_NOT_LOGIN);
    }
}
