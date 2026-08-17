package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.QuestionType;

import java.util.List;
import java.util.Optional;

/**
 * 聚合题型库主表仓储接口。
 *
 * <p>topic_label 唯一（UNIQUE uk_topic_label），UPSERT 幂等。CANDIDATE → STABLE。
 */
public interface QuestionTypeRepository {

    /**
     * UPSERT 题型条目（topic_label 唯一，存在则更新统计/状态）。
     */
    QuestionType upsert(QuestionType questionType);

    /**
     * 按题型 label 查条目。
     */
    Optional<QuestionType> findByTopicLabel(String topicLabel);

    /**
     * 按 canonical 或别名命中题型（变体名 → canonical，LEFT JOIN 别名表，canonical 优先）。
     */
    Optional<QuestionType> findByTopicLabelOrAlias(String topicLabel);

    /**
     * 按主键查条目。
     */
    Optional<QuestionType> findById(Long id);

    /**
     * 分页列题型（按 id 升序）。
     */
    List<QuestionType> findPage(int offset, int limit);

    /**
     * 题型总数（分页 total）。
     */
    long count();

    /**
     * 按命中数降序取常用题型名（供题目理解参考词表注入，收敛 LLM 命名）。
     */
    List<String> findTopTopicLabels(int limit);

    /**
     * 查全部题型条目（供聚合变体合并预载 kp 签名比对）。
     */
    List<QuestionType> findAll();
}
