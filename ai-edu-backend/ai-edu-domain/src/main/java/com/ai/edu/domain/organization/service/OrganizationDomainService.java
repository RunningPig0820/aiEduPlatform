package com.ai.edu.domain.organization.service;

import com.ai.edu.domain.organization.model.aggregate.ClassAggregate;
import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.model.entity.School;
import com.ai.edu.domain.organization.model.entity.StudentClass;
import com.ai.edu.domain.organization.model.entity.TeacherClass;
import com.ai.edu.domain.organization.repository.ClassRepository;
import com.ai.edu.domain.organization.repository.SchoolRepository;
import com.ai.edu.domain.organization.repository.StudentClassRepository;
import com.ai.edu.domain.organization.repository.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 组织领域服务
 * 处理学校和班级相关的复杂业务逻辑
 */
@Service
@RequiredArgsConstructor
public class OrganizationDomainService {

    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;
    private final StudentClassRepository studentClassRepository;
    private final TeacherClassRepository teacherClassRepository;

    // ==================== 学校相关 ====================

    /**
     * 创建学校
     */
    public School createSchool(String name, String code, String schoolType) {
        if (schoolRepository.existsByCode(code)) {
            throw new IllegalArgumentException("School code already exists: " + code);
        }
        School school = School.create(name, code, schoolType);
        return schoolRepository.save(school);
    }

    /**
     * 获取学校及其班级
     */
    public List<Class> getSchoolClasses(Long schoolId) {
        return classRepository.findActiveBySchoolId(schoolId);
    }

    // ==================== 班级相关 ====================

    /**
     * 创建班级
     */
    public Class createClass(String name, String grade, String schoolYear, Long schoolId) {
        if (classRepository.existsByNameAndSchoolYear(name, schoolYear)) {
            throw new IllegalArgumentException("Class already exists with name: " + name + " in school year: " + schoolYear);
        }
        Class classEntity = Class.createWithSchool(name, grade, schoolYear, schoolId);
        return classRepository.save(classEntity);
    }

    /**
     * 获取班级聚合根
     */
    public ClassAggregate getClassAggregate(Long classId) {
        Class classEntity = classRepository.findActiveById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));

        ClassAggregate aggregate = new ClassAggregate(classEntity);

        // 加载学生关联
        List<StudentClass> studentClasses = studentClassRepository.findActiveByClassId(classId);
        studentClasses.forEach(sc -> aggregate.getStudentClasses().add(sc));

        // 加载老师关联
        List<TeacherClass> teacherClasses = teacherClassRepository.findActiveByClassId(classId);
        teacherClasses.forEach(tc -> aggregate.getTeacherClasses().add(tc));

        return aggregate;
    }

    /**
     * 班级毕业
     */
    public void graduateClass(Long classId) {
        ClassAggregate aggregate = getClassAggregate(classId);
        aggregate.graduate();
        classRepository.save(aggregate.getClassEntity());

        // 更新学生状态
        studentClassRepository.findActiveByClassId(classId)
                .forEach(sc -> {
                    sc.graduate();
                    studentClassRepository.save(sc);
                });
    }

    // ==================== 学生班级关联 ====================

    /**
     * 学生加入班级
     */
    public StudentClass addStudentToClass(Long studentId, Long classId, String studentNo) {
        Class classEntity = classRepository.findActiveById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found or inactive: " + classId));

        if (studentClassRepository.existsByStudentIdAndClassId(studentId, classId)) {
            throw new IllegalArgumentException("Student already in class");
        }

        StudentClass studentClass = StudentClass.createWithNo(studentId, classId, studentNo);
        return studentClassRepository.save(studentClass);
    }

    /**
     * 学生转班
     */
    public void transferStudent(Long studentId, Long fromClassId, Long toClassId, String newStudentNo) {
        // 从原班级移除
        studentClassRepository.findByStudentIdAndClassId(studentId, fromClassId)
                .ifPresent(sc -> {
                    sc.transfer();
                    studentClassRepository.save(sc);
                });

        // 加入新班级
        addStudentToClass(studentId, toClassId, newStudentNo);
    }

    /**
     * 获取学生的当前班级
     */
    public Optional<StudentClass> getStudentCurrentClass(Long studentId) {
        return studentClassRepository.findActiveByStudentId(studentId);
    }

    /**
     * 获取班级的学生列表
     */
    public List<StudentClass> getClassStudents(Long classId) {
        return studentClassRepository.findActiveByClassId(classId);
    }

    /**
     * 统计班级学生数
     */
    public int getClassStudentCount(Long classId) {
        return studentClassRepository.countByClassIdAndStatus(classId, "ACTIVE");
    }

    // ==================== 老师班级关联 ====================

    /**
     * 老师加入班级
     */
    public TeacherClass addTeacherToClass(Long teacherId, Long classId, String subject) {
        Class classEntity = classRepository.findActiveById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found or inactive: " + classId));

        if (teacherClassRepository.existsByTeacherIdAndClassId(teacherId, classId)) {
            throw new IllegalArgumentException("Teacher already in class");
        }

        TeacherClass teacherClass = TeacherClass.create(teacherId, classId, subject);
        return teacherClassRepository.save(teacherClass);
    }

    /**
     * 设置班主任
     */
    public TeacherClass setHeadTeacher(Long teacherId, Long classId, String subject) {
        // 先移除现任班主任
        teacherClassRepository.findHeadTeacherByClassId(classId)
                .ifPresent(tc -> {
                    tc.removeHeadTeacher();
                    teacherClassRepository.save(tc);
                });

        // 添加或更新班主任
        TeacherClass teacherClass = teacherClassRepository.findByTeacherIdAndClassId(teacherId, classId)
                .orElseGet(() -> TeacherClass.createAsHeadTeacher(teacherId, classId, subject));

        if (!teacherClass.isHeadTeacher()) {
            teacherClass.assignAsHeadTeacher();
        }

        return teacherClassRepository.save(teacherClass);
    }

    /**
     * 获取班级的班主任
     */
    public Optional<TeacherClass> getHeadTeacher(Long classId) {
        return teacherClassRepository.findHeadTeacherByClassId(classId);
    }

    /**
     * 获取老师任教的所有班级
     */
    public List<TeacherClass> getTeacherClasses(Long teacherId) {
        return teacherClassRepository.findActiveByTeacherId(teacherId);
    }

    /**
     * 获取班级的老师列表
     */
    public List<TeacherClass> getClassTeachers(Long classId) {
        return teacherClassRepository.findActiveByClassId(classId);
    }

    /**
     * 移除老师
     */
    public void removeTeacherFromClass(Long teacherId, Long classId) {
        teacherClassRepository.findByTeacherIdAndClassId(teacherId, classId)
                .ifPresent(tc -> {
                    tc.deactivate();
                    teacherClassRepository.save(tc);
                });
    }
}