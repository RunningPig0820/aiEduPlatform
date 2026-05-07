package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.organization.model.valueobject.SchoolUserAssociationId;
import com.ai.edu.domain.organization.model.valueobject.SchoolUserRole;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.domain.shared.valueobject.UserId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 学校用户关联实体
 * 表示用户与学校的关联关系及角色
 */
@Getter
public class SchoolUserAssociation {

    private SchoolUserAssociationId id;
    private SchoolId schoolId;
    private UserId userId;
    private SchoolUserRole role;
    private LocalDateTime createdAt;

    // Protected constructor for JPA
    protected SchoolUserAssociation() {}

    /**
     * 创建学校用户关联
     */
    public static SchoolUserAssociation create(SchoolId schoolId, UserId userId, SchoolUserRole role) {
        if (schoolId == null) {
            throw new IllegalArgumentException("SchoolId cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        if (role == null) {
            throw new IllegalArgumentException("SchoolUserRole cannot be null");
        }

        SchoolUserAssociation association = new SchoolUserAssociation();
        association.schoolId = schoolId;
        association.userId = userId;
        association.role = role;
        association.createdAt = LocalDateTime.now();
        return association;
    }

    /**
     * 设置ID（由Repository在持久化后设置）
     */
    public void setId(SchoolUserAssociationId id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }

    /**
     * 更新角色
     */
    public void updateRole(SchoolUserRole newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("SchoolUserRole cannot be null");
        }
        this.role = newRole;
    }

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin() {
        return role != null && role.isAdmin();
    }

    /**
     * 检查是否为教师
     */
    public boolean isTeacher() {
        return role != null && role.isTeacher();
    }

    /**
     * 检查是否为学生
     */
    public boolean isStudent() {
        return role != null && role.isStudent();
    }

    /**
     * 检查是否为家长
     */
    public boolean isParent() {
        return role != null && role.isParent();
    }
}