package com.ai.edu.application.dto.org;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教职工响应DTO（聚合返回完整信息）
 * 包含关联关系(userId, departmentId) + 用户基本信息(name, phone)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgTeacherDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 教职工关联关系ID
     */
    private Long id;

    /**
     * 用户ID（用户域）
     */
    private Long userId;

    /**
     * 所属学校ID
     */
    private Long schoolId;

    /**
     * 所属行政部门ID
     */
    private Long departmentId;

    /**
     * 所属行政部门名称
     */
    private String departmentName;

    /**
     * 用户姓名（来自用户域）
     */
    private String name;

    /**
     * 用户手机号（来自用户域）
     */
    private String phone;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 最后修改人ID
     */
    private Long modifiedBy;

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