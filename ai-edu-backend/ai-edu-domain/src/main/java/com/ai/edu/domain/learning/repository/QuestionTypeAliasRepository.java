package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;

import java.util.List;
import java.util.Optional;

/**
 * 题型库变体别名仓储接口。
 *
 * <p>alias_label 唯一（UNIQUE uk_alias_label），UPSERT 幂等。变体名 → canonical 题型映射。
 */
public interface QuestionTypeAliasRepository {

    /**
     * UPSERT 别名条目（alias_label 唯一，存在则更新关联题型）。
     */
    QuestionTypeAlias upsert(QuestionTypeAlias alias);

    /**
     * 按变体题型名查别名（供 findByTopicLabelOrAlias 别名兜底）。
     */
    Optional<QuestionTypeAlias> findByAliasLabel(String aliasLabel);

    /**
     * 按 canonical 题型查全部别名（供聚合变体合并 union 重建）。
     */
    List<QuestionTypeAlias> findByQuestionTypeId(Long questionTypeId);
}
