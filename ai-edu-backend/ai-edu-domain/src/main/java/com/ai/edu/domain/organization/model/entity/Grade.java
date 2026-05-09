package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.organization.model.valueobject.GradeLevel;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import lombok.Getter;

/**
 * 年级实体
 */
@Getter
public class Grade {

    private Long id;
    private SchoolId schoolId;
    private String name;
    private String code;
    private GradeLevel gradeLevel;
    private String description;
    private Long createdBy;
    private Long modifiedBy;
    private boolean deleted;

    protected Grade() {}

    public static Grade create(String name, GradeLevel gradeLevel) {
        Grade grade = new Grade();
        grade.name = name;
        grade.gradeLevel = gradeLevel;
        grade.createdBy = 0L;
        grade.modifiedBy = 0L;
        grade.deleted = false;
        return grade;
    }

    public static Grade createWithSchool(String name, GradeLevel gradeLevel, SchoolId schoolId) {
        Grade grade = create(name, gradeLevel);
        grade.schoolId = schoolId;
        return grade;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
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
        return gradeLevel != null && gradeLevel.isPrimary();
    }

    public boolean isJuniorHigh() {
        return gradeLevel != null && gradeLevel.isJuniorHigh();
    }

    public boolean isHighSchool() {
        return gradeLevel != null && gradeLevel.isHighSchool();
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Integer getGradeLevelValue() {
        return gradeLevel != null ? gradeLevel.getValue() : null;
    }

    public Long getSchoolIdValue() {
        return schoolId != null ? schoolId.getValue() : null;
    }
}