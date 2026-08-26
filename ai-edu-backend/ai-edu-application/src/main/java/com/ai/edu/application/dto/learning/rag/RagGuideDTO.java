package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * RAG 助手开始引导（会话入口 chips，前端契约，camelCase）。
 *
 * <p>由 Python {@code GET /api/rag/assistant/guide} 返回的引导池 JSON 经 SNAKE_MAPPER
 * 反序列化。底座池驱动（known-issues 问题6 落地）：每模块一个 {direction: [问题]} 池，
 * 引导必含 ≥1 条 RAG 方向。0 token、非 SSE、不占冻结时序。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagGuideDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 开始引导建议（1~3 条，必含 ≥1 条 RAG 方向） */
    private List<RagGuideSuggestionDTO> suggestions;
}
