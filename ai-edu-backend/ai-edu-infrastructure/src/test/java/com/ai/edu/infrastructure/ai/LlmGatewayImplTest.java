package com.ai.edu.infrastructure.ai;

import com.ai.edu.common.exception.LlmGatewayException;
import com.ai.edu.domain.shared.model.AllowedModelsResponse;
import com.ai.edu.domain.shared.model.ChatRequest;
import com.ai.edu.domain.shared.model.ChatResponse;
import com.ai.edu.domain.shared.model.ModelsResponse;
import com.ai.edu.domain.shared.model.ScenesResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmGatewayImpl 单元测试
 * 使用 MockWebServer 模拟 Python 服务
 *
 * @author AI Edu Platform
 */
class LlmGatewayImplTest {

    private MockWebServer mockWebServer;
    private LlmGatewayImpl llmGateway;
    private WebClient llmWebClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(9527);

        // 创建 WebClient，指向 MockWebServer
        llmWebClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader("x-internal-token", "test-internal-token")
                .build();

        llmGateway = new LlmGatewayImpl();
        // 使用反射注入 llmWebClient
        try {
            var field = LlmGatewayImpl.class.getDeclaredField("llmWebClient");
            field.setAccessible(true);
            field.set(llmGateway, llmWebClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ==================== chat() 测试 ====================

    @Test
    @DisplayName("chat() 成功 - 正常返回对话响应")
    void chat_Success() {
        // Given: 模拟 Python 服务返回成功响应
        mockChatSuccess("这是一个测试响应");

        ChatRequest request = ChatRequest.builder()
                .message("你好")
                .userId(1001L)
                .scene("homework_help")
                .build();

        // When: 调用 chat 方法
        Mono<ChatResponse> result = llmGateway.chat(request);

        // Then: 验证响应
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("这是一个测试响应", response.getResponse());
                    assertEquals("test-session-id", response.getSessionId());
                    assertEquals("zhipu/glm-4-flash", response.getModelUsed());
                    assertNotNull(response.getUsage());
                    assertEquals(10L, response.getUsage().getTotalTokens());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat() 服务不可用 - 连接失败")
    void chat_ServiceUnavailable() throws IOException {
        // Given: 关闭 MockWebServer 模拟服务不可用
        mockWebServer.shutdown();

        // 重新创建 WebClient 指向一个无效地址
        WebClient unavailableClient = WebClient.builder()
                .baseUrl("http://localhost:9999")
                .build();

        try {
            var field = LlmGatewayImpl.class.getDeclaredField("llmWebClient");
            field.setAccessible(true);
            field.set(llmGateway, unavailableClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ChatRequest request = ChatRequest.builder()
                .message("你好")
                .userId(1001L)
                .build();

        // When: 调用 chat 方法
        Mono<ChatResponse> result = llmGateway.chat(request);

        // Then: 验证抛出 LlmGatewayException
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof LlmGatewayException &&
                    ((LlmGatewayException) throwable).getCode().equals("60001"))
                .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("chat() 参数无效 - 返回 400 错误")
    void chat_InvalidParams() {
        // Given: 模拟 Python 服务返回 400 错误
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Invalid request parameters\"}"));

        ChatRequest request = ChatRequest.builder()
                .message("")
                .userId(1001L)
                .build();

        // When: 调用 chat 方法
        Mono<ChatResponse> result = llmGateway.chat(request);

        // Then: 验证抛出 LlmGatewayException
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof LlmGatewayException &&
                    ((LlmGatewayException) throwable).getCode().equals("60005"))
                .verify();
    }

    @Test
    @DisplayName("chat() 模型不允许 - 返回 403 错误")
    void chat_ModelNotAllowed() {
        // Given: 模拟 Python 服务返回 403 错误
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Model not allowed\"}"));

        ChatRequest request = ChatRequest.builder()
                .message("你好")
                .userId(1001L)
                .model("restricted-model")
                .build();

        // When: 调用 chat 方法
        Mono<ChatResponse> result = llmGateway.chat(request);

        // Then: 验证抛出 LlmGatewayException
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof LlmGatewayException &&
                    ((LlmGatewayException) throwable).getCode().equals("60002"))
                .verify();
    }

    // ==================== chatStream() 测试 ====================

    @Test
    @DisplayName("chatStream() SSE 流式响应成功")
    void chatStream_Success() {
        // Given: 模拟 SSE 流式响应
        mockSseStreamResponse();

        ChatRequest request = ChatRequest.builder()
                .message("请解释一下牛顿第一定律")
                .userId(1001L)
                .scene("knowledge_qa")
                .build();

        // When: 调用 chatStream 方法
        Flux<ServerSentEvent<String>> result = llmGateway.chatStream(request);

        // Then: 验证 SSE 事件流
        StepVerifier.create(result.take(2))
                .assertNext(event -> {
                    assertNotNull(event);
                    // SSE event 可能有 null event type
                })
                .assertNext(event -> {
                    assertNotNull(event);
                })
                .verifyComplete();
    }

    // ==================== getAllowedModels() 测试 ====================

    @Test
    @DisplayName("getAllowedModels() 成功 - 返回允许调用的模型列表")
    void getAllowedModels_Success() {
        // Given: 模拟 Python 服务返回模型列表
        mockAllowedModelsResponse();

        // When: 调用 getAllowedModels 方法
        Mono<AllowedModelsResponse> result = llmGateway.getAllowedModels();

        // Then: 验证响应
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getAllowedModels());
                    assertEquals(2, response.getAllowedModels().size());
                    assertEquals("zhipu/glm-4-flash", response.getDefaultModel());

                    assertEquals("zhipu", response.getAllowedModels().get(0).getProvider());
                    assertEquals("glm-4-flash", response.getAllowedModels().get(0).getModel());
                    assertTrue(response.getAllowedModels().get(0).getFree());
                })
                .verifyComplete();
    }

    // ==================== getModels() 测试 ====================

    @Test
    @DisplayName("getModels() 成功 - 返回所有模型列表")
    void getModels_Success() {
        // Given: 模拟 Python 服务返回所有模型列表
        mockModelsResponse();

        // When: 调用 getModels 方法
        Mono<ModelsResponse> result = llmGateway.getModels();

        // Then: 验证响应
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getProviders());
                    assertEquals(2, response.getProviders().size());

                    assertEquals("zhipu", response.getProviders().get(0).getName());
                    assertEquals("智谱 AI", response.getProviders().get(0).getDisplayName());
                    assertEquals(2, response.getProviders().get(0).getModels().size());
                })
                .verifyComplete();
    }

    // ==================== getScenes() 测试 ====================

    @Test
    @DisplayName("getScenes() 成功 - 返回场景列表")
    void getScenes_Success() {
        // Given: 模拟 Python 服务返回场景列表
        mockScenesResponse();

        // When: 调用 getScenes 方法
        Mono<ScenesResponse> result = llmGateway.getScenes();

        // Then: 验证响应
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getScenes());
                    assertEquals(3, response.getScenes().size());

                    assertEquals("homework_help", response.getScenes().get(0).getCode());
                    assertEquals("作业辅导", response.getScenes().get(0).getDescription());
                    assertEquals("zhipu", response.getScenes().get(0).getDefaultProvider());
                    assertEquals("glm-4-flash", response.getScenes().get(0).getDefaultModel());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("chat() 验证请求头和请求体")
    void chat_VerifyRequestHeadersAndBody() throws InterruptedException {
        // Given
        mockChatSuccess("测试响应");

        ChatRequest request = ChatRequest.builder()
                .message("测试消息")
                .userId(1001L)
                .scene("test_scene")
                .build();

        // When
        llmGateway.chat(request).block(Duration.ofSeconds(5));

        // Then: 验证请求
        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().startsWith("/api/llm/chat"));
        assertEquals("application/json", recordedRequest.getHeader("Content-Type"));
        assertEquals("test-internal-token", recordedRequest.getHeader("x-internal-token"));

        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("测试消息"));
        assertTrue(requestBody.contains("1001"));
        assertTrue(requestBody.contains("test_scene"));
    }

    // ==================== Mock 辅助方法 ====================

    private void mockChatSuccess(String response) {
        String responseBody = String.format(
                "{\"response\":\"%s\",\"session_id\":\"test-session-id\",\"model_used\":\"zhipu/glm-4-flash\"," +
                "\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":5,\"total_tokens\":10}}",
                response
        );
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));
    }

    private void mockSseStreamResponse() {
        String sseBody = "data: {\"content\":\"牛顿第一定律\"}\n\n" +
                "data: {\"content\":\"又称为惯性定律\"}\n\n" +
                "event: done\ndata: {}\n\n";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody));
    }

    private void mockAllowedModelsResponse() {
        String responseBody = "{\"code\":\"00000\",\"message\":\"success\",\"data\":{" +
                "\"allowed_models\":[" +
                "{\"provider\":\"zhipu\",\"model\":\"glm-4-flash\",\"full_name\":\"zhipu/glm-4-flash\"," +
                "\"display_name\":\"GLM-4-Flash\",\"free\":true,\"supports_tools\":true,\"supports_vision\":false}," +
                "{\"provider\":\"openai\",\"model\":\"gpt-4o-mini\",\"full_name\":\"openai/gpt-4o-mini\"," +
                "\"display_name\":\"GPT-4o Mini\",\"free\":true,\"supports_tools\":true,\"supports_vision\":true}" +
                "],\"default_model\":\"zhipu/glm-4-flash\"}}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));
    }

    private void mockModelsResponse() {
        String responseBody = "{\"code\":\"00000\",\"message\":\"success\",\"data\":{" +
                "\"providers\":[" +
                "{\"name\":\"zhipu\",\"display_name\":\"智谱 AI\",\"models\":[" +
                "{\"model\":\"glm-4-flash\",\"display_name\":\"GLM-4-Flash\",\"free\":true}," +
                "{\"model\":\"glm-4\",\"display_name\":\"GLM-4\",\"free\":false}" +
                "]}," +
                "{\"name\":\"openai\",\"display_name\":\"OpenAI\",\"models\":[" +
                "{\"model\":\"gpt-4o-mini\",\"display_name\":\"GPT-4o Mini\",\"free\":true}" +
                "]}" +
                "]}}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));
    }

    private void mockScenesResponse() {
        String responseBody = "{\"code\":\"00000\",\"message\":\"success\",\"data\":{" +
                "\"scenes\":[" +
                "{\"code\":\"homework_help\",\"default_provider\":\"zhipu\",\"default_model\":\"glm-4-flash\",\"description\":\"作业辅导\"}," +
                "{\"code\":\"error_analysis\",\"default_provider\":\"zhipu\",\"default_model\":\"glm-4-flash\",\"description\":\"错题分析\"}," +
                "{\"code\":\"knowledge_qa\",\"default_provider\":\"zhipu\",\"default_model\":\"glm-4-flash\",\"description\":\"知识问答\"}" +
                "]}}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));
    }
}