package com.ai.edu.domain.organization.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 班级实体
 */
@Entity
@Table(name = "t_class")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Class {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 20)
    private String grade;

    @Column(name = "school_year", length = 20)
    private String schoolYear;

    @Column(name = "class_type", length = 50)
    private String classType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;

    @Column(name = "modified_by", nullable = false)
    private Long modifiedBy = 0L;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    public static Class create(String name, String grade, String schoolYear) {
        Class cls = new Class();
        cls.name = name;
        cls.grade = grade;
        cls.schoolYear = schoolYear;
        return cls;
    }

    public static Class createWithSchool(String name, String grade, String schoolYear, Long schoolId) {
        Class cls = create(name, grade, schoolYear);
        cls.schoolId = schoolId;
        return cls;
    }

    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void assignSchool(Long schoolId) {
        this.schoolId = schoolId;
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public void graduate() {
        this.status = "GRADUATED";
    }

    public void archive() {
        this.status = "ARCHIVED";
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

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isGraduated() {
        return "GRADUATED".equals(status);
    }

    public boolean isArchived() {
        return "ARCHIVED".equals(status);
    }
}