package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.command.RagAskCommand;
import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import com.ai.edu.domain.learning.service.RagAssistantPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RagAssistantAppService SSE 中继测试（阶段 1）：permission 前置（含 traceId）、
 * Python snake 事件 → 前端 camel 重建、done traceId 回显一致。
 */
class RagAssistantAppServiceTest {

    private RagAssistantAppService appService;
    private RagAssistantPort port;

    @BeforeEach
    void setUp() {
        port = mock(RagAssistantPort.class);
        appService = new RagAssistantAppService();
        ReflectionTestUtils.setField(appService, "ragAssistantPort", port);
    }

    private RagAskCommand command() {
        return RagAskCommand.builder()
                .question("这个项目的整体架构是什么？")
                .sessionId("sess-001")
                .currentProject("ai-tutoring")
                .topK(3)
                .stream(true)
                .build();
    }

    private ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.<String>builder().event(event).data(data).build();
    }

    @Test
    @DisplayName("ask: permission 前置(camel+traceId) → intent/rewrite/done 重建为 camel")
    void ask_rebuildsSnakeToCamel() {
        when(port.ask(any())).thenAnswer(inv -> {
            RagAskRequest req = inv.getArgument(0);
            return Flux.just(
                    sse("intent", "{\"anchor\":\"ai-tutoring\",\"category\":\"项目介绍\",\"switch_detected\":false,\"ambiguous\":false,\"candidates\":[],\"locked_sections\":[\"04\"],\"degraded\":false}"),
                    sse("rewrite", "{\"original_question\":\"这个项目的整体架构是什么？\",\"rewritten_query\":\"项目 整体 架构\"}"),
                    sse("done", "{\"answer\":\"答案\",\"quoted_keys\":[],\"tokens_usage\":{\"prompt_tokens\":320,\"completion_tokens\":140,\"cache_hit_tokens\":0,\"total_tokens\":460},\"trace_id\":\"" + req.getTraceId() + "\",\"suggestions\":[],\"reason\":null}"));
        });

        StepVerifier.create(appService.ask(command()))
                .assertNext(ev -> {
                    assertTrue("permission".equals(ev.event()), ev.event());
                    assertTrue(ev.data().contains("\"allowed\":true"), ev.data());
                    assertTrue(ev.data().contains("\"traceId\""), ev.data());
                    assertFalse(ev.data().contains("trace_id"), ev.data());
                })
                .assertNext(ev -> {
                    assertTrue("intent".equals(ev.event()));
                    assertTrue(ev.data().contains("\"switchDetected\":false"), ev.data());
                    assertTrue(ev.data().contains("\"lockedSections\""), ev.data());
                    assertFalse(ev.data().contains("switch_detected"), ev.data());
                })
                .assertNext(ev -> {
                    assertTrue("rewrite".equals(ev.event()));
                    assertTrue(ev.data().contains("\"originalQuestion\""), ev.data());
                    assertFalse(ev.data().contains("original_question"), ev.data());
                })
                .assertNext(ev -> {
                    assertTrue("done".equals(ev.event()));
                    assertTrue(ev.data().contains("\"quotedKeys\""), ev.data());
                    assertTrue(ev.data().contains("\"tokensUsage\""), ev.data());
                    assertTrue(ev.data().contains("\"promptTokens\":320"), ev.data());
                    assertTrue(ev.data().contains("\"traceId\""), ev.data());
                    assertFalse(ev.data().contains("quoted_keys"), ev.data());
                    assertFalse(ev.data().contains("tokens_usage"), ev.data());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("ask: clarify 事件重建，default 字段正确映射（Java 关键字绕过）")
    void ask_rebuildsClarifyDefault() {
        when(port.ask(any())).thenReturn(Flux.just(
                sse("intent", "{\"anchor\":null,\"ambiguous\":true,\"candidates\":[\"ai-tutoring\",\"rag-system\"]}"),
                sse("clarify", "{\"message\":\"您的问题涉及多个功能，请明确功能名。\",\"candidates\":[\"ai-tutoring\",\"rag-system\"],\"default\":\"ai-tutoring\"}"),
                sse("done", "{\"answer\":\"\",\"trace_id\":null}")));

        StepVerifier.create(appService.ask(command()))
                .expectNextMatches(ev -> "permission".equals(ev.event()))
                .expectNextMatches(ev -> "intent".equals(ev.event()))
                .assertNext(ev -> {
                    assertTrue("clarify".equals(ev.event()));
                    assertTrue(ev.data().contains("\"default\":\"ai-tutoring\""), ev.data());
                    assertFalse(ev.data().contains("defaultModule"), ev.data());
                })
                .expectNextMatches(ev -> "done".equals(ev.event()))
                .verifyComplete();
    }
}
