package com.ai.edu.domain.organization.model.aggregate;

import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.model.entity.StudentClass;
import com.ai.edu.domain.organization.model.entity.TeacherClass;
import com.ai.edu.domain.organization.model.valueobject.GradeLevel;
import com.ai.edu.domain.organization.model.valueobject.SchoolYear;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.UserId;
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

    public static ClassAggregate create(String name, GradeLevel grade, SchoolYear schoolYear) {
        Class classEntity = Class.create(name, grade, schoolYear);
        return new ClassAggregate(classEntity);
    }

    public static ClassAggregate createWithSchool(String name, GradeLevel grade, SchoolYear schoolYear, Long schoolId) {
        Class classEntity = Class.createWithSchool(name, grade, schoolYear, null);
        return new ClassAggregate(classEntity);
    }

    public ClassId getId() {
        return classEntity.getId();
    }

    public Long getIdValue() {
        return classEntity.getIdValue();
    }

    public String getName() {
        return classEntity.getName();
    }

    public GradeLevel getGrade() {
        return classEntity.getGrade();
    }

    public String getGradeValue() {
        return classEntity.getGradeValue();
    }

    public SchoolYear getSchoolYear() {
        return classEntity.getSchoolYear();
    }

    public String getSchoolYearValue() {
        return classEntity.getSchoolYearValue();
    }

    public String getStatus() {
        return classEntity.getStatusValue();
    }

    public boolean isActive() {
        return classEntity.isActive();
    }

    // 学生管理
    public void addStudent(UserId studentId, String studentNo) {
        if (!isActive()) {
            throw new IllegalStateException("Cannot add student to inactive class");
        }
        if (isStudentInClass(studentId)) {
            throw new IllegalArgumentException("Student already in class: " + studentId.getValue());
        }
        StudentClass sc = StudentClass.createWithNo(studentId, classEntity.getId(), studentNo);
        studentClasses.add(sc);
    }

    public void removeStudent(UserId studentId) {
        studentClasses.stream()
                .filter(sc -> sc.getStudentId().equals(studentId) && sc.isActive())
                .findFirst()
                .ifPresent(StudentClass::transfer);
    }

    public boolean isStudentInClass(UserId studentId) {
        return studentClasses.stream()
                .anyMatch(sc -> sc.getStudentId().equals(studentId) && sc.isActive());
    }

    public int getActiveStudentCount() {
        return (int) studentClasses.stream()
                .filter(StudentClass::isActive)
                .count();
    }

    // 老师管理
    public void addTeacher(UserId teacherId, String subject) {
        if (!isActive()) {
            throw new IllegalStateException("Cannot add teacher to inactive class");
        }
        if (isTeacherInClass(teacherId)) {
            throw new IllegalArgumentException("Teacher already in class: " + teacherId.getValue());
        }
        TeacherClass tc = TeacherClass.create(teacherId, classEntity.getId(), subject);
        teacherClasses.add(tc);
    }

    public void addHeadTeacher(UserId teacherId, String subject) {
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

    public void removeTeacher(UserId teacherId) {
        teacherClasses.stream()
                .filter(tc -> tc.getTeacherId().equals(teacherId) && tc.isActive())
                .findFirst()
                .ifPresent(TeacherClass::deactivate);
    }

    public boolean isTeacherInClass(UserId teacherId) {
        return teacherClasses.stream()
                .anyMatch(tc -> tc.getTeacherId().equals(teacherId) && tc.isActive());
    }

    public UserId getHeadTeacherId() {
        return teacherClasses.stream()
                .filter(tc -> tc.isHeadTeacher() && tc.isActive())
                .map(TeacherClass::getTeacherId)
                .findFirst()
                .orElse(null);
    }

    public Long getHeadTeacherIdValue() {
        UserId headTeacherId = getHeadTeacherId();
        return headTeacherId != null ? headTeacherId.getValue() : null;
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

    // 加载关联数据（由 Repository 调用）
    public void loadStudents(List<StudentClass> students) {
        this.studentClasses.clear();
        this.studentClasses.addAll(students);
    }

    public void loadTeachers(List<TeacherClass> teachers) {
        this.teacherClasses.clear();
        this.teacherClasses.addAll(teachers);
    }
}