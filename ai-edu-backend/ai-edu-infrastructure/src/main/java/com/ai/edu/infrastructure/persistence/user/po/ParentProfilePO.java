package com.ai.edu.infrastructure.persistence.user.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家长信息持久化对象
 */
@Data
@TableName("t_parent_profile")
public class ParentProfilePO {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_user_id")
    private Long studentUserId;

    @TableField("parent_user_id")
    private Long parentUserId;

    @TableField("relationship")
    private String relationship;

    @TableField("is_primary")
    private Boolean isPrimary;

    @TableField("created_by")
    private Long createdBy;

    @TableField("modified_by")
    private Long modifiedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Boolean deleted;
}
