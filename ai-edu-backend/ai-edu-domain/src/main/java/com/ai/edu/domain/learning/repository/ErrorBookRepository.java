package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.ErrorBook;
import java.util.List;
import java.util.Optional;

/**
 * 错题仓储接口
 */
public interface ErrorBookRepository {

    ErrorBook save(ErrorBook errorBook);

    Optional<ErrorBook> findById(Long id);

    List<ErrorBook> findByStudentId(Long studentId);

    Optional<ErrorBook> findByStudentIdAndQuestionId(Long studentId, Long questionId);

    List<ErrorBook> findByStudentIdAndIsCorrected(Long studentId, Boolean isCorrected);

    void deleteById(Long id);
}