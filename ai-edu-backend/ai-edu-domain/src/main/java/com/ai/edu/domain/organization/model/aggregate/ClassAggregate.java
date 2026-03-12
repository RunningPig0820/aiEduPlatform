package com.ai.edu.domain.organization.model.aggregate;

import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.model.entity.StudentClass;
import com.ai.edu.domain.organization.model.entity.TeacherClass;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 班级聚合根
 * 管理班级及其学生、老师关联
 */
@Getter
public class ClassAggregate {

    private final Class classEntity;
    private final List<StudentClass> studentClasses;
    private final List<TeacherClass> teacherClasses;

    public ClassAggregate(Class classEntity) {
        this.classEntity = classEntity;
        this.studentClasses = new ArrayList<>();
        this.teacherClasses = new ArrayList<>();
    }

    public static ClassAggregate create(String name, String grade, String schoolYear) {
        Class classEntity = Class.create(name, grade, schoolYear);
        return new ClassAggregate(classEntity);
    }

    public static ClassAggregate createWithSchool(String name, String grade, String schoolYear, Long schoolId) {
        Class classEntity = Class.createWithSchool(name, grade, schoolYear, schoolId);
        return new ClassAggregate(classEntity);
    }

    public Long getId() {
        return classEntity.getId();
    }

    public String getName() {
        return classEntity.getName();
    }

    public String getGrade() {
        return classEntity.getGrade();
    }

    public String getSchoolYear() {
        return classEntity.getSchoolYear();
    }

    public String getStatus() {
        return classEntity.getStatus();
    }

    public boolean isActive() {
        return classEntity.isActive();
    }

    // 学生管理
    public void addStudent(Long studentId, String studentNo) {
        if (!isActive()) {
            throw new IllegalStateException("Cannot add student to inactive class");
        }
        if (isStudentInClass(studentId)) {
            throw new IllegalArgumentException("Student already in class: " + studentId);
        }
        StudentClass sc = StudentClass.createWithNo(studentId, classEntity.getId(), studentNo);
        studentClasses.add(sc);
    }

    public void removeStudent(Long studentId) {
        studentClasses.stream()
                .filter(sc -> sc.getStudentId().equals(studentId) && sc.isActive())
                .findFirst()
                .ifPresent(StudentClass::transfer);
    }

    public boolean isStudentInClass(Long studentId) {
        return studentClasses.stream()
                .anyMatch(sc -> sc.getStudentId().equals(studentId) && sc.isActive());
    }

    public int getActiveStudentCount() {
        return (int) studentClasses.stream()
                .filter(StudentClass::isActive)
                .count();
    }

    // 老师管理
    public void addTeacher(Long teacherId, String subject) {
        if (!isActive()) {
            throw new IllegalStateException("Cannot add teacher to inactive class");
        }
        if (isTeacherInClass(teacherId)) {
            throw new IllegalArgumentException("Teacher already in class: " + teacherId);
        }
        TeacherClass tc = TeacherClass.create(teacherId, classEntity.getId(), subject);
        teacherClasses.add(tc);
    }

    public void addHeadTeacher(Long teacherId, String subject) {
        if (!isActive()) {
            throw new IllegalStateException("Cannot add head teacher to inactive class");
        }
        // 移除现任班主任
        teacherClasses.stream()
                .filter(TeacherClass::isHeadTeacher)
                .forEach(TeacherClass::removeHeadTeacher);
        // 添加新班主任
        TeacherClass tc = TeacherClass.createAsHeadTeacher(teacherId, classEntity.getId(), subject);
        teacherClasses.add(tc);
    }

    public void removeTeacher(Long teacherId) {
        teacherClasses.stream()
                .filter(tc -> tc.getTeacherId().equals(teacherId) && tc.isActive())
                .findFirst()
                .ifPresent(TeacherClass::deactivate);
    }

    public boolean isTeacherInClass(Long teacherId) {
        return teacherClasses.stream()
                .anyMatch(tc -> tc.getTeacherId().equals(teacherId) && tc.isActive());
    }

    public Long getHeadTeacherId() {
        return teacherClasses.stream()
                .filter(tc -> tc.isHeadTeacher() && tc.isActive())
                .map(TeacherClass::getTeacherId)
                .findFirst()
                .orElse(null);
    }

    // 班级状态管理
    public void graduate() {
        classEntity.graduate();
        studentClasses.forEach(StudentClass::graduate);
    }

    public void archive() {
        classEntity.archive();
    }

    public void updateInfo(String name, String description) {
        classEntity.updateInfo(name, description);
    }
}