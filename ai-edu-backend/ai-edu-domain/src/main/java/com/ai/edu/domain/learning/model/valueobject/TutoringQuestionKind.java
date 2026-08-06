package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;

/**
 * 答疑题类（答疑侧独立可扩展枚举）。
 *
 * <p>题类（计算/应用/证明）与题型独立；Python 可输出新题类——容错回 UNKNOWN。
 * 字段可空（t_tutoring_session.question_kind），仅用于统计。
 */
public enum TutoringQuestionKind implements ValueObject {
    /** 计算 */
    CALCULATION,
    /** 应用 */
    APPLICATION,
    /** 证明 */
    PROOF,
    /** 未知/新题类（可扩展，容错落点） */
    UNKNOWN;

    /** 容错解析：未知或 null → UNKNOWN。 */
    public static TutoringQuestionKind fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        try {
            return TutoringQuestionKind.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
