package com.ai.edu.domain.edukg.model.valueobject;

import lombok.Getter;

/**
 * 认知层次枚举
 */
@Getter
public enum KgCognitiveLevel {

    REMEMBER("记忆", "记忆"),
    UNDERSTAND("理解", "理解"),
    APPLY("应用", "应用"),
    ANALYZE("分析", "分析");

    private final String value;
    private final String label;

    KgCognitiveLevel(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static KgCognitiveLevel fromValue(String value) {
        for (KgCognitiveLevel level : values()) {
            if (level.value.equals(value)) {
                return level;
            }
        }
        return UNDERSTAND;
    }
}
