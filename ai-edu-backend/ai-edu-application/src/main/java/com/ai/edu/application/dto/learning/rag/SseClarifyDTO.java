package com.ai.edu.application.dto.learning.rag;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * SSE clarify 事件数据（前端契约，camelCase）。
 *
 * <p>澄清轮：歧义且候选 ≥2 时发（0 token、不计答案轮次、最多一轮）。candidates 为字符串 id 闭集，
 * 中文 label 由前端 pageModuleMap 维护；点选候选 = 重发原问 + currentProject=点选模块。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseClarifyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 固定澄清话术（"您的问题涉及多个功能，请明确功能名…"） */
    private String message;

    /** 候选模块 id 闭集（≥2 才触发澄清） */
    private List<String> candidates;

    /** 默认功能（前端 current_project > 会话锚点）；字段名避开 Java 关键字 default，序列化为 default */
    @JsonProperty("default")
    private String defaultModule;
}
