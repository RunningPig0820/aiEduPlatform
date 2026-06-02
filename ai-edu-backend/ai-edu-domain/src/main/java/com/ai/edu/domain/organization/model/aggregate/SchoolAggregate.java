package com.ai.edu.domain.organization.model.aggregate;

import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.model.entity.School;
import com.ai.edu.domain.organization.model.valueobject.SchoolInstitutionalType;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 学校聚合根
 * 管理学校及其下属班级
 */
@Getter
public class SchoolAggregate {

    private final School school;
    private final List<Class> classes;

    public SchoolAggregate(School school) {
        this.school = school;
        this.classes = new ArrayList<>();
    }

    public static SchoolAggregate create(String name, SchoolInstitutionalType schoolType) {
        School school = School.create(name, schoolType);
        return new SchoolAggregate(school);
    }

    public SchoolId getId() {
        return school.getId();
    }

    public Long getIdValue() {
        return school.getIdValue();
    }

    public String getName() {
        return school.getName();
    }

    public SchoolInstitutionalType getSchoolType() {
        return school.getSchoolType();
    }

    public String getSchoolTypeValue() {
        return school.getSchoolTypeValue();
    }

    // 班级管理
    public void addClass(Class classEntity) {
        if (classEntity.getSchoolId() != null && !classEntity.getSchoolId().equals(school.getId())) {
            throw new IllegalArgumentException("Class belongs to another school");
        }
        classes.add(classEntity);
    }

    public void removeClass(ClassId classId) {
        classes.removeIf(cls -> cls.getId().equals(classId));
    }

    public Class getClassById(ClassId classId) {
        return classes.stream()
                .filter(cls -> cls.getId().equals(classId))
                .findFirst()
                .orElse(null);
    }

    public List<Class> getActiveClasses() {
        return classes.stream()
                .filter(Class::isActive)
                .toList();
    }

    public List<Class> getClassesByGrade(String grade) {
        return classes.stream()
                .filter(cls -> cls.getGradeValue() != null && cls.getGradeValue().equals(grade) && cls.isActive())
                .toList();
    }

    public int getTotalClassCount() {
        return (int) classes.stream().filter(Class::isActive).count();
    }

    public int getTotalStudentCount() {
        // 需要通过 StudentClass 统计，这里返回活跃班级数作为参考
        return getActiveClasses().size();
    }

    // 学校信息管理
    public void update(String name, SchoolInstitutionalType schoolType,
                       String iconUrl, String stages,
                       String province, String city, String district, String address,
                       String description) {
        school.update(name, schoolType, iconUrl, stages, province, city, district, address, description);
    }

    public boolean isKindergarten() {
        return school.isKindergarten();
    }

    public boolean isPrimary() {
        return school.isPrimary();
    }

    public boolean isJunior() {
        return school.isJunior();
    }

    public boolean isSenior() {
        return school.isSenior();
    }

    public boolean isComprehensive() {
        return school.isComprehensive();
    }

    public boolean isNormal() {
        return school.isNormal();
    }

    public boolean isArchive() {
        return school.isArchive();
    }

    public boolean isFail() {
        return school.isFail();
    }
}