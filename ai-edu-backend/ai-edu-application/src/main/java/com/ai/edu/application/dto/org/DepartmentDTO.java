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
 * 部门响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private Long id;

    /**
     * 学校ID
     */
    private Long schoolId;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 上级部门ID
     */
    private Long parentId;

    /**
     * 上级部门名称
     */
    private String parentName;

    /**
     * 部门路径
     */
    private String departmentPath;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 部门描述
     */
    private String description;

    /**
     * 是否为根部门
     */
    private Boolean isRoot;

    /**
     * 子部门列表（用于树形结构）
     */
    private List<DepartmentDTO> children;

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