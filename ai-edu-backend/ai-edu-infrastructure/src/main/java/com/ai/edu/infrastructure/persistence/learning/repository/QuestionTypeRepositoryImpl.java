package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.QuestionTypeMapper;
import com.ai.edu.infrastructure.persistence.learning.po.QuestionTypePo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 聚合题型库主表仓储实现（PO ↔ 实体桥接，UPSERT 幂等）。
 */
@Repository
public class QuestionTypeRepositoryImpl implements QuestionTypeRepository {

    @Resource
    private QuestionTypeMapper questionTypeMapper;

    @Override
    public QuestionType upsert(QuestionType questionType) {
        QuestionTypePo po = QuestionTypePo.from(questionType);
        questionTypeMapper.upsert(po);
        if (po.getId() != null) {
            questionType.setId(po.getId());
        }
        return questionType;
    }

    @Override
    public Optional<QuestionType> findByTopicLabel(String topicLabel) {
        QuestionTypePo po = questionTypeMapper.selectByTopicLabel(topicLabel);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public Optional<QuestionType> findByTopicLabelOrAlias(String topicLabel) {
        QuestionTypePo po = questionTypeMapper.selectByTopicLabelOrAlias(topicLabel);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public Optional<QuestionType> findById(Long id) {
        QuestionTypePo po = questionTypeMapper.selectActiveById(id);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public List<QuestionType> findPage(int offset, int limit) {
        return QuestionTypePo.toEntityList(questionTypeMapper.selectPage(offset, limit));
    }

    @Override
    public long count() {
        return questionTypeMapper.count();
    }

    @Override
    public List<String> findTopTopicLabels(int limit) {
        return questionTypeMapper.selectTopTopicLabels(limit);
    }

    @Override
    public List<QuestionType> findAll() {
        return QuestionTypePo.toEntityList(questionTypeMapper.selectAll());
    }
}
