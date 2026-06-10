package com.ai.edu.domain.organization.model.valueobject.enums;

import lombok.Getter;

/**
 * 学年制度枚举
 * 小学六年制、初中三年制、高中三年制
 */
@Getter
public enum StageYearCodeEnum {


//    KINDERGARTEN_SMALL_SAML_CLASS(1, "从小小班开始"),
//    KINDERGARTEN_SMALL_CLASS(2, "从小班开始"),

    PRIMARY_FIVE("3", "小学五年制",3),
    /** 小学六年制 */
    PRIMARY_SIX("4", "小学六年制", 6),

    /** 初中三年制 */
    JUNIOR_THREE("5", "初中三年制", 3),

    /** 高中三年制 */
    SENIOR_THREE("7", "高中三年制", 3);

    private final String value;
    private final String description;
    private final int yearCount;

    StageYearCodeEnum(String value, String description, int yearCount) {
        this.value = value;
        this.description = description;
        this.yearCount = yearCount;
    }

    public static StageYearCodeEnum of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("StageYearCode value cannot be null or blank");
        }
        for (StageYearCodeEnum code : values()) {
            if (code.value.equals(value)) {
                return code;
            }
        }
        throw new IllegalArgumentException("StageYearCode must be 4(小学六年制), 5(初中三年制), or 7(高中三年制), got: " + value);
    }

    public boolean isPrimary() {
        return this == PRIMARY_SIX;
    }

    public boolean isJuniorHigh() {
        return this == JUNIOR_THREE;
    }

    public boolean isSeniorHigh() {
        return this == SENIOR_THREE;
    }
}
