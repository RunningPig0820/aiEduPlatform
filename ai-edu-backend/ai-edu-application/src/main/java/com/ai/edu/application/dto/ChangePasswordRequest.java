package com.ai.edu.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改密码请求DTO（已登录状态）
 */
@Data
public class ChangePasswordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 原密码
     */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    private String newPassword;
}