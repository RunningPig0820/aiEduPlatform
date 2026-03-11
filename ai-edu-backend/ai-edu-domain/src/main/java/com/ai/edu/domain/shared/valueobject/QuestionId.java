package com.ai.edu.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.io.Serializable;

/**
 * 题目ID值对象
 */
@Getter
@EqualsAndHashCode
public class QuestionId implements ValueObject, Serializable {

    private final Long value;

    public QuestionId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("QuestionId must be positive");
        }
        this.value = value;
    }

    public static QuestionId of(Long value) {
        return new QuestionId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}