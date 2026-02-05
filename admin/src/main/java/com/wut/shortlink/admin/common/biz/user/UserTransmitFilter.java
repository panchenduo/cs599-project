//package com.wut.shortlink.admin.common.biz.user;
//
//import com.alibaba.fastjson2.JSON;
//import com.wut.shortlink.admin.common.convention.exception.ClientException;
//import jakarta.annotation.Resource;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.StringRedisTemplate;
//
//import java.io.IOException;
//
//import static com.wut.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
//import static com.wut.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NOT_AUTHORIZED;
//
///**
// * 用户信息传输过滤器
// */
//@RequiredArgsConstructor
//public class UserTransmitFilter implements Filter {
//    @Resource
//    private final StringRedisTemplate stringRedisTemplate;
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
//        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
//        String requestURI = ((HttpServletRequest) servletRequest).getRequestURI();
//        if (!requestURI.equals("/api/short-link/admin/v1/user/login")) { //不是登录的话进行拦截
//            String username = httpServletRequest.getHeader("username");
//            String token = httpServletRequest.getHeader("token");
//            Object userInfoStr = stringRedisTemplate.opsForHash().get(USER_LOGIN_KEY + username, token);
//            if (userInfoStr != null) {
//                UserInfoDTO userInfoDTO = JSON.parseObject(userInfoStr.toString(), UserInfoDTO.class);
//                UserContext.setUser(userInfoDTO);
//            } else {
//                throw new ClientException(USER_NOT_AUTHORIZED);
//            }
//        }
//        try {
//            filterChain.doFilter(servletRequest, servletResponse);
//        } finally {
//            UserContext.removeUser();
//        }
//    }
//}
package com.wut.shortlink.admin.common.biz.user;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static com.wut.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.wut.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NOT_AUTHORIZED;

/**
 * 用户信息传输过滤器
 */
@RequiredArgsConstructor
@Slf4j
public class UserTransmitFilter implements Filter {
    @Resource
    private final StringRedisTemplate stringRedisTemplate;
    private static final List<String> IGNOREURLS = Lists.newArrayList("/api/short-link/admin/v1/user/has-username", "/api/short-link/admin/v1/user/login");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = ((HttpServletRequest) servletRequest).getRequestURI();
        if (!IGNOREURLS.contains(requestURI)) { //不是登录的话进行拦截
            String method = httpServletRequest.getMethod();
            if (!requestURI.equals("/api/short-link/admin/v1/user") && method.equals("POST")) {
                String username = httpServletRequest.getHeader("username");
                String token = httpServletRequest.getHeader("token");
                if (!StrUtil.isAllBlank(username, token)) {
//                    throw new ClientException(USER_NOT_AUTHORIZED);
                    returnJson(servletResponse, JSON.toJSONString(USER_NOT_AUTHORIZED));
                    return;
                }
                Object userInfoStr;
                try {
                    userInfoStr = stringRedisTemplate.opsForHash().get(USER_LOGIN_KEY + username, token);
                    if (userInfoStr == null) {
//                        throw new ClientException(USER_NOT_AUTHORIZED);
                        returnJson(servletResponse, JSON.toJSONString(USER_NOT_AUTHORIZED));
                        return;
                    }
                } catch (Exception e) {
//                    throw new ClientException(USER_NOT_AUTHORIZED);
                    returnJson(servletResponse, JSON.toJSONString(USER_NOT_AUTHORIZED));
                    return;
                }
                UserInfoDTO userInfoDTO = JSON.parseObject(userInfoStr.toString(), UserInfoDTO.class);
                UserContext.setUser(userInfoDTO);
            }
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }

    private void returnJson(ServletResponse response, String json) {
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        try {
            writer = response.getWriter();
            writer.print(json);

        } catch (IOException e) {
            log.error("response error", e);
        } finally {
            if (writer != null)
                writer.close();
        }
    }

}