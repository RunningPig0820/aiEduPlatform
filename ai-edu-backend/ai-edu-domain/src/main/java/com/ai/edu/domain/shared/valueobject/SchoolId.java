package com.ai.edu.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 学校ID值对象
 */
@Getter
@EqualsAndHashCode
public class SchoolId implements ValueObject, Serializable {

    private final Long value;

    public SchoolId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("SchoolId must be positive");
        }
        this.value = value;
    }

    public static SchoolId of(Long value) {
        return new SchoolId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}