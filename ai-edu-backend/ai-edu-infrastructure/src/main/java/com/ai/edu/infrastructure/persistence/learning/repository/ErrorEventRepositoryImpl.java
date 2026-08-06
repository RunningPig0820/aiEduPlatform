package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.ErrorEvent;
import com.ai.edu.domain.learning.repository.ErrorEventRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.ErrorEventMapper;
import com.ai.edu.infrastructure.persistence.learning.po.ErrorEventPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 答疑错误事件仓储实现（PO ↔ 实体桥接，路由 ai_edu_learning）。
 */
@Repository
public class ErrorEventRepositoryImpl implements ErrorEventRepository {

    @Resource
    private ErrorEventMapper errorEventMapper;

    @Override
    public ErrorEvent save(ErrorEvent errorEvent) {
        ErrorEventPo po = ErrorEventPo.from(errorEvent);
        errorEventMapper.insert(po);
        errorEvent.setId(po.getId());
        return errorEvent;
    }

    @Override
    public List<ErrorEvent> findByStudentId(Long studentId) {
        return ErrorEventPo.toEntityList(errorEventMapper.selectByStudentId(studentId));
    }
}
