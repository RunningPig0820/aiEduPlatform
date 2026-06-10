package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.organization.model.valueobject.enums.ClassStatus;
import com.ai.edu.domain.organization.model.valueobject.enums.ClassType;
import com.ai.edu.domain.organization.model.valueobject.GradeLevel;
import com.ai.edu.domain.organization.model.valueobject.SchoolYear;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import lombok.Getter;

/**
 * 班级实体
 */
@Getter
public class Class {

    private ClassId id;
    private SchoolId schoolId;
    private String name;
    private String code;
    private GradeLevel grade;
    private SchoolYear schoolYear;
    private ClassType classType;
    private ClassStatus status;
    private String description;
    private Long createdBy;
    private Long modifiedBy;
    private boolean deleted;

    protected Class() {}

    public static Class create(String name, GradeLevel grade, SchoolYear schoolYear) {
        Class cls = new Class();
        cls.name = name;
        cls.grade = grade;
        cls.schoolYear = schoolYear;
        cls.status = ClassStatus.active();
        cls.createdBy = 0L;
        cls.modifiedBy = 0L;
        cls.deleted = false;
        return cls;
    }

    public static Class createWithSchool(String name, GradeLevel grade, SchoolYear schoolYear, SchoolId schoolId) {
        Class cls = create(name, grade, schoolYear);
        cls.schoolId = schoolId;
        return cls;
    }

    public static Class createWithCode(String name, GradeLevel grade, SchoolYear schoolYear, String code) {
        Class cls = create(name, grade, schoolYear);
        cls.code = code;
        return cls;
    }

    public void setId(ClassId id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }

    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void assignSchool(SchoolId schoolId) {
        this.schoolId = schoolId;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setClassType(ClassType classType) {
        this.classType = classType;
    }

    public void setStatus(ClassStatus status) {
        this.status = status;
    }

    public void activate() {
        this.status = ClassStatus.active();
    }

    public void graduate() {
        this.status = ClassStatus.graduated();
    }

    public void archive() {
        this.status = ClassStatus.archived();
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
        return status != null && status.isActive();
    }

    public boolean isGraduated() {
        return status != null && status.isGraduated();
    }

    public boolean isArchived() {
        return status != null && status.isArchived();
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getStatusValue() {
        return status != null ? status.getValue() : null;
    }

    public String getGradeValue() {
        return grade != null ? grade.toString() : null;
    }

    public String getSchoolYearValue() {
        return schoolYear != null ? schoolYear.toString() : null;
    }

    public String getClassTypeValue() {
        return classType != null ? classType.getValue() : null;
    }

    public Long getIdValue() {
        return id != null ? id.getValue() : null;
    }

    public Long getSchoolIdValue() {
        return schoolId != null ? schoolId.getValue() : null;
    }
}