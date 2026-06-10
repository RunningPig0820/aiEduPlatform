package com.ai.edu.domain.organization.model.valueobject.enums;

import lombok.Getter;

/**
 * 班级状态枚举
 */
@Getter
public enum ClassStatusEnum {

    /** 活跃 */
    ACTIVE("ACTIVE", "活跃"),

    /** 已毕业 */
    GRADUATED("GRADUATED", "已毕业"),

    /** 已归档 */
    ARCHIVED("ARCHIVED", "已归档");

    private final String value;
    private final String description;

    ClassStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static ClassStatusEnum of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ClassStatus value cannot be null or blank");
        }
        for (ClassStatusEnum status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ClassStatus: " + value);
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isGraduated() {
        return this == GRADUATED;
    }

    public boolean isArchived() {
        return this == ARCHIVED;
    }
}
