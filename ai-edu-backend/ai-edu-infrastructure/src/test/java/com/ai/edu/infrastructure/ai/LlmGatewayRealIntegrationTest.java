package com.ai.edu.infrastructure.ai;

import com.ai.edu.domain.shared.model.AllowedModelsResponse;
import com.ai.edu.domain.shared.model.ChatRequest;
import com.ai.edu.domain.shared.model.ChatResponse;
import com.ai.edu.domain.shared.model.ModelsResponse;
import com.ai.edu.domain.shared.model.ScenesResponse;
import com.ai.edu.domain.shared.service.LlmGateway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLM Gateway 集成测试
 * 真实调用 Python LLM 服务（端口 9527）
 *
 * 运行条件：Python LLM 服务必须启动
 *
 * 运行方式：
 * 1. 确保 Python LLM 服务在 localhost:9527 运行
 * 2. 运行测试：mvn test -pl ai-edu-infrastructure -Dtest=LlmGatewayRealIntegrationTest
 *
 * @author AI Edu Platform
 */
class LlmGatewayRealIntegrationTest {

    private static final String BASE_URL = "http://127.0.0.1:9527";
    private static final String INTERNAL_TOKEN = "my-secret-token-123";

    private static boolean llmServiceAvailable = false;
    private LlmGateway llmGateway;

    @BeforeAll
    static void checkLlmService() {
        // 检查 Python LLM 服务是否可用
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeader("x-internal-token", INTERNAL_TOKEN)
                    .build();

            String result = webClient.get()
                    .uri("/api/llm/allowed-models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            llmServiceAvailable = result != null;
            System.out.println("[集成测试] ✓ Python LLM 服务可用: " + BASE_URL);
        } catch (Exception e) {
            llmServiceAvailable = false;
            System.out.println("[集成测试] ✗ Python LLM 服务不可用: " + e.getMessage());
            System.out.println("[集成测试] 请确保 Python LLM 服务在 " + BASE_URL + " 运行");
        }
    }

    @BeforeEach
    void setUp() {
        // 创建 WebClient
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60));

        WebClient webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("x-internal-token", INTERNAL_TOKEN)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        // 创建 LlmGateway 实现
        LlmGatewayImpl gateway = new LlmGatewayImpl();
        gateway.setLlmWebClient(webClient);
        this.llmGateway = gateway;
    }

    // ==================== chat() 真实调用测试 ====================

    @Test
    @DisplayName("[真实调用] chat() 同步对话成功")
    @EnabledIf("isLlmServiceAvailable")
    void chat_RealCall_Success() {
        // Given: 构建请求
        ChatRequest request = ChatRequest.builder()
                .message("你好，请用一句话介绍自己")
                .userId(1001L)
                .scene("page_assistant")
                .build();

        // When: 调用 LLM Gateway
        ChatResponse response = llmGateway.chat(request)
                .timeout(Duration.ofSeconds(30))
                .block();

        // Then: 验证响应
        assertNotNull(response, "响应不能为空");
        assertNotNull(response.getResponse(), "响应内容不能为空");
        assertNotNull(response.getModelUsed(), "使用的模型不能为空");
        assertNotNull(response.getSessionId(), "会话ID不能为空");

        System.out.println("[集成测试] 响应: " + response.getResponse());
        System.out.println("[集成测试] 使用模型: " + response.getModelUsed());
        System.out.println("[集成测试] 会话ID: " + response.getSessionId());

        if (response.getUsage() != null) {
            System.out.println("[集成测试] Token 使用: " + response.getUsage().getTotalTokens());
        }
    }

    @Test
    @DisplayName("[真实调用] chat() 指定模型")
    @EnabledIf("isLlmServiceAvailable")
    void chat_RealCall_WithSpecificModel() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("1+1等于几？")
                .userId(1002L)
                .provider("zhipu")
                .model("glm-4-flash")
                .build();

        // When
        ChatResponse response = llmGateway.chat(request)
                .timeout(Duration.ofSeconds(30))
                .block();

        // Then
        assertNotNull(response);
        assertNotNull(response.getResponse());
        assertTrue(response.getModelUsed().contains("zhipu") || response.getModelUsed().contains("glm"),
                "应该使用指定模型: " + response.getModelUsed());

        System.out.println("[集成测试] 指定模型响应: " + response.getResponse());
        System.out.println("[集成测试] 实际使用模型: " + response.getModelUsed());
    }

    @Test
    @DisplayName("[真实调用] chat() 多轮对话")
    @EnabledIf("isLlmServiceAvailable")
    void chat_RealCall_MultiTurn() {
        // 第一轮对话
        ChatRequest request1 = ChatRequest.builder()
                .message("我的名字叫小明")
                .userId(1003L)
                .scene("page_assistant")
                .build();

        ChatResponse response1 = llmGateway.chat(request1)
                .timeout(Duration.ofSeconds(30))
                .block();

        assertNotNull(response1);
        String sessionId = response1.getSessionId();
        assertNotNull(sessionId, "会话ID不能为空");
        System.out.println("[集成测试] 第一轮响应: " + response1.getResponse());
        System.out.println("[集成测试] 会话ID: " + sessionId);

        // 第二轮对话（使用相同的 sessionId）
        ChatRequest request2 = ChatRequest.builder()
                .message("我叫什么名字？")
                .userId(1003L)
                .sessionId(sessionId)
                .scene("page_assistant")
                .build();

        ChatResponse response2 = llmGateway.chat(request2)
                .timeout(Duration.ofSeconds(30))
                .block();

        assertNotNull(response2);
        System.out.println("[集成测试] 第二轮响应: " + response2.getResponse());
        // 多轮对话应该能记住上下文（取决于模型能力）
    }

    // ==================== chatStream() 真实调用测试 ====================

    @Test
    @DisplayName("[真实调用] chatStream() 流式对话成功")
    @EnabledIf("isLlmServiceAvailable")
    void chatStream_RealCall_Success() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .message("请用三句话介绍人工智能")
                .userId(1004L)
                .scene("page_assistant")
                .build();

        // When: 获取流式响应
        Flux<ServerSentEvent<String>> stream = llmGateway.chatStream(request)
                .timeout(Duration.ofSeconds(60));

        // Then: 验证流式响应
        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<String> modelUsed = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean(false);

        System.out.println("[集成测试] 开始接收 SSE 事件...");

        // 收集事件直到 done
        stream.doOnNext(event -> {
            String eventType = event.event();
            String data = event.data();

            if ("token".equals(eventType) && data != null) {
                try {
                    if (data.contains("\"content\"")) {
                        String content = extractContent(data);
                        if (content != null) {
                            fullResponse.append(content);
                            System.out.print(content);
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            } else if ("done".equals(eventType)) {
                System.out.println("\n[集成测试] SSE 流结束");
                if (data != null && data.contains("model_used")) {
                    modelUsed.set(extractModelUsed(data));
                }
                done.set(true);
            } else if ("error".equals(eventType)) {
                System.out.println("[集成测试] SSE 错误: " + data);
            }
        }).blockLast(Duration.ofSeconds(60));

        // 验证收到了内容
        assertTrue(fullResponse.length() > 0 || done.get(), "应该收到流式响应内容");
        System.out.println("[集成测试] 完整响应: " + fullResponse);
        System.out.println("[集成测试] 使用模型: " + modelUsed.get());
    }

    // ==================== getAllowedModels() 真实调用测试 ====================

    @Test
    @DisplayName("[真实调用] getAllowedModels() 获取允许调用的模型列表")
    @EnabledIf("isLlmServiceAvailable")
    void getAllowedModels_RealCall_Success() {
        // When
        AllowedModelsResponse response = llmGateway.getAllowedModels()
                .timeout(Duration.ofSeconds(10))
                .block();

        // Then
        assertNotNull(response, "响应不能为空");
        assertNotNull(response.getAllowedModels(), "模型列表不能为空");
        assertFalse(response.getAllowedModels().isEmpty(), "模型列表不能为空");
        assertNotNull(response.getDefaultModel(), "默认模型不能为空");

        System.out.println("[集成测试] 默认模型: " + response.getDefaultModel());
        System.out.println("[集成测试] 允许调用的模型数量: " + response.getAllowedModels().size());

        response.getAllowedModels().forEach(model -> {
            System.out.println("  - " + model.getFullName() + " (" + model.getDisplayName() + ")" +
                    (Boolean.TRUE.equals(model.getFree()) ? " [免费]" : ""));
        });
    }

    // ==================== getModels() 真实调用测试 ====================

    @Test
    @DisplayName("[真实调用] getModels() 获取所有模型列表")
    @EnabledIf("isLlmServiceAvailable")
    void getModels_RealCall_Success() {
        // When
        ModelsResponse response = llmGateway.getModels()
                .timeout(Duration.ofSeconds(10))
                .block();

        // Then
        assertNotNull(response, "响应不能为空");
        assertNotNull(response.getProviders(), "Provider 列表不能为空");
        assertFalse(response.getProviders().isEmpty(), "Provider 列表不能为空");

        System.out.println("[集成测试] Provider 数量: " + response.getProviders().size());

        response.getProviders().forEach(provider -> {
            System.out.println("  Provider: " + provider.getDisplayName() + " (" + provider.getName() + ")");
            if (provider.getModels() != null) {
                provider.getModels().forEach(model -> {
                    System.out.println("    - " + model.getDisplayName());
                });
            }
        });
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查 LLM 服务是否可用（用于 @EnabledIf）
     */
    static boolean isLlmServiceAvailable() {
        return llmServiceAvailable;
    }

    /**
     * 从 SSE data 中提取 content
     */
    private String extractContent(String data) {
        // 简单解析 JSON 中的 content 字段
        int start = data.indexOf("\"content\":\"");
        if (start == -1) return null;
        start += 11;
        int end = data.indexOf("\"", start);
        if (end == -1) return null;
        return data.substring(start, end);
    }

    /**
     * 从 SSE done data 中提取 model_used
     */
    private String extractModelUsed(String data) {
        int start = data.indexOf("\"model_used\":\"");
        if (start == -1) return null;
        start += 14;
        int end = data.indexOf("\"", start);
        if (end == -1) return null;
        return data.substring(start, end);
    }
}