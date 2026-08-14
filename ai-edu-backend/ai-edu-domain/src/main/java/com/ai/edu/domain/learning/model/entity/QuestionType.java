package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 聚合题型库主表实体——跨学生共现沉淀"知识点的题型"。
 *
 * <p>业务隔离于权威图谱：topic_label 唯一，CANDIDATE → 审核 → STABLE。
 * STABLE 条目的 kp 分布（{@link QuestionTypeKp}）作为解析先验被复用。
 */
@Getter
public class QuestionType {

    private Long id;
    private String topicLabel;
    private QuestionTypeStatus status;
    private String definition;
    private Integer hitStudents;
    private Integer hitCount;
    private Long promotedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private QuestionType() {
    }

    /** 从持久化状态恢复（重新水合），供仓储实现经 PO.toEntity 调用。 */
    public static QuestionType restore(Long id, String topicLabel, QuestionTypeStatus status,
                                       String definition, Integer hitStudents, Integer hitCount,
                                       Long promotedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        QuestionType qt = new QuestionType();
        qt.id = id;
        qt.topicLabel = topicLabel;
        qt.status = status;
        qt.definition = definition;
        qt.hitStudents = hitStudents == null ? 0 : hitStudents;
        qt.hitCount = hitCount == null ? 0 : hitCount;
        qt.promotedBy = promotedBy;
        qt.createdAt = createdAt;
        qt.updatedAt = updatedAt;
        return qt;
    }

    /** 工厂创建：新建候选题型（promoted_by=首个触发学生）。 */
    public static QuestionType create(String topicLabel, QuestionTypeStatus status, Long promotedBy) {
        QuestionType qt = new QuestionType();
        qt.topicLabel = topicLabel;
        qt.status = status;
        qt.hitStudents = 0;
        qt.hitCount = 0;
        qt.promotedBy = promotedBy;
        qt.createdAt = LocalDateTime.now();
        qt.updatedAt = LocalDateTime.now();
        return qt;
    }

    /** 恢复持久化生成的主键（仓储实现经 PO.toEntity / upsert 调用，沿用全项目 setId 约定）。 */
    public void setId(Long id) {
        this.id = id;
    }
}
