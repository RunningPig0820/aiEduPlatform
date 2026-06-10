package com.ai.edu.domain.organization.model.valueobject.enums;

import lombok.Getter;

/**
 * 班级类型枚举
 */
@Getter
public enum ClassTypeEnum {

    /** 普通班 */
    NORMAL("NORMAL", "普通班"),

    /** 实验班 */
    EXPERIMENTAL("EXPERIMENTAL", "实验班"),

    /** 国际班 */
    INTERNATIONAL("INTERNATIONAL", "国际班");

    private final String value;
    private final String description;

    ClassTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static ClassTypeEnum of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ClassType value cannot be null or blank");
        }
        for (ClassTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ClassType: " + value);
    }

    public boolean isNormal() {
        return this == NORMAL;
    }

    public boolean isExperimental() {
        return this == EXPERIMENTAL;
    }

    public boolean isInternational() {
        return this == INTERNATIONAL;
    }
}
