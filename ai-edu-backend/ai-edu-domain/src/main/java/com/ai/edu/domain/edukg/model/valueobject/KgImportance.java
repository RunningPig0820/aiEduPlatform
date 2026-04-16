package com.ai.edu.domain.edukg.model.valueobject;

import lombok.Getter;

/**
 * 知识点重要性枚举
 */
@Getter
public enum KgImportance {

    LOW("low", "低"),
    MEDIUM("medium", "中"),
    HIGH("high", "高");

    private final String value;
    private final String label;

    KgImportance(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static KgImportance fromValue(String value) {
        for (KgImportance importance : values()) {
            if (importance.value.equals(value)) {
                return importance;
            }
        }
        return MEDIUM;
    }
}
