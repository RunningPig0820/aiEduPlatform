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
}
