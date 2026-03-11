package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 掌握程度值对象
 */
@Getter
@EqualsAndHashCode
public class MasteryLevel implements ValueObject {

    private final int value;

    private MasteryLevel(int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Mastery level must be between 0 and 100");
        }
        this.value = value;
    }

    public static MasteryLevel of(int value) {
        return new MasteryLevel(value);
    }

    public static MasteryLevel notStarted() {
        return new MasteryLevel(0);
    }

    public static MasteryLevel beginner() {
        return new MasteryLevel(25);
    }

    public static MasteryLevel intermediate() {
        return new MasteryLevel(50);
    }

    public static MasteryLevel advanced() {
        return new MasteryLevel(75);
    }

    public static MasteryLevel master() {
        return new MasteryLevel(100);
    }

    public boolean isPoor() {
        return value < 40;
    }

    public boolean isFair() {
        return value >= 40 && value < 70;
    }

    public boolean isGood() {
        return value >= 70;
    }

    public MasteryLevel increase(int delta) {
        int newValue = Math.min(100, this.value + delta);
        return new MasteryLevel(newValue);
    }

    public MasteryLevel decrease(int delta) {
        int newValue = Math.max(0, this.value - delta);
        return new MasteryLevel(newValue);
    }
}