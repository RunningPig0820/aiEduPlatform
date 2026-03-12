package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 班级状态值对象
 */
@Getter
@EqualsAndHashCode
public class ClassStatus implements ValueObject {

    private final String value;

    private ClassStatus(String value) {
        this.value = value;
    }

    public static ClassStatus active() {
        return new ClassStatus("ACTIVE");
    }

    public static ClassStatus graduated() {
        return new ClassStatus("GRADUATED");
    }

    public static ClassStatus archived() {
        return new ClassStatus("ARCHIVED");
    }

    public static ClassStatus of(String value) {
        return new ClassStatus(value);
    }

    public boolean isActive() {
        return "ACTIVE".equals(value);
    }

    public boolean isGraduated() {
        return "GRADUATED".equals(value);
    }

    public boolean isArchived() {
        return "ARCHIVED".equals(value);
    }

    public String getDescription() {
        return switch (value) {
            case "ACTIVE" -> "活跃";
            case "GRADUATED" -> "已毕业";
            case "ARCHIVED" -> "已归档";
            default -> value;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}