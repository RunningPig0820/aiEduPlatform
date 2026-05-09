package com.ai.edu.infrastructure.persistence.organization.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级持久化对象
 */
@Data
@TableName("t_class")
public class ClassPO {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("school_id")
    private Long schoolId;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("grade")
    private String grade;

    @TableField("school_year")
    private String schoolYear;

    @TableField("class_type")
    private String classType;

    @TableField("description")
    private String description;

    @TableField("status")
    private String status;

    @TableField("created_by")
    private Long createdBy;

    @TableField("modified_by")
    private Long modifiedBy;

    @TableField("is_deleted")
    private Boolean deleted = false;
}