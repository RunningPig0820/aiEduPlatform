package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.StudentClass;
import com.ai.edu.domain.organization.model.valueobject.StudentClassStatus;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 学生-班级关联仓储接口
 */
public interface StudentClassRepository {

    StudentClass save(StudentClass studentClass);

    Optional<StudentClass> findById(Long id);

    Optional<StudentClass> findByStudentIdAndClassId(UserId studentId, ClassId classId);

    Optional<StudentClass> findActiveByStudentId(UserId studentId);

    List<StudentClass> findByClassId(ClassId classId);

    List<StudentClass> findActiveByClassId(ClassId classId);

    List<StudentClass> findByStudentId(UserId studentId);

    List<StudentClass> findByStatus(StudentClassStatus status);

    boolean existsByStudentIdAndClassId(UserId studentId, ClassId classId);

    int countByClassIdAndStatus(ClassId classId, StudentClassStatus status);

    void deleteById(Long id);

    void deleteByStudentId(UserId studentId);

    void deleteByClassId(ClassId classId);
}