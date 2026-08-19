package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生题型掌握度实体（掌握度主体翻转：题型直接观测）。
 *
 * <p>以归一化题型标识（{@link TopicKey}）为 key 幂等落库（student_id + topic_key 唯一）。
 * mastery_level 0-100，**连续百分比（累计平均正确率）**：{@link #applyScore(BigDecimal)} 每次作答
 * 累计平均（new = old×n/(n+1) + score×1/(n+1)，score 为生效分值 0.0/0.5/1.0 × 打折），trainCount 递增。
 * 替代旧 max 单调不减（备用信号 {@link #applySignal(MasterySignal)} 保留至聚合改写任务 3.4 移除）。
 *
 * <p>知识点覆盖率按需由「本实体（题型掌握度）× 题型→知识点映射（t_kp_question_type_kp）」派生，不直接观测。
 */
@Getter
public class StudentTopicMastery {

    private Long id;
    private Long studentId;
    private TopicKey topicKey;
    private String topicLabel;
    private MasteryLevel masteryLevel;
    private String source;
    private long trainCount;
    private LocalDateTime updatedAt;

    private StudentTopicMastery() {
    }

    /** 从持久化状态恢复（重新水合），供仓储实现经 PO.toEntity 调用。 */
    public static StudentTopicMastery restore(Long id, Long studentId, TopicKey topicKey, String topicLabel,
                                              MasteryLevel masteryLevel, String source,
                                              long trainCount, LocalDateTime updatedAt) {
        StudentTopicMastery mastery = new StudentTopicMastery();
        mastery.id = id;
        mastery.studentId = studentId;
        mastery.topicKey = topicKey;
        mastery.topicLabel = topicLabel;
        mastery.masteryLevel = masteryLevel;
        mastery.source = source == null ? "ai" : source;
        mastery.trainCount = trainCount;
        mastery.updatedAt = updatedAt;
        return mastery;
    }

    /** 新建题型掌握度记录（初始 notStarted=0，source=ai，trainCount=0）。 */
    public static StudentTopicMastery create(Long studentId, TopicKey topicKey, String topicLabel) {
        return restore(null, studentId, topicKey, topicLabel, MasteryLevel.notStarted(),
                "ai", 0, LocalDateTime.now());
    }

    /** 恢复持久化生成的主键（仓储实现经 PO.toEntity / upsert 调用，沿用全项目 setId 约定）。 */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 累计平均正确率：new = old×n/(n+1) + score×1/(n+1)，trainCount += 1。
     *
     * <p>score 为**生效分值**（0.00/0.50/1.00，含 per-题型打折后；答错/未完成 0.0），
     * 与题目记录（{@link StudentQuestionRecord}）同源可追溯。一次作答算一次（不做题目去重，
     * 同题做两次计两次训练量）。可解释「某题型练 10 道对 6 道 = 64%」，替代 max 单调不减。
     */
    public void applyScore(BigDecimal score) {
        double scorePct = score == null ? 0d : score.doubleValue() * 100d;
        double accumulated = (this.masteryLevel.getValue() * (double) this.trainCount + scorePct)
                / (this.trainCount + 1);
        this.masteryLevel = MasteryLevel.of((int) Math.round(accumulated));
        this.trainCount += 1;
        touch();
    }

    /**
     * 常规掌握度信号：mastered→75 / practicing→50 / struggling→25，取 max 单调不减
     * （错误只记错误事件，不因 struggling 降分）。——**待移除**（3.4 聚合改 write 后替换为 applyScore）。
     */
    public void applySignal(MasterySignal signal) {
        int signalValue = signal.getSignal().masteryValue();
        if (signalValue > this.masteryLevel.getValue()) {
            this.masteryLevel = MasteryLevel.of(signalValue);
            touch();
        }
    }

    /** COMPLETED 收尾校正：提升到 75+（不低于 75；已有更高保持）。 */
    public void raiseByCorrection() {
        if (this.masteryLevel.getValue() < 75) {
            this.masteryLevel = MasteryLevel.advanced();
            touch();
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
