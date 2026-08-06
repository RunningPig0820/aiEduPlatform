package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;

/**
 * 答疑情绪（F7 七态）——Python 输出方权威，Java 存储侧对齐。
 *
 * <p>t_tutoring_error_event.emotion / t_tutoring_session.last_emotion 存 F7 字符串。
 * 不复用学习域 {@code EmotionState}（F7 是答疑契约闭集）。
 */
public enum TutoringEmotion implements ValueObject {
    /** 平静/中性（默认态） */
    NEUTRAL,
    /** 困惑 */
    CONFUSED,
    /** 沮丧 */
    FRUSTRATED,
    /** 焦虑 */
    ANXIOUS,
    /** 自信 */
    CONFIDENT,
    /** 感兴趣 */
    INTERESTED,
    /** 无聊 */
    BORED;

    /** 容错解析：未知或 null → NEUTRAL。 */
    public static TutoringEmotion fromCode(String code) {
        if (code == null || code.isBlank()) {
            return NEUTRAL;
        }
        try {
            return TutoringEmotion.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEUTRAL;
        }
    }
}
