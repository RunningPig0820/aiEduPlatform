package com.ai.edu.domain.organization.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 班级实体
 */
@TableName("t_class")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Class {

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
    private String status = "ACTIVE";

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static Class create(String name, String grade, String schoolYear) {
        Class cls = new Class();
        cls.name = name;
        cls.grade = grade;
        cls.schoolYear = schoolYear;
        return cls;
    }

    public static Class createWithSchool(String name, String grade, String schoolYear, Long schoolId) {
        Class cls = create(name, grade, schoolYear);
        cls.schoolId = schoolId;
        return cls;
    }

    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void assignSchool(Long schoolId) {
        this.schoolId = schoolId;
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public void graduate() {
        this.status = "GRADUATED";
    }

    public void archive() {
        this.status = "ARCHIVED";
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

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isGraduated() {
        return "GRADUATED".equals(status);
    }

    public boolean isArchived() {
        return "ARCHIVED".equals(status);
    }
}