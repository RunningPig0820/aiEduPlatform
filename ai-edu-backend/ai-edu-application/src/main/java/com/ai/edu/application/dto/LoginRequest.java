package com.ai.edu.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录请求DTO
 *
 * 支持三种登录方式：
 * 1. 用户名+密码：loginType=USERNAME_PASSWORD, username + password
 * 2. 手机号+密码：loginType=PHONE_PASSWORD, phone + password
 * 3. 手机号+验证码：loginType=PHONE_CODE, phone + code
 */
@Data
public class LoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录类型
     * USERNAME_PASSWORD: 用户名+密码
     * PHONE_PASSWORD: 手机号+密码
     * PHONE_CODE: 手机号+验证码
     */
    @NotBlank(message = "登录类型不能为空")
    private String loginType;

    /**
     * 用户名（loginType=USERNAME_PASSWORD 时必填）
     */
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    private String username;

    /**
     * 手机号（loginType=PHONE_PASSWORD 或 PHONE_CODE 时必填）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 密码（loginType=USERNAME_PASSWORD 或 PHONE_PASSWORD 时必填）
     */
    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    private String password;

    /**
     * 验证码（loginType=PHONE_CODE 时必填）
     */
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
    private String code;
}