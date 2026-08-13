package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.DecideContext;
import com.ai.edu.domain.learning.model.contract.GenerateContext;
import com.ai.edu.domain.learning.service.TutoringConfig;
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

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * decide 流式消费测试（D7 演进：decide 从「同步取 meta」改「SSE 原始事件流」，
 * 客户端原样透传，meta 提取与空流/无 meta 判定全部上移至编排层）。
 */
class TutoringLlmClientTest {

    private TutoringLlmClient buildClient(Flux<ServerSentEvent<String>> decideFlux) {
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
                .thenReturn(decideFlux);

        TutoringLlmClient client = new TutoringLlmClient();
        ReflectionTestUtils.setField(client, "tutoringWebClient", webClient);
        return client;
    }

    private ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.<String>builder().event(event).data(data).build();
    }

    @Test
    @DisplayName("decideStream: 原样透传全部 SSE 事件（thinking/agent/meta/done），保序")
    void decideStream_relaysAllEvents() {
        TutoringLlmClient client = buildClient(Flux.just(
                sse("thinking", "{\"content\":\"先识别题型\"}"),
                sse("agent", "{\"level\":\"sub\",\"stage\":\"perceive\",\"label\":\"读取题目\"}"),
                // meta 事件含 Python reason/question_kps（Java 已建模）→ 客户端仅原样透传原始 JSON，语义由编排层解析
                sse("meta", "{\"type\":\"hint\",\"reason\":\"debug field\",\"question_kps\":[\"二元一次方程组\"],\"eval\":{\"correct\":false,\"emotion\":\"NEUTRAL\"}}"),
                sse("done", "{\"model_used\":\"doubao/doubao-seed-2-0-lite\"}")));

        Flux<ServerSentEvent<String>> flux =
                client.decideStream(DecideContext.builder().history(List.of()).build());

        StepVerifier.create(flux)
                .assertNext(ev -> assertEquals("thinking", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
    }

    @Test
    @DisplayName("decideStream: 上游错误 → 包装为 TutoringAgentException（50005）")
    void decideStream_errorWrapped() {
        TutoringLlmClient client = buildClient(Flux.error(new RuntimeException("upstream down")));

        StepVerifier.create(client.decideStream(DecideContext.builder().history(List.of()).build()))
                .expectError(TutoringAgentException.class)
                .verify();
    }

    @Test
    @DisplayName("decideStream: 空流正常完成（无 meta 判定上移至编排层，客户端不抛）")
    void decideStream_emptyCompletes() {
        TutoringLlmClient client = buildClient(Flux.empty());

        StepVerifier.create(client.decideStream(DecideContext.builder().history(List.of()).build()))
                .verifyComplete();
    }

    @Test
    @DisplayName("generate: 上游挂起无响应 → 超时包装为 TutoringAgentException（GENERATE_TIMEOUT 兜底，SSE 不无限停滞）")
    void generate_hungStream_timesOut() {
        // 注入短超时 config，模拟 Python generate 挂起（Flux.never 永不吐事件）。
        // 回归：曾漏配 .timeout()，generate 挂起则 SSE 在 meta 后无限停滞 → 无 done、
        // 前端刷新丢 sessionId（会话被拆成多个单轮孤儿的根因之一）。
        TutoringConfig config = mock(TutoringConfig.class);
        when(config.generateTimeout()).thenReturn(Duration.ofMillis(200));
        when(config.generatePath()).thenReturn("/generate");

        TutoringLlmClient client = buildClient(Flux.never());
        ReflectionTestUtils.setField(client, "tutoringConfig", config);

        StepVerifier.withVirtualTime(() ->
                        client.generate(GenerateContext.builder().actionType("hint").history(List.of()).build()))
                .expectSubscription()
                .thenAwait(Duration.ofMillis(250))
                .expectErrorSatisfies(e -> {
                    assertEquals(TutoringAgentException.class, e.getClass());
                    assertEquals("答疑生成服务暂不可用", e.getMessage());
                })
                .verify();
    }
}
