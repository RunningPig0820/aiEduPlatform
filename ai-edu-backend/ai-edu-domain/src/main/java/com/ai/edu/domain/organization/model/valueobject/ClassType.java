package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 班级类型值对象
 */
@Getter
@EqualsAndHashCode
public class ClassType implements ValueObject {

    private final String value;

    private ClassType(String value) {
        this.value = value;
    }

    public static ClassType normal() {
        return new ClassType("NORMAL");
    }

    public static ClassType experimental() {
        return new ClassType("EXPERIMENTAL");
    }

    public static ClassType international() {
        return new ClassType("INTERNATIONAL");
    }

    public static ClassType of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ClassType value cannot be null or blank");
        }
        return new ClassType(value);
    }

    public boolean isNormal() {
        return "NORMAL".equals(value);
    }

    public boolean isExperimental() {
        return "EXPERIMENTAL".equals(value);
    }

    public boolean isInternational() {
        return "INTERNATIONAL".equals(value);
    }

    public String getDescription() {
        return switch (value) {
            case "NORMAL" -> "普通班";
            case "EXPERIMENTAL" -> "实验班";
            case "INTERNATIONAL" -> "国际班";
            default -> value;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}