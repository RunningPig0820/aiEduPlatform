package com.ai.edu.domain.organization.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 部门ID值对象
 */
@Getter
@EqualsAndHashCode
public class DepartmentId implements Serializable {

    private final Long value;

    public DepartmentId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("DepartmentId must be positive");
        }
        this.value = value;
    }

    public static DepartmentId of(Long value) {
        return new DepartmentId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}