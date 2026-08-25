package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * tokens_usage（前端契约，camelCase）。
 *
 * <p>每轮/会话 token 消耗四字段；cacheHitTokens 取不到时前端标注"估算"。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseTokensUsageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** prompt token 数 */
    private Integer promptTokens;

    /** completion token 数 */
    private Integer completionTokens;

    /** 缓存命中 token 数（取不到 → 估算） */
    private Integer cacheHitTokens;

    /** 总 token 数 */
    private Integer totalTokens;
}
