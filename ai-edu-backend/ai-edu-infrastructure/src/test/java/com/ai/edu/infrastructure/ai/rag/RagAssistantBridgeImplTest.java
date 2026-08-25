package com.ai.edu.infrastructure.ai.rag;

import com.ai.edu.common.exception.EntityNotFoundException;
import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import com.ai.edu.infrastructure.ai.LlmGatewayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RagAssistantBridgeImpl 桥单测（mock WebClient，验证：请求 snake 序列化、SSE 中继保序、
 * permission 过滤、degraded 200 不报错、Python 异常冒泡）。
 */
class RagAssistantBridgeImplTest {

    private RagAssistantBridgeImpl buildClient(Flux<ServerSentEvent<String>> pythonFlux) {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.accept(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ArgumentMatchers.<ParameterizedTypeReference<ServerSentEvent<String>>>any()))
                .thenReturn(pythonFlux);

        RagAssistantBridgeImpl client = new RagAssistantBridgeImpl();
        ReflectionTestUtils.setField(client, "ragWebClient", webClient);
        return client;
    }

    private ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.<String>builder().event(event).data(data).build();
    }

    private RagAskRequest askRequest() {
        return RagAskRequest.builder()
                .question("这个项目的整体架构是什么？")
                .sessionId("sess-001")
                .currentProject("ai-tutoring")
                .history(List.of())
                .traceId("trc-abc123")
                .topK(3)
                .build();
    }

    // ==================== snake↔camel 请求序列化 ====================

    @Test
    @DisplayName("RagAskRequest 序列化为 snake_case 调 Python（current_project/session_id/trace_id/top_k）")
    void request_serializesSnakeCase() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(askRequest());

        assertTrue(json.contains("\"current_project\""), json);
        assertTrue(json.contains("\"session_id\""), json);
        assertTrue(json.contains("\"trace_id\""), json);
        assertTrue(json.contains("\"top_k\""), json);
        assertFalse(json.contains("currentProject"), json);
    }

    // ==================== SSE 中继保序 ====================

    @Test
    @DisplayName("ask: 原始中继 Python SSE（intent→rewrite→done）保序，不重排不丢失")
    void ask_relaysEventsInOrder() {
        RagAssistantBridgeImpl client = buildClient(Flux.just(
                sse("intent", "{\"anchor\":\"ai-tutoring\",\"switch_detected\":false}"),
                sse("rewrite", "{\"original_question\":\"Q\",\"rewritten_query\":\"R\"}"),
                sse("done", "{\"answer\":\"A\",\"trace_id\":\"trc-abc123\"}")));

        StepVerifier.create(client.ask(askRequest()))
                .expectNextMatches(ev -> "intent".equals(ev.event()))
                .expectNextMatches(ev -> "rewrite".equals(ev.event()))
                .expectNextMatches(ev -> "done".equals(ev.event()))
                .verifyComplete();
    }

    // ==================== permission 过滤 ====================

    @Test
    @DisplayName("ask: Python 侧 permission 被过滤（permission 仅 Java 发），从 intent 开始中继")
    void ask_filtersPythonPermission() {
        RagAssistantBridgeImpl client = buildClient(Flux.just(
                sse("permission", "{\"role\":\"STUDENT\",\"allowed\":true}"),
                sse("intent", "{\"anchor\":\"ai-tutoring\"}"),
                sse("done", "{\"answer\":\"A\"}")));

        StepVerifier.create(client.ask(askRequest()))
                .expectNextMatches(ev -> "intent".equals(ev.event()))
                .expectNextMatches(ev -> "done".equals(ev.event()))
                .verifyComplete();
    }

    // ==================== degraded 200 ====================

    @Test
    @DisplayName("ask: 200 + degraded 内容按普通结果中继，不视为错误")
    void ask_degraded200() {
        RagAssistantBridgeImpl client = buildClient(Flux.just(
                sse("intent", "{\"anchor\":\"ai-tutoring\",\"degraded\":true}"),
                sse("done", "{\"answer\":\"A\"}")));

        StepVerifier.create(client.ask(askRequest()))
                .assertNext(ev -> {
                    assertEquals("intent", ev.event());
                    assertTrue(ev.data().contains("degraded"), ev.data());
                })
                .expectNextMatches(ev -> "done".equals(ev.event()))
                .verifyComplete();
    }

    // ==================== Python 异常冒泡 ====================

    @Test
    @DisplayName("ask: Python 流异常 → TutoringAgentException（桥不吞，由编排层降级）")
    void ask_pythonErrorBubbles() {
        RagAssistantBridgeImpl client = buildClient(Flux.error(new RuntimeException("python down")));

        assertThrows(TutoringAgentException.class,
                () -> client.ask(askRequest()).blockLast());
    }

    @Test
    @DisplayName("ask: Python HTTP 500 → TutoringAgentException（网关降级，不吞错不 200 化）")
    void ask_http500Bubbles() {
        WebClientResponseException ex = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        RagAssistantBridgeImpl client = buildClient(Flux.error(ex));

        assertThrows(TutoringAgentException.class, () -> client.ask(askRequest()).blockLast());
    }

    // ==================== M4 真流消费（token* 逐块） ====================

    @Test
    @DisplayName("ask: 真流消费 intent→rewrite→rerank→token*→done 保序透传，token 逐块不重排不丢失")
    void ask_relaysTokenStream() {
        RagAssistantBridgeImpl client = buildClient(Flux.just(
                sse("intent", "{\"anchor\":\"rag-system\"}"),
                sse("rewrite", "{\"original_question\":\"Q\",\"rewritten_query\":\"R\"}"),
                sse("rerank", "{\"blocks\":[]}"),
                sse("token", "{\"text\":\"RAG 项目\"}"),
                sse("token", "{\"text\":\"的整体架构\"}"),
                sse("done", "{\"answer\":\"RAG 项目的整体架构\",\"tokens_usage\":{\"prompt_tokens\":320,\"completion_tokens\":140,\"cache_hit_tokens\":0,\"total_tokens\":460},\"trace_id\":\"trc-abc123\"}")));

        StepVerifier.create(client.ask(askRequest()))
                .expectNextMatches(ev -> "intent".equals(ev.event()))
                .expectNextMatches(ev -> "rewrite".equals(ev.event()))
                .expectNextMatches(ev -> "rerank".equals(ev.event()))
                .expectNextMatches(ev -> "token".equals(ev.event()) && ev.data().contains("RAG"))
                .expectNextMatches(ev -> "token".equals(ev.event()) && ev.data().contains("整体架构"))
                .expectNextMatches(ev -> "done".equals(ev.event()) && ev.data().contains("tokens_usage"))
                .verifyComplete();
    }

    // ==================== source 查看原文 ====================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private WebClient buildGetClient(WebClient.ResponseSpec responseSpec) {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(URI.class))).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        return webClient;
    }

    private RagAssistantBridgeImpl buildSourceClient(WebClient.ResponseSpec responseSpec) {
        RagAssistantBridgeImpl client = new RagAssistantBridgeImpl();
        ReflectionTestUtils.setField(client, "ragWebClient", buildGetClient(responseSpec));
        var props = new LlmGatewayProperties();
        props.setBaseUrl("http://127.0.0.1:9527");
        ReflectionTestUtils.setField(client, "llmGatewayProperties", props);
        return client;
    }

    @Test
    @DisplayName("source: 原文存在 → 返回文件内容")
    void source_returnsContent() {
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("# 文档内容\n正文"));

        RagAssistantBridgeImpl client = buildSourceClient(responseSpec);

        StepVerifier.create(client.source("4.完善文档/02-一次完整答疑怎么走.md"))
                .expectNext("# 文档内容\n正文")
                .verifyComplete();
    }

    @Test
    @DisplayName("source: 原文不存在 → 注册 404 谓词，错误函数产 EntityNotFoundException（10002）")
    void source_notFoundWiring() {
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        AtomicReference<Predicate<HttpStatusCode>> predicateRef = new AtomicReference<>();
        AtomicReference<Function<ClientResponse, Mono<? extends Throwable>>> errRef = new AtomicReference<>();
        when(responseSpec.onStatus(any(), any())).thenAnswer(inv -> {
            predicateRef.set(inv.getArgument(0));
            errRef.set(inv.getArgument(1));
            return responseSpec;
        });
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("x"));

        RagAssistantBridgeImpl client = buildSourceClient(responseSpec);
        client.source("不存在.md").block();

        assertTrue(predicateRef.get().test(HttpStatus.NOT_FOUND), "404 应命中谓词");
        assertThrows(EntityNotFoundException.class,
                () -> errRef.get().apply(mock(ClientResponse.class)).block());
    }

    @Test
    @DisplayName("source: 传输异常 → TutoringAgentException")
    void source_transportError() {
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new WebClientRequestException(new RuntimeException("down"),
                        org.springframework.http.HttpMethod.GET, URI.create("http://x"), HttpHeaders.EMPTY)));

        RagAssistantBridgeImpl client = buildSourceClient(responseSpec);

        assertThrows(TutoringAgentException.class, () -> client.source("x.md").block());
    }
}
