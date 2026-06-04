package com.ai.edu.domain.organization.model.valueobject;

import lombok.Getter;

import java.util.Objects;

/**
 * 教职工ID值对象
 */
@Getter
public class OrgTeacherId {

    private final Long value;

    private OrgTeacherId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("OrgTeacherId must be positive");
        }
        this.value = value;
    }

    public static OrgTeacherId of(Long value) {
        return new OrgTeacherId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrgTeacherId that = (OrgTeacherId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}