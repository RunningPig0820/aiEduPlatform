package com.ai.edu.domain.learning.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 掌握信号 → 生效分值映射（tasks 3.3，纯函数无框架依赖）。
 *
 * <p>设计 Decision 3：信号三档 × per-题型打折（第1题 0.7 / 第2题 0.8 / 第3题起 1.0，配置化）。
 * 打折作用于 score 不作用于结果（避免「第1题答错 → 题型 0%」假低）。
 *
 * <p>供 3.4 落题目表时算 {@code score}（与掌握表 {@code applyScore} 同源可追溯）：
 * 直接答对 1.0 / 引导后答对 0.5 / 答错 0.0 × 打折系数。
 */
public final class ScoreMapper {

    private ScoreMapper() {
    }

    /** 信号 → 打折前分值：直接答对 1.0 / 引导后答对 0.5 / 答错或未完成 0.0。 */
    public static BigDecimal baseScore(boolean correct, boolean hinted) {
        if (!correct) {
            return BigDecimal.ZERO;
        }
        return hinted ? new BigDecimal("0.5") : BigDecimal.ONE;
    }

    /**
     * per-题型打折系数：trainCount = 该题型已训练数，本次为其第 {@code trainCount + 1} 题。
     * 第1题 0.7 / 第2题 0.8 / 第3题起 1.0（系数可配置）。
     */
    public static BigDecimal discountFactor(long trainCount, double first, double second, double rest) {
        if (trainCount == 0) {
            return BigDecimal.valueOf(first);
        }
        if (trainCount == 1) {
            return BigDecimal.valueOf(second);
        }
        return BigDecimal.valueOf(rest);
    }

    /**
     * 生效分值 = baseScore × discount（setScale 2 HALF_UP）。
     *
     * @param trainCount 该题型已训练数（第1题=0 / 第2题=1 / 第3题起≥2）
     * @param first      第1题打折系数（默认 0.7）
     * @param second     第2题打折系数（默认 0.8）
     * @param rest       第3题起打折系数（默认 1.0）
     */
    public static BigDecimal effectiveScore(boolean correct, boolean hinted, long trainCount,
                                            double first, double second, double rest) {
        return baseScore(correct, hinted).multiply(discountFactor(trainCount, first, second, rest))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
