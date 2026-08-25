package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * RAG 项目介绍助手端口（Java → Python 白盒链路引擎的调用契约）。
 *
 * <p>基础设施层 {@code RagAssistantBridgeImpl} 以 WebClient 实现（复用 llm-gateway internalToken 模式，
 * 后续里程碑接真实 Python 端点）。<b>permission 由 Java 网关发</b>，桥返回的 SSE 流从 intent 事件开始；
 * meta/done 由网关重建，不透传 Python 原始事件。流式不可重试，失败由编排层降级。
 */
public interface RagAssistantPort {

    /**
     * 发起一轮问答（SSE 流）：返回 Python 白盒事件流（intent → (clarify|switch) → rewrite → rerank → (boundary|token*) → done）。
     * 实现内部<b>不重试</b>（流式不可重试，失败由编排层降级）。
     *
     * @param request ask 请求（history/traceId 由 Java 组装传入）
     */
    Flux<ServerSentEvent<String>> ask(RagAskRequest request);
}
