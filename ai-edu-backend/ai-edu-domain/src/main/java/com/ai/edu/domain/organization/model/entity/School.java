package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.organization.model.valueobject.SchoolInstitutionalType;
import com.ai.edu.domain.organization.model.valueobject.SchoolStatus;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 学校实体
 */
@Getter
public class School {

    private SchoolId id;
    private String name;
    private String province;
    private String city;
    private String district;
    private String address;
    private SchoolInstitutionalType schoolType;
    private String description;
    private String iconUrl;
    private String stages;
    private SchoolStatus status;
    private Long createdBy;
    private Long modifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;

    protected School() {}

    /**
     * 创建学校 - 完整参数
     */
    public static School create(String name, SchoolInstitutionalType schoolType,
                                String iconUrl, String stages,
                                String province, String city, String district, String address,
                                String description) {
        School school = new School();
        school.name = name;
        school.schoolType = schoolType;
        school.iconUrl = iconUrl;
        school.stages = stages;
        school.province = province;
        school.city = city;
        school.district = district;
        school.address = address;
        school.description = description;
        school.status = SchoolStatus.NORMAL;
        school.createdBy = 0L;
        school.modifiedBy = 0L;
        school.deleted = false;
        return school;
    }

    /**
     * 创建学校 - 最简参数
     */
    public static School create(String name, SchoolInstitutionalType schoolType) {
        return create(name, schoolType, null, null, null, null, null, null, null);
    }

    public void setId(SchoolId id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }

    /**
     * 更新学校信息
     */
    public void update(String name, SchoolInstitutionalType schoolType,
                       String iconUrl, String stages,
                       String province, String city, String district, String address,
                       String description) {
        this.name = name;
        this.schoolType = schoolType;
        if (iconUrl != null) {
            this.iconUrl = iconUrl;
        }
        if (stages != null) {
            this.stages = stages;
        }
        if (province != null || city != null || district != null || address != null) {
            this.province = province;
            this.city = city;
            this.district = district;
            this.address = address;
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void setStatus(SchoolStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void archive() {
        this.status = SchoolStatus.ARCHIVE;
    }

    public void markAsFail() {
        this.status = SchoolStatus.FAIL;
    }

    public void delete() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }

    public void modify(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public boolean isKindergarten() {
        return schoolType != null && schoolType.isKindergarten();
    }

    public boolean isPrimary() {
        return schoolType != null && schoolType.isPrimary();
    }

    public boolean isJunior() {
        return schoolType != null && schoolType.isJunior();
    }

    public boolean isSenior() {
        return schoolType != null && schoolType.isSenior();
    }

    public boolean isComprehensive() {
        return schoolType != null && schoolType.isComprehensive();
    }

    public boolean isNormal() {
        return status == SchoolStatus.NORMAL;
    }

    public boolean isArchive() {
        return status == SchoolStatus.ARCHIVE;
    }

    public boolean isFail() {
        return status == SchoolStatus.FAIL;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Long getIdValue() {
        return id != null ? id.getValue() : null;
    }

    public String getSchoolTypeValue() {
        return schoolType != null ? schoolType.getValue() : null;
    }

    public String getStatusValue() {
        return status != null ? status.getValue() : null;
    }

    public String getStatusDescription() {
        return status != null ? status.getDescription() : null;
    }
}