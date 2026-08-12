package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.ActionMeta;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * decide SSE 流式消费测试（tutoring-agent-protocol BREAKING：decide 从 JSON 改 SSE 流，取 meta 事件）。
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
    @DisplayName("decide: SSE 流取 meta 事件解析 ActionMeta（agent/done 忽略）")
    void decide_extractsMetaFromSse() {
        TutoringLlmClient client = buildClient(Flux.just(
                sse("agent", "{\"level\":\"sub\",\"stage\":\"perceive\",\"label\":\"读取题目\"}"),
                // meta 事件含 Python 调试字段 reason（Java 不建模）→ 必须容忍，不报错
                sse("meta", "{\"type\":\"hint\",\"reason\":\"debug field\",\"eval\":{\"correct\":false,\"emotion\":\"NEUTRAL\"}}"),
                sse("done", "{\"model_used\":\"doubao/doubao-seed-2-0-lite\"}")));

        ActionMeta meta = client.decide(DecideContext.builder().history(List.of()).build());

        assertEquals("hint", meta.getType());
    }

    @Test
    @DisplayName("decide: 流中无 meta（event: error）→ 抛 TutoringAgentException")
    void decide_noMetaEvent_throws() {
        TutoringLlmClient client = buildClient(Flux.just(
                sse("agent", "{\"stage\":\"perceive\"}"),
                sse("error", "{\"detail\":\"failed\"}")));

        assertThrows(TutoringAgentException.class,
                () -> client.decide(DecideContext.builder().history(List.of()).build()));
    }

    @Test
    @DisplayName("decide: 空流（无任何事件）→ 抛 TutoringAgentException")
    void decide_emptyStream_throws() {
        TutoringLlmClient client = buildClient(Flux.empty());
        assertThrows(TutoringAgentException.class,
                () -> client.decide(DecideContext.builder().history(List.of()).build()));
    }
}
