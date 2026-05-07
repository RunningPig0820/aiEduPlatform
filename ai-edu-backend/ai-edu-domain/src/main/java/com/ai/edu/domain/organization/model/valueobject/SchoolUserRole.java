package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 学校用户角色值对象
 * 表示用户在学校中的角色：管理员、教师、学生、家长
 */
@Getter
@EqualsAndHashCode
public class SchoolUserRole implements ValueObject {

    private final String value;

    private SchoolUserRole(String value) {
        this.value = value;
    }

    /**
     * 管理员
     */
    public static SchoolUserRole admin() {
        return new SchoolUserRole("ADMIN");
    }

    /**
     * 教师
     */
    public static SchoolUserRole teacher() {
        return new SchoolUserRole("TEACHER");
    }

    /**
     * 学生
     */
    public static SchoolUserRole student() {
        return new SchoolUserRole("STUDENT");
    }

    /**
     * 家长
     */
    public static SchoolUserRole parent() {
        return new SchoolUserRole("PARENT");
    }

    public static SchoolUserRole of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SchoolUserRole value cannot be null or blank");
        }
        return new SchoolUserRole(value);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(value);
    }

    public boolean isTeacher() {
        return "TEACHER".equals(value);
    }

    public boolean isStudent() {
        return "STUDENT".equals(value);
    }

    public boolean isParent() {
        return "PARENT".equals(value);
    }

    public String getDescription() {
        return switch (value) {
            case "ADMIN" -> "管理员";
            case "TEACHER" -> "教师";
            case "STUDENT" -> "学生";
            case "PARENT" -> "家长";
            default -> value;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}