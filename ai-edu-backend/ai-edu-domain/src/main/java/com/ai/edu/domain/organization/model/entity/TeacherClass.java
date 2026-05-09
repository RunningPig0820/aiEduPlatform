package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.organization.model.valueobject.TeacherClassStatus;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.UserId;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 教师-班级关联实体
 */
@Getter
public class TeacherClass {

    private Long id;
    private UserId teacherId;
    private ClassId classId;
    private String subject;
    private boolean headTeacher;
    private LocalDate startDate;
    private LocalDate endDate;
    private TeacherClassStatus status;
    private Long createdBy;
    private Long modifiedBy;
    private boolean deleted;

    protected TeacherClass() {}

    public static TeacherClass create(UserId teacherId, ClassId classId, String subject) {
        TeacherClass tc = new TeacherClass();
        tc.teacherId = teacherId;
        tc.classId = classId;
        tc.subject = subject;
        tc.startDate = LocalDate.now();
        tc.status = TeacherClassStatus.active();
        tc.headTeacher = false;
        tc.createdBy = 0L;
        tc.modifiedBy = 0L;
        tc.deleted = false;
        return tc;
    }

    public static TeacherClass createAsHeadTeacher(UserId teacherId, ClassId classId, String subject) {
        TeacherClass tc = create(teacherId, classId, subject);
        tc.headTeacher = true;
        return tc;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void assignAsHeadTeacher() {
        this.headTeacher = true;
    }

    public void removeHeadTeacher() {
        this.headTeacher = false;
    }

    public void deactivate() {
        this.status = TeacherClassStatus.inactive();
        this.endDate = LocalDate.now();
    }

    public void activate() {
        this.status = TeacherClassStatus.active();
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

    public boolean isHeadTeacher() {
        return headTeacher;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getStatusValue() {
        return status != null ? status.getValue() : null;
    }

    public Long getTeacherIdValue() {
        return teacherId != null ? teacherId.getValue() : null;
    }

    public Long getClassIdValue() {
        return classId != null ? classId.getValue() : null;
    }
}