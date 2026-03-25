package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.llm.AllowedModelsResponse;
import com.ai.edu.application.dto.llm.ChatRequest;
import com.ai.edu.application.dto.llm.ChatResponse;
import com.ai.edu.application.dto.llm.ModelInfo;
import com.ai.edu.application.dto.llm.ModelsResponse;
import com.ai.edu.application.dto.llm.ScenesResponse;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.interface_.util.LlmServiceChecker;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmApiController 真实集成测试
 * 直接调用真实的 LLM 服务（需要 LLM 服务运行）
 *
 * 如果 LLM 服务不可用，测试将被跳过
 *
 * @author AI Edu Platform
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LlmApiControllerRealTest {


    @Resource
    private LlmApiController llmApiController;

    // 测试数据常量
    private static final Long TEST_USER_ID = 1001L;
    private static final String TEST_USERNAME = "test_user";
    private static final String TEST_MESSAGE = "你好，请简单介绍一下自己";
    private static final String TEST_SCENE = "knowledge_qa";

    // LLM 服务可用性标记
    private static boolean llmServiceAvailable = false;

    @BeforeAll
    static void setUp(@Value("${ai-edu.llm.gateway.base-url:}") String baseUrl, @Value("${ai-edu.llm.gateway.internal-token:}") String token) {
        // 检测 LLM 服务是否可用
        if (baseUrl != null && !baseUrl.isEmpty()) {
            llmServiceAvailable = LlmServiceChecker.isAvailable(baseUrl, token, 3000);
            System.out.println("[LLM Service] baseUrl=" + baseUrl + ", available=" + llmServiceAvailable);
        } else {
            System.out.println("[LLM Service] baseUrl not configured, tests will be skipped");
        }
    }

    // ==================== 同步对话测试 ====================

    /**
     * 场景 CHAT-REAL-001: 真实同步对话测试
     */
    @Test
    @Order(100)
    void chat_Real_Success() {
        Assumptions.assumeTrue(llmServiceAvailable, "LLM 服务不可用，跳过测试");

        // Given: 准备登录会话和请求
        MockHttpSession session = createLoginSession();
        ChatRequest request = createChatRequest(TEST_MESSAGE, TEST_SCENE);

        // When: 调用真实 chat 接口
        Mono<ApiResponse<ChatResponse>> result = llmApiController.chat(request, session);

        // Then: 验证响应
        StepVerifier.create(result)
                .assertNext(response -> {
                    System.out.println("[CHAT Response] code=" + response.getCode());
                    assertEquals(ErrorCode.SUCCESS, response.getCode(), "响应码应为成功");
                    assertNotNull(response.getData(), "响应数据不应为空");

                    ChatResponse chatResponse = response.getData();
                    assertNotNull(chatResponse.getResponse(), "对话响应内容不应为空");
                    assertNotNull(chatResponse.getModelUsed(), "使用的模型不应为空");

                    System.out.println("[CHAT Response] modelUsed=" + chatResponse.getModelUsed());
                    System.out.println("[CHAT Response] response=" +
                            (chatResponse.getResponse().length() > 100
                                    ? chatResponse.getResponse().substring(0, 100) + "..."
                                    : chatResponse.getResponse()));
                })
                .verifyComplete();

        System.out.println("[SUCCESS] 真实同步对话测试通过");
    }

    /**
     * 场景 CHAT-REAL-002: 未登录用户对话失败
     */
    @Test
    @Order(101)
    void chat_Real_Fail_NotLoggedIn() {
        Assumptions.assumeTrue(llmServiceAvailable, "LLM 服务不可用，跳过测试");

        // Given: 未登录的会话
        MockHttpSession session = new MockHttpSession();
        ChatRequest request = createChatRequest(TEST_MESSAGE, TEST_SCENE);

        // When & Then: 验证抛出未登录异常
        StepVerifier.create(llmApiController.chat(request, session))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                        ErrorCode.UNAUTHORIZED.equals(((BusinessException) throwable).getCode()))
                .verify();

        System.out.println("[SUCCESS] 未登录用户对话测试通过");
    }

    // ==================== 流式对话测试 ====================

    /**
     * 场景 STREAM-REAL-001: 真实流式对话测试
     */
    @Test
    @Order(200)
    void chatStream_Real_Success() {
        Assumptions.assumeTrue(llmServiceAvailable, "LLM 服务不可用，跳过测试");

        // Given
        MockHttpSession session = createLoginSession();
        ChatRequest request = createChatRequest("请用一句话介绍牛顿第一定律", TEST_SCENE);

        // When: 调用真实流式接口
        Flux<ServerSentEvent<String>> result = llmApiController.chatStream(request, session);

        // Then: 验证收到 SSE 事件
        List<ServerSentEvent<String>> events = result.collectList().block(Duration.ofSeconds(60));

        assertNotNull(events, "事件列表不应为空");
        assertFalse(events.isEmpty(), "应收到至少一个 SSE 事件");

        for (ServerSentEvent<String> event : events) {
            System.out.println("[SSE Event] event=" + event.event() + ", data=" + event.data());
        }

        System.out.println("[SUCCESS] 收到 " + events.size() + " 个 SSE 事件");
        System.out.println("[SUCCESS] 真实流式对话测试通过");
    }

    /**
     * 场景 STREAM-REAL-002: 未登录用户流式对话失败
     */
    @Test
    @Order(201)
    void chatStream_Real_Fail_NotLoggedIn() {
        Assumptions.assumeTrue(llmServiceAvailable, "LLM 服务不可用，跳过测试");

        // Given: 未登录的会话
        MockHttpSession session = new MockHttpSession();
        ChatRequest request = createChatRequest(TEST_MESSAGE, TEST_SCENE);

        // When
        Flux<ServerSentEvent<String>> result = llmApiController.chatStream(request, session);

        // Then: 验证返回错误事件
        StepVerifier.create(result)
                .assertNext(event -> {
                    assertEquals("error", event.event(), "事件类型应为 error");
                    assertNotNull(event.data(), "错误数据不应为空");
                    assertTrue(event.data().contains(ErrorCode.UNAUTHORIZED), "错误数据应包含未登录错误码");
                })
                .verifyComplete();

        System.out.println("[SUCCESS] 未登录用户流式对话测试通过");
    }

    // ==================== 模型查询测试 ====================

    /**
     * 场景 MODEL-REAL-001: 真实获取允许调用的模型列表
     */
    @Test
    @Order(300)
    void getAllowedModels_Real_Success() {
        Assumptions.assumeTrue(llmServiceAvailable, "LLM 服务不可用，跳过测试");

        // When: 调用真实接口（无需登录）
        Mono<ApiResponse<AllowedModelsResponse>> result = llmApiController.getAllowedModels();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    System.out.println("[AllowedModels Response] code=" + response.getCode());
                    assertEquals(ErrorCode.SUCCESS, response.getCode(), "响应码应为成功");
                    assertNotNull(response.getData(), "响应数据不应为空");

                    AllowedModelsResponse data = response.getData();
                    assertNotNull(data.getAllowedModels(), "模型列表不应为空");

                    System.out.println("[AllowedModels] defaultModel=" + data.getDefaultModel());
                    System.out.println("[AllowedModels] count=" + data.getAllowedModels().size());

                    for (ModelInfo model : data.getAllowedModels()) {
                        System.out.println("  - " + model.getFullName() + " (" + model.getDisplayName() + ")");
                    }
                })
                .verifyComplete();

        System.out.println("[SUCCESS] 获取允许调用的模型列表测试通过");
    }

    /**
     * 场景 MODEL-REAL-002: 真实获取所有模型列表
     */
    @Test
    @Order(301)
    void getModels_Real_Success() {
        Assumptions.assumeTrue(llmServiceAvailable, "LLM 服务不可用，跳过测试");

        // When
        Mono<ApiResponse<ModelsResponse>> result = llmApiController.getModels();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    System.out.println("[Models Response] code=" + response.getCode());
                    assertEquals(ErrorCode.SUCCESS, response.getCode(), "响应码应为成功");
                    assertNotNull(response.getData(), "响应数据不应为空");

                    ModelsResponse data = response.getData();
                    assertNotNull(data.getProviders(), "提供商列表不应为空");

                    System.out.println("[Models] provider count=" + data.getProviders().size());

                    for (ModelsResponse.ProviderInfo provider : data.getProviders()) {
                        System.out.println("  Provider: " + provider.getDisplayName());
                        if (provider.getModels() != null) {
                            for (ModelsResponse.ModelSummary model : provider.getModels()) {
                                System.out.println("    - " + model.getDisplayName() + " (free=" + model.getFree() + ")");
                            }
                        }
                    }
                })
                .verifyComplete();

        System.out.println("[SUCCESS] 获取所有模型列表测试通过");
    }

    // ==================== 场景查询测试 ====================

    /**
     * 场景 SCENE-REAL-001: 真实获取场景列表
     */
    @Test
    @Order(400)
    void getScenes_Real_Success() {
        Assumptions.assumeTrue(llmServiceAvailable, "LLM 服务不可用，跳过测试");

        // When: 调用真实接口（无需登录）
        Mono<ApiResponse<ScenesResponse>> result = llmApiController.getScenes();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    System.out.println("[Scenes Response] code=" + response.getCode());
                    assertEquals(ErrorCode.SUCCESS, response.getCode(), "响应码应为成功");
                    assertNotNull(response.getData(), "响应数据不应为空");

                    ScenesResponse data = response.getData();
                    assertNotNull(data.getScenes(), "场景列表不应为空");

                    System.out.println("[Scenes] count=" + data.getScenes().size());

                    for (ScenesResponse.SceneInfo scene : data.getScenes()) {
                        System.out.println("  - " + scene.getCode() + ": " + scene.getDescription() +
                                " (default=" + scene.getDefaultModel() + ")");
                    }
                })
                .verifyComplete();

        System.out.println("[SUCCESS] 获取场景列表测试通过");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建登录会话
     */
    private MockHttpSession createLoginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", TEST_USER_ID);
        session.setAttribute("username", TEST_USERNAME);
        session.setAttribute("role", "STUDENT");
        return session;
    }

    /**
     * 创建对话请求
     */
    private ChatRequest createChatRequest(String message, String scene) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setScene(scene);
        return request;
    }
}