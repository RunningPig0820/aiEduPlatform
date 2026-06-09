package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.organization.model.valueobject.OrgTeacherId;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 教职工实体
 * 教职工本质是用户与部门的关联关系，不存储用户基本信息
 * 用户基本信息（姓名、手机号）存储在用户域
 */
@Getter
public class OrgTeacher {

    private OrgTeacherId id;
    private SchoolId schoolId;
    private Long userId;         // 关联用户域用户ID
    private Long departmentId;   // 所属行政部门ID
    private Long createdBy;      // 创建人(登录用户ID)
    private Long modifiedBy;     // 最后修改人(登录用户ID)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;

    protected OrgTeacher() {}

    /**
     * 创建教职工关联关系
     * @param schoolId 所属学校
     * @param userId 用户域用户ID
     * @param departmentId 所属行政部门ID
     * @param createdBy 创建人(登录用户ID)
     */
    public static OrgTeacher create(SchoolId schoolId, Long userId, Long departmentId, Long createdBy) {
        OrgTeacher teacher = new OrgTeacher();
        teacher.schoolId = schoolId;
        teacher.userId = userId;
        teacher.departmentId = departmentId;
        teacher.createdBy = createdBy;
        teacher.modifiedBy = createdBy;
        teacher.deleted = false;
        return teacher;
    }

    /**
     * 从 PO 重建教职工实体（用于 Repository 层）
     */
    public static OrgTeacher fromPO(Long id, SchoolId schoolId, Long userId, Long departmentId,
                                     Long createdBy, Long modifiedBy,
                                     LocalDateTime createdAt, LocalDateTime updatedAt, boolean deleted) {
        OrgTeacher teacher = new OrgTeacher();
        teacher.id = id != null ? OrgTeacherId.of(id) : null;
        teacher.schoolId = schoolId;
        teacher.userId = userId;
        teacher.departmentId = departmentId;
        teacher.createdBy = createdBy;
        teacher.modifiedBy = modifiedBy;
        teacher.createdAt = createdAt;
        teacher.updatedAt = updatedAt;
        teacher.deleted = deleted;
        return teacher;
    }

    public void setId(OrgTeacherId id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }

    /**
     * 更新所属部门
     * @param newDepartmentId 新部门ID
     * @param modifiedBy 修改人(登录用户ID)
     */
    public void updateDepartment(Long newDepartmentId, Long modifiedBy) {
        this.departmentId = newDepartmentId;
        this.modifiedBy = modifiedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public Long getIdValue() {
        return id != null ? id.getValue() : null;
    }

    public Long getSchoolIdValue() {
        return schoolId != null ? schoolId.getValue() : null;
    }
}