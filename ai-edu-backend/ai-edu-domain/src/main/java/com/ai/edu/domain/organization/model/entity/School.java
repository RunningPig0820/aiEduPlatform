package com.ai.edu.domain.organization.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 学校实体
 */
@Entity
@Table(name = "t_school")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String code;

    @Column(length = 50)
    private String province;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String district;

    @Column(length = 500)
    private String address;

    @Column(name = "school_type", length = 50)
    private String schoolType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;

    @Column(name = "modified_by", nullable = false)
    private Long modifiedBy = 0L;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    public static School create(String name, String code, String schoolType) {
        School school = new School();
        school.name = name;
        school.code = code;
        school.schoolType = schoolType;
        return school;
    }

    public void updateAddress(String province, String city, String district, String address) {
        this.province = province;
        this.city = city;
        this.district = district;
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

    public boolean isPrimary() {
        return "PRIMARY".equals(schoolType);
    }

    public boolean isJuniorHigh() {
        return "JUNIOR_HIGH".equals(schoolType);
    }

    public boolean isHighSchool() {
        return "HIGH_SCHOOL".equals(schoolType);
    }
}