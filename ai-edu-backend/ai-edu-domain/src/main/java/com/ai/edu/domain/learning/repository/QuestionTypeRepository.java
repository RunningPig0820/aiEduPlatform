package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.QuestionType;

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
}
