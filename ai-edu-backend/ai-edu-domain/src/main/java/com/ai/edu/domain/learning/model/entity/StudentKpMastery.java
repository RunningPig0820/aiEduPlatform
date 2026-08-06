package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.KpKey;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 学生知识点掌握度实体。
 *
 * <p>以 TextbookKP URI（{@link KpKey}）为 key 幂等落库（student_id + kp_key 唯一）。
 * mastery_level 0-100，复用学习域 {@link MasteryLevel}：常规信号取 max 单调不减，
 * 显式纠正允许下调（例外）；COMPLETED 收尾经 {@link #raiseByCorrection()} 提升到 75+。
 */
@Getter
public class StudentKpMastery {

    private Long id;
    private Long studentId;
    private KpKey kpKey;
    private String kpLabel;
    private MasteryLevel masteryLevel;
    private String evidence;
    private Long lastSessionId;
    private LocalDateTime updatedAt;

    private StudentKpMastery() {
    }

    /** 从持久化状态恢复（重新水合），供仓储实现经 PO.toEntity 调用。 */
    public static StudentKpMastery restore(Long id, Long studentId, KpKey kpKey, String kpLabel,
                                           MasteryLevel masteryLevel, String evidence,
                                           Long lastSessionId, LocalDateTime updatedAt) {
        StudentKpMastery mastery = new StudentKpMastery();
        mastery.id = id;
        mastery.studentId = studentId;
        mastery.kpKey = kpKey;
        mastery.kpLabel = kpLabel;
        mastery.masteryLevel = masteryLevel;
        mastery.evidence = evidence;
        mastery.lastSessionId = lastSessionId;
        mastery.updatedAt = updatedAt;
        return mastery;
    }

    /** 新建掌握度记录（初始 notStarted=0）。 */
    public static StudentKpMastery create(Long studentId, KpKey kpKey, String kpLabel) {
        StudentKpMastery mastery = new StudentKpMastery();
        mastery.studentId = studentId;
        mastery.kpKey = kpKey;
        mastery.kpLabel = kpLabel;
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

    /** 显式纠正例外：允许下调到信号分值（学生明确纠正时覆盖 max 单调性）。 */
    public void applyExplicitCorrection(MasterySignal signal) {
        this.masteryLevel = MasteryLevel.of(signal.getSignal().masteryValue());
        touch();
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
