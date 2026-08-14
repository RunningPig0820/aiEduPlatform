package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;

/**
 * 题型库条目状态（闭集）——CANDIDATE 待审核，STABLE 可进解析先验。
 */
public enum QuestionTypeStatus implements ValueObject {
    /** 候选（达聚合阈值，待审核） */
    CANDIDATE,
    /** 稳定（审核通过，进解析先验） */
    STABLE;

    /** 容错解析：未知或 null 返回 null。 */
    public static QuestionTypeStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return QuestionTypeStatus.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
