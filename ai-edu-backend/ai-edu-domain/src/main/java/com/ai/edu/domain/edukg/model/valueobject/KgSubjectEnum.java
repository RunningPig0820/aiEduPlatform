package com.ai.edu.domain.edukg.model.valueobject;

import lombok.Getter;

/**
 * 知识图谱-学科枚举
 */
@Getter
public enum KgSubjectEnum {

    MATH("math", "数学", 1),
    CHINESE("chinese", "语文", 2),
    ENGLISH("english", "英语", 3),
    PHYSICS("physics", "物理", 4),
    CHEMISTRY("chemistry", "化学", 5),
    BIOLOGY("biology", "生物", 6);

    private final String code;
    private final String label;
    private final int orderIndex;

    KgSubjectEnum(String code, String label, int orderIndex) {
        this.code = code;
        this.label = label;
        this.orderIndex = orderIndex;
    }
}
