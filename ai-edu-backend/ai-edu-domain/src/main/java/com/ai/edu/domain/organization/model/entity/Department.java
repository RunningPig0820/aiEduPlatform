package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.organization.model.valueobject.DepartmentId;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 部门实体
 * 用于行政组织归属，支持树形层级结构
 */
@Getter
public class Department {

    private DepartmentId id;
    private SchoolId schoolId;
    private String name;
    private Long parentId;
    private String departmentPath;
    private Integer sortOrder;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;

    protected Department() {}

    /**
     * 创建根级部门
     */
    public static Department createRoot(SchoolId schoolId, String name, Integer sortOrder, String description) {
        Department department = new Department();
        department.schoolId = schoolId;
        department.name = name;
        department.parentId = null;
        department.departmentPath = null;  // 根节点路径为空，保存后需要更新为自己的ID
        department.sortOrder = sortOrder != null ? sortOrder : 0;
        department.description = description;
        department.deleted = false;
        return department;
    }

    /**
     * 创建子部门
     * @param parentDepartment 父部门实体（用于计算 department_path）
     */
    public static Department createChild(SchoolId schoolId, String name, Department parentDepartment,
                                          Integer sortOrder, String description) {
        Department department = new Department();
        department.schoolId = schoolId;
        department.name = name;
        department.parentId = parentDepartment.getIdValue();
        // department_path = 父部门的路径（已包含父部门ID）
        // 子部门的路径会在保存后更新为：父路径_子ID
        department.departmentPath = parentDepartment.getDepartmentPath();
        department.sortOrder = sortOrder != null ? sortOrder : 0;
        department.description = description;
        department.deleted = false;
        return department;
    }

    /**
     * 从 PO 重建部门实体（用于 Repository 层）
     */
    public static Department fromPO(Long id, SchoolId schoolId, String name, Long parentId,
                                     String departmentPath, Integer sortOrder, String description,
                                     LocalDateTime createdAt, LocalDateTime updatedAt, boolean deleted) {
        Department department = new Department();
        department.id = id != null ? DepartmentId.of(id) : null;
        department.schoolId = schoolId;
        department.name = name;
        department.parentId = parentId;
        department.departmentPath = departmentPath;
        department.sortOrder = sortOrder;
        department.description = description;
        department.createdAt = createdAt;
        department.updatedAt = updatedAt;
        department.deleted = deleted;
        return department;
    }

    /**
     * 更新部门路径（保存后调用，设置完整路径）
     * @param id 当前部门的ID
     */
    public void updateDepartmentPathAfterSave(Long id) {
        if (id == null) {
            return;
        }

        if (parentId == null) {
            // 根节点：路径就是自己的ID
            this.departmentPath = String.valueOf(id);
        } else {
            // 子节点：路径 = 父路径 + "_" + 自己的ID
            // 注意：父路径已经包含父部门ID
            if (this.departmentPath == null || this.departmentPath.isEmpty()) {
                // 如果父路径为空（不应该发生），直接用父ID
                this.departmentPath = parentId + "_" + id;
            } else {
                this.departmentPath = this.departmentPath + "_" + id;
            }
        }
    }

    /**
     * 构建部门路径（用于更新父部门时计算新路径）
     * 格式：父部门路径（已包含父部门ID） + "_" + 当前部门ID
     */
    private static String buildDepartmentPath(Department parent) {
        if (parent == null) {
            return null;
        }
        String parentPath = parent.getDepartmentPath();
        Long parentId = parent.getIdValue();

        if (parentPath == null || parentPath.isEmpty()) {
            // 父部门是根节点，路径就是父部门ID
            return String.valueOf(parentId);
        } else {
            // 父部门有路径，追加父部门ID
            return parentPath + "_" + parentId;
        }
    }

    public void setId(DepartmentId id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }

    /**
     * 更新部门信息
     */
    public void update(String name, Integer sortOrder, String description) {
        this.name = name;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        if (description != null) {
            this.description = description;
        }
    }

    /**
     * 更新上级部门（需要重新计算 department_path）
     */
    public void updateParent(Department newParent) {
        if (newParent != null && newParent.getIdValue().equals(this.getIdValue())) {
            throw new IllegalArgumentException("Cannot set parent to self");
        }
        this.parentId = newParent != null ? newParent.getIdValue() : null;
        this.departmentPath = buildDepartmentPath(newParent);
    }

    /**
     * 更新部门路径（用于批量更新子部门路径）
     */
    public void updateDepartmentPath(String newParentPath, Long newParentId) {
        if (newParentPath == null || newParentPath.isEmpty()) {
            this.departmentPath = String.valueOf(newParentId);
        } else {
            this.departmentPath = newParentPath + "_" + newParentId;
        }
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public boolean isRoot() {
        return parentId == null;
    }

    public Long getIdValue() {
        return id != null ? id.getValue() : null;
    }

    public Long getSchoolIdValue() {
        return schoolId != null ? schoolId.getValue() : null;
    }
}