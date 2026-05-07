package com.ai.edu.application.dto.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户学校关联响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSchoolAssociationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    private Long id;

    /**
     * 学校ID
     */
    private Long schoolId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色
     */
    private String role;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}