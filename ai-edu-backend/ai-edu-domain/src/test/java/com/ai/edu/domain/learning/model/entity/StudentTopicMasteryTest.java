package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    // ===== 累计平均正确率（test.md AGG-001~002，替代 max 单调不减） =====

    @Test
    @DisplayName("create() 初始 trainCount=0 / source=ai")
    void create_shouldStartWithZeroTrainCount() {
        StudentTopicMastery mastery = StudentTopicMastery.create(1001L, TopicKey.of("鸡兔同笼"), "鸡兔同笼");
        assertEquals(0, mastery.getTrainCount());
        assertEquals("ai", mastery.getSource());
    }

    @Test
    @DisplayName("applyScore 累计平均：60% 练 9 道，直接答对 1.0 → 64%，trainCount=10")
    void applyScore_shouldCumulativeAverage() {
        // 旧值 60 + trainCount=9（历史迁移：旧 mastery_level 作初始正确率、train_count=1 平滑）
        StudentTopicMastery mastery = StudentTopicMastery.restore(1L, 1001L, TopicKey.of("鸡兔同笼"), "鸡兔同笼",
                MasteryLevel.of(60), "ai", 9, LocalDateTime.now());

        mastery.applyScore(BigDecimal.valueOf(1.00)); // 直接答对

        // new = 60×9/10 + 100×1/10 = 54 + 10 = 64
        assertEquals(64, mastery.getMasteryLevel().getValue());
        assertEquals(10, mastery.getTrainCount());
    }

    @Test
    @DisplayName("applyScore 首题建锚：0 训练 + score 生效分值 → 生效分值百分比")
    void applyScore_shouldSeedFirstScore() {
        StudentTopicMastery mastery = StudentTopicMastery.create(1001L, TopicKey.of("鸡兔同笼"), "鸡兔同笼");
        mastery.applyScore(BigDecimal.valueOf(0.50)); // 引导后答对（打折后的生效分值）

        assertEquals(50, mastery.getMasteryLevel().getValue());
        assertEquals(1, mastery.getTrainCount());
    }

    @Test
    @DisplayName("applyScore 答错拉低：64% 练 10 道答错 0.0 → 58%（累计平均 vs max 单调不减的核心差异）")
    void applyScore_shouldDropOnWrong() {
        StudentTopicMastery mastery = StudentTopicMastery.restore(1L, 1001L, TopicKey.of("鸡兔同笼"), "鸡兔同笼",
                MasteryLevel.of(64), "ai", 10, LocalDateTime.now());

        mastery.applyScore(BigDecimal.valueOf(0.00)); // 答错

        // new = 64×10/11 + 0×1/11 ≈ 58.18 → 58
        assertEquals(58, mastery.getMasteryLevel().getValue());
        assertEquals(11, mastery.getTrainCount());
    }
}
