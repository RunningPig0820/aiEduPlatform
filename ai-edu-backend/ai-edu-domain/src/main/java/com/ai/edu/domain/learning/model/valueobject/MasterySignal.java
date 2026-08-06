package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 掌握度信号值对象——decide 每轮输出的 {@code mastery_signals} 项。
 *
 * <p>Java 收到后经 {@code TutoringKpResolver} 将 label 解析为 TextbookKP URI，
 * 按 {@link Level#masteryValue()} 的分值 UPSERT 掌握度（取 max 单调不减）。
 */
@Getter
@EqualsAndHashCode
public final class MasterySignal implements ValueObject {

    private final String kpLabel;
    private final Level signal;

    private MasterySignal(String kpLabel, Level signal) {
        if (kpLabel == null || kpLabel.isBlank()) {
            throw new IllegalArgumentException("kpLabel must not be blank");
        }
        if (signal == null) {
            throw new IllegalArgumentException("signal must not be null");
        }
        this.kpLabel = kpLabel;
        this.signal = signal;
    }

    public static MasterySignal of(String kpLabel, Level signal) {
        return new MasterySignal(kpLabel, signal);
    }

    /** 容错解析（Python 输出值），未知 signal 保守默认 STRUGGLING。 */
    public static MasterySignal fromCode(String kpLabel, String signalCode) {
        return new MasterySignal(kpLabel, Level.fromCode(signalCode));
    }

    /**
     * 掌握度信号等级（mastered→75 / practicing→50 / struggling→25）。
     */
    public enum Level implements ValueObject {
        /** 已掌握（能独立解出） */
        MASTERED,
        /** 练习中（会但需引导/会出错） */
        PRACTICING,
        /** 薄弱（明显困难/多次出错） */
        STRUGGLING;

        /** 信号对应的掌握度分值（设计决策 9，取 max 用）。 */
        public int masteryValue() {
            switch (this) {
                case MASTERED:
                    return 75;
                case PRACTICING:
                    return 50;
                default:
                    return 25;
            }
        }

        /** 容错解析：未知或 null → STRUGGLING（保守默认，低分值不伤害 max 单调性）。 */
        public static Level fromCode(String code) {
            if (code == null || code.isBlank()) {
                return STRUGGLING;
            }
            try {
                return Level.valueOf(code.toUpperCase());
            } catch (IllegalArgumentException e) {
                return STRUGGLING;
            }
        }
    }
}
