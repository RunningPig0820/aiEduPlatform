package com.ai.edu.application.dto.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学校响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 学校ID
     */
    private Long id;

    /**
     * 学校名称
     */
    private String name;

    /**
     * 学校图标URL
     */
    private String iconUrl;

    /**
     * 学校性质类型: PUBLIC, PRIVATE, TRAINING_INSTITUTE
     */
    private String type;

    /**
     * 学校状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}