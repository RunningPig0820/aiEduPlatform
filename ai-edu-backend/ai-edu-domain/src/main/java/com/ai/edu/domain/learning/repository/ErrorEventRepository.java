package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.ErrorEvent;

import java.util.List;

/**
 * 答疑错误事件仓储接口（结构化错误事件，用于薄弱点分析/错题集联动）。
 */
public interface ErrorEventRepository {

    /**
     * 保存错误事件。
     */
    ErrorEvent save(ErrorEvent errorEvent);

    /**
     * 按学生查错误历史/趋势。
     */
    List<ErrorEvent> findByStudentId(Long studentId);
}
