package com.ai.edu.application.dto.org;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

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
     * 学校类型: KINDERGARTEN, PRIMARY, JUNIOR, SENIOR, COMPREHENSIVE
     */
    private String type;

    /**
     * 包含学段: KINDERGARTEN, PRIMARY, JUNIOR, SENIOR
     */
    private List<String> stages;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区/县
     */
    private String district;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 学校描述
     */
    private String description;

    /**
     * 学校状态
     */
    private String status;

    /**
     * 学校状态
     */
    private String statusValue;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}