package com.ai.edu.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 演示登录请求DTO
 */
@Data
public class DemoLoginRequest {

    /**
     * 角色：STUDENT, TEACHER, PARENT
     */
    @NotBlank(message = "角色不能为空")
    private String role;
}