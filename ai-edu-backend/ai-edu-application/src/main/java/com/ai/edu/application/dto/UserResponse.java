package com.ai.edu.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String role;
    private Boolean enabled;
}