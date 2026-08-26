package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话累计 token（关闭对话结算，前端契约，camelCase）。
 *
 * <p>Java 每轮 done 后把 tokens_usage 累加进 Redis `rag:assistant:session:{sessionId}:usage`（TTL 24h），
 * close 时读回返回。补上 spec 第 4 条"对话消耗总 token"缺口（原来只有每轮）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSessionUsageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 累计 prompt tokens */
    private Integer promptTokens;

    /** 累计 completion tokens */
    private Integer completionTokens;

    /** 累计缓存命中 tokens */
    private Integer cacheHitTokens;

    /** 累计总 tokens */
    private Integer totalTokens;
}
