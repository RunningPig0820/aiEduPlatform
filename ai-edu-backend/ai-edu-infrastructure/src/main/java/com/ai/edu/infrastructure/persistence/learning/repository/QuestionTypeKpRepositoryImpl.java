package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.QuestionTypeKpMapper;
import com.ai.edu.infrastructure.persistence.learning.po.QuestionTypeKpPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 题型↔知识点 年级分布桶仓储实现（PO ↔ 实体桥接，UPSERT 幂等）。
 */
@Repository
public class QuestionTypeKpRepositoryImpl implements QuestionTypeKpRepository {

    @Resource
    private QuestionTypeKpMapper questionTypeKpMapper;

    @Override
    public QuestionTypeKp upsert(QuestionTypeKp kp) {
        QuestionTypeKpPo po = QuestionTypeKpPo.from(kp);
        questionTypeKpMapper.upsert(po);
        if (po.getId() != null) {
            kp.setId(po.getId());
        }
        return kp;
    }

    @Override
    public List<QuestionTypeKp> findByQuestionTypeId(Long questionTypeId) {
        return QuestionTypeKpPo.toEntityList(questionTypeKpMapper.selectByQuestionTypeId(questionTypeId));
    }

    @Override
    public List<QuestionTypeKp> findAll() {
        return QuestionTypeKpPo.toEntityList(questionTypeKpMapper.selectAll());
    }
}
