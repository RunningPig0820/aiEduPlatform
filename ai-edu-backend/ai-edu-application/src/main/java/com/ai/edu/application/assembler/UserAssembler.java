package com.ai.edu.application.assembler;

import com.ai.edu.application.dto.UserResponse;
import com.ai.edu.domain.user.model.entity.User;

/**
 * 用户组装器
 */
public final class UserAssembler {

    private UserAssembler() {}

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .build();
    }
}