package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.KpKey;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StudentKpMastery 掌握度规则领域单测（test.md 领域单测一节）。
 */
class StudentKpMasteryTest {

    private static final String KP_URI = "http://edukg.org/TextbookKP/1";

    @Test
    @DisplayName("create() 初始掌握度为 notStarted=0")
    void create_shouldStartAtZero() {
        StudentKpMastery mastery = StudentKpMastery.create(1001L, KpKey.of(KP_URI), "二元一次方程组");
        assertEquals(MasteryLevel.notStarted(), mastery.getMasteryLevel());
        assertEquals(KP_URI, mastery.getKpKey().getValue());
    }

    @Test
    @DisplayName("applySignal 分值映射 mastered→75/practicing→50/struggling→25")
    void applySignal_shouldMapValues() {
        StudentKpMastery mastery = StudentKpMastery.create(1001L, KpKey.of(KP_URI), "二元一次方程组");

        mastery.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.STRUGGLING));
        assertEquals(MasteryLevel.beginner(), mastery.getMasteryLevel()); // 25

        mastery.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.PRACTICING));
        assertEquals(MasteryLevel.intermediate(), mastery.getMasteryLevel()); // 50

        mastery.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.MASTERED));
        assertEquals(MasteryLevel.advanced(), mastery.getMasteryLevel()); // 75
    }

    @Test
    @DisplayName("applySignal 取 max 单调不减（低信号不降分）")
    void applySignal_shouldBeMonotonic() {
        StudentKpMastery mastery = StudentKpMastery.create(1001L, KpKey.of(KP_URI), "二元一次方程组");
        mastery.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.MASTERED)); // 75
        mastery.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.STRUGGLING)); // 25 不降
        assertEquals(MasteryLevel.advanced(), mastery.getMasteryLevel());
    }

    @Test
    @DisplayName("applyExplicitCorrection 显式纠正允许下调")
    void applyExplicitCorrection_shouldAllowDown() {
        StudentKpMastery mastery = StudentKpMastery.create(1001L, KpKey.of(KP_URI), "二元一次方程组");
        mastery.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.MASTERED)); // 75
        mastery.applyExplicitCorrection(MasterySignal.of("二元一次方程组", MasterySignal.Level.STRUGGLING));
        assertEquals(MasteryLevel.beginner(), mastery.getMasteryLevel()); // 25
    }

    @Test
    @DisplayName("raiseByCorrection COMPLETED 提升到 75+，不超 100")
    void raiseByCorrection_shouldRaiseTo75Plus() {
        StudentKpMastery mastery = StudentKpMastery.create(1001L, KpKey.of(KP_URI), "二元一次方程组");
        mastery.raiseByCorrection();
        assertEquals(MasteryLevel.advanced(), mastery.getMasteryLevel()); // 0 → 75

        // 已 75 保持
        StudentKpMastery advanced = StudentKpMastery.create(1001L, KpKey.of(KP_URI), "二元一次方程组");
        advanced.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.MASTERED));
        advanced.raiseByCorrection();
        assertEquals(MasteryLevel.advanced(), advanced.getMasteryLevel());

        // 已达 100 保持（上限）
        StudentKpMastery mastered = StudentKpMastery.create(1001L, KpKey.of(KP_URI), "二元一次方程组");
        mastered.applyExplicitCorrection(MasterySignal.of("二元一次方程组", MasterySignal.Level.MASTERED));
        mastered.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.MASTERED));
        mastered.raiseByCorrection();
        assertEquals(75, mastered.getMasteryLevel().getValue());
    }

    @Test
    @DisplayName("recordSession 记录最近会话与证据")
    void recordSession_shouldSetSessionAndEvidence() {
        StudentKpMastery mastery = StudentKpMastery.create(1001L, KpKey.of(KP_URI), "二元一次方程组");
        mastery.recordSession(500L, "{\"steps\":[\"round=3\"]}");
        assertEquals(500L, mastery.getLastSessionId());
        assertEquals("{\"steps\":[\"round=3\"]}", mastery.getEvidence());
        assertNotNull(mastery.getUpdatedAt());
    }
}
