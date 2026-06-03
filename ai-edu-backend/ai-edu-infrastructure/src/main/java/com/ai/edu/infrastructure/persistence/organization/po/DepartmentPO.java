package com.ai.edu.infrastructure.persistence.organization.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门持久化对象
 */
@Data
@TableName("t_department")
public class DepartmentPO {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("school_id")
    private Long schoolId;

    @TableField("name")
    private String name;

    @TableField("parent_id")
    private Long parentId;

    @TableField("department_path")
    private String departmentPath;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Boolean deleted = false;
}