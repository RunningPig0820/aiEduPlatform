package com.ai.edu.domain.homework.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 分数值对象
 */
@Getter
@EqualsAndHashCode
public class Score implements ValueObject {

    private final Integer value;
    private final Integer maxValue;

    public Score(Integer value, Integer maxValue) {
        if (value == null || maxValue == null) {
            throw new IllegalArgumentException("Score value and maxValue cannot be null");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }
        if (maxValue <= 0) {
            throw new IllegalArgumentException("MaxValue must be positive");
        }
        if (value > maxValue) {
            throw new IllegalArgumentException("Score cannot exceed maxValue");
        }
        this.value = value;
        this.maxValue = maxValue;
    }

    public static Score of(Integer value) {
        return new Score(value, 100);
    }

    public static Score of(Integer value, Integer maxValue) {
        return new Score(value, maxValue);
    }

    public double getPercentage() {
        return (double) value / maxValue * 100;
    }

    public boolean isPassing(int passingScore) {
        return value >= passingScore;
    }
}