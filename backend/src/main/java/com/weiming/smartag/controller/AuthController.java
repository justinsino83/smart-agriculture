package com.weiming.smartag.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.User;
import com.weiming.smartag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 - 登录/登出/获取用户信息
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        try {
            String username = params.get("username");
            String password = params.get("password");

            if (username == null || username.trim().isEmpty()) {
                return Result.error(400, "用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return Result.error(400, "密码不能为空");
            }

            User user = userService.login(username, password);
            if (user == null) {
                return Result.error(401, "用户名或密码错误");
            }

            // 登录并创建Token
            StpUtil.login(user.getId());
            String token = StpUtil.getTokenValue();

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", buildUserInfo(user));

            log.info("用户登录成功, username: {}", username);
            return Result.success(data);
        } catch (Exception e) {
            log.error("用户登录失败", e);
            return Result.error(500, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        try {
            StpUtil.logout();
            log.info("用户登出成功");
            return Result.success("登出成功");
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return Result.error("登出失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo() {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            User user = userService.getById(userId);
            if (user == null) {
                return Result.error(404, "用户不存在");
            }
            return Result.success(buildUserInfo(user));
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 检查登录状态
     */
    @GetMapping("/check")
    public Result<Map<String, Object>> checkLogin() {
        try {
            boolean isLogin = StpUtil.isLogin();
            Map<String, Object> data = new HashMap<>();
            data.put("isLogin", isLogin);
            if (isLogin) {
                data.put("userId", StpUtil.getLoginId());
            }
            return Result.success(data);
        } catch (Exception e) {
            log.error("检查登录状态失败", e);
            return Result.error("检查登录状态失败: " + e.getMessage());
        }
    }

    /**
     * 构建用户信息（脱敏处理）
     */
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("phone", user.getPhone());
        info.put("email", user.getEmail());
        info.put("role", user.getRole());
        info.put("department", user.getDepartment());
        info.put("avatar", "");
        return info;
    }
}
