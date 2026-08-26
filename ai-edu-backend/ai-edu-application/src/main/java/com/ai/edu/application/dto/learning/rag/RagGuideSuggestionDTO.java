package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 开始引导单条建议（前端 chip）。
 *
 * <p>title=引导问题（直接可问）；direction=引导方向（intro/operation/data_relation/difficulty/rag，
 * rag 为 RAG 引擎常驻方向）。来源：模块引导底座池。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagGuideSuggestionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 引导问题（可直接点击发送） */
    private String title;

    /** 引导方向标签（intro/operation/data_relation/difficulty/rag） */
    private String direction;
}
