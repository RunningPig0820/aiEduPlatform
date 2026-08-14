package com.ai.edu.application.dto.learning;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题型解析请求（POST /api/kp/resolve）。
 */
@Data
public class KpResolveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 题型/知识点原文（必填，非空） */
    private String label;

    /** 学生年级（可选 1-12，用于年级锚；缺省走纯 LLM 消歧） */
    private Integer studentGrade;
}
