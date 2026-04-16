package com.ai.edu.domain.edukg.model.valueobject;

import lombok.Getter;

/**
 * 知识点难度枚举
 */
@Getter
public enum KgDifficulty {

    EASY("easy", "简单"),
    MEDIUM("medium", "中等"),
    HARD("hard", "困难");

    private final String value;
    private final String label;

    KgDifficulty(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static KgDifficulty fromValue(String value) {
        for (KgDifficulty difficulty : values()) {
            if (difficulty.value.equals(value)) {
                return difficulty;
            }
        }
        return MEDIUM;
    }
}
