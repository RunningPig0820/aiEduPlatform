package com.ai.edu.domain.homework.repository;

import com.ai.edu.domain.homework.model.entity.Homework;
import java.util.List;
import java.util.Optional;

/**
 * 作业仓储接口
 */
public interface HomeworkRepository {

    Homework save(Homework homework);

    Optional<Homework> findById(Long id);

    List<Homework> findByStudentId(Long studentId);

    List<Homework> findByStudentIdAndStatus(Long studentId, String status);

    void deleteById(Long id);
}