package com.ai.edu.domain.organization.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 年级实体
 */
@TableName("t_grade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("school_id")
    private Long schoolId;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("grade_level")
    private Integer gradeLevel;

    @TableField("description")
    private String description;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static Grade create(String name, Integer gradeLevel) {
        Grade grade = new Grade();
        grade.name = name;
        grade.gradeLevel = gradeLevel;
        return grade;
    }

    public static Grade createWithSchool(String name, Integer gradeLevel, Long schoolId) {
        Grade grade = create(name, gradeLevel);
        grade.schoolId = schoolId;
        return grade;
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

    public boolean isPrimarySchool() {
        return gradeLevel != null && gradeLevel >= 1 && gradeLevel <= 6;
    }

    public boolean isJuniorHigh() {
        return gradeLevel != null && gradeLevel >= 7 && gradeLevel <= 9;
    }

    public boolean isHighSchool() {
        return gradeLevel != null && gradeLevel >= 10 && gradeLevel <= 12;
    }
}