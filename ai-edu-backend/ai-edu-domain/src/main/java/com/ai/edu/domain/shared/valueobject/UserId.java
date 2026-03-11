package com.ai.edu.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.io.Serializable;

/**
 * 用户ID值对象
 */
@Getter
@EqualsAndHashCode
public class UserId implements ValueObject, Serializable {

    private final Long value;

    public UserId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("UserId must be positive");
        }
        this.value = value;
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}