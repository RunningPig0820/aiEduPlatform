package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 学校学段值对象
 * 表示学校包含的学段：小学、初中、高中、大学
 */
@Getter
@EqualsAndHashCode
public class SchoolStage implements ValueObject {

    private final String value;

    private SchoolStage(String value) {
        this.value = value;
    }

    /**
     * 小学
     */
    public static SchoolStage primary() {
        return new SchoolStage("PRIMARY");
    }

    /**
     * 初中
     */
    public static SchoolStage juniorHigh() {
        return new SchoolStage("JUNIOR_HIGH");
    }

    /**
     * 高中
     */
    public static SchoolStage seniorHigh() {
        return new SchoolStage("SENIOR_HIGH");
    }

    /**
     * 大学
     */
    public static SchoolStage university() {
        return new SchoolStage("UNIVERSITY");
    }

    public static SchoolStage of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SchoolStage value cannot be null or blank");
        }
        return new SchoolStage(value);
    }

    public boolean isPrimary() {
        return "PRIMARY".equals(value);
    }

    public boolean isJuniorHigh() {
        return "JUNIOR_HIGH".equals(value);
    }

    public boolean isSeniorHigh() {
        return "SENIOR_HIGH".equals(value);
    }

    public boolean isUniversity() {
        return "UNIVERSITY".equals(value);
    }

    public String getDescription() {
        return switch (value) {
            case "PRIMARY" -> "小学";
            case "JUNIOR_HIGH" -> "初中";
            case "SENIOR_HIGH" -> "高中";
            case "UNIVERSITY" -> "大学";
            default -> value;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}