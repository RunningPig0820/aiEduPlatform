package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;

/**
 * 派生观测状态（状态机）——观测可修正、可溯源，打标重判而非覆盖删除。
 *
 * <p>NEW → (WEAK) → RESOLVED；CONFLICTED → READJUDICATED / HUMAN_REVIEW。
 * WEAK=冷启动弱确定（首条 LLM 消歧无先验支撑，不直接点亮）。
 */
public enum DerivedKpStatus implements ValueObject {
    /** 新建（未解析） */
    NEW,
    /** 弱确定（冷启动首条，需第二独立信号才转 RESOLVED） */
    WEAK,
    /** 已解析确定（可点亮） */
    RESOLVED,
    /** 挂起待确认（未命中/低置信，kp_uri 空，待学生澄清或人工确认） */
    PENDING,
    /** 冲突（待重判） */
    CONFLICTED,
    /** 已重判（修正后） */
    READJUDICATED,
    /** 转人工审核（LLM/学生都摇摆） */
    HUMAN_REVIEW;

    /** 容错解析：未知或 null 返回 null。 */
    public static DerivedKpStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return DerivedKpStatus.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
