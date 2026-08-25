package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * SSE intent 事件数据（前端契约，camelCase）。
 *
 * <p>意图分析结果：anchor=模块路由（闭集），lockedSections=节级加权，candidates=歧义候选模块（供 clarify）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseIntentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模块锚点（ai-tutoring/knowledge-graph/question-analysis/rag-system） */
    private String anchor;

    /** 意图类别（项目介绍/操作/数据关联/难点） */
    private String category;

    /** 是否检测到功能切换 */
    private Boolean switchDetected;

    /** 是否歧义（多候选功能） */
    private Boolean ambiguous;

    /** 歧义候选模块 id 闭集（LLM 主源，会话锚点兜底） */
    private List<String> candidates;

    /** 节级锁定（池内加权，如 ["04","07"]） */
    private List<String> lockedSections;

    /** LLM 失败回退关键词锚定标记（200 返回，不阻断） */
    private Boolean degraded;
}
