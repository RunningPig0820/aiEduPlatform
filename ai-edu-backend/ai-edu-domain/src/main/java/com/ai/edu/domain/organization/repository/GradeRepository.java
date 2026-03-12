package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.Grade;

import java.util.List;
import java.util.Optional;

/**
 * 年级仓储接口
 */
public interface GradeRepository {

    Grade save(Grade grade);

    Optional<Grade> findById(Long id);

    Optional<Grade> findByCode(String code);

    List<Grade> findBySchoolId(Long schoolId);

    List<Grade> findByGradeLevel(Integer gradeLevel);

    List<Grade> findAllActive();

    boolean existsByCode(String code);

    void deleteById(Long id);

    void restoreById(Long id);
}