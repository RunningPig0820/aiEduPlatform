package com.ai.edu.application.dto.org.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 关联用户与学校请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssociateUserWithSchoolCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须为正数")
    private Long userId;

    /**
     * 用户在学校中的角色: ADMIN, TEACHER, STUDENT, PARENT
     */
    @NotNull(message = "角色不能为空")
    private String role;
}