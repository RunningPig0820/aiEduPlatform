package com.ai.edu.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发送验证码请求DTO
 */
@Data
public class SendCodeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 验证码场景
     * REGISTER: 注册
     * LOGIN: 登录
     * RESET_PASSWORD: 重置密码
     */
    @NotBlank(message = "验证码场景不能为空")
    private String scene;
}