package com.ai.edu.domain.organization.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 老师-班级关联实体
 */
@TableName("t_teacher_class")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherClass {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("class_id")
    private Long classId;

    @TableField("subject")
    private String subject;

    @TableField("is_head_teacher")
    private Boolean headTeacher = false;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("status")
    private String status = "ACTIVE";

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
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