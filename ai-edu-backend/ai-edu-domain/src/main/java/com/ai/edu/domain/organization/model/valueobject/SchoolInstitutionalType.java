package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 学校类型值对象
 * 表示学校的类型：幼儿园、小学、初中、高中、综合性学校
 */
@Getter
@EqualsAndHashCode
public class SchoolInstitutionalType implements ValueObject {

    private final String value;

    private SchoolInstitutionalType(String value) {
        this.value = value;
    }

    /**
     * 幼儿园
     */
    public static SchoolInstitutionalType kindergarten() {
        return new SchoolInstitutionalType("KINDERGARTEN");
    }

    /**
     * 小学
     */
    public static SchoolInstitutionalType primary() {
        return new SchoolInstitutionalType("PRIMARY");
    }

    /**
     * 初中
     */
    public static SchoolInstitutionalType junior() {
        return new SchoolInstitutionalType("JUNIOR");
    }

    /**
     * 高中
     */
    public static SchoolInstitutionalType senior() {
        return new SchoolInstitutionalType("SENIOR");
    }

    /**
     * 综合性学校
     */
    public static SchoolInstitutionalType comprehensive() {
        return new SchoolInstitutionalType("COMPREHENSIVE");
    }

    public static SchoolInstitutionalType of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SchoolInstitutionalType value cannot be null or blank");
        }
        return new SchoolInstitutionalType(value);
    }

    public boolean isKindergarten() {
        return "KINDERGARTEN".equals(value);
    }

    public boolean isPrimary() {
        return "PRIMARY".equals(value);
    }

    public boolean isJunior() {
        return "JUNIOR".equals(value);
    }

    public boolean isSenior() {
        return "SENIOR".equals(value);
    }

    public boolean isComprehensive() {
        return "COMPREHENSIVE".equals(value);
    }

    public String getDescription() {
        return switch (value) {
            case "KINDERGARTEN" -> "幼儿园";
            case "PRIMARY" -> "小学";
            case "JUNIOR" -> "初中";
            case "SENIOR" -> "高中";
            case "COMPREHENSIVE" -> "综合性学校";
            default -> value;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}