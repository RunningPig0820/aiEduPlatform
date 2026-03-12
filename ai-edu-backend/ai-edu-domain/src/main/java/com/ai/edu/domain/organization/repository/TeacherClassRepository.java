package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.TeacherClass;

import java.util.List;
import java.util.Optional;

/**
 * 老师-班级关联仓储接口
 */
public interface TeacherClassRepository {

    TeacherClass save(TeacherClass teacherClass);

    Optional<TeacherClass> findById(Long id);

    Optional<TeacherClass> findByTeacherIdAndClassId(Long teacherId, Long classId);

    List<TeacherClass> findByClassId(Long classId);

    List<TeacherClass> findActiveByClassId(Long classId);

    List<TeacherClass> findByTeacherId(Long teacherId);

    List<TeacherClass> findActiveByTeacherId(Long teacherId);

    Optional<TeacherClass> findHeadTeacherByClassId(Long classId);

    List<TeacherClass> findByTeacherIdAndSubject(Long teacherId, String subject);

    boolean existsByTeacherIdAndClassId(Long teacherId, Long classId);

    boolean existsHeadTeacherByClassId(Long classId);

    int countByClassIdAndStatus(Long classId, String status);

    void deleteById(Long id);

    void deleteByTeacherId(Long teacherId);

    void deleteByClassId(Long classId);
}