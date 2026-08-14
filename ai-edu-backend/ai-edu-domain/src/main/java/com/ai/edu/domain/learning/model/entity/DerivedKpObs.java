package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.DerivedKpSource;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 个体派生观测实体——某学生某题型解析到某知识点的一条观测记录。
 *
 * <p>派生层核心事实：student_id + topic_label → kp_uri（可空，PENDING）。
 * 可修正、可溯源（source/status），同生同题型同 kp 去重计数（occurrence_count）。
 * kp_uri 用 String 而非 {@code KpKey}，因 PENDING 时为空（KpKey 校验非空）。
 */
@Getter
public class DerivedKpObs {

    private Long id;
    private Long studentId;
    private String topicLabel;
    private String kpUri;
    private Integer studentGrade;
    private Integer confidence;
    private DerivedKpSource source;
    private DerivedKpStatus status;
    private Integer occurrenceCount;
    private LocalDateTime firstSeenAt;
    private LocalDateTime updatedAt;

    private DerivedKpObs() {
    }

    /** 从持久化状态恢复（重新水合），供仓储实现经 PO.toEntity 调用。 */
    public static DerivedKpObs restore(Long id, Long studentId, String topicLabel, String kpUri,
                                       Integer studentGrade, Integer confidence,
                                       DerivedKpSource source, DerivedKpStatus status,
                                       Integer occurrenceCount, LocalDateTime firstSeenAt,
                                       LocalDateTime updatedAt) {
        DerivedKpObs obs = new DerivedKpObs();
        obs.id = id;
        obs.studentId = studentId;
        obs.topicLabel = topicLabel;
        obs.kpUri = kpUri;
        obs.studentGrade = studentGrade;
        obs.confidence = confidence;
        obs.source = source;
        obs.status = status;
        obs.occurrenceCount = occurrenceCount == null ? 1 : occurrenceCount;
        obs.firstSeenAt = firstSeenAt;
        obs.updatedAt = updatedAt;
        return obs;
    }

    /** 工厂创建：记录一次解析（首次 occurrence_count=1）。 */
    public static DerivedKpObs create(Long studentId, String topicLabel, String kpUri,
                                      Integer studentGrade, Integer confidence,
                                      DerivedKpSource source, DerivedKpStatus status) {
        DerivedKpObs obs = new DerivedKpObs();
        obs.studentId = studentId;
        obs.topicLabel = topicLabel;
        obs.kpUri = kpUri;
        obs.studentGrade = studentGrade;
        obs.confidence = confidence;
        obs.source = source;
        obs.status = status;
        obs.occurrenceCount = 1;
        obs.firstSeenAt = LocalDateTime.now();
        obs.updatedAt = LocalDateTime.now();
        return obs;
    }

    /** 恢复持久化生成的主键（仓储实现经 PO.toEntity / upsert 调用，沿用全项目 setId 约定）。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 同生再次遇到同题型同 kp：occurrence_count +1（去重，不建新行）。 */
    public void incrementOccurrence() {
        this.occurrenceCount = (this.occurrenceCount == null ? 0 : this.occurrenceCount) + 1;
        this.updatedAt = LocalDateTime.now();
    }
}
