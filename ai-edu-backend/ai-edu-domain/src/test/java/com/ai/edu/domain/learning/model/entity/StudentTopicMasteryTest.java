package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * StudentTopicMastery 题型掌握度规则领域单测（test.md TPM-001/002）。
 */
class StudentTopicMasteryTest {

    @Test
    @DisplayName("create() 初始掌握度为 notStarted=0")
    void create_shouldStartAtZero() {
        StudentTopicMastery mastery = StudentTopicMastery.create(1001L, TopicKey.of("鸡兔同笼"), "鸡兔同笼");
        assertEquals(MasteryLevel.notStarted(), mastery.getMasteryLevel());
        assertEquals("鸡兔同笼", mastery.getTopicKey().getValue());
    }

    @Test
    @DisplayName("applySignal 分值映射 mastered→75/practicing→50/struggling→25")
    void applySignal_shouldMapValues() {
        StudentTopicMastery mastery = StudentTopicMastery.create(1001L, TopicKey.of("鸡兔同笼"), "鸡兔同笼");

        mastery.applySignal(MasterySignal.of("鸡兔同笼", MasterySignal.Level.STRUGGLING));
        assertEquals(MasteryLevel.beginner(), mastery.getMasteryLevel()); // 25

        mastery.applySignal(MasterySignal.of("鸡兔同笼", MasterySignal.Level.PRACTICING));
        assertEquals(MasteryLevel.intermediate(), mastery.getMasteryLevel()); // 50

        mastery.applySignal(MasterySignal.of("鸡兔同笼", MasterySignal.Level.MASTERED));
        assertEquals(MasteryLevel.advanced(), mastery.getMasteryLevel()); // 75
    }

    @Test
    @DisplayName("applySignal 取 max 单调不减（低信号不降分）")
    void applySignal_shouldBeMonotonic() {
        StudentTopicMastery mastery = StudentTopicMastery.create(1001L, TopicKey.of("鸡兔同笼"), "鸡兔同笼");
        mastery.applySignal(MasterySignal.of("鸡兔同笼", MasterySignal.Level.MASTERED)); // 75
        mastery.applySignal(MasterySignal.of("鸡兔同笼", MasterySignal.Level.STRUGGLING)); // 25 不降
        assertEquals(MasteryLevel.advanced(), mastery.getMasteryLevel());
    }

    @Test
    @DisplayName("raiseByCorrection COMPLETED 提升到 75")
    void raiseByCorrection_shouldRaiseTo75() {
        StudentTopicMastery mastery = StudentTopicMastery.create(1001L, TopicKey.of("鸡兔同笼"), "鸡兔同笼");
        mastery.raiseByCorrection();
        assertEquals(MasteryLevel.advanced(), mastery.getMasteryLevel()); // 0 → 75
    }

    @Test
    @DisplayName("recordSession 记录最近会话与证据")
    void recordSession_shouldSetSessionAndEvidence() {
        StudentTopicMastery mastery = StudentTopicMastery.create(1001L, TopicKey.of("鸡兔同笼"), "鸡兔同笼");
        mastery.recordSession(500L, "{\"steps\":[\"round=3\"]}");
        assertEquals(500L, mastery.getLastSessionId());
        assertEquals("{\"steps\":[\"round=3\"]}", mastery.getEvidence());
        assertNotNull(mastery.getUpdatedAt());
    }
}
