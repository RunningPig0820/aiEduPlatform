package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.organization.model.valueobject.StudentClassStatus;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.UserId;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 学生-班级关联实体
 */
@Getter
public class StudentClass {

    private Long id;
    private UserId studentId;
    private ClassId classId;
    private String studentNo;
    private LocalDate joinDate;
    private LocalDate leaveDate;
    private StudentClassStatus status;
    private Long createdBy;
    private Long modifiedBy;
    private boolean deleted;

    protected StudentClass() {}

    public static StudentClass create(UserId studentId, ClassId classId) {
        StudentClass sc = new StudentClass();
        sc.studentId = studentId;
        sc.classId = classId;
        sc.joinDate = LocalDate.now();
        sc.status = StudentClassStatus.active();
        sc.createdBy = 0L;
        sc.modifiedBy = 0L;
        sc.deleted = false;
        return sc;
    }

    public static StudentClass createWithNo(UserId studentId, ClassId classId, String studentNo) {
        StudentClass sc = create(studentId, classId);
        sc.studentNo = studentNo;
        return sc;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public void transfer() {
        this.status = StudentClassStatus.transferred();
        this.leaveDate = LocalDate.now();
    }

    public void graduate() {
        this.status = StudentClassStatus.graduated();
        this.leaveDate = LocalDate.now();
    }

    public void activate() {
        this.status = StudentClassStatus.active();
    }

    public void delete() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }

    public boolean isActive() {
        return status != null && status.isActive();
    }

    public boolean isGraduated() {
        return status != null && status.isGraduated();
    }

    public boolean isTransferred() {
        return status != null && status.isTransferred();
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getStatusValue() {
        return status != null ? status.getValue() : null;
    }

    public Long getStudentIdValue() {
        return studentId != null ? studentId.getValue() : null;
    }

    public Long getClassIdValue() {
        return classId != null ? classId.getValue() : null;
    }
}