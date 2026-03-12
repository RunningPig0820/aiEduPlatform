package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 学校类型值对象
 */
@Getter
@EqualsAndHashCode
public class SchoolType implements ValueObject {

    private final String value;

    private SchoolType(String value) {
        this.value = value;
    }

    public static SchoolType primary() {
        return new SchoolType("PRIMARY");
    }

    public static SchoolType juniorHigh() {
        return new SchoolType("JUNIOR_HIGH");
    }

    public static SchoolType highSchool() {
        return new SchoolType("HIGH_SCHOOL");
    }

    public static SchoolType of(String value) {
        return new SchoolType(value);
    }

    public boolean isPrimary() {
        return "PRIMARY".equals(value);
    }

    public boolean isJuniorHigh() {
        return "JUNIOR_HIGH".equals(value);
    }

    public boolean isHighSchool() {
        return "HIGH_SCHOOL".equals(value);
    }

    public String getDescription() {
        return switch (value) {
            case "PRIMARY" -> "小学";
            case "JUNIOR_HIGH" -> "初中";
            case "HIGH_SCHOOL" -> "高中";
            default -> value;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}