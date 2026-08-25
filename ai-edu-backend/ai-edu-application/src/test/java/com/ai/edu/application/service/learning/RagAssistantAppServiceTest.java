package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.command.RagAskCommand;
import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import com.ai.edu.domain.learning.model.contract.RagQualityScore;
import com.ai.edu.domain.learning.service.RagAssistantPort;
import com.ai.edu.domain.learning.service.RagQualityGrader;
import com.ai.edu.domain.shared.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RagAssistantAppService SSE 中继测试（阶段 1）：permission 前置（含 traceId）、
 * Python snake 事件 → 前端 camel 重建、done traceId 回显一致。
 */
class RagAssistantAppServiceTest {

    private RagAssistantAppService appService;
    private RagAssistantPort port;
    private RagQualityGrader grader;
    private RedisService redis;

    @BeforeEach
    void setUp() {
        port = mock(RagAssistantPort.class);
        grader = mock(RagQualityGrader.class);
        redis = mock(RedisService.class);
        when(grader.grade(any(), any(), any(), any())).thenReturn(reactor.core.publisher.Mono.empty());
        appService = new RagAssistantAppService();
        ReflectionTestUtils.setField(appService, "ragAssistantPort", port);
        ReflectionTestUtils.setField(appService, "ragQualityGrader", grader);
        ReflectionTestUtils.setField(appService, "redisService", redis);
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
    @DisplayName("askStages: 非流式返回不抛 NPE（reason 为 null，Map.of 会崩，须用 LinkedHashMap）")
    void askStages_noNpeOnNullReason() {
        Map<String, Object> result = appService.askStages(command());
        assertTrue(result.containsKey("answer"));
        assertTrue(result.containsKey("reason"));
        assertNull(result.get("reason"));
        assertTrue(result.containsKey("stages"));
    }

    @Test
    @DisplayName("ask: 全量时序 permission→intent→rewrite→rerank→token*→done（RAG-SSE-001），token 逐块重建、done 带 tokensUsage")
    void ask_fullTokenSequence() {
        when(port.ask(any())).thenAnswer(inv -> {
            RagAskRequest req = inv.getArgument(0);
            return Flux.just(
                    sse("intent", "{\"anchor\":\"rag-system\",\"category\":\"项目介绍\"}"),
                    sse("rewrite", "{\"original_question\":\"这个项目的整体架构是什么？\",\"rewritten_query\":\"项目 整体 架构\"}"),
                    sse("rerank", "{\"blocks\":[{\"block_id\":\"b1\",\"title\":\"架构\",\"summary\":\"摘要\",\"file_path\":\"4.完善文档/02-…md\",\"score\":0.0323}]}"),
                    sse("token", "{\"text\":\"RAG 项目\"}"),
                    sse("token", "{\"text\":\"的整体架构\"}"),
                    sse("done", "{\"answer\":\"RAG 项目的整体架构\",\"quoted_keys\":[\"b1\"],\"tokens_usage\":{\"prompt_tokens\":320,\"completion_tokens\":140,\"cache_hit_tokens\":0,\"total_tokens\":460},\"trace_id\":\"" + req.getTraceId() + "\",\"suggestions\":[],\"reason\":null}"));
        });

        StepVerifier.create(appService.ask(command()))
                .expectNextMatches(ev -> "permission".equals(ev.event()))
                .expectNextMatches(ev -> "intent".equals(ev.event()))
                .expectNextMatches(ev -> "rewrite".equals(ev.event()))
                .expectNextMatches(ev -> "rerank".equals(ev.event()))
                .assertNext(ev -> {
                    assertEquals("token", ev.event());
                    assertTrue(ev.data().contains("\"text\":\"RAG 项目\""), ev.data());
                    assertFalse(ev.data().contains("snake"), ev.data());
                })
                .assertNext(ev -> {
                    assertEquals("token", ev.event());
                    assertTrue(ev.data().contains("\"text\":\"的整体架构\""), ev.data());
                })
                .assertNext(ev -> {
                    assertEquals("done", ev.event());
                    assertTrue(ev.data().contains("\"tokensUsage\""), ev.data());
                    assertTrue(ev.data().contains("\"promptTokens\":320"), ev.data());
                    assertTrue(ev.data().contains("\"completionTokens\":140"), ev.data());
                    assertTrue(ev.data().contains("\"cacheHitTokens\":0"), ev.data());
                    assertTrue(ev.data().contains("\"totalTokens\":460"), ev.data());
                    assertFalse(ev.data().contains("tokens_usage"), ev.data());
                    assertFalse(ev.data().contains("cache_hit_tokens"), ev.data());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("evalReport：Python snake_case 报告解析为 camelCase DTO（hit_at_3→hitAt3、样本细节、running）")
    void evalReport_snakeToCamel() {
        String pyJson = "{\"version\":\"v1\",\"count\":15,\"hit_at_3\":0.8,\"quality_avg\":4.2,"
                + "\"avg_latency_ms\":2300,\"avg_cost_yuan\":0.012,\"judged_ratio\":1.0,"
                + "\"precision_at_3\":0.6,\"quoted_valid_ratio\":0.9,"
                + "\"evaluated_at\":\"2026-08-25T11:02:00\",\"hit_cases\":12,\"avg_tokens\":3912,"
                + "\"total_cost_yuan\":0.0806,\"running\":false}";
        when(port.evalReport()).thenReturn(reactor.core.publisher.Mono.just(pyJson));

        var report = appService.evalReport().block();
        assertTrue(report.getHitAt3() == 0.8, String.valueOf(report));
        assertTrue(report.getCount() == 15);
        assertTrue(report.getAvgLatencyMs() == 2300L);
        assertTrue(report.getQuotedValidRatio() == 0.9);
        assertTrue("v1".equals(report.getVersion()));
        assertTrue("2026-08-25T11:02:00".equals(report.getEvaluatedAt()));
        assertTrue(report.getHitCases() == 12);
        assertTrue(report.getAvgTokens() == 3912);
        assertTrue(report.getTotalCostYuan() == 0.0806);
        assertFalse(report.getRunning());
    }

    @Test
    @DisplayName("evalRun：已有一轮在跑 → already_running 解析为 alreadyRunning（幂等非错误）")
    void evalRun_parsesRunningState() {
        when(port.evalRun()).thenReturn(reactor.core.publisher.Mono.just(
                "{\"running\":true,\"already_running\":true}"));

        var run = appService.evalRun().block();
        assertTrue(run.getRunning());
        assertTrue(run.getAlreadyRunning());
    }

    @Test
    @DisplayName("ask: done（非空答案+正常轮）触发异步质量打分，传 question/answer/quotedKeys/块摘要")
    void ask_schedulesGradeOnDone() {
        when(port.ask(any())).thenAnswer(inv -> Flux.just(
                sse("intent", "{\"anchor\":\"rag-system\"}"),
                sse("rerank", "{\"blocks\":[{\"block_id\":\"b1\",\"title\":\"架构\",\"summary\":\"分四层……\",\"file_path\":\"f\",\"score\":0.3}]}"),
                sse("done", "{\"answer\":\"RAG 项目的整体架构是……\",\"quoted_keys\":[\"b1\"],"
                        + "\"tokens_usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"cache_hit_tokens\":0,\"total_tokens\":2},"
                        + "\"trace_id\":\"trc\",\"suggestions\":[],\"reason\":null}")));
        when(grader.grade(any(), any(), any(), any()))
                .thenReturn(reactor.core.publisher.Mono.just(
                        RagQualityScore.builder().score(4).reason("ok").build()));

        StepVerifier.create(appService.ask(command()))
                .expectNextCount(4) // permission + intent + rerank + done
                .verifyComplete();

        // grade 调用在 doOnNext 内同步发生（订阅计分是异步的），可直接 verify
        verify(grader).grade(eq("这个项目的整体架构是什么？"), eq("RAG 项目的整体架构是……"),
                eq(List.of("b1")), eq(List.of("【架构】分四层……")));
    }

    @Test
    @DisplayName("ask: boundary 轮（reason=low_confidence）不触发质量打分")
    void ask_skipsGradeOnBoundary() {
        when(port.ask(any())).thenReturn(Flux.just(
                sse("intent", "{\"anchor\":\"rag-system\"}"),
                sse("done", "{\"answer\":\"\",\"tokens_usage\":{\"prompt_tokens\":0,\"completion_tokens\":0,"
                        + "\"cache_hit_tokens\":0,\"total_tokens\":0},\"trace_id\":\"trc\",\"suggestions\":[],\"reason\":\"low_confidence\"}")));

        StepVerifier.create(appService.ask(command()))
                .expectNextCount(3) // permission + intent + done
                .verifyComplete();

        verify(grader, never()).grade(any(), any(), any(), any());
    }

    @Test
    @DisplayName("evalReport: 并入 realConversation（读 Redis 聚合 → avgQuality/quotedRatio/avgLatencyMs）")
    void evalReport_mergesRealConversation() {
        when(port.evalReport()).thenReturn(reactor.core.publisher.Mono.just(
                "{\"version\":\"v1\",\"count\":6,\"hit_at_3\":0.667}"));
        when(redis.get("rag:assistant:eval:recent"))
                .thenReturn("{\"count\":10,\"sum_quality\":38,\"quoted_count\":9,\"sum_latency_ms\":50000}");

        var report = appService.evalReport().block();
        assertEquals(3.8, report.getRealConversation().getAvgQuality());
        assertEquals(0.9, report.getRealConversation().getQuotedRatio());
        assertEquals(5000L, report.getRealConversation().getAvgLatencyMs());
        assertEquals(10, report.getRealConversation().getCount());
    }

    @Test
    @DisplayName("evalReport: 无真实对话累计（Redis 无 key）→ realConversation 为 null")
    void evalReport_noRealConversationWhenRedisEmpty() {
        when(port.evalReport()).thenReturn(reactor.core.publisher.Mono.just(
                "{\"version\":\"v1\",\"count\":6,\"hit_at_3\":0.667}"));
        when(redis.get("rag:assistant:eval:recent")).thenReturn(null);

        var report = appService.evalReport().block();
        assertNull(report.getRealConversation());
    }

    @Test
    @DisplayName("ask: rerank/boundary 事件重建为 camel（blocks/filePath、boundary 话术）")
    void ask_rebuildsRerankAndBoundary() {
        when(port.ask(any())).thenReturn(Flux.just(
                sse("intent", "{\"anchor\":\"ai-tutoring\"}"),
                sse("rerank", "{\"blocks\":[{\"block_id\":\"b1\",\"title\":\"块1\",\"summary\":\"摘要\",\"file_path\":\"4.完善文档/02-…md\",\"score\":0.0323}]}"),
                sse("boundary", "{\"message\":\"未找到关联文档，我尚未掌握。\",\"reason\":\"low_confidence\"}"),
                sse("done", "{\"answer\":\"\",\"trace_id\":null}")));

        StepVerifier.create(appService.ask(command()))
                .expectNextMatches(ev -> "permission".equals(ev.event()))
                .expectNextMatches(ev -> "intent".equals(ev.event()))
                .assertNext(ev -> {
                    assertTrue("rerank".equals(ev.event()));
                    assertTrue(ev.data().contains("\"blockId\":\"b1\""), ev.data());
                    assertTrue(ev.data().contains("\"filePath\""), ev.data());
                    assertFalse(ev.data().contains("block_id"), ev.data());
                    assertFalse(ev.data().contains("file_path"), ev.data());
                })
                .assertNext(ev -> {
                    assertTrue("boundary".equals(ev.event()));
                    assertTrue(ev.data().contains("\"reason\":\"low_confidence\""), ev.data());
                })
                .expectNextMatches(ev -> "done".equals(ev.event()))
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
