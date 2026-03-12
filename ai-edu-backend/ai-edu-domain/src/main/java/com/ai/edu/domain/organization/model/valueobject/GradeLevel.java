package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 年级级别值对象
 */
@Getter
@EqualsAndHashCode
public class GradeLevel implements ValueObject {

    private final Integer value;

    private GradeLevel(Integer value) {
        if (value == null || value < 1 || value > 12) {
            throw new IllegalArgumentException("Grade level must be between 1 and 12");
        }
        this.value = value;
    }

    public static GradeLevel of(Integer value) {
        return new GradeLevel(value);
    }

    // 小学年级 1-6
    public static GradeLevel primary(int grade) {
        if (grade < 1 || grade > 6) {
            throw new IllegalArgumentException("Primary grade must be between 1 and 6");
        }
        return new GradeLevel(grade);
    }

    // 初中年级 7-9
    public static GradeLevel juniorHigh(int grade) {
        if (grade < 1 || grade > 3) {
            throw new IllegalArgumentException("Junior high grade must be between 1 and 3");
        }
        return new GradeLevel(grade + 6);
    }

    // 高中年级 10-12
    public static GradeLevel highSchool(int grade) {
        if (grade < 1 || grade > 3) {
            throw new IllegalArgumentException("High school grade must be between 1 and 3");
        }
        return new GradeLevel(grade + 9);
    }

    public boolean isPrimary() {
        return value >= 1 && value <= 6;
    }

    public boolean isJuniorHigh() {
        return value >= 7 && value <= 9;
    }

    public boolean isHighSchool() {
        return value >= 10 && value <= 12;
    }

    public String getDisplayName() {
        if (isPrimary()) {
            return "小学" + value + "年级";
        } else if (isJuniorHigh()) {
            return "初" + (value - 6) + "年级";
        } else {
            return "高" + (value - 9) + "年级";
        }
    }

    public GradeLevel next() {
        if (value < 12) {
            return new GradeLevel(value + 1);
        }
        return this;
    }

    public GradeLevel previous() {
        if (value > 1) {
            return new GradeLevel(value - 1);
        }
        return this;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}