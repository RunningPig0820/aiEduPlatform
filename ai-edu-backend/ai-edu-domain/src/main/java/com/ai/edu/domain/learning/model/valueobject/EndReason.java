package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;

/**
 * 答疑会话收尾原因（type=end 联动）——收尾时按 end_reason 校正掌握度。
 *
 * <p>COMPLETED → 提升到 75+；ANSWER_REVEALED / ABANDONED / ROUND_LIMIT → 不提升。
 */
public enum EndReason implements ValueObject {
    /** 独立解出 */
    COMPLETED,
    /** 看过答案 */
    ANSWER_REVEALED,
    /** 学生放弃/主动结束 */
    ABANDONED,
    /** 轮次上限 */
    ROUND_LIMIT;

    /** 容错解析：未知或 null 返回 null。 */
    public static EndReason fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return EndReason.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
