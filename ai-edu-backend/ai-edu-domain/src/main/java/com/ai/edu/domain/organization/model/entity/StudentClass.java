package com.ai.edu.domain.organization.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 学生-班级关联实体
 */
@TableName("t_student_class")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentClass {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("class_id")
    private Long classId;

    @TableField("student_no")
    private String studentNo;

    @TableField("join_date")
    private LocalDate joinDate;

    @TableField("leave_date")
    private LocalDate leaveDate;

    @TableField("status")
    private String status = "ACTIVE";

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
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