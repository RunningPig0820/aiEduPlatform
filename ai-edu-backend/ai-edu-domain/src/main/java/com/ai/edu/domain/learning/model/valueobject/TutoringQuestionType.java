package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;

/**
 * 答疑题型（答疑侧独立可扩展枚举，不绑定作业域 QuestionType）。
 *
 * <p>Python 可输出闭集之外的新题型——经 {@link #fromCode} 容错回 UNKNOWN，不阻断答疑。
 * 字段可空（t_tutoring_session.question_type），仅用于统计。
 */
public enum TutoringQuestionType implements ValueObject {
    /** 选择 */
    CHOICE,
    /** 填空 */
    FILL_BLANK,
    /** 解答 */
    SOLUTION,
    /** 未知/新题型（可扩展，容错落点） */
    UNKNOWN;

    /** 容错解析：未知或 null → UNKNOWN。 */
    public static TutoringQuestionType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        try {
            return TutoringQuestionType.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
