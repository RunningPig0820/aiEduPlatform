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
}
