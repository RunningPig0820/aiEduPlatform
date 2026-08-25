package com.ai.edu.infrastructure.ai.rag;

import com.ai.edu.common.exception.EntityNotFoundException;
import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import com.ai.edu.domain.learning.service.RagAssistantPort;
import com.ai.edu.infrastructure.ai.LlmGatewayProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

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
    private static final String SOURCE_PATH = "/api/rag/source";
    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(60);

    @Resource(name = "ragWebClient")
    private WebClient ragWebClient;

    @Resource
    private LlmGatewayProperties llmGatewayProperties;

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

    private static String stripTrailingSlash(String url) {
        return url == null ? "" : url.replaceAll("/+$", "");
    }

    @Override
    public Mono<String> source(String filePath) {
        // Python StaticFiles 按目录分隔服务：逐段 urlencode 保留 "/" 目录结构（中文/空格安全）
        String encodedPath = Arrays.stream(filePath.split("/"))
                .map(seg -> URLEncoder.encode(seg, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
        // 必须用绝对 URI：.uri(URI) 传相对路径(无 scheme/host)无法与 baseUrl 拼接 → WebClientRequestException
        URI fullUri = URI.create(stripTrailingSlash(llmGatewayProperties.getBaseUrl())
                + SOURCE_PATH + "/" + encodedPath);
        log.info("[rag-assistant] 桥调 Python source, filePath={}, uri={}", filePath, fullUri);
        return ragWebClient.get()
                .uri(fullUri)
                .retrieve()
                // 原文不存在 → EntityNotFoundException（10002，HTTP 404）
                .onStatus(status -> status.value() == 404,
                        resp -> Mono.error(new EntityNotFoundException("原文不存在")))
                .bodyToMono(String.class)
                .onErrorMap(WebClientRequestException.class,
                        e -> new TutoringAgentException("RAG 原文服务暂不可用", e))
                .onErrorMap(WebClientResponseException.class,
                        e -> new TutoringAgentException("RAG 原文服务暂不可用 (status="
                                + e.getStatusCode().value() + ")", e));
    }
}
