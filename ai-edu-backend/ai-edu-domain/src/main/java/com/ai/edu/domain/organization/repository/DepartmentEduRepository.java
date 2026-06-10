package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.DepartmentEdu;
import com.ai.edu.domain.organization.model.valueobject.DepartmentEduId;
import com.ai.edu.domain.shared.valueobject.SchoolId;

import java.util.List;
import java.util.Optional;

/**
 * 教育部门扩展属性仓储接口
 */
public interface DepartmentEduRepository {

    /**
     * 保存教育部门扩展属性
     */
    DepartmentEdu save(DepartmentEdu departmentEdu);

    /**
     * 根据 ID 查找
     */
    Optional<DepartmentEdu> findById(DepartmentEduId id);

    /**
     * 根据部门 ID 查找扩展属性
     */
    Optional<DepartmentEdu> findByDeptId(Long deptId);

    /**
     * 根据学校 ID 查找所有扩展属性
     */
    List<DepartmentEdu> findBySchoolId(SchoolId schoolId);

    /**
     * 根据部门 ID 删除扩展属性（逻辑删除）
     */
    void deleteByDeptId(Long deptId);
}
