package com.ai.edu.infrastructure.ai.rag;

import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

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
}
