package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 情绪状态值对象
 */
@Getter
@EqualsAndHashCode
public class EmotionState implements ValueObject {

    private final String value;
    private final double confidence;

    private EmotionState(String value, double confidence) {
        this.value = value;
        this.confidence = confidence;
    }

    public static EmotionState positive(double confidence) {
        return new EmotionState("POSITIVE", confidence);
    }

    public static EmotionState neutral(double confidence) {
        return new EmotionState("NEUTRAL", confidence);
    }

    public static EmotionState frustrated(double confidence) {
        return new EmotionState("FRUSTRATED", confidence);
    }

    public static EmotionState confused(double confidence) {
        return new EmotionState("CONFUSED", confidence);
    }

    public static EmotionState anxious(double confidence) {
        return new EmotionState("ANXIOUS", confidence);
    }

    public static EmotionState of(String value, double confidence) {
        return new EmotionState(value, confidence);
    }

    public boolean isNegative() {
        return "FRUSTRATED".equals(value) ||
               "CONFUSED".equals(value) ||
               "ANXIOUS".equals(value);
    }

    public boolean needsSupport() {
        return isNegative() && confidence > 0.6;
    }
}