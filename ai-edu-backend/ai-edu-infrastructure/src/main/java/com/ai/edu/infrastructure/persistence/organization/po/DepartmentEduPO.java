package com.ai.edu.infrastructure.persistence.organization.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教育部门扩展属性持久化对象
 */
@Data
@TableName("t_department_edu")
public class DepartmentEduPO {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("dept_id")
    private Long deptId;

    @TableField("school_id")
    private Long schoolId;

    @TableField("dept_type")
    private Integer deptType;

    @TableField("stage_code")
    private String stageCode;

    @TableField("stage_year_code")
    private String stageYearCode;

    @TableField("grade_code")
    private String gradeCode;

    @TableField("enrollment_year")
    private String enrollmentYear;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_on")
    private LocalDateTime createdOn;

    @TableField("modified_by")
    private Long modifiedBy;

    @TableField("modified_on")
    private LocalDateTime modifiedOn;

    @TableField("is_deleted")
    private Boolean deleted = false;
}
