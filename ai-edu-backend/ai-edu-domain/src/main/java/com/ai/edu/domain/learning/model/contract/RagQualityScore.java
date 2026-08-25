package com.ai.edu.domain.learning.model.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 回答质量分（每轮真实对话 LLM 打分，0-5）。
 *
 * <p>由 LLM 从相关性/完整性/忠实度/清晰度评出，Java 后台异步累计进评估报告
 * （realConversation 区段），不阻塞回答 SSE。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagQualityScore implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 质量分（0-5） */
    private int score;

    /** 简短评审理由 */
    private String reason;
}
