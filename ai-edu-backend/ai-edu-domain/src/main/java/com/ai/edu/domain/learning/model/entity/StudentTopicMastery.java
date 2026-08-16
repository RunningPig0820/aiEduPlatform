package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 学生题型掌握度实体（掌握度主体翻转：题型直接观测）。
 *
 * <p>以归一化题型标识（{@link TopicKey}）为 key 幂等落库（student_id + topic_key 唯一）。
 * mastery_level 0-100，复用学习域 {@link MasteryLevel}：常规信号取 max 单调不减，
 * COMPLETED 收尾经 {@link #raiseByCorrection()} 提升到 75+。
 *
 * <p>知识点掌握度不再直接观测，改为由本实体 + 题型→知识点映射运行时派生（见 KpCoverageAppService）。
 */
@Getter
public class StudentTopicMastery {

    private Long id;
    private Long studentId;
    private TopicKey topicKey;
    private String topicLabel;
    private MasteryLevel masteryLevel;
    private String evidence;
    private Long lastSessionId;
    private LocalDateTime updatedAt;

    private StudentTopicMastery() {
    }

    /** 从持久化状态恢复（重新水合），供仓储实现经 PO.toEntity 调用。 */
    public static StudentTopicMastery restore(Long id, Long studentId, TopicKey topicKey, String topicLabel,
                                              MasteryLevel masteryLevel, String evidence,
                                              Long lastSessionId, LocalDateTime updatedAt) {
        StudentTopicMastery mastery = new StudentTopicMastery();
        mastery.id = id;
        mastery.studentId = studentId;
        mastery.topicKey = topicKey;
        mastery.topicLabel = topicLabel;
        mastery.masteryLevel = masteryLevel;
        mastery.evidence = evidence;
        mastery.lastSessionId = lastSessionId;
        mastery.updatedAt = updatedAt;
        return mastery;
    }

    /** 新建题型掌握度记录（初始 notStarted=0）。 */
    public static StudentTopicMastery create(Long studentId, TopicKey topicKey, String topicLabel) {
        StudentTopicMastery mastery = new StudentTopicMastery();
        mastery.studentId = studentId;
        mastery.topicKey = topicKey;
        mastery.topicLabel = topicLabel;
        mastery.masteryLevel = MasteryLevel.notStarted();
        mastery.updatedAt = LocalDateTime.now();
        return mastery;
    }

    /** 恢复持久化生成的主键（仓储实现经 PO.toEntity / upsert 调用，沿用全项目 setId 约定）。 */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 常规掌握度信号：mastered→75 / practicing→50 / struggling→25，取 max 单调不减
     * （错误只记错误事件，不因 struggling 降分）。
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

    /** 记录最近会话与证据（每次信号落库时更新）。 */
    public void recordSession(Long sessionId, String evidence) {
        this.lastSessionId = sessionId;
        this.evidence = evidence;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
