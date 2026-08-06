package com.ai.edu.infrastructure.persistence.learning.po;

import com.ai.edu.domain.learning.model.entity.ErrorEvent;
import com.ai.edu.domain.learning.model.valueobject.KpKey;
import com.ai.edu.domain.learning.model.valueobject.TutoringEmotion;
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
 * 答疑错误事件持久化对象（表：t_tutoring_error_event，ai_edu_learning 库）。
 *
 * <p>记录引导过程中学生对易错分支的选择/典型误解（eval.correct=false 时写入）。
 * emotion 存该轮情绪（F7 七态）。
 */
@TableName("t_tutoring_error_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorEventPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("session_id")
    private Long sessionId;

    @TableField("kp_key")
    private String kpKey;

    @TableField("error_type")
    private String errorType;

    @TableField("emotion")
    private String emotion;

    @TableField("step_index")
    private Integer stepIndex;

    @TableField("student_answer")
    private String studentAnswer;

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

    public static ErrorEventPo from(ErrorEvent entity) {
        if (entity == null) {
            return null;
        }
        ErrorEventPo po = new ErrorEventPo();
        po.id = entity.getId();
        po.studentId = entity.getStudentId();
        po.sessionId = entity.getSessionId();
        po.kpKey = entity.getKpKey() == null ? null : entity.getKpKey().getValue();
        po.errorType = entity.getErrorType();
        po.emotion = entity.getEmotion() == null ? null : entity.getEmotion().name();
        po.stepIndex = entity.getStepIndex();
        po.studentAnswer = entity.getStudentAnswer();
        po.createdAt = entity.getCreatedAt();
        return po;
    }

    public ErrorEvent toEntity() {
        KpKey key = this.kpKey == null || this.kpKey.isBlank() ? null : KpKey.of(this.kpKey);
        return ErrorEvent.restore(
                this.id, this.studentId, this.sessionId, key,
                this.errorType, TutoringEmotion.fromCode(this.emotion),
                this.stepIndex, this.studentAnswer, this.createdAt);
    }

    public static List<ErrorEventPo> fromList(List<ErrorEvent> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(ErrorEventPo::from).collect(Collectors.toList());
    }

    public static List<ErrorEvent> toEntityList(List<ErrorEventPo> pos) {
        if (pos == null) {
            return null;
        }
        return pos.stream().map(ErrorEventPo::toEntity).collect(Collectors.toList());
    }
}
