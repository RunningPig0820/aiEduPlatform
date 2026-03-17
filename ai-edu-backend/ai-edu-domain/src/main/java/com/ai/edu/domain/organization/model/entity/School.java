package com.ai.edu.domain.organization.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 学校实体
 */
@TableName("t_school")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class School {

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

    @TableField("description")
    private String description;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static School create(String name, String code, String schoolType) {
        School school = new School();
        school.name = name;
        school.code = code;
        school.schoolType = schoolType;
        return school;
    }

    public void updateAddress(String province, String city, String district, String address) {
        this.province = province;
        this.city = city;
        this.district = district;
        this.address = address;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void delete() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }

    public void modify(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public boolean isPrimary() {
        return "PRIMARY".equals(schoolType);
    }

    public boolean isJuniorHigh() {
        return "JUNIOR_HIGH".equals(schoolType);
    }

    public boolean isHighSchool() {
        return "HIGH_SCHOOL".equals(schoolType);
    }
}