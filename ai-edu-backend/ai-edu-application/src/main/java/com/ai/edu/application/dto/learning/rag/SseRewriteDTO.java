package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE rewrite 事件数据（前端契约，camelCase）。
 *
 * <p>Query 改写结果：原始问题 vs 改写后检索式，前端展示"语义分析"阶段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseRewriteDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 原始问题（用户输入） */
    private String originalQuestion;

    /** 改写后检索式 query */
    private String rewrittenQuery;
}
