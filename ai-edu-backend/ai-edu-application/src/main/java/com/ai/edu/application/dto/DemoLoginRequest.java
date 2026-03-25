package com.ai.edu.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 演示登录请求DTO
 */
@Data
public class DemoLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色：STUDENT, TEACHER, PARENT
     */
    @NotBlank(message = "角色不能为空")
    private String role;
}