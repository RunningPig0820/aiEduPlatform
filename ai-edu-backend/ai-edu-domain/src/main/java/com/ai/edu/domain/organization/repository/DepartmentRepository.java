package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.Department;
import com.ai.edu.domain.organization.model.valueobject.DepartmentId;
import com.ai.edu.domain.organization.model.valueobject.DepartmentQueryParam;
import com.ai.edu.domain.shared.valueobject.PageResult;
import com.ai.edu.domain.shared.valueobject.SchoolId;

import java.util.List;
import java.util.Optional;

/**
 * 部门仓储接口
 * 定义部门实体的持久化操作
 */
public interface DepartmentRepository {

    /**
     * 保存部门
     */
    Department save(Department department);

    /**
     * 根据ID查找部门
     */
    Optional<Department> findById(DepartmentId id);

    /**
     * 根据学校ID和部门名称查找部门
     */
    Optional<Department> findBySchoolIdAndName(SchoolId schoolId, String name);

    /**
     * 查找学校的所有部门
     */
    List<Department> findBySchoolId(SchoolId schoolId);

    /**
     * 查找学校的根部门（parent_id 为空）
     */
    List<Department> findRootDepartments(SchoolId schoolId);

    /**
     * 查找指定部门的直接子部门
     */
    List<Department> findChildren(DepartmentId parentId);

    /**
     * 查找指定部门的所有子孙部门（通过 department_path 查询）
     */
    List<Department> findAllDescendants(DepartmentId departmentId);

    /**
     * 分页查询部门
     */
    PageResult<Department> queryPage(DepartmentQueryParam param);

    /**
     * 检查学校下部门名称是否已存在
     */
    boolean existsBySchoolIdAndName(SchoolId schoolId, String name);

    /**
     * 检查部门是否有子部门
     */
    boolean hasChildren(DepartmentId id);

    /**
     * 根据ID删除部门（逻辑删除）
     */
    void deleteById(DepartmentId id);

    /**
     * 根据ID恢复部门
     */
    void restoreById(DepartmentId id);

    /**
     * 批量更新子孙部门的路径
     * 当部门层级变更时，需要更新所有子孙部门的 department_path
     */
    void updateDescendantsPath(DepartmentId departmentId, String newParentPath);
}