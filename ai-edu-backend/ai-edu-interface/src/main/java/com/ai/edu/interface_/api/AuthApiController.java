package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.*;
import com.ai.edu.application.service.UserAppService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    @Resource
    private UserAppService userAppService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = userAppService.register(request);
        return ApiResponse.success(user);
    }

    /**
     * 用户登录（支持三种方式：用户名+密码、手机号+密码、手机号+验证码）
     */
    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        UserResponse user = userAppService.login(request);

        // 设置 Session
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());

        log.info("用户登录成功: username={}", user.getUsername());
        return ApiResponse.success(user);
    }

    /**
     * 演示账号快捷登录
     */
    @PostMapping("/demo-login")
    public ApiResponse<UserResponse> demoLogin(@Valid @RequestBody DemoLoginRequest request, HttpSession session) {
        UserResponse user = userAppService.demoLogin(request, session);
        return ApiResponse.success(user);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        userAppService.sendCode(request.getPhone(), request.getScene());
        return ApiResponse.success(null);
    }

    /**
     * 重置密码（忘记密码）
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userAppService.resetPassword(request);
        return ApiResponse.success(null);
    }

    /**
     * 修改密码（已登录状态）
     */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpSession session) {
        userAppService.changePassword(request, session);
        return ApiResponse.success(null);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        userAppService.logout(session);
        return ApiResponse.success(null);
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/current-user")
    public ApiResponse<UserResponse> getCurrentUser(HttpSession session) {
        UserResponse user = userAppService.getCurrentUser(session);
        return ApiResponse.success(user);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/user/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        UserResponse user = userAppService.getUserById(id);
        return ApiResponse.success(user);
    }
}