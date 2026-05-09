package com.ai.edu.infrastructure.persistence.organization.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校用户关联持久化对象
 */
@Data
@TableName("t_school_user")
public class SchoolUserPO {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("school_id")
    private Long schoolId;

    @TableField("user_id")
    private Long userId;

    @TableField("role_type")
    private String roleType;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    private Boolean deleted = false;
}