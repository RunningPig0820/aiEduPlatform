package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.contract.ActionMeta;
import com.ai.edu.domain.learning.model.contract.DecideContext;
import com.ai.edu.domain.learning.model.contract.GenerateContext;
import com.ai.edu.domain.learning.model.contract.OcrResult;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * 答疑 LLM 端口（Java → Python 答疑 agent 的调用契约）。
 *
 * <p>基础设施层 {@code TutoringLlmClient} 以 WebClient 实现（复用 llm-gateway internalToken 模式）。
 * <b>类型先行流式</b>：decide（非流式快调用，出 action 元数据）→ Java 护栏校验 → generate（流式正文）。
 */
public interface TutoringLlmPort {

    /**
     * 决策（非流式，快模型）：输出 action 元数据（type 闭集 + eval + mastery_signals + ...）。
     *
     * <p>实现内部对错误重试 {@code AGENT_RETRY}（1）次。
     */
    ActionMeta decide(DecideContext context);

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
}
