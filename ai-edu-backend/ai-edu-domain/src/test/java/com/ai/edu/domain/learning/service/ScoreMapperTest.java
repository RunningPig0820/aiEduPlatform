package com.ai.edu.domain.learning.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 掌握信号映射 {@link ScoreMapper} 测试（tasks 3.3，test.md SIG-001~005）。
 *
 * <p>直接答对 1.0 / 引导后答对 0.5 / 答错 0.0 × per-题型打折（第1题 0.7 / 第2题 0.8 / 第3题起 1.0，
 * 配置化）；打折作用于 score 不作用于结果。
 */
class ScoreMapperTest {

    // ---------- 基础分值（SIG-001~003） ----------

    @Test
    @DisplayName("SIG-001: 直接答对（correct, 无引导）→ 原始分值 1.0")
    void directCorrect_baseScoreOne() {
        assertEquals(BigDecimal.ONE, ScoreMapper.baseScore(true, false));
    }

    @Test
    @DisplayName("SIG-002: 引导后答对（correct + hinted）→ 原始分值 0.5")
    void hintedCorrect_baseScoreHalf() {
        assertEquals(new BigDecimal("0.5"), ScoreMapper.baseScore(true, true));
    }

    @Test
    @DisplayName("SIG-003: 答错/未完成 → 原始分值 0.0")
    void wrong_baseScoreZero() {
        assertEquals(BigDecimal.ZERO, ScoreMapper.baseScore(false, false));
        assertEquals(BigDecimal.ZERO, ScoreMapper.baseScore(false, true));
    }

    // ---------- per-题型打折（SIG-004~005） ----------

    @Test
    @DisplayName("SIG-004: 首题打折 70%——trainCount=0 直接答对 → 生效 1.0×0.7=0.70")
    void firstQuestion_discount70() {
        BigDecimal score = ScoreMapper.effectiveScore(true, false, 0, 0.7, 0.8, 1.0);
        assertEquals(new BigDecimal("0.70"), score);
    }

    @Test
    @DisplayName("SIG-005: 第2题打折 80%——trainCount=1 直接答对 → 生效 1.0×0.8=0.80")
    void secondQuestion_discount80() {
        BigDecimal score = ScoreMapper.effectiveScore(true, false, 1, 0.7, 0.8, 1.0);
        assertEquals(new BigDecimal("0.80"), score);
    }

    @Test
    @DisplayName("第3题起不打折——trainCount=2 → 生效 1.0×1.0=1.00")
    void thirdQuestionOnwards_full() {
        assertEquals(new BigDecimal("1.00"), ScoreMapper.effectiveScore(true, false, 2, 0.7, 0.8, 1.0));
        assertEquals(new BigDecimal("1.00"), ScoreMapper.effectiveScore(true, false, 9, 0.7, 0.8, 1.0));
    }

    @Test
    @DisplayName("打折作用于 score 不作用于结果：首题引导后答对 → 0.5×0.7=0.35")
    void discountAppliedToScoreNotResult() {
        BigDecimal score = ScoreMapper.effectiveScore(true, true, 0, 0.7, 0.8, 1.0);
        assertEquals(new BigDecimal("0.35"), score);
    }

    @Test
    @DisplayName("答错即使打折也保持 0.0（打折不放大错误）")
    void wrong_alwaysZero() {
        assertEquals(new BigDecimal("0.00"), ScoreMapper.effectiveScore(false, false, 0, 0.7, 0.8, 1.0));
    }
}
