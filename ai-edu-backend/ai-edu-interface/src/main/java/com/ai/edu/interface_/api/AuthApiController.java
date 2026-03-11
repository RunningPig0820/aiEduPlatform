package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.LoginRequest;
import com.ai.edu.application.dto.RegisterRequest;
import com.ai.edu.application.dto.UserResponse;
import com.ai.edu.application.service.UserAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证API控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final UserAppService userAppService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = userAppService.register(request);
        return ApiResponse.success(user);
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        UserResponse user = userAppService.login(request);
        return ApiResponse.success(user);
    }

    @GetMapping("/user/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        UserResponse user = userAppService.getUserById(id);
        return ApiResponse.success(user);
    }
}