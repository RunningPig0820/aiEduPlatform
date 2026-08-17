package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.contract.DecideContext;
import com.ai.edu.domain.learning.model.contract.GenerateContext;
import com.ai.edu.domain.learning.model.contract.OcrResult;
import com.ai.edu.domain.learning.model.contract.QuestionUnderstandResult;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 答疑 LLM 端口（Java → Python 答疑 agent 的调用契约）。
 *
 * <p>基础设施层 {@code TutoringLlmClient} 以 WebClient 实现（复用 llm-gateway internalToken 模式）。
 * <b>类型先行流式</b>：decide（SSE 流，thinking 实时中继 + meta 提取）→ Java 护栏校验 → generate（流式正文）。
 */
public interface TutoringLlmPort {

    /**
     * 决策流（流式 SSE）：返回 Python decide 原始事件流（thinking / agent / meta / done 全保留），
     * 由编排层决定消费哪些（实时中继 thinking + 提取 meta）。
     *
     * <p>实现内部<b>不重试</b>（流式不可重试，重试会重发已透传的 thinking，失败由编排层降级）。
     */
    Flux<ServerSentEvent<String>> decideStream(DecideContext context);

    /**
     * 生成正文（流式 SSE，强模型）：按已放行 action_type 输出 token 流。
     *
     * <p>实现内部<b>不重试</b>（流式不可重试，失败由编排层降级）。
     */
    Flux<ServerSentEvent<String>> generate(GenerateContext context);

    /**
     * 拍照识别题目（非流式，OCR 前置）：识别为 {@code {text, confidence}}。
     *
     * <p>实现内部对错误重试 {@code AGENT_RETRY}（1）次。
     *
     * @param imageData        图片字节（jpg/png）
     * @param originalFilename 原始文件名（用于识别格式/扩展名）
     */
    OcrResult recognize(byte[] imageData, String originalFilename);

    /**
     * 图片题目理解（非流式）：Python 视觉模型直接看图 → 题型名 + 顺带知识点。
     *
     * <p>模型 Python 侧写死（doubao-seed-2-0-mini-260428）；识别失败返回空 topicLabels（调用方降级 PENDING）。
     * 实现内部对错误重试 {@code agentRetry} 次。
     *
     * @param imageUrl  COS 签名 URL（Python 直接看图）
     * @param topicHint 题型名候选提示（传 findTopTopicLabels(20) 收敛命名，可选）
     * @param grade     学生年级（年级锚定，可选）
     */
    QuestionUnderstandResult understandQuestion(String imageUrl, List<String> topicHint, Integer grade);
}
