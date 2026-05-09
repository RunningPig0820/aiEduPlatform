package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.model.valueobject.ClassStatus;
import com.ai.edu.domain.organization.model.valueobject.GradeLevel;
import com.ai.edu.domain.organization.model.valueobject.SchoolYear;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.SchoolId;

import java.util.List;
import java.util.Optional;

/**
 * 班级仓储接口
 */
public interface ClassRepository {

    Class save(Class classEntity);

    Optional<Class> findById(ClassId id);

    Optional<Class> findByCode(String code);

    List<Class> findBySchoolId(SchoolId schoolId);

    List<Class> findByGrade(GradeLevel grade);

    List<Class> findBySchoolYear(SchoolYear schoolYear);

    List<Class> findByStatus(ClassStatus status);

    List<Class> findActiveBySchoolId(SchoolId schoolId);

    Optional<Class> findActiveById(ClassId id);

    boolean existsByNameAndSchoolYear(String name, SchoolYear schoolYear);

    int countBySchoolIdAndStatus(SchoolId schoolId, ClassStatus status);

    void deleteById(ClassId id);

    void restoreById(ClassId id);
}