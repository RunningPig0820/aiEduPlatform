package com.ai.edu.domain.organization.model.valueobject.enums;

import lombok.Getter;

/**
 * 学校学段枚举
 */
@Getter
public enum SchoolStageEnum {

    /** 小学 */
    PRIMARY("1","PRIMARY", "小学"),

    /** 初中 */
    JUNIOR_HIGH("2","JUNIOR_HIGH", "初中"),

    /** 高中 */
    SENIOR_HIGH("3","SENIOR_HIGH", "高中")
    ;

    private final String code;
    private final String value;
    private final String description;

    SchoolStageEnum(String code , String value, String description) {
        this.code = code;
        this.value = value;
        this.description = description;
    }

    public static SchoolStageEnum of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SchoolStage value cannot be null or blank");
        }
        for (SchoolStageEnum stage : values()) {
            if (stage.value.equals(value)) {
                return stage;
            }
        }
        throw new IllegalArgumentException("Unknown SchoolStage: " + value);
    }

    public boolean isPrimary() {
        return this == PRIMARY;
    }

    public boolean isJuniorHigh() {
        return this == JUNIOR_HIGH;
    }

    public boolean isSeniorHigh() {
        return this == SENIOR_HIGH;
    }

}
