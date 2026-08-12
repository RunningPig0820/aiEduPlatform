package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.DecideContext;
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
                // meta 事件含 Python 调试字段 reason（Java 不建模）→ 原样透传，容忍未知字段
                sse("meta", "{\"type\":\"hint\",\"reason\":\"debug field\",\"eval\":{\"correct\":false,\"emotion\":\"NEUTRAL\"}}"),
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
}
