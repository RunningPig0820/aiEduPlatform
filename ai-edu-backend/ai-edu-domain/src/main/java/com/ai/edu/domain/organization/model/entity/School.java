package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.organization.model.valueobject.SchoolInstitutionalType;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import lombok.Getter;

/**
 * 学校实体
 */
@Getter
public class School {

    private SchoolId id;
    private String name;
    private String province;
    private String city;
    private String address;
    private SchoolInstitutionalType schoolType;
    private String description;
    private String iconUrl;
    private String stages;
    private String status;
    private Long createdBy;
    private Long modifiedBy;
    private boolean deleted;

    protected School() {}

    public static School create(String name, SchoolInstitutionalType schoolType) {
        School school = new School();
        school.name = name;
        school.schoolType = schoolType;
        school.status = "ACTIVE";
        school.createdBy = 0L;
        school.modifiedBy = 0L;
        school.deleted = false;
        return school;
    }

    public static School createWithAddress(String name, SchoolInstitutionalType schoolType,
                                           String province, String city, String address) {
        School school = create(name, schoolType);
        school.province = province;
        school.city = city;
        school.address = address;
        return school;
    }

    public void setId(SchoolId id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public void setStages(String stages) {
        this.stages = stages;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void updateAddress(String province, String city, String address) {
        this.province = province;
        this.city = city;
        this.address = address;
    }

    public void updateDescription(String description) {
        this.description = description;
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

    public boolean isActive() {
        return "ACTIVE".equals(status);
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
}