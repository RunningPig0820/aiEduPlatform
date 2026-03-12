package com.ai.edu.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 班级ID值对象
 */
@Getter
@EqualsAndHashCode
public class ClassId implements ValueObject, Serializable {

    private final Long value;

    public ClassId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("ClassId must be positive");
        }
        this.value = value;
    }

    public static ClassId of(Long value) {
        return new ClassId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}