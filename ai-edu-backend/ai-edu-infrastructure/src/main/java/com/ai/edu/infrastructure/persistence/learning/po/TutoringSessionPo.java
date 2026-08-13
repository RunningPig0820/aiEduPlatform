package com.ai.edu.infrastructure.persistence.learning.po;

import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.EndReason;
import com.ai.edu.domain.learning.model.valueobject.TutoringEmotion;
import com.ai.edu.domain.learning.model.valueobject.TutoringQuestionKind;
import com.ai.edu.domain.learning.model.valueobject.TutoringQuestionType;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 答疑会话持久化对象（表：t_tutoring_session，ai_edu_learning 库）。
 *
 * <p>与领域聚合根 {@link TutoringSession} 双向转换：枚举值对象 ↔ 字符串列。
 * 不存题目内容（对话每轮实时整写 COS，Redis 活跃期热存）。
 */
@TableName("t_tutoring_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TutoringSessionPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("subject")
    private String subject;

    @TableField("title")
    private String title;

    @TableField("question_type")
    private String questionType;

    @TableField("question_kind")
    private String questionKind;

    @TableField("intent_category")
    private String intentCategory;

    @TableField("last_emotion")
    private String lastEmotion;

    @TableField("status")
    private String status;

    @TableField("round_count")
    private Integer roundCount;

    @TableField("answer_request_count")
    private Integer answerRequestCount;

    @TableField("end_reason")
    private String endReason;

    @TableField("transcript_url")
    private String transcriptUrl;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("archived_at")
    private LocalDateTime archivedAt;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static TutoringSessionPo from(TutoringSession entity) {
        if (entity == null) {
            return null;
        }
        TutoringSessionPo po = new TutoringSessionPo();
        po.id = entity.getId();
        po.studentId = entity.getStudentId();
        po.subject = entity.getSubject();
        po.title = entity.getTitle();
        po.questionType = entity.getQuestionType() == null ? null : entity.getQuestionType().name();
        po.questionKind = entity.getQuestionKind() == null ? null : entity.getQuestionKind().name();
        po.intentCategory = entity.getIntentCategory();
        po.lastEmotion = entity.getLastEmotion() == null ? null : entity.getLastEmotion().name();
        po.status = entity.getStatus() == null ? null : entity.getStatus().name();
        po.roundCount = entity.getRoundCount();
        po.answerRequestCount = entity.getAnswerRequestCount();
        po.endReason = entity.getEndReason() == null ? null : entity.getEndReason().name();
        po.transcriptUrl = entity.getTranscriptUrl();
        po.createdAt = entity.getCreatedAt();
        po.updatedAt = entity.getUpdatedAt();
        po.archivedAt = entity.getArchivedAt();
        return po;
    }

    public TutoringSession toEntity() {
        return TutoringSession.restore(
                this.id,
                this.studentId,
                this.subject,
                this.title,
                TutoringQuestionType.fromCode(this.questionType),
                TutoringQuestionKind.fromCode(this.questionKind),
                this.intentCategory,
                TutoringEmotion.fromCode(this.lastEmotion),
                TutoringState.fromCode(this.status),
                this.roundCount == null ? 0 : this.roundCount,
                this.answerRequestCount == null ? 0 : this.answerRequestCount,
                EndReason.fromCode(this.endReason),
                this.transcriptUrl,
                this.createdAt,
                this.updatedAt,
                this.archivedAt);
    }

    public static List<TutoringSessionPo> fromList(List<TutoringSession> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(TutoringSessionPo::from).collect(Collectors.toList());
    }

    public static List<TutoringSession> toEntityList(List<TutoringSessionPo> pos) {
        if (pos == null) {
            return null;
        }
        return pos.stream().map(TutoringSessionPo::toEntity).collect(Collectors.toList());
    }
}
