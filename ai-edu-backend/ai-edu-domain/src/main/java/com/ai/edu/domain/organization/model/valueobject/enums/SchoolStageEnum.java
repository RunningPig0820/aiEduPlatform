package com.ai.edu.domain.organization.model.valueobject.enums;

import lombok.Getter;

/**
 * 学校学段枚举
 * 小学、初中、高中、大学
 */
@Getter
public enum SchoolStageEnum {

    /** 小学 */
    PRIMARY("PRIMARY", "小学", 1),

    /** 初中 */
    JUNIOR_HIGH("JUNIOR_HIGH", "初中", 7),

    /** 高中 */
    SENIOR_HIGH("SENIOR_HIGH", "高中", 10),

    /** 大学 */
    UNIVERSITY("UNIVERSITY", "大学", 1);

    private final String value;
    private final String description;
    private final int startGradeLevel;

    SchoolStageEnum(String value, String description, int startGradeLevel) {
        this.value = value;
        this.description = description;
        this.startGradeLevel = startGradeLevel;
    }

    public static SchoolStageEnum of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SchoolStage value cannot be null or blank");
        }
        for (SchoolStageEnum stage : values()) {
            if (stage.value.equals(value)) {
                return stage;
            }
        }
        throw new IllegalArgumentException("Unknown SchoolStage: " + value);
    }

    public boolean isPrimary() {
        return this == PRIMARY;
    }

    public boolean isJuniorHigh() {
        return this == JUNIOR_HIGH;
    }

    public boolean isSeniorHigh() {
        return this == SENIOR_HIGH;
    }

    public boolean isUniversity() {
        return this == UNIVERSITY;
    }

    /**
     * 获取该学段的结束年级级别
     * @param yearCount 年制年数（由 StageYearCode 提供）
     */
    public int getEndGradeLevel(int yearCount) {
        return startGradeLevel + yearCount - 1;
    }
}
