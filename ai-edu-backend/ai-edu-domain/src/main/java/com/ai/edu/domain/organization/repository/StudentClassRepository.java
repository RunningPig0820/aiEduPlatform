package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.StudentClass;

import java.util.List;
import java.util.Optional;

/**
 * 学生-班级关联仓储接口
 */
public interface StudentClassRepository {

    StudentClass save(StudentClass studentClass);

    Optional<StudentClass> findById(Long id);

    Optional<StudentClass> findByStudentIdAndClassId(Long studentId, Long classId);

    Optional<StudentClass> findActiveByStudentId(Long studentId);

    List<StudentClass> findByClassId(Long classId);

    List<StudentClass> findActiveByClassId(Long classId);

    List<StudentClass> findByStudentId(Long studentId);

    List<StudentClass> findByStatus(String status);

    boolean existsByStudentIdAndClassId(Long studentId, Long classId);

    int countByClassIdAndStatus(Long classId, String status);

    void deleteById(Long id);

    void deleteByStudentId(Long studentId);

    void deleteByClassId(Long classId);
}