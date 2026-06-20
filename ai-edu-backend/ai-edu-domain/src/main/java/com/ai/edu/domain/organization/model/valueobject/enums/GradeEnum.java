package com.ai.edu.domain.organization.model.valueobject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author 张敏
 * @date 2026-06-12 14:28
 */
@Getter
@AllArgsConstructor
public enum GradeEnum {

    //毕业班
    GRADUATE(-1L, "毕业"),
    //一年级
    GRADE_ONE(1L, "一年级"),
    //二年级x
    GRADE_TWO(2L, "二年级"),
    //三年级
    GRADE_THREE(3L, "三年级"),
    //四年级
    GRADE_FOUR(4L, "四年级"),
    //五年级
    GRADE_FIVE(5L, "五年级"),
    //六年级
    GRADE_SIX(6L, "六年级"),

    //七年级
    GRADE_SEVEN(7L, "七年级"),
    //八年级
    GRADE_EIGHT(8L, "八年级"),
    //九年级
    GRADE_NINE(9L, "九年级"),

    //高一
    GRADE_TEN(10L, "高一"),
    //高二
    GRADE_ELEVEN(11L, "高二"),
    //高三
    GRADE_TWELVE(12L, "高三"),

    //小班
    SMALL_CLASS(31L, "小班"),
    //中班
    MIDDLE_CLASS(32L, "中班"),
    //大班
    BIG_CLASS(33L, "大班"),
    //小小班
    SMALL_SMALL_CLASS(34L, "小小班"),
    //学前班
    PRE_SCHOOL_CLASS(35L, "学前班");


    private  Long value;
    private  String description;
}
