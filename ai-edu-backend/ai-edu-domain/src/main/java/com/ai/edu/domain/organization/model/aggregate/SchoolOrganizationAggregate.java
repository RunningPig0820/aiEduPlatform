package com.ai.edu.domain.organization.model.aggregate;

import com.ai.edu.domain.organization.model.entity.SchoolUserAssociation;
import com.ai.edu.domain.organization.model.valueobject.SchoolInstitutionalType;
import com.ai.edu.domain.organization.model.valueobject.SchoolStage;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学校组织聚合根
 * 管理学校组织的属性、学段和用户关联
 */
@Getter
public class SchoolOrganizationAggregate {

    private SchoolId id;
    private String name;
    private String iconUrl;
    private SchoolInstitutionalType institutionalType;
    private List<SchoolStage> stages;
    private List<SchoolUserAssociation> userAssociations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Protected constructor for JPA
    protected SchoolOrganizationAggregate() {
        this.stages = new ArrayList<>();
        this.userAssociations = new ArrayList<>();
    }

    /**
     * 创建学校组织（工厂方法）
     */
    public static SchoolOrganizationAggregate create(
            String name,
            String iconUrl,
            SchoolInstitutionalType institutionalType,
            List<SchoolStage> stages) {

        // 业务规则验证
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("学校名称不能为空");
        }
        if (institutionalType == null) {
            throw new IllegalArgumentException("学校类型不能为空");
        }
        if (stages == null || stages.isEmpty()) {
            throw new IllegalArgumentException("学段不能为空");
        }

        SchoolOrganizationAggregate aggregate = new SchoolOrganizationAggregate();
        aggregate.name = name;
        aggregate.iconUrl = iconUrl;
        aggregate.institutionalType = institutionalType;
        aggregate.stages = new ArrayList<>(stages);
        aggregate.userAssociations = new ArrayList<>();
        aggregate.createdAt = LocalDateTime.now();
        aggregate.updatedAt = LocalDateTime.now();

        return aggregate;
    }

    /**
     * 设置ID（由Repository在持久化后设置）
     */
    public void setId(SchoolId id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }

    /**
     * 更新学校信息
     */
    public void update(String name, String iconUrl, SchoolInstitutionalType institutionalType, List<SchoolStage> stages) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("学校名称不能为空");
        }
        if (institutionalType == null) {
            throw new IllegalArgumentException("学校类型不能为空");
        }
        if (stages == null || stages.isEmpty()) {
            throw new IllegalArgumentException("学段不能为空");
        }

        this.name = name;
        this.iconUrl = iconUrl;
        this.institutionalType = institutionalType;
        this.stages = new ArrayList<>(stages);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加用户关联
     */
    public void addUserAssociation(SchoolUserAssociation association) {
        if (association == null) {
            throw new IllegalArgumentException("关联不能为空");
        }
        if (!association.getSchoolId().equals(this.id)) {
            throw new IllegalArgumentException("关联的学校ID不匹配");
        }
        // 检查是否已存在相同用户
        boolean exists = userAssociations.stream()
                .anyMatch(a -> a.getUserId().equals(association.getUserId()));
        if (exists) {
            throw new IllegalArgumentException("用户已关联该学校");
        }
        userAssociations.add(association);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 移除用户关联
     */
    public void removeUserAssociation(SchoolId schoolId, com.ai.edu.domain.shared.valueobject.UserId userId) {
        userAssociations.removeIf(a -> a.getSchoolId().equals(schoolId) && a.getUserId().equals(userId));
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查用户是否关联
     */
    public boolean hasUserAssociation(com.ai.edu.domain.shared.valueobject.UserId userId) {
        return userAssociations.stream()
                .anyMatch(a -> a.getUserId().equals(userId));
    }

    /**
     * 获取用户角色
     */
    public com.ai.edu.domain.organization.model.valueobject.SchoolUserRole getUserRole(com.ai.edu.domain.shared.valueobject.UserId userId) {
        return userAssociations.stream()
                .filter(a -> a.getUserId().equals(userId))
                .findFirst()
                .map(SchoolUserAssociation::getRole)
                .orElse(null);
    }

    /**
     * 检查是否为幼儿园
     */
    public boolean isKindergarten() {
        return institutionalType != null && institutionalType.isKindergarten();
    }

    /**
     * 检查是否为小学
     */
    public boolean isPrimary() {
        return institutionalType != null && institutionalType.isPrimary();
    }

    /**
     * 检查是否为初中
     */
    public boolean isJunior() {
        return institutionalType != null && institutionalType.isJunior();
    }

    /**
     * 检查是否为高中
     */
    public boolean isSenior() {
        return institutionalType != null && institutionalType.isSenior();
    }

    /**
     * 检查是否为综合性学校
     */
    public boolean isComprehensive() {
        return institutionalType != null && institutionalType.isComprehensive();
    }

    /**
     * 检查是否包含指定学段
     */
    public boolean hasStage(SchoolStage stage) {
        return stages.contains(stage);
    }

    /**
     * 获取学段数量
     */
    public int getStageCount() {
        return stages.size();
    }

    /**
     * 获取用户关联数量
     */
    public int getUserAssociationCount() {
        return userAssociations.size();
    }
}