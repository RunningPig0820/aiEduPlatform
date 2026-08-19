package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.learning.model.contract.SubjectClassifyRequest;
import com.ai.edu.domain.learning.model.contract.SubjectClassifyResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * subject-classify Java 桥（{@link SubjectClassifyClient}）测试——Python 学科分类端点契约对齐
 * （tutoring-subject-gate）：请求 snake_case（content/image_url）、响应 {subject} 映射、
 * 空响应/异常绝不抛异常（降级空 subject，编排层按 math 放行）。
 */
class SubjectClassifyClientTest {

    /** mock WebClient 链 + 持有 bodySpec（捕获请求体）/responseSpec（stub 响应）。 */
    private static class Ctx {
        final SubjectClassifyClient client;
        final WebClient.RequestBodySpec bodySpec;
        final WebClient.ResponseSpec responseSpec;

        Ctx(SubjectClassifyClient client, WebClient.RequestBodySpec bodySpec, WebClient.ResponseSpec responseSpec) {
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

        SubjectClassifyClient client = new SubjectClassifyClient();
        ReflectionTestUtils.setField(client, "tutoringWebClient", webClient);
        return new Ctx(client, bodySpec, responseSpec);
    }

    // ---------- 成功分类 ----------

    @Test
    @DisplayName("classify: 成功返回 subject=math → isMath()=true，请求体携带 content/image_url")
    void classify_successMath() {
        Ctx ctx = buildClient();
        SubjectClassifyResult resp = new SubjectClassifyResult("math");
        when(ctx.responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(resp));

        SubjectClassifyResult result = ctx.client.classify(
                SubjectClassifyRequest.builder().content("鸡兔同笼").imageUrl("https://cos/q.png").build());

        assertTrue(result.isMath());
        assertEquals("math", result.getSubject());
        // 契约：请求体字段 snake_case（image_url 对齐 Python）
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ctx.bodySpec).bodyValue(captor.capture());
        SubjectClassifyRequest sent = (SubjectClassifyRequest) captor.getValue();
        assertEquals("鸡兔同笼", sent.getContent());
        assertEquals("https://cos/q.png", sent.getImageUrl());
    }

    @Test
    @DisplayName("classify: 成功返回 subject=physics → 映射到 DTO，非 math")
    void classify_successPhysics() {
        Ctx ctx = buildClient();
        when(ctx.responseSpec.bodyToMono(any(Class.class)))
                .thenReturn(Mono.just(new SubjectClassifyResult("physics")));

        SubjectClassifyResult result = ctx.client.classify(
                SubjectClassifyRequest.builder().content("自由落体").build());

        assertEquals("physics", result.getSubject());
        assertFalse(result.isMath());
    }

    // ---------- 降级（绝不抛异常） ----------

    @Test
    @DisplayName("classify: 空响应 {}（subject 缺失）→ subject=null，不抛异常（Java 降级放行）")
    void classify_emptySubject_tolerated() {
        Ctx ctx = buildClient();
        when(ctx.responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(new SubjectClassifyResult()));

        SubjectClassifyResult result = ctx.client.classify(
                SubjectClassifyRequest.builder().content("未知").build());

        assertNotNull(result);
        assertNull(result.getSubject());
        assertFalse(result.isMath(), "空 subject 不视为 math，由编排层按 math 放行");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("classify: 上游异常（超时/5xx）→ 降级空 subject，绝不抛异常")
    void classify_upstreamError_degradesEmpty() {
        Ctx ctx = buildClient();
        when(ctx.responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(new RuntimeException("upstream down")));

        SubjectClassifyResult result = ctx.client.classify(
                SubjectClassifyRequest.builder().content("自由落体").build());

        assertNotNull(result);
        assertNull(result.getSubject());
        assertTrue(result.isEmpty(), "失败返回空结果（不抛异常），编排层按 math 放行");
    }

    @Test
    @DisplayName("classify: 响应 null（空 body → Mono.empty）→ 降级空 subject，不抛异常")
    void classify_nullResponse_degradesEmpty() {
        Ctx ctx = buildClient();
        when(ctx.responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.empty());

        SubjectClassifyResult result = ctx.client.classify(
                SubjectClassifyRequest.builder().content("自由落体").build());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ---------- 序列化契约（snake_case，Python 端字段名错则 400） ----------

    @Test
    @DisplayName("契约 CON-001: 请求 body 字段 snake_case（image_url）")
    void request_serializesSnakeCase() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SubjectClassifyRequest req = SubjectClassifyRequest.builder()
                .content("自由落体运动的问题").imageUrl("https://cos/q.png").build();

        String json = mapper.writeValueAsString(req);

        assertTrue(json.contains("\"content\":\"自由落体运动的问题\""), "缺 content: " + json);
        assertTrue(json.contains("\"image_url\":\"https://cos/q.png\""), "缺 image_url: " + json);
        assertFalse(json.contains("imageUrl"), "不应出现 camelCase imageUrl: " + json);
    }

    @Test
    @DisplayName("契约 CON-002: 响应 {\"subject\":\"physics\"} → 映射到 DTO")
    void response_deserializesSubject() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        SubjectClassifyResult result = mapper.readValue("{\"subject\":\"physics\"}", SubjectClassifyResult.class);

        assertEquals("physics", result.getSubject());
    }

    @Test
    @DisplayName("契约 CON-003: 响应 {}（subject 缺失）→ subject=null，不抛错")
    void response_missingSubject_tolerated() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        SubjectClassifyResult result = mapper.readValue("{}", SubjectClassifyResult.class);

        assertNotNull(result);
        assertNull(result.getSubject());
        assertTrue(result.isEmpty());
    }
}
