package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.KpKey;
import com.ai.edu.domain.learning.model.valueobject.TutoringEmotion;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 答疑错误事件实体——引导过程中学生对易错分支的选择/典型误解
 * （eval.correct=false 时写入），形成结构化错误事件。
 */
@Getter
public class ErrorEvent {

    private Long id;
    private Long studentId;
    private Long sessionId;
    private KpKey kpKey;
    private String errorType;
    private TutoringEmotion emotion;
    private Integer stepIndex;
    private String studentAnswer;
    private LocalDateTime createdAt;

    private ErrorEvent() {
    }

    /** 从持久化状态恢复（重新水合），供仓储实现经 PO.toEntity 调用。 */
    public static ErrorEvent restore(Long id, Long studentId, Long sessionId, KpKey kpKey,
                                     String errorType, TutoringEmotion emotion,
                                     Integer stepIndex, String studentAnswer, LocalDateTime createdAt) {
        ErrorEvent event = new ErrorEvent();
        event.id = id;
        event.studentId = studentId;
        event.sessionId = sessionId;
        event.kpKey = kpKey;
        event.errorType = errorType;
        event.emotion = emotion;
        event.stepIndex = stepIndex;
        event.studentAnswer = studentAnswer;
        event.createdAt = createdAt;
        return event;
    }

    /** 恢复持久化生成的主键（仓储实现经 PO.toEntity / save 调用，沿用全项目 setId 约定）。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 工厂创建：记录出错轮次的学生原答、错误类型、情绪（F7）。 */
    public static ErrorEvent create(Long studentId, Long sessionId, KpKey kpKey,
                                    String errorType, TutoringEmotion emotion,
                                    Integer stepIndex, String studentAnswer) {
        ErrorEvent event = new ErrorEvent();
        event.studentId = studentId;
        event.sessionId = sessionId;
        event.kpKey = kpKey;
        event.errorType = errorType;
        event.emotion = (emotion == null) ? TutoringEmotion.NEUTRAL : emotion;
        event.stepIndex = stepIndex;
        event.studentAnswer = studentAnswer;
        event.createdAt = LocalDateTime.now();
        return event;
    }
}
