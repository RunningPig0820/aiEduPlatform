package com.ai.edu.domain.organization.model.valueobject;

import lombok.Getter;

/**
 * 学校状态枚举
 */
@Getter
public enum SchoolStatus {

    NORMAL("NORMAL", "正常"),
    ARCHIVE("ARCHIVE", "归档"),
    FAIL("FAIL", "失败");

    private final String value;
    private final String description;

    SchoolStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static SchoolStatus of(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        for (SchoolStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid school status: " + value);
    }

    public boolean isNormal() {
        return this == NORMAL;
    }

    public boolean isArchive() {
        return this == ARCHIVE;
    }

    public boolean isFail() {
        return this == FAIL;
    }
}