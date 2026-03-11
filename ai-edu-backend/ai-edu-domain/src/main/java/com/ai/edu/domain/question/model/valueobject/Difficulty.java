package com.ai.edu.domain.question.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 难度值对象
 */
@Getter
@EqualsAndHashCode
public class Difficulty implements ValueObject {

    private final String value;

    private Difficulty(String value) {
        this.value = value;
    }

    public static Difficulty easy() {
        return new Difficulty("EASY");
    }

    public static Difficulty medium() {
        return new Difficulty("MEDIUM");
    }

    public static Difficulty hard() {
        return new Difficulty("HARD");
    }

    public static Difficulty of(String value) {
        return new Difficulty(value);
    }

    public boolean isEasy() {
        return "EASY".equals(value);
    }

    public boolean isMedium() {
        return "MEDIUM".equals(value);
    }

    public boolean isHard() {
        return "HARD".equals(value);
    }
}