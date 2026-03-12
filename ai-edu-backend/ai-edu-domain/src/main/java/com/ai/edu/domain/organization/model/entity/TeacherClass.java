package com.ai.edu.domain.organization.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 老师-班级关联实体
 */
@Entity
@Table(name = "t_teacher_class")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(length = 50)
    private String subject;

    @Column(name = "is_head_teacher", nullable = false)
    private Boolean headTeacher = false;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;

    @Column(name = "modified_by", nullable = false)
    private Long modifiedBy = 0L;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    public static TeacherClass create(Long teacherId, Long classId, String subject) {
        TeacherClass tc = new TeacherClass();
        tc.teacherId = teacherId;
        tc.classId = classId;
        tc.subject = subject;
        tc.startDate = LocalDate.now();
        return tc;
    }

    public static TeacherClass createAsHeadTeacher(Long teacherId, Long classId, String subject) {
        TeacherClass tc = create(teacherId, classId, subject);
        tc.headTeacher = true;
        return tc;
    }

    public void assignAsHeadTeacher() {
        this.headTeacher = true;
    }

    public void removeHeadTeacher() {
        this.headTeacher = false;
    }

    public void deactivate() {
        this.status = "INACTIVE";
        this.endDate = LocalDate.now();
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public void delete() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isHeadTeacher() {
        return Boolean.TRUE.equals(headTeacher);
    }
}