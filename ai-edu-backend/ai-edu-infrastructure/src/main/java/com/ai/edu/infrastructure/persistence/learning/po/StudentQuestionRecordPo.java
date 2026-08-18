package com.ai.edu.infrastructure.persistence.learning.po;

import com.ai.edu.domain.learning.model.entity.StudentQuestionRecord;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生题目记录持久化对象（表：t_student_question_record，ai_edu_learning 库）。
 *
 * <p>掌握度事实源：一条作答一条记录。canonical 可空（题型未识别 PENDING，信号照常采集）。
 * score 为生效分值（0.00/0.50/1.00，含 per-题型打折后），与掌握表累计平均聚合同源。
 */
@TableName("t_student_question_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentQuestionRecordPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("content")
    private String content;

    @TableField("source")
    private String source;

    @TableField("topic_label")
    private String topicLabel;

    @TableField("canonical_label")
    private String canonicalLabel;

    @TableField("score")
    private BigDecimal score;

    @TableField("hint_count")
    private Integer hintCount;

    @TableField("answer_request_count")
    private Integer answerRequestCount;

    @TableField("session_id")
    private Long sessionId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static StudentQuestionRecordPo from(StudentQuestionRecord entity) {
        if (entity == null) {
            return null;
        }
        StudentQuestionRecordPo po = new StudentQuestionRecordPo();
        po.id = entity.getId();
        po.studentId = entity.getStudentId();
        po.content = entity.getContent();
        po.source = entity.getSource();
        po.topicLabel = entity.getTopicLabel();
        po.canonicalLabel = entity.getCanonicalLabel();
        po.score = entity.getScore();
        po.hintCount = entity.getHintCount();
        po.answerRequestCount = entity.getAnswerRequestCount();
        po.sessionId = entity.getSessionId();
        po.createdAt = entity.getCreatedAt();
        po.updatedAt = entity.getCreatedAt();
        return po;
    }

    public StudentQuestionRecord toEntity() {
        return StudentQuestionRecord.restore(
                this.id, this.studentId, this.content, this.source, this.topicLabel,
                this.canonicalLabel, this.score, this.hintCount, this.answerRequestCount,
                this.sessionId, this.createdAt);
    }

    public static List<StudentQuestionRecordPo> fromList(List<StudentQuestionRecord> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(StudentQuestionRecordPo::from).collect(Collectors.toList());
    }

    public static List<StudentQuestionRecord> toEntityList(List<StudentQuestionRecordPo> pos) {
        if (pos == null) {
            return null;
        }
        return pos.stream().map(StudentQuestionRecordPo::toEntity).collect(Collectors.toList());
    }
}