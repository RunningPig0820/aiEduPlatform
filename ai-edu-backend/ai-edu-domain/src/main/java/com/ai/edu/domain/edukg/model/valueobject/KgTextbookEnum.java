package com.ai.edu.domain.edukg.model.valueobject;

import lombok.Getter;

/**
 * 知识图谱-教材枚举
 * 用于前端下拉选择器，教材相对固定，通过枚举维护
 */
@Getter
public enum KgTextbookEnum {

    REN_JIAO_BAN("REN_JIAO_BAN", "人教版",0),
    ;

    private final String code;
    private final String desc;
    private final Integer orderIndex;


    KgTextbookEnum(String code, String desc, Integer orderIndex) {
        this.code = code;
        this.desc = desc;
        this.orderIndex = orderIndex;
    }
}
