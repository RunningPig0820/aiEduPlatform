package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * SSE done 事件数据（前端契约，camelCase）。
 *
 * <p>一轮完整结果，由 Java 重建（不透传 Python 原始 done）。quotedKeys 在 M5 补发，
 * suggestions 在 M6 补发（契约冻结：字段追加不重排）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseDoneDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 生成答案 */
    private String answer;

    /** is_quoted 命中的精排块 blockId 集合（LCS 硬匹配，非 LLM 自述） */
    private List<String> quotedKeys;

    /** 本轮 token 消耗 */
    private SseTokensUsageDTO tokensUsage;

    /** 本轮 trace id */
    private String traceId;

    /** 完成后引导建议（1~3 条，必含 ≥1 条 RAG 方向） */
    private List<String> suggestions;

    /** boundary=low_confidence / 超时降级=timeout，正常为 null */
    private String reason;
}
