package com.ai.edu.domain.organization.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 年级实体
 */
@Entity
@Table(name = "t_grade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String code;

    @Column(name = "grade_level")
    private Integer gradeLevel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;

    @Column(name = "modified_by", nullable = false)
    private Long modifiedBy = 0L;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    public static Grade create(String name, Integer gradeLevel) {
        Grade grade = new Grade();
        grade.name = name;
        grade.gradeLevel = gradeLevel;
        return grade;
    }

    public static Grade createWithSchool(String name, Integer gradeLevel, Long schoolId) {
        Grade grade = create(name, gradeLevel);
        grade.schoolId = schoolId;
        return grade;
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

    public boolean isPrimarySchool() {
        return gradeLevel != null && gradeLevel >= 1 && gradeLevel <= 6;
    }

    public boolean isJuniorHigh() {
        return gradeLevel != null && gradeLevel >= 7 && gradeLevel <= 9;
    }

    public boolean isHighSchool() {
        return gradeLevel != null && gradeLevel >= 10 && gradeLevel <= 12;
    }
}