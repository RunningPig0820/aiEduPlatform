package com.ai.edu.domain.organization.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 学生-班级关联实体
 */
@Entity
@Table(name = "t_student_class")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "student_no", length = 50)
    private String studentNo;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "leave_date")
    private LocalDate leaveDate;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;

    @Column(name = "modified_by", nullable = false)
    private Long modifiedBy = 0L;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    public static StudentClass create(Long studentId, Long classId) {
        StudentClass sc = new StudentClass();
        sc.studentId = studentId;
        sc.classId = classId;
        sc.joinDate = LocalDate.now();
        return sc;
    }

    public static StudentClass createWithNo(Long studentId, Long classId, String studentNo) {
        StudentClass sc = create(studentId, classId);
        sc.studentNo = studentNo;
        return sc;
    }

    public void transfer() {
        this.status = "TRANSFERRED";
        this.leaveDate = LocalDate.now();
    }

    public void graduate() {
        this.status = "GRADUATED";
        this.leaveDate = LocalDate.now();
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
}