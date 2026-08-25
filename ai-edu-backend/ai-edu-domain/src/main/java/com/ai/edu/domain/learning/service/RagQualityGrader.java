package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.contract.RagQualityScore;
import reactor.core.publisher.Mono;

/**
 * RAG 回答质量评审端口：每轮真实对话生成完后，把【问题 + 答案 + 引用块摘要】发给 LLM 打分（0-5）。
 *
 * <p>Java 后台异步调用（不阻塞回答 SSE），分数累计进评估报告 realConversation 区段。
 * 打分失败不应影响问答链路 —— 实现内兜底（返回降级分/空），由调用方静默记录。
 */
public interface RagQualityGrader {

    /**
     * 对一轮真实回答打分。
     *
     * @param question       学生问题
     * @param answer         生成答案（非空才评）
     * @param quotedKeys     答案实际引用的精排块 blockId 集合（可为空）
     * @param blockSummaries 引用知识库片段摘要（"【标题】摘要" 预格式化列表，可为空；
     *                       喂原文片段让忠实度可评，避免只传 id 靠 LLM 猜）
     */
    Mono<RagQualityScore> grade(String question, String answer, java.util.List<String> quotedKeys,
                                java.util.List<String> blockSummaries);
}
