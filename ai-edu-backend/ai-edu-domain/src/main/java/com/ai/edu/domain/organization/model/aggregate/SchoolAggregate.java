package com.ai.edu.domain.organization.model.aggregate;

import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.model.entity.School;
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

    public static SchoolAggregate create(String name, String code, String schoolType) {
        School school = School.create(name, code, schoolType);
        return new SchoolAggregate(school);
    }

    public Long getId() {
        return school.getId();
    }

    public String getName() {
        return school.getName();
    }

    public String getCode() {
        return school.getCode();
    }

    public String getSchoolType() {
        return school.getSchoolType();
    }

    // 班级管理
    public void addClass(Class classEntity) {
        if (classEntity.getSchoolId() != null && !classEntity.getSchoolId().equals(school.getId())) {
            throw new IllegalArgumentException("Class belongs to another school");
        }
        classes.add(classEntity);
    }

    public void removeClass(Long classId) {
        classes.removeIf(cls -> cls.getId().equals(classId));
    }

    public Class getClassById(Long classId) {
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
                .filter(cls -> cls.getGrade().equals(grade) && cls.isActive())
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
    public void updateAddress(String province, String city, String district, String address) {
        school.updateAddress(province, city, district, address);
    }

    public void updateDescription(String description) {
        school.updateDescription(description);
    }

    public boolean isPrimary() {
        return school.isPrimary();
    }

    public boolean isJuniorHigh() {
        return school.isJuniorHigh();
    }

    public boolean isHighSchool() {
        return school.isHighSchool();
    }
}