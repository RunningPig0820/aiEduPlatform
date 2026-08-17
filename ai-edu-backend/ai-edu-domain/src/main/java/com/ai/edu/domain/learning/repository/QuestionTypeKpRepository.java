package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;

import java.util.List;

/**
 * 题型↔知识点 年级分布桶仓储接口。
 *
 * <p>question_type_id + kp_uri 唯一（UNIQUE uk_type_kp），UPSERT 幂等。
 * 分布桶承载 kp 占比（ratio），供解析管线②年级匹配取先验。
 */
public interface QuestionTypeKpRepository {

    /**
     * UPSERT 分布桶（同题型同知识点唯一，存在则更新统计）。
     */
    QuestionTypeKp upsert(QuestionTypeKp kp);

    /**
     * 按题型主表 ID 查全部分布桶（供解析先验）。
     */
    List<QuestionTypeKp> findByQuestionTypeId(Long questionTypeId);

    /**
     * 查全部分布桶（供聚合变体合并预载题型 kp 签名）。
     */
    List<QuestionTypeKp> findAll();
}
