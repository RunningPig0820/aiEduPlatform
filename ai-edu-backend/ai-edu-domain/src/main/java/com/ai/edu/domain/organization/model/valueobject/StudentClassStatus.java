package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 学生班级状态值对象
 */
@Getter
@EqualsAndHashCode
public class StudentClassStatus implements ValueObject {

    private final String value;

    private StudentClassStatus(String value) {
        this.value = value;
    }

    public static StudentClassStatus active() {
        return new StudentClassStatus("ACTIVE");
    }

    public static StudentClassStatus graduated() {
        return new StudentClassStatus("GRADUATED");
    }

    public static StudentClassStatus transferred() {
        return new StudentClassStatus("TRANSFERRED");
    }

    public static StudentClassStatus of(String value) {
        return new StudentClassStatus(value);
    }

    public boolean isActive() {
        return "ACTIVE".equals(value);
    }

    public boolean isGraduated() {
        return "GRADUATED".equals(value);
    }

    public boolean isTransferred() {
        return "TRANSFERRED".equals(value);
    }

    public String getDescription() {
        return switch (value) {
            case "ACTIVE" -> "在读";
            case "GRADUATED" -> "已毕业";
            case "TRANSFERRED" -> "已转出";
            default -> value;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}