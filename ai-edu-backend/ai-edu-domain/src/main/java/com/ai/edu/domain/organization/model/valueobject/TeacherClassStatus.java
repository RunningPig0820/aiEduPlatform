package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 教师班级状态值对象
 */
@Getter
@EqualsAndHashCode
public class TeacherClassStatus implements ValueObject {

    private final String value;

    private TeacherClassStatus(String value) {
        this.value = value;
    }

    public static TeacherClassStatus active() {
        return new TeacherClassStatus("ACTIVE");
    }

    public static TeacherClassStatus inactive() {
        return new TeacherClassStatus("INACTIVE");
    }

    public static TeacherClassStatus of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TeacherClassStatus value cannot be null or blank");
        }
        return new TeacherClassStatus(value);
    }

    public boolean isActive() {
        return "ACTIVE".equals(value);
    }

    public boolean isInactive() {
        return "INACTIVE".equals(value);
    }

    public String getDescription() {
        return switch (value) {
            case "ACTIVE" -> "任教中";
            case "INACTIVE" -> "已结束";
            default -> value;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}