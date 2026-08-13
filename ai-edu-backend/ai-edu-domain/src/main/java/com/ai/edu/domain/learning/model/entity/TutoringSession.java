package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.ActionType;
import com.ai.edu.domain.learning.model.valueobject.EndReason;
import com.ai.edu.domain.learning.model.valueobject.TutoringConstants;
import com.ai.edu.domain.learning.model.valueobject.TutoringEmotion;
import com.ai.edu.domain.learning.model.valueobject.TutoringQuestionKind;
import com.ai.edu.domain.learning.model.valueobject.TutoringQuestionType;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 答疑会话聚合根。
 *
 * <p>仅保留生命周期 3 态 + 护栏计数器（round_count ≤ 20 / answer_request_count），
 * 无流程状态机——答疑流程由 Python agent 上下文承载，Java 不记录题目内容
 * （换题/当前题目判定全在 Python decide，Java 只认 {@link ActionType#SWITCH} 事件重置计数）。
 *
 * <p>持久化映射在 Infrastructure 层 PO（TutoringSessionPO），本类为纯领域对象。
 */
@Getter
public class TutoringSession {

    private Long id;
    private Long studentId;
    private String subject;
    private String title;
    private TutoringQuestionType questionType;
    private TutoringQuestionKind questionKind;
    private String intentCategory;
    private TutoringEmotion lastEmotion;
    private TutoringState status;
    private int roundCount;
    private int answerRequestCount;
    private EndReason endReason;
    private String transcriptUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime archivedAt;

    private TutoringSession() {
    }

    /**
     * 从持久化状态恢复完整聚合根（重新水合）。
     *
     * <p>仓储实现经 PO.toEntity 调用——领域实体除 setId 外不暴露状态写入，
     * 重新水合由领域工厂完成，保证状态一致性由聚合根自身掌控。
     */
    public static TutoringSession restore(Long id, Long studentId, String subject, String title,
                                          TutoringQuestionType questionType, TutoringQuestionKind questionKind,
                                          String intentCategory, TutoringEmotion lastEmotion, TutoringState status,
                                          int roundCount, int answerRequestCount, EndReason endReason,
                                          String transcriptUrl, LocalDateTime createdAt,
                                          LocalDateTime updatedAt, LocalDateTime archivedAt) {
        TutoringSession session = new TutoringSession();
        session.id = id;
        session.studentId = studentId;
        session.subject = subject;
        session.title = title;
        session.questionType = questionType;
        session.questionKind = questionKind;
        session.intentCategory = intentCategory;
        session.lastEmotion = lastEmotion;
        session.status = status;
        session.roundCount = roundCount;
        session.answerRequestCount = answerRequestCount;
        session.endReason = endReason;
        session.transcriptUrl = transcriptUrl;
        session.createdAt = createdAt;
        session.updatedAt = updatedAt;
        session.archivedAt = archivedAt;
        return session;
    }

    /** 发起会话：置 ACTIVE，计数器清零。subject 本期恒为 math。 */
    public static TutoringSession start(Long studentId, String subject) {
        TutoringSession session = new TutoringSession();
        session.studentId = studentId;
        session.subject = (subject == null || subject.isBlank()) ? "math" : subject;
        session.status = TutoringState.ACTIVE;
        session.roundCount = 0;
        session.answerRequestCount = 0;
        session.lastEmotion = TutoringEmotion.NEUTRAL;
        session.createdAt = LocalDateTime.now();
        session.updatedAt = session.createdAt;
        return session;
    }

    public boolean isActive() {
        return status == TutoringState.ACTIVE;
    }

    /** 恢复持久化生成的主键（仓储实现经 PO.toEntity 调用，沿用全项目 setId 约定）。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 会话标题（首条用户消息生成，历史列表展示；应用层生成后设置，随会话落库）。 */
    public void setTitle(String title) {
        this.title = title;
    }

    /** 记录一轮引导（hint/approach 消耗轮次）；已达上限抛异常。 */
    public void recordRound() {
        ensureActive();
        if (this.roundCount >= TutoringConstants.SESSION_ROUND_LIMIT) {
            throw new TutoringRoundLimitException(TutoringConstants.SESSION_ROUND_LIMIT);
        }
        this.roundCount++;
        touch();
    }

    /**
     * 请求答案：递增计数并返回第几次（第 1 次思路 / 第 2 次答案）。
     * 仅在学生表达要答案时调用（答案护栏据此放行/拒绝）。
     */
    public int requestAnswer() {
        ensureActive();
        this.answerRequestCount++;
        touch();
        return this.answerRequestCount;
    }

    /** 换题：仅重置计数（换题判定在 Python decide，Java 不记录题目内容）。 */
    public void switchQuestion() {
        ensureActive();
        this.roundCount = 0;
        this.answerRequestCount = 0;
        touch();
    }

    /** 正常收尾：置 ARCHIVED + endReason + 归档时间。 */
    public void complete(EndReason reason) {
        if (this.status != TutoringState.ACTIVE) {
            throw new IllegalStateException("仅 ACTIVE 会话可收尾，当前: " + this.status);
        }
        this.status = TutoringState.ARCHIVED;
        this.endReason = reason;
        this.archivedAt = LocalDateTime.now();
        touch();
    }

    /** 终止（无关/非数学/安全）：置 TERMINATED + endReason（可空）。 */
    public void terminate(EndReason reason) {
        if (this.status != TutoringState.ACTIVE) {
            throw new IllegalStateException("仅 ACTIVE 会话可终止，当前: " + this.status);
        }
        this.status = TutoringState.TERMINATED;
        this.endReason = reason;
        touch();
    }

    /** 题型/题类分类（decide 输出，用于统计，可空）。 */
    public void setKpClassification(TutoringQuestionType type, TutoringQuestionKind kind) {
        this.questionType = type;
        this.questionKind = kind;
    }

    /** 更新最近一轮情绪（F7，Python 输出方权威）。 */
    public void setLastEmotion(TutoringEmotion emotion) {
        this.lastEmotion = (emotion == null) ? TutoringEmotion.NEUTRAL : emotion;
        touch();
    }

    /** COS 对话归档回填（首次实时写即回填 objectKey）。 */
    public void updateTranscriptUrl(String transcriptUrl) {
        this.transcriptUrl = transcriptUrl;
        touch();
    }

    private void ensureActive() {
        if (!isActive()) {
            throw new IllegalStateException("会话非 ACTIVE 不能执行此操作，当前: " + this.status);
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
