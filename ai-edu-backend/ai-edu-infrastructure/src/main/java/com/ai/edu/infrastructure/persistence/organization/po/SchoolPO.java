package com.ai.edu.infrastructure.persistence.organization.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校持久化对象
 */
@Data
@TableName("t_school")
public class SchoolPO {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("district")
    private String district;

    @TableField("address")
    private String address;

    @TableField("school_type")
    private String schoolType;

    @TableField("icon_url")
    private String iconUrl;

    //删除
    @TableField("institutional_type")
    private String institutionalType;

    //删除
    @TableField("stages")
    private String stages;

    @TableField("status")
    private String status;

    @TableField("description")
    private String description;

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