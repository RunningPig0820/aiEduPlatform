package com.ai.edu.domain.learning.model.entity;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 题型库变体别名实体——相似题型名（「鸡兔同笼」vs「鸡兔同笼问题」）收敛到 canonical 题型的映射。
 *
 * <p>变体名经聚合 kp 分布重叠判定后并入 canonical {@link QuestionType}，记录为别名；
 * 解析管线②/投票/聚合查询经别名命中同一 canonical 条目（findByTopicLabelOrAlias）。
 * 业务隔离于权威图谱，只存 MySQL ai_edu_learning。
 */
@Getter
public class QuestionTypeAlias {

    private Long id;
    private String aliasLabel;
    private Long questionTypeId;
    private LocalDateTime createdAt;

    private QuestionTypeAlias() {
    }

    /** 从持久化状态恢复（重新水合），供仓储实现经 PO.toEntity 调用。 */
    public static QuestionTypeAlias restore(Long id, String aliasLabel, Long questionTypeId, LocalDateTime createdAt) {
        QuestionTypeAlias alias = new QuestionTypeAlias();
        alias.id = id;
        alias.aliasLabel = aliasLabel;
        alias.questionTypeId = questionTypeId;
        alias.createdAt = createdAt;
        return alias;
    }

    /** 工厂创建：变体题型名 → canonical 题型。 */
    public static QuestionTypeAlias create(String aliasLabel, Long questionTypeId) {
        QuestionTypeAlias alias = new QuestionTypeAlias();
        alias.aliasLabel = aliasLabel;
        alias.questionTypeId = questionTypeId;
        alias.createdAt = LocalDateTime.now();
        return alias;
    }

    /** 恢复持久化生成的主键（仓储实现经 PO.toEntity / upsert 调用，沿用全项目 setId 约定）。 */
    public void setId(Long id) {
        this.id = id;
    }
}
