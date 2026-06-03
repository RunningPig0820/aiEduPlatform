package com.ai.edu.domain.organization.model.valueobject;

import lombok.Getter;

/**
 * 学校状态枚举
 */
@Getter
public enum SchoolStatus {

    ACTIVE("ACTIVE", "正常"),
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
            return ACTIVE;
        }
        // 兼容旧数据：NORMAL -> ACTIVE
        if ("NORMAL".equalsIgnoreCase(value)) {
            return ACTIVE;
        }
        for (SchoolStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid school status: " + value);
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isArchive() {
        return this == ARCHIVE;
    }

    public boolean isFail() {
        return this == FAIL;
    }
}