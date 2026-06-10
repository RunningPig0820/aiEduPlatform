package com.ai.edu.domain.organization.model.valueobject.enums;

import lombok.Getter;

/**
 * 部门类型枚举
 * 区分普通组织部门和行政班节点
 */
@Getter
public enum DepartmentTypeEnum {

    /** 普通组织部门 */
    ORG("ORG", "普通组织部门"),

    /** 行政班节点 */
    ADMIN_CLASS("ADMIN_CLASS", "行政班节点");

    private final String value;
    private final String description;

    DepartmentTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static DepartmentTypeEnum of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DepartmentType value cannot be null or blank");
        }
        for (DepartmentTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("DepartmentType must be ORG or ADMIN_CLASS, got: " + value);
    }

    public boolean isOrg() {
        return this == ORG;
    }

    public boolean isAdminClass() {
        return this == ADMIN_CLASS;
    }
}
