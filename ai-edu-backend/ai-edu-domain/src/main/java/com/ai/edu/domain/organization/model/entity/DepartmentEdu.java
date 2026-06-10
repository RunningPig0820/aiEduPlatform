package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.organization.model.valueobject.DepartmentEduId;
import com.ai.edu.domain.organization.model.valueobject.enums.DeptEduTypeEnum;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 教育部门扩展属性实体
 * 存储行政班节点的教育特有属性：学段、年制、年级编码、入学年份
 */
@Getter
public class DepartmentEdu {

    private DepartmentEduId id;
    private Long deptId;
    private SchoolId schoolId;
    private DeptEduTypeEnum deptType;
    private String stageCode;
    private String stageYearCode;
    private String gradeCode;
    private String enrollmentYear;
    private Long createdBy;
    private Long modifiedBy;
    private LocalDateTime createdOn;
    private LocalDateTime modifiedOn;
    private boolean deleted;

    protected DepartmentEdu() {}

    /**
     * 创建学段节点的扩展属性
     */
    public static DepartmentEdu createStage(Long deptId, SchoolId schoolId, String stageCode, String stageYearCode) {
        DepartmentEdu edu = new DepartmentEdu();
        edu.deptId = deptId;
        edu.schoolId = schoolId;
        edu.deptType = DeptEduTypeEnum.STAGE;
        edu.stageCode = stageCode;
        edu.stageYearCode = stageYearCode;
        edu.gradeCode = "";
        edu.enrollmentYear = "";
        edu.createdBy = 0L;
        edu.modifiedBy = 0L;
        edu.deleted = false;
        return edu;
    }

    /**
     * 创建年级节点的扩展属性
     */
    public static DepartmentEdu createGrade(Long deptId, SchoolId schoolId, String stageCode, String stageYearCode,
                                             String gradeCode, String enrollmentYear) {
        DepartmentEdu edu = new DepartmentEdu();
        edu.deptId = deptId;
        edu.schoolId = schoolId;
        edu.deptType = DeptEduTypeEnum.GRADE;
        edu.stageCode = stageCode;
        edu.stageYearCode = stageYearCode;
        edu.gradeCode = gradeCode;
        edu.enrollmentYear = enrollmentYear;
        edu.createdBy = 0L;
        edu.modifiedBy = 0L;
        edu.deleted = false;
        return edu;
    }

    /**
     * 创建班级节点的扩展属性
     */
    public static DepartmentEdu createClass(Long deptId, SchoolId schoolId, String stageCode, String stageYearCode,
                                             String gradeCode, String enrollmentYear) {
        DepartmentEdu edu = new DepartmentEdu();
        edu.deptId = deptId;
        edu.schoolId = schoolId;
        edu.deptType = DeptEduTypeEnum.CLASS;
        edu.stageCode = stageCode;
        edu.stageYearCode = stageYearCode;
        edu.gradeCode = gradeCode;
        edu.enrollmentYear = enrollmentYear;
        edu.createdBy = 0L;
        edu.modifiedBy = 0L;
        edu.deleted = false;
        return edu;
    }

    /**
     * 从 PO 重建实体
     */
    public static DepartmentEdu fromPO(Long id, Long deptId, SchoolId schoolId, DeptEduTypeEnum deptType,
                                        String stageCode, String stageYearCode, String gradeCode,
                                        String enrollmentYear, Long createdBy, Long modifiedBy,
                                        LocalDateTime createdOn, LocalDateTime modifiedOn, boolean deleted) {
        DepartmentEdu edu = new DepartmentEdu();
        edu.id = id != null ? DepartmentEduId.of(id) : null;
        edu.deptId = deptId;
        edu.schoolId = schoolId;
        edu.deptType = deptType;
        edu.stageCode = stageCode;
        edu.stageYearCode = stageYearCode;
        edu.gradeCode = gradeCode;
        edu.enrollmentYear = enrollmentYear;
        edu.createdBy = createdBy;
        edu.modifiedBy = modifiedBy;
        edu.createdOn = createdOn;
        edu.modifiedOn = modifiedOn;
        edu.deleted = deleted;
        return edu;
    }

    public void setId(DepartmentEduId id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }

    /**
     * 更新教育扩展属性
     */
    public void update(String stageCode, String stageYearCode, String gradeCode, String enrollmentYear) {
        if (stageCode != null) {
            this.stageCode = stageCode;
        }
        if (stageYearCode != null) {
            this.stageYearCode = stageYearCode;
        }
        if (gradeCode != null) {
            this.gradeCode = gradeCode;
        }
        if (enrollmentYear != null) {
            this.enrollmentYear = enrollmentYear;
        }
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public void setModifiedOn(LocalDateTime modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public void setModifiedBy(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void delete() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isStage() {
        return deptType != null && deptType.isStage();
    }

    public boolean isGrade() {
        return deptType != null && deptType.isGrade();
    }

    public boolean isClass() {
        return deptType != null && deptType.isClass();
    }

    public Integer getDeptTypeValue() {
        return deptType != null ? deptType.getValue() : null;
    }

    public Long getIdValue() {
        return id != null ? id.getValue() : null;
    }

    public Long getSchoolIdValue() {
        return schoolId != null ? schoolId.getValue() : null;
    }
}
