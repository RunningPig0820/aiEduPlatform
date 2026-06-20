package com.ai.edu.domain.organization.model.valueobject.enums;

import lombok.Getter;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 学年制度枚举
 * 每种年制直接持有固定的年级编码列表
 */
@Getter
public enum StageYearCodeEnum {

    /** 小学五年制 */
    PRIMARY_FIVE("3", "小学五年制", IntStream.rangeClosed(1, 5).boxed().toList()),

    /** 小学六年制 */
    PRIMARY_SIX("4", "小学六年制", IntStream.rangeClosed(1, 6).boxed().toList()),

    /** 初中三年制（6年级开始） */
    JUNIOR_THREE("5", "初中三年制", IntStream.rangeClosed(6, 8).boxed().toList()),

    /** 初中四年制（7年级开始） */
    JUNIOR_FOUR("6", "初中四年制", IntStream.rangeClosed(7, 10).boxed().toList()),

    /** 高中三年制 */
    SENIOR_THREE("7", "高中三年制", IntStream.rangeClosed(10, 12).boxed().toList());

    private final String value;
    private final String description;
    private final List<Integer> gradeCodes;

    StageYearCodeEnum(String value, String description, List<Integer> gradeCodes) {
        this.value = value;
        this.description = description;
        this.gradeCodes = gradeCodes;
    }

    public int getYearCount() {
        return gradeCodes.size();
    }

    public int getStartGrade() {
        return gradeCodes.get(0);
    }

    public int getEndGrade() {
        return gradeCodes.get(gradeCodes.size() - 1);
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
        throw new IllegalArgumentException("Unknown StageYearCode: " + value);
    }

    public boolean isPrimary() {
        return this == PRIMARY_FIVE || this == PRIMARY_SIX;
    }

    public boolean isJuniorHigh() {
        return this == JUNIOR_THREE || this == JUNIOR_FOUR;
    }

    public boolean isSeniorHigh() {
        return this == SENIOR_THREE;
    }
}
