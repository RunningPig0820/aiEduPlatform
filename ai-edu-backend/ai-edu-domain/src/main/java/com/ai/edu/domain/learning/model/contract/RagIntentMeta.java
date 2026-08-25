package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Python intent 事件数据（Java → Python 内部契约，snake_case 反序列化）。
 *
 * <p>anchor=模块级路由（闭集：ai-tutoring/knowledge-graph/question-analysis/rag-system），
 * locked_sections=节级加权（池内精化），candidates=歧义候选模块（供 clarify），两层锚定并存。
 * LLM 失败回退关键词时 degraded=true（200 返回，不阻断链路）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagIntentMeta implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模块锚点 */
    private String anchor;

    /** 意图类别 */
    private String category;

    @JsonProperty("switch_detected")
    private Boolean switchDetected;

    private Boolean ambiguous;

    /** 歧义候选模块 id 闭集 */
    private List<String> candidates;

    @JsonProperty("locked_sections")
    private List<String> lockedSections;

    private Boolean degraded;
}
