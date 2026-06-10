package com.ai.edu.domain.organization.model.aggregate;

import com.ai.edu.domain.organization.model.entity.Department;
import com.ai.edu.domain.organization.model.entity.DepartmentEdu;
import com.ai.edu.domain.organization.model.valueobject.enums.DeptEduTypeEnum;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import lombok.Getter;

/**
 * 行政班聚合根
 * 封装 Department（组织树）和 DepartmentEdu（教育属性）的关系
 */
@Getter
public class AdminClassAggregate {

    private final Department department;
    private DepartmentEdu departmentEdu;

    public AdminClassAggregate(Department department) {
        if (department == null) {
            throw new IllegalArgumentException("Department cannot be null");
        }
        if (!department.isAdminClass()) {
            throw new IllegalArgumentException("Department must be ADMIN_CLASS type");
        }
        this.department = department;
    }

    public AdminClassAggregate(Department department, DepartmentEdu departmentEdu) {
        this(department);
        this.departmentEdu = departmentEdu;
    }

    /**
     * 创建学段节点
     */
    public static AdminClassAggregate createStage(SchoolId schoolId, String name, String stageCode,
                                                   String stageYearCode, Integer sortOrder, String description) {
        Department dept = Department.createAdminClassRoot(schoolId, name, sortOrder, description);
        AdminClassAggregate aggregate = new AdminClassAggregate(dept);
        aggregate.departmentEdu = DepartmentEdu.createStage(null, schoolId, stageCode, stageYearCode);
        return aggregate;
    }

    /**
     * 创建年级节点（挂在学段下）
     */
    public static AdminClassAggregate createGrade(SchoolId schoolId, String name, Department parentStage,
                                                   String stageCode, String stageYearCode,
                                                   String gradeCode, String enrollmentYear,
                                                   Integer sortOrder, String description) {
        Department dept = Department.createAdminClassChild(schoolId, name, parentStage, sortOrder, description);
        AdminClassAggregate aggregate = new AdminClassAggregate(dept);
        aggregate.departmentEdu = DepartmentEdu.createGrade(null, schoolId, stageCode, stageYearCode,
                gradeCode, enrollmentYear);
        return aggregate;
    }

    /**
     * 创建班级节点（挂在年级下）
     */
    public static AdminClassAggregate createClass(SchoolId schoolId, String name, Department parentGrade,
                                                   String stageCode, String stageYearCode,
                                                   String gradeCode, String enrollmentYear,
                                                   Integer sortOrder, String description) {
        Department dept = Department.createAdminClassChild(schoolId, name, parentGrade, sortOrder, description);
        AdminClassAggregate aggregate = new AdminClassAggregate(dept);
        aggregate.departmentEdu = DepartmentEdu.createClass(null, schoolId, stageCode, stageYearCode,
                gradeCode, enrollmentYear);
        return aggregate;
    }

    /**
     * 从已有 Department + DepartmentEdu 重建聚合
     */
    public static AdminClassAggregate from(Department department, DepartmentEdu departmentEdu) {
        return new AdminClassAggregate(department, departmentEdu);
    }

    /**
     * 更新节点基本信息（名称、描述等）
     */
    public void updateInfo(String name, Integer sortOrder, String description) {
        department.update(name, sortOrder, description);
    }

    /**
     * 更新教育扩展属性
     */
    public void updateEduInfo(String stageCode, String stageYearCode, String gradeCode, String enrollmentYear) {
        if (departmentEdu == null) {
            throw new IllegalStateException("DepartmentEdu not loaded");
        }
        departmentEdu.update(stageCode, stageYearCode, gradeCode, enrollmentYear);
    }

    /**
     * 移动节点到新的父节点下
     */
    public void moveTo(Department newParent) {
        department.updateParent(newParent);
    }

    /**
     * 加载教育扩展属性（由 Repository 延迟加载）
     */
    public void loadEdu(DepartmentEdu edu) {
        this.departmentEdu = edu;
    }

    /**
     * 软删除节点
     */
    public void delete() {
        department.delete();
        if (departmentEdu != null) {
            departmentEdu.delete();
        }
    }

    public Long getDeptIdValue() {
        return department.getIdValue();
    }

    public Long getSchoolIdValue() {
        return department.getSchoolIdValue();
    }

    public String getName() {
        return department.getName();
    }

    public DeptEduTypeEnum getDeptType() {
        return departmentEdu != null ? departmentEdu.getDeptType() : null;
    }

    public boolean isStage() {
        return departmentEdu != null && departmentEdu.isStage();
    }

    public boolean isGrade() {
        return departmentEdu != null && departmentEdu.isGrade();
    }

    public boolean isClass() {
        return departmentEdu != null && departmentEdu.isClass();
    }
}
