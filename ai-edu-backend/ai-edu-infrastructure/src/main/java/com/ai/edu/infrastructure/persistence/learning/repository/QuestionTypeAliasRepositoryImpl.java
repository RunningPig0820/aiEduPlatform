package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;
import com.ai.edu.domain.learning.repository.QuestionTypeAliasRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.QuestionTypeAliasMapper;
import com.ai.edu.infrastructure.persistence.learning.po.QuestionTypeAliasPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 题型库变体别名仓储实现（PO ↔ 实体桥接，UPSERT 幂等）。
 */
@Repository
public class QuestionTypeAliasRepositoryImpl implements QuestionTypeAliasRepository {

    @Resource
    private QuestionTypeAliasMapper questionTypeAliasMapper;

    @Override
    public QuestionTypeAlias upsert(QuestionTypeAlias alias) {
        QuestionTypeAliasPo po = QuestionTypeAliasPo.from(alias);
        questionTypeAliasMapper.upsert(po);
        if (po.getId() != null) {
            alias.setId(po.getId());
        }
        return alias;
    }

    @Override
    public Optional<QuestionTypeAlias> findByAliasLabel(String aliasLabel) {
        QuestionTypeAliasPo po = questionTypeAliasMapper.selectByAliasLabel(aliasLabel);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public List<QuestionTypeAlias> findByQuestionTypeId(Long questionTypeId) {
        return questionTypeAliasMapper.selectByQuestionTypeId(questionTypeId).stream()
                .map(QuestionTypeAliasPo::toEntity)
                .collect(Collectors.toList());
    }
}
