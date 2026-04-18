package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.llm.AllowedModelsResponse;
import com.ai.edu.application.dto.llm.ChatRequest;
import com.ai.edu.application.dto.llm.ChatResponse;
import com.ai.edu.application.dto.llm.ModelInfo;
import com.ai.edu.application.dto.llm.ModelsResponse;
import com.ai.edu.application.dto.llm.ScenesResponse;
import com.ai.edu.application.service.llm.LlmAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LlmApiController 集成测试
 * 直接注入 Controller，使用 MockHttpSession 模拟登录
 *
 * 测试场景覆盖：
 * 1. 同步对话 (CHAT-001 ~ CHAT-006)
 * 2. 流式对话 (STREAM-001 ~ STREAM-003)
 * 3. 模型查询 (MODEL-001)
 * 4. 场景查询 (SCENE-001)
 *
 * @author AI Edu Platform
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LlmApiControllerTest {

    @Resource
    private LlmApiController llmApiController;

    @MockBean
    private LlmAppService llmAppService;

    // 测试数据常量
    private static final Long TEST_USER_ID = 1001L;
    private static final String TEST_USERNAME = "test_user";
    private static final String TEST_MESSAGE = "请解释一下牛顿第一定律";
    private static final String TEST_SCENE = "knowledge_qa";

    // ==================== 同步对话测试 ====================

    /**
     * 场景 CHAT-001: 正常对话 - 仅消息
     */
    @Test
    @Order(100)
    void chat_Success_WithMessageOnly() {
        // Given: 准备登录会话和请求
        MockHttpSession session = createLoginSession();
        ChatRequest request = createChatRequest(TEST_MESSAGE, null);

        // Mock LlmAppService 返回
        ChatResponse mockResponse = ChatResponse.builder()
                .response("牛顿第一定律，又称为惯性定律...")
                .sessionId("session-123")
                .modelUsed("zhipu/glm-4-flash")
                .build();

        when(llmAppService.chat(any(ChatRequest.class))).thenReturn(Mono.just(mockResponse));

        // When: 调用 chat 接口
        Mono<ApiResponse<ChatResponse>> result = llmApiController.chat(request, session);

        // Then: 验证响应
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(ErrorCode.SUCCESS, response.getCode());
                    assertNotNull(response.getData());
                    assertEquals("牛顿第一定律，又称为惯性定律...", response.getData().getResponse());
                    assertEquals("session-123", response.getData().getSessionId());
                    assertEquals("zhipu/glm-4-flash", response.getData().getModelUsed());
                })
                .verifyComplete();

        // 验证 userId 已设置
        verify(llmAppService).chat(argThat(req -> TEST_USER_ID.equals(req.getUserId())));
    }

    /**
     * 场景 CHAT-005: 异常 - 未登录
     */
    @Test
    @Order(500)
    void chat_Fail_NotLoggedIn() {
        // Given: 未登录的会话
        MockHttpSession session = new MockHttpSession();
        ChatRequest request = createChatRequest(TEST_MESSAGE, null);

        // When & Then: 验证抛出未登录异常
        StepVerifier.create(llmApiController.chat(request, session))
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException &&
                    ErrorCode.UNAUTHORIZED.equals(((BusinessException) throwable).getCode()))
                .verify();

        // 验证未调用 LlmAppService
        verify(llmAppService, never()).chat(any());
    }

    /**
     * 场景 CHAT-006: 异常 - 消息为空
     * 注：实际的消息校验由 @Valid 注解完成，这里测试 Controller 行为
     */
    @Test
    @Order(600)
    void chat_Fail_MessageEmpty() {
        // Given
        MockHttpSession session = createLoginSession();
        ChatRequest request = new ChatRequest();
        request.setMessage(""); // 空消息
        request.setUserId(TEST_USER_ID);

        // Mock 返回错误（实际由 validation 框架处理）
        when(llmAppService.chat(any())).thenReturn(Mono.error(
                new BusinessException(ErrorCode.INVALID_PARAMS, "消息内容不能为空")));

        // When & Then
        StepVerifier.create(llmApiController.chat(request, session))
                .expectErrorMatches(throwable ->
                    throwable instanceof BusinessException)
                .verify();
    }

    /**
     * 场景 CHAT-补充: 正常对话 - 带场景和模型
     */
    @Test
    @Order(101)
    void chat_Success_WithSceneAndModel() {
        // Given
        MockHttpSession session = createLoginSession();
        ChatRequest request = createChatRequest(TEST_MESSAGE, TEST_SCENE);
        request.setProvider("zhipu");
        request.setModel("glm-4-flash");

        ChatResponse mockResponse = ChatResponse.builder()
                .response("响应内容")
                .modelUsed("zhipu/glm-4-flash")
                .build();

        when(llmAppService.chat(any())).thenReturn(Mono.just(mockResponse));

        // When
        Mono<ApiResponse<ChatResponse>> result = llmApiController.chat(request, session);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(ErrorCode.SUCCESS, response.getCode());
                    assertNotNull(response.getData());
                })
                .verifyComplete();

        verify(llmAppService).chat(argThat(req ->
                TEST_SCENE.equals(req.getScene()) &&
                "zhipu".equals(req.getProvider()) &&
                "glm-4-flash".equals(req.getModel())));
    }

    // ==================== 流式对话测试 ====================

    /**
     * 场景 STREAM-001: 正常流式对话
     */
    @Test
    @Order(200)
    void chatStream_Success() {
        // Given
        MockHttpSession session = createLoginSession();
        ChatRequest request = createChatRequest(TEST_MESSAGE, TEST_SCENE);

        // Mock SSE 流
        ServerSentEvent<String> event1 = ServerSentEvent.<String>builder()
                .event("message")
                .data("{\"content\":\"牛顿第一定律\"}")
                .build();
        ServerSentEvent<String> event2 = ServerSentEvent.<String>builder()
                .event("message")
                .data("{\"content\":\"又称为惯性定律\"}")
                .build();
        ServerSentEvent<String> event3 = ServerSentEvent.<String>builder()
                .event("done")
                .data("{}")
                .build();

        when(llmAppService.chatStream(any())).thenReturn(Flux.just(event1, event2, event3));

        // When
        Flux<ServerSentEvent<String>> result = llmApiController.chatStream(request, session);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(event -> "message".equals(event.event()))
                .expectNextMatches(event -> "message".equals(event.event()))
                .expectNextMatches(event -> "done".equals(event.event()))
                .verifyComplete();

        verify(llmAppService).chatStream(any());
    }

    /**
     * 场景 STREAM-003: 异常 - 未登录
     */
    @Test
    @Order(503)
    void chatStream_Fail_NotLoggedIn() {
        // Given: 未登录的会话
        MockHttpSession session = new MockHttpSession();
        ChatRequest request = createChatRequest(TEST_MESSAGE, TEST_SCENE);

        // When
        Flux<ServerSentEvent<String>> result = llmApiController.chatStream(request, session);

        // Then: 验证返回错误事件
        StepVerifier.create(result)
                .assertNext(event -> {
                    assertEquals("error", event.event());
                    assertNotNull(event.data());
                    assertTrue(event.data().contains(ErrorCode.UNAUTHORIZED));
                })
                .verifyComplete();

        // 验证未调用 LlmAppService
        verify(llmAppService, never()).chatStream(any());
    }

    // ==================== 模型查询测试 ====================

    /**
     * 场景 MODEL-001: 正常获取模型列表
     */
    @Test
    @Order(300)
    void getAllowedModels_Success() {
        // Given
        ModelInfo modelInfo = ModelInfo.builder()
                .provider("zhipu")
                .model("glm-4-flash")
                .fullName("zhipu/glm-4-flash")
                .displayName("GLM-4-Flash")
                .free(true)
                .supportsTools(true)
                .supportsVision(false)
                .build();

        AllowedModelsResponse mockResponse = AllowedModelsResponse.builder()
                .allowedModels(List.of(modelInfo))
                .defaultModel("zhipu/glm-4-flash")
                .build();

        when(llmAppService.getAllowedModels()).thenReturn(Mono.just(mockResponse));

        // When: 调用接口（无需登录）
        Mono<ApiResponse<AllowedModelsResponse>> result = llmApiController.getAllowedModels();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(ErrorCode.SUCCESS, response.getCode());
                    assertNotNull(response.getData());
                    assertEquals("zhipu/glm-4-flash", response.getData().getDefaultModel());
                    assertEquals(1, response.getData().getAllowedModels().size());

                    ModelInfo model = response.getData().getAllowedModels().get(0);
                    assertEquals("zhipu", model.getProvider());
                    assertEquals("glm-4-flash", model.getModel());
                })
                .verifyComplete();

        verify(llmAppService).getAllowedModels();
    }

    /**
     * 场景 MODEL-002: 正常获取所有模型列表
     */
    @Test
    @Order(301)
    void getModels_Success() {
        // Given
        ModelsResponse.ModelSummary modelSummary = ModelsResponse.ModelSummary.builder()
                .model("glm-4-flash")
                .displayName("GLM-4-Flash")
                .free(true)
                .build();

        ModelsResponse.ProviderInfo providerInfo = ModelsResponse.ProviderInfo.builder()
                .name("zhipu")
                .displayName("智谱 AI")
                .models(List.of(modelSummary))
                .build();

        ModelsResponse mockResponse = ModelsResponse.builder()
                .providers(List.of(providerInfo))
                .build();

        when(llmAppService.getModels()).thenReturn(Mono.just(mockResponse));

        // When
        Mono<ApiResponse<ModelsResponse>> result = llmApiController.getModels();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(ErrorCode.SUCCESS, response.getCode());
                    assertNotNull(response.getData());
                    assertEquals(1, response.getData().getProviders().size());
                    assertEquals("zhipu", response.getData().getProviders().get(0).getName());
                })
                .verifyComplete();

        verify(llmAppService).getModels();
    }

    // ==================== 场景查询测试 ====================

    /**
     * 场景 SCENE-001: 正常获取场景列表
     */
    @Test
    @Order(400)
    void getScenes_Success() {
        // Given
        ScenesResponse.SceneInfo sceneInfo = ScenesResponse.SceneInfo.builder()
                .code("homework_help")
                .defaultProvider("zhipu")
                .defaultModel("glm-4-flash")
                .description("作业辅导场景")
                .build();

        ScenesResponse mockResponse = ScenesResponse.builder()
                .scenes(List.of(sceneInfo))
                .build();

        when(llmAppService.getScenes()).thenReturn(Mono.just(mockResponse));

        // When: 调用接口（无需登录）
        Mono<ApiResponse<ScenesResponse>> result = llmApiController.getScenes();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(ErrorCode.SUCCESS, response.getCode());
                    assertNotNull(response.getData());
                    assertEquals(1, response.getData().getScenes().size());

                    ScenesResponse.SceneInfo scene = response.getData().getScenes().get(0);
                    assertEquals("homework_help", scene.getCode());
                    assertEquals("zhipu", scene.getDefaultProvider());
                    assertEquals("glm-4-flash", scene.getDefaultModel());
                    assertEquals("作业辅导场景", scene.getDescription());
                })
                .verifyComplete();

        verify(llmAppService).getScenes();
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