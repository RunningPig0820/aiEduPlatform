package com.ai.edu.infrastructure.ai.rag;

import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import com.ai.edu.domain.learning.service.RagAssistantPort;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * RAG 助手 Python 引擎桥（WebClient，复用 llm-gateway internalToken 模式）。
 *
 * <p>调 Python {@code /api/rag/assistant/ask}，SSE 消费并**原始中继**（从 intent 事件开始）。
 * <b>permission 仅 Java 网关发</b>，本桥防御性过滤 Python 侧 permission；meta/done 由应用层重建，
 * 本桥不透传 Python 原始 meta/done 语义（只透传事件流）。流式<b>不可重试</b>（重试会重发已透传事件），
 * 失败由编排层降级。RagAskRequest 含 history/traceId（应用层组装），序列化为 snake_case 调 Python。
 */
@Slf4j
@Repository
public class RagAssistantBridgeImpl implements RagAssistantPort {

    private static final String ASK_PATH = "/api/rag/assistant/ask";
    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(60);

    @Resource(name = "ragWebClient")
    private WebClient ragWebClient;

    @Override
    public Flux<ServerSentEvent<String>> ask(RagAskRequest request) {
        log.info("[rag-assistant] 桥调 Python ask, sessionId={}, traceId={}",
                request.getSessionId(), request.getTraceId());
        return ragWebClient.post()
                .uri(ASK_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                // permission 仅 Java 发；防御性过滤 Python 侧 permission（生产不产，无则恒通过）
                .filter(ev -> ev != null && !"permission".equals(ev.event()))
                .doOnNext(ev -> log.trace("[rag-assistant] Python SSE: {} {}", ev.event(), ev.data()))
                .timeout(ASK_TIMEOUT)
                .onErrorResume(e -> {
                    log.error("[rag-assistant] 桥调 Python 失败: {}", e.getMessage(), e);
                    return Flux.error(new TutoringAgentException("RAG 助手服务暂不可用", e));
                });
    }
}
