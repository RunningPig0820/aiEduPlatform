package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.TopicVectorMetadata;
import com.ai.edu.domain.learning.model.contract.TopicVectorNeighbor;
import com.ai.edu.domain.learning.model.contract.TopicVectorPutRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题型名向量 Java 桥（{@link TopicVectorClient}）测试——Python 向量端点契约对齐
 * （tasks 2.3.3）：put 恒带 vector_type="topic" + metadata；query 解析 {@code data["vectors"]}
 * （非 hits）；空 vectors → Optional.empty；端点异常 → TutoringAgentException（调用方降级）。
 */
class TopicVectorClientTest {

    private static final String VECTOR_TYPE = "topic";

    /** mock WebClient 链 + 持有 bodySpec（捕获请求体）/responseSpec（stub 响应）。 */
    private static class Ctx {
        final TopicVectorClient client;
        final WebClient.RequestBodySpec bodySpec;
        final WebClient.ResponseSpec responseSpec;

        Ctx(TopicVectorClient client, WebClient.RequestBodySpec bodySpec, WebClient.ResponseSpec responseSpec) {
            this.client = client;
            this.bodySpec = bodySpec;
            this.responseSpec = responseSpec;
        }
    }

    private Ctx buildClient() {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        TopicVectorClient client = new TopicVectorClient();
        ReflectionTestUtils.setField(client, "tutoringWebClient", webClient);
        return new Ctx(client, bodySpec, responseSpec);
    }

    // ---------- putVector ----------

    @Test
    @DisplayName("putVector: 成功 → 返回落库 key（{ok,key} 响应）")
    void putVector_success() {
        Ctx ctx = buildClient();
        TopicVectorClient.VectorPutResponse resp = new TopicVectorClient.VectorPutResponse();
        resp.setOk(true);
        resp.setKey("q_5001");
        when(ctx.responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(resp));

        String key = ctx.client.putVector(TopicVectorPutRequest.builder()
                .key("q_5001").text("鸡兔同笼")
                .metadata(TopicVectorMetadata.builder().studentId("1001").build())
                .build());

        assertEquals("q_5001", key);
        // 契约：vector_type 由桥内部强制为 "topic"（Python 必填路由键，无缺省）
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ctx.bodySpec).bodyValue(captor.capture());
        TopicVectorPutRequest sent = (TopicVectorPutRequest) captor.getValue();
        assertEquals(VECTOR_TYPE, sent.getVectorType(), "vector_type 必须恒为 topic");
        assertEquals("鸡兔同笼", sent.getText());
        assertEquals("1001", sent.getMetadata().getStudentId());
    }

    @Test
    @DisplayName("putVector: 上游异常 → TutoringAgentException（调用方降级回退字符规则）")
    void putVector_upstreamError_wrapped() {
        Ctx ctx = buildClient();
        when(ctx.responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(new RuntimeException("upstream down")));

        TopicVectorClient client = ctx.client;
        TutoringAgentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                TutoringAgentException.class,
                () -> client.putVector(TopicVectorPutRequest.builder().key("q_5001").text("鸡兔同笼").build()));
        assertTrue(ex.getMessage().contains("向量"), "错误信息应含'向量'： " + ex.getMessage());
    }

    // ---------- queryNearestTop1 ----------

    @Test
    @DisplayName("queryNearestTop1: 命中 → Optional.of(最近邻)，解析 data[\"vectors\"] 的 distance/metadata")
    void queryNearestTop1_hit() {
        Ctx ctx = buildClient();
        TopicVectorClient.VectorQueryResponse resp = new TopicVectorClient.VectorQueryResponse();
        resp.setVectors(List.of(TopicVectorNeighbor.builder()
                .key("q_5001")
                .metadata(TopicVectorMetadata.builder().topicLabel("鸡兔同笼").canonicalLabel("鸡兔同笼").build())
                .distance(0.12)
                .build()));
        when(ctx.responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(resp));

        Optional<TopicVectorNeighbor> result = ctx.client.queryNearestTop1("鸡兔同笼问题");

        assertTrue(result.isPresent());
        assertEquals("q_5001", result.get().getKey());
        assertEquals(0.12, result.get().getDistance());
        assertEquals("鸡兔同笼", result.get().getMetadata().getCanonicalLabel());
    }

    @Test
    @DisplayName("queryNearestTop1: 空 vectors（库空 / put 未异步生效）→ Optional.empty")
    void queryNearestTop1_emptyVectors_empty() {
        Ctx ctx = buildClient();
        TopicVectorClient.VectorQueryResponse resp = new TopicVectorClient.VectorQueryResponse();
        resp.setVectors(List.of());
        when(ctx.responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(resp));

        Optional<TopicVectorNeighbor> result = ctx.client.queryNearestTop1("鸡兔同笼问题");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("queryNearestTop1: 上游异常 → TutoringAgentException（调用方降级建新 canonical）")
    void queryNearestTop1_upstreamError_wrapped() {
        Ctx ctx = buildClient();
        when(ctx.responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(new RuntimeException("upstream down")));

        TutoringAgentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                TutoringAgentException.class,
                () -> ctx.client.queryNearestTop1("鸡兔同笼问题"));
        assertTrue(ex.getMessage().contains("向量"), "错误信息应含'向量'： " + ex.getMessage());
    }

    // ---------- 序列化契约（snake_case，Python 端字段名错则 400） ----------

    @Test
    @DisplayName("序列化契约: put 请求 body 字段 snake_case（vector_type / student_id / canonical_label）")
    void putRequest_serializesSnakeCase() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TopicVectorPutRequest req = TopicVectorPutRequest.builder()
                .key("q_5001").text("鸡兔同笼").vectorType(VECTOR_TYPE)
                .metadata(TopicVectorMetadata.builder()
                        .studentId("1001").topicLabel("鸡兔同笼").canonicalLabel("鸡兔同笼")
                        .timestamp("2026-08-18T10:00:00").build())
                .build();

        String json = mapper.writeValueAsString(req);

        assertTrue(json.contains("\"vector_type\":\"topic\""), "缺 vector_type: " + json);
        assertTrue(json.contains("\"student_id\":\"1001\""), "缺 student_id: " + json);
        assertTrue(json.contains("\"canonical_label\":\"鸡兔同笼\""), "缺 canonical_label: " + json);
    }
}
