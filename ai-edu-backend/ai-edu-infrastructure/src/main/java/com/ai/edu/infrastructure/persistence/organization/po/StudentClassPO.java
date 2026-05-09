package com.ai.edu.infrastructure.persistence.organization.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生班级关联持久化对象
 */
@Data
@TableName("t_student_class")
public class StudentClassPO {

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
    private String status;

    @TableField("created_by")
    private Long createdBy;

    @TableField("modified_by")
    private Long modifiedBy;

    @TableField("is_deleted")
    private Boolean deleted = false;
}