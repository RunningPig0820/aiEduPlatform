package com.ai.edu.domain.edukg.model.valueobject;

import lombok.Getter;

/**
 * 知识图谱-学段枚举
 */
@Getter
public enum KgStageEnum {

    PRIMARY("primary", "小学", 1),
    MIDDLE("middle", "初中", 2),
    HIGH("high", "高中", 3);

    private final String code;
    private final String label;
    private final int orderIndex;

    KgStageEnum(String code, String label, int orderIndex) {
        this.code = code;
        this.label = label;
        this.orderIndex = orderIndex;
    }

    /** code → 枚举（未知或 null 返回 null）。 */
    public static KgStageEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (KgStageEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }

    /** 中文 label → 枚举（未知或 null 返回 null）。 */
    public static KgStageEnum fromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (KgStageEnum e : values()) {
            if (e.label.equals(label)) {
                return e;
            }
        }
        return null;
    }
}
