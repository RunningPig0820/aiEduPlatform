package com.ai.edu.domain.learning.model.entity;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 题型↔知识点 年级分布桶实体（1:N）。
 *
 * <p>例：鸡兔同笼 → 假设法(4-6,38) + 二元一次方程组(7-8,21)。
 * ratio=该 kp 占比，作为解析管线②年级匹配的先验。kp_uri 非空（每个桶必有归属知识点）。
 */
@Getter
public class QuestionTypeKp {

    private Long id;
    private Long questionTypeId;
    private String kpUri;
    private String gradeRange;
    private Integer hitStudents;
    private Integer hitCount;
    private Double ratio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private QuestionTypeKp() {
    }

    /** 从持久化状态恢复（重新水合），供仓储实现经 PO.toEntity 调用。 */
    public static QuestionTypeKp restore(Long id, Long questionTypeId, String kpUri, String gradeRange,
                                         Integer hitStudents, Integer hitCount, Double ratio,
                                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        QuestionTypeKp kp = new QuestionTypeKp();
        kp.id = id;
        kp.questionTypeId = questionTypeId;
        kp.kpUri = kpUri;
        kp.gradeRange = gradeRange;
        kp.hitStudents = hitStudents == null ? 0 : hitStudents;
        kp.hitCount = hitCount == null ? 0 : hitCount;
        kp.ratio = ratio == null ? 0.0 : ratio;
        kp.createdAt = createdAt;
        kp.updatedAt = updatedAt;
        return kp;
    }

    /** 工厂创建：新建分布桶（初始 0 命中）。 */
    public static QuestionTypeKp create(Long questionTypeId, String kpUri, String gradeRange) {
        QuestionTypeKp kp = new QuestionTypeKp();
        kp.questionTypeId = questionTypeId;
        kp.kpUri = kpUri;
        kp.gradeRange = gradeRange;
        kp.hitStudents = 0;
        kp.hitCount = 0;
        kp.ratio = 0.0;
        kp.createdAt = LocalDateTime.now();
        kp.updatedAt = LocalDateTime.now();
        return kp;
    }

    /** 恢复持久化生成的主键（仓储实现经 PO.toEntity / upsert 调用，沿用全项目 setId 约定）。 */
    public void setId(Long id) {
        this.id = id;
    }
}
