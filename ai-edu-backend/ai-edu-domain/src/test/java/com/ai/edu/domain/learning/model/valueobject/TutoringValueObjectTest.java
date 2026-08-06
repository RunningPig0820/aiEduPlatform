package com.ai.edu.domain.learning.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 答疑值对象测试——领域层核心单测（test.md 领域单测一节）。
 *
 * <p>覆盖：KpKey 空校验 / ActionType·EndReason 取值封闭 / MasterySignal 分值映射 /
 * TutoringEmotion F7 / 题型·题类 UNKNOWN 容错 / TutoringState 容错 / 常量值。
 */
class TutoringValueObjectTest {

    // ==================== KpKey ====================

    @Test
    @DisplayName("KpKey.of() 合法 URI 应正确包装")
    void kpKey_of_shouldWrapUri() {
        KpKey key = KpKey.of("http://edukg.org/TextbookKP/1");
        assertEquals("http://edukg.org/TextbookKP/1", key.getValue());
        assertEquals("http://edukg.org/TextbookKP/1", key.toString());
    }

    @Test
    @DisplayName("KpKey.of(null) 应抛异常")
    void kpKey_of_shouldThrowOnNull() {
        assertThrows(IllegalArgumentException.class, () -> KpKey.of(null));
    }

    @Test
    @DisplayName("KpKey.of(空串/空白) 应抛异常")
    void kpKey_of_shouldThrowOnBlank() {
        assertThrows(IllegalArgumentException.class, () -> KpKey.of(""));
        assertThrows(IllegalArgumentException.class, () -> KpKey.of("   "));
    }

    @Test
    @DisplayName("KpKey equals/hashCode 按 URI 一致")
    void kpKey_equalsAndHashCode() {
        KpKey a = KpKey.of("http://edukg.org/TextbookKP/1");
        KpKey b = KpKey.of("http://edukg.org/TextbookKP/1");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, KpKey.of("http://edukg.org/TextbookKP/2"));
    }

    // ==================== ActionType（闭集） ====================

    @Test
    @DisplayName("ActionType 闭集六值应为 hint/approach/reveal/concept/switch/end")
    void actionType_closedSet() {
        assertEquals(6, ActionType.values().length);
        assertNotNull(ActionType.valueOf("HINT"));
        assertNotNull(ActionType.valueOf("APPROACH"));
        assertNotNull(ActionType.valueOf("REVEAL"));
        assertNotNull(ActionType.valueOf("CONCEPT"));
        assertNotNull(ActionType.valueOf("SWITCH"));
        assertNotNull(ActionType.valueOf("END"));
    }

    @Test
    @DisplayName("ActionType.fromCode 小写输入应正确解析（Python 输出）")
    void actionType_fromCode_caseInsensitive() {
        assertEquals(ActionType.HINT, ActionType.fromCode("hint"));
        assertEquals(ActionType.REVEAL, ActionType.fromCode("reveal"));
        assertEquals(ActionType.SWITCH, ActionType.fromCode("switch"));
    }

    @Test
    @DisplayName("ActionType.fromCode 非法值应返回 null")
    void actionType_fromCode_invalidShouldReturnNull() {
        assertNull(ActionType.fromCode("ILLEGAL"));
        assertNull(ActionType.fromCode(null));
        assertNull(ActionType.fromCode(""));
    }

    @Test
    @DisplayName("ActionType.fromCodeOrDefault 非法值应默认 HINT（不阻断）")
    void actionType_fromCodeOrDefault_shouldDefaultToHint() {
        assertEquals(ActionType.HINT, ActionType.fromCodeOrDefault("ILLEGAL"));
        assertEquals(ActionType.HINT, ActionType.fromCodeOrDefault(null));
        assertEquals(ActionType.END, ActionType.fromCodeOrDefault("end"));
    }

    // ==================== EndReason（取值封闭） ====================

    @Test
    @DisplayName("EndReason 四值封闭 + 容错解析")
    void endReason_closedSetAndParsing() {
        assertEquals(4, EndReason.values().length);
        assertEquals(EndReason.COMPLETED, EndReason.fromCode("COMPLETED"));
        assertEquals(EndReason.ANSWER_REVEALED, EndReason.fromCode("answer_revealed"));
        assertEquals(EndReason.ABANDONED, EndReason.fromCode("ABANDONED"));
        assertEquals(EndReason.ROUND_LIMIT, EndReason.fromCode("round_limit"));
        assertNull(EndReason.fromCode("ILLEGAL"));
        assertNull(EndReason.fromCode(null));
    }

    // ==================== MasterySignal ====================

    @Test
    @DisplayName("MasterySignal 分值映射 mastered→75/practicing→50/struggling→25")
    void masterySignal_levelValues() {
        assertEquals(75, MasterySignal.Level.MASTERED.masteryValue());
        assertEquals(50, MasterySignal.Level.PRACTICING.masteryValue());
        assertEquals(25, MasterySignal.Level.STRUGGLING.masteryValue());
    }

    @Test
    @DisplayName("MasterySignal.fromCode 应正确解析并保守兜底")
    void masterySignal_fromCode() {
        MasterySignal mastered = MasterySignal.fromCode("鸡兔同笼", "mastered");
        assertEquals("鸡兔同笼", mastered.getKpLabel());
        assertEquals(MasterySignal.Level.MASTERED, mastered.getSignal());

        // 未知 signal 保守默认 STRUGGLING
        assertEquals(MasterySignal.Level.STRUGGLING,
                MasterySignal.fromCode("鸡兔同笼", "garbage").getSignal());
        assertThrows(IllegalArgumentException.class,
                () -> MasterySignal.fromCode("", "mastered"));
    }

    @Test
    @DisplayName("MasterySignal.of 空 label 应抛异常")
    void masterySignal_blankLabel_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> MasterySignal.of("", MasterySignal.Level.MASTERED));
        assertThrows(IllegalArgumentException.class,
                () -> MasterySignal.of("鸡兔同笼", null));
    }

    // ==================== TutoringEmotion（F7） ====================

    @Test
    @DisplayName("TutoringEmotion F7 七态且大小写不敏感")
    void tutoringEmotion_f7Set() {
        assertEquals(7, TutoringEmotion.values().length);
        assertEquals(TutoringEmotion.NEUTRAL, TutoringEmotion.fromCode("NEUTRAL"));
        assertEquals(TutoringEmotion.CONFUSED, TutoringEmotion.fromCode("confused"));
        assertEquals(TutoringEmotion.FRUSTRATED, TutoringEmotion.fromCode("frustrated"));
        assertEquals(TutoringEmotion.ANXIOUS, TutoringEmotion.fromCode("ANXIOUS"));
        assertEquals(TutoringEmotion.CONFIDENT, TutoringEmotion.fromCode("confident"));
        assertEquals(TutoringEmotion.INTERESTED, TutoringEmotion.fromCode("interested"));
        assertEquals(TutoringEmotion.BORED, TutoringEmotion.fromCode("bored"));
    }

    @Test
    @DisplayName("TutoringEmotion.fromCode 未知值应默认 NEUTRAL")
    void tutoringEmotion_unknownShouldDefaultToNeutral() {
        assertEquals(TutoringEmotion.NEUTRAL, TutoringEmotion.fromCode("HAPPY"));
        assertEquals(TutoringEmotion.NEUTRAL, TutoringEmotion.fromCode(null));
        assertEquals(TutoringEmotion.NEUTRAL, TutoringEmotion.fromCode(""));
    }

    // ==================== 题型/题类（UNKNOWN 容错） ====================

    @Test
    @DisplayName("TutoringQuestionType 含 UNKNOWN，未知值容错回落")
    void questionType_unknownFallback() {
        assertEquals(TutoringQuestionType.CHOICE, TutoringQuestionType.fromCode("choice"));
        assertEquals(TutoringQuestionType.FILL_BLANK, TutoringQuestionType.fromCode("fill_blank"));
        assertEquals(TutoringQuestionType.SOLUTION, TutoringQuestionType.fromCode("solution"));
        assertEquals(TutoringQuestionType.UNKNOWN, TutoringQuestionType.fromCode("new_type"));
        assertEquals(TutoringQuestionType.UNKNOWN, TutoringQuestionType.fromCode(null));
    }

    @Test
    @DisplayName("TutoringQuestionKind 含 UNKNOWN，未知值容错回落")
    void questionKind_unknownFallback() {
        assertEquals(TutoringQuestionKind.CALCULATION, TutoringQuestionKind.fromCode("calculation"));
        assertEquals(TutoringQuestionKind.APPLICATION, TutoringQuestionKind.fromCode("application"));
        assertEquals(TutoringQuestionKind.PROOF, TutoringQuestionKind.fromCode("proof"));
        assertEquals(TutoringQuestionKind.UNKNOWN, TutoringQuestionKind.fromCode("new_kind"));
        assertEquals(TutoringQuestionKind.UNKNOWN, TutoringQuestionKind.fromCode(null));
    }

    // ==================== TutoringState ====================

    @Test
    @DisplayName("TutoringState 三态容错解析")
    void tutoringState_parsing() {
        assertEquals(TutoringState.ACTIVE, TutoringState.fromCode("ACTIVE"));
        assertEquals(TutoringState.ARCHIVED, TutoringState.fromCode("archived"));
        assertEquals(TutoringState.TERMINATED, TutoringState.fromCode("TERMINATED"));
        assertNull(TutoringState.fromCode("PAUSED"));
        assertNull(TutoringState.fromCode(null));
    }

    // ==================== TutoringConstants ====================

    @Test
    @DisplayName("TutoringConstants 护栏数字与超时")
    void tutoringConstants_values() {
        assertEquals(20, TutoringConstants.SESSION_ROUND_LIMIT);
        assertEquals(2, TutoringConstants.ANSWER_REQUEST_LIMIT);
        assertEquals(3, TutoringConstants.SESSION_CREATE_LIMIT);
        assertEquals(5, TutoringConstants.SESSION_CREATE_WINDOW_MINUTES);
        assertEquals(1, TutoringConstants.AGENT_RETRY);
        assertEquals(Duration.ofSeconds(15), TutoringConstants.DECIDE_TIMEOUT);
        assertEquals(Duration.ofSeconds(60), TutoringConstants.GENERATE_TIMEOUT);
        assertEquals(Duration.ofSeconds(30), TutoringConstants.OCR_TIMEOUT);
    }
}
