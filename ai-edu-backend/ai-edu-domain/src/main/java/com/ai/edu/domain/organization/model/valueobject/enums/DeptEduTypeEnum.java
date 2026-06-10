package com.ai.edu.domain.organization.model.valueobject.enums;

import lombok.Getter;

/**
 * 教育部门节点类型枚举
 * 标识行政班树中的节点层级：学段、年级、班级
 */
@Getter
public enum DeptEduTypeEnum {

    /** 学段 */
    STAGE(3, "学段"),

    /** 年级 */
    GRADE(4, "年级"),

    /** 班级 */
    CLASS(5, "班级");

    private final int value;
    private final String description;

    DeptEduTypeEnum(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 根据整数值获取枚举
     */
    public static DeptEduTypeEnum of(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("DeptEduType value cannot be null");
        }
        for (DeptEduTypeEnum type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("DeptEduType must be 3(学段), 4(年级), or 5(班级), got: " + value);
    }

    public boolean isStage() {
        return this == STAGE;
    }

    public boolean isGrade() {
        return this == GRADE;
    }

    public boolean isClass() {
        return this == CLASS;
    }

    /**
     * 是否是需要 grade_code 和 enrollment_year 的节点类型（年级、班级）
     */
    public boolean requiresGradeInfo() {
        return isGrade() || isClass();
    }
}
