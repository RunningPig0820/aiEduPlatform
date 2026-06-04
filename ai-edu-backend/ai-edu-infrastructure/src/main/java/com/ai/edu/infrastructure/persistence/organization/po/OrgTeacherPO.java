package com.ai.edu.infrastructure.persistence.organization.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教职工持久化对象
 * 教职工本质是用户与部门的关联关系
 */
@Data
@TableName("t_org_teacher")
public class OrgTeacherPO {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("school_id")
    private Long schoolId;

    @TableField("user_id")
    private Long userId;

    @TableField("department_id")
    private Long departmentId;

    @TableField("created_by")
    private Long createdBy;

    @TableField("modified_by")
    private Long modifiedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Boolean deleted = false;
}