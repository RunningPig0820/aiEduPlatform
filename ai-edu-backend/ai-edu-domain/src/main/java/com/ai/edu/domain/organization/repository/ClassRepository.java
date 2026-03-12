package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.Class;

import java.util.List;
import java.util.Optional;

/**
 * 班级仓储接口
 */
public interface ClassRepository {

    Class save(Class classEntity);

    Optional<Class> findById(Long id);

    Optional<Class> findByCode(String code);

    List<Class> findBySchoolId(Long schoolId);

    List<Class> findByGrade(String grade);

    List<Class> findBySchoolYear(String schoolYear);

    List<Class> findByStatus(String status);

    List<Class> findActiveBySchoolId(Long schoolId);

    Optional<Class> findActiveById(Long id);

    boolean existsByNameAndSchoolYear(String name, String schoolYear);

    int countBySchoolIdAndStatus(Long schoolId, String status);

    void deleteById(Long id);

    void restoreById(Long id);
}