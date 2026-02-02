package com.wut.shortlink.admin.controller;

import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.dto.resp.UserRespDTO;
import com.wut.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * 用户控制器
 */
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    /**
     * 根据用户名查询用户
     */
    @RequestMapping("/api/short-link/admin/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        return userService.getUserByUsername(username);
    }
}
