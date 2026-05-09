package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.TeacherClass;
import com.ai.edu.domain.organization.model.valueobject.TeacherClassStatus;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 教师-班级关联仓储接口
 */
public interface TeacherClassRepository {

    TeacherClass save(TeacherClass teacherClass);

    Optional<TeacherClass> findById(Long id);

    Optional<TeacherClass> findByTeacherIdAndClassId(UserId teacherId, ClassId classId);

    List<TeacherClass> findByClassId(ClassId classId);

    List<TeacherClass> findActiveByClassId(ClassId classId);

    List<TeacherClass> findByTeacherId(UserId teacherId);

    List<TeacherClass> findActiveByTeacherId(UserId teacherId);

    Optional<TeacherClass> findHeadTeacherByClassId(ClassId classId);

    List<TeacherClass> findByTeacherIdAndSubject(UserId teacherId, String subject);

    boolean existsByTeacherIdAndClassId(UserId teacherId, ClassId classId);

    boolean existsHeadTeacherByClassId(ClassId classId);

    int countByClassIdAndStatus(ClassId classId, TeacherClassStatus status);

    void deleteById(Long id);

    void deleteByTeacherId(UserId teacherId);

    void deleteByClassId(ClassId classId);
}