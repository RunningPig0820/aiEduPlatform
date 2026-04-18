package com.ai.edu.application.service;

import com.ai.edu.application.dto.llm.AllowedModelsResponse;
import com.ai.edu.application.dto.llm.ChatRequest;
import com.ai.edu.application.dto.llm.ChatResponse;
import com.ai.edu.application.dto.llm.ModelInfo;
import com.ai.edu.application.dto.llm.ModelsResponse;
import com.ai.edu.application.dto.llm.ScenesResponse;
import com.ai.edu.application.service.llm.LlmAppService;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.model.Usage;
import com.ai.edu.domain.llm.service.LlmGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LlmAppService 单元测试
 * Mock LlmGateway，测试 DTO 和领域模型的转换
 *
 * @author AI Edu Platform
 */
@ExtendWith(MockitoExtension.class)
class LlmAppServiceTest {

    @Mock
    private LlmGateway llmGateway;

    @InjectMocks
    private LlmAppService llmAppService;

    private ChatRequest dtoRequest;
    private AiEduChatRequest domainRequest;
    private AiEduChatResponse domainResponse;

    @BeforeEach
    void setUp() {
        // 准备 DTO 请求
        dtoRequest = ChatRequest.builder()
                .message("请解释一下牛顿第一定律")
                .userId(1001L)
                .scene("knowledge_qa")
                .provider("zhipu")
                .model("glm-4-flash")
                .sessionId("test-session-id")
                .pageCode("homework_page")
                .build();

        // 准备领域模型请求
        domainRequest = AiEduChatRequest.builder()
                .message("请解释一下牛顿第一定律")
                .userId(1001L)
                .scene("knowledge_qa")
                .provider("zhipu")
                .model("glm-4-flash")
                .sessionId("test-session-id")
                .pageCode("homework_page")
                .build();

        // 准备领域模型响应
        domainResponse = AiEduChatResponse.builder()
                .response("牛顿第一定律，又称为惯性定律...")
                .sessionId("session-123")
                .modelUsed("zhipu/glm-4-flash")
                .usage(Usage.builder()
                        .promptTokens(10L)
                        .completionTokens(50L)
                        .totalTokens(60L)
                        .build())
                .build();
    }

    // ==================== chat() 测试 ====================

    @Test
    @DisplayName("chat() 成功 - 验证 DTO 到领域模型的转换")
    void chat_Success_VerifyDtoToDomainConversion() {
        // Given: Mock LlmGateway 返回领域模型响应
        when(llmGateway.chat(any(AiEduChatRequest.class)))
                .thenReturn(Mono.just(domainResponse));

        // When: 调用 chat 方法
        Mono<ChatResponse> result = llmAppService.chat(dtoRequest);

        // Then: 验证响应
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("牛顿第一定律，又称为惯性定律...", response.getResponse());
                    assertEquals("session-123", response.getSessionId());
                    assertEquals("zhipu/glm-4-flash", response.getModelUsed());

                    // 验证 Usage 转换
                    assertNotNull(response.getUsage());
                    assertEquals(10, response.getUsage().getPromptTokens());
                    assertEquals(50, response.getUsage().getCompletionTokens());
                    assertEquals(60, response.getUsage().getTotalTokens());
                })
                .verifyComplete();

        // 验证调用了 LlmGateway.chat
        verify(llmGateway, times(1)).chat(any(AiEduChatRequest.class));
    }

    @Test
    @DisplayName("chat() 验证 userId 直接传递 Long")
    void chat_VerifyUserIdConversion() {
        // Given
        when(llmGateway.chat(any())).thenReturn(Mono.just(domainResponse));

        // When
        llmAppService.chat(dtoRequest).block();

        // Then: 验证 userId 直接传递 Long
        verify(llmGateway).chat(argThat(req -> Long.valueOf(1001L).equals(req.getUserId())));
    }

    @Test
    @DisplayName("chat() userId 为 null 时不转换")
    void chat_UserIdNull() {
        // Given
        dtoRequest.setUserId(null);
        when(llmGateway.chat(any())).thenReturn(Mono.just(domainResponse));

        // When
        llmAppService.chat(dtoRequest).block();

        // Then
        verify(llmGateway).chat(argThat(req -> req.getUserId() == null));
    }

    @Test
    @DisplayName("chat() 带工具调用响应 - 验证 ToolCalls 转换")
    void chat_WithToolCalls() {
        // Given: 准备带工具调用的领域响应
        com.ai.edu.domain.llm.model.ToolCall toolCall = com.ai.edu.domain.llm.model.ToolCall.builder()
                .id("call-123")
                .type("function")
                .name("get_weather")
                .arguments("{\"city\":\"Beijing\"}")
                .build();

        domainResponse = AiEduChatResponse.builder()
                .response(null)
                .sessionId("session-123")
                .modelUsed("zhipu/glm-4-flash")
                .toolCalls(List.of(toolCall))
                .build();

        when(llmGateway.chat(any())).thenReturn(Mono.just(domainResponse));

        // When
        Mono<ChatResponse> result = llmAppService.chat(dtoRequest);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response.getToolCalls());
                    assertEquals(1, response.getToolCalls().size());

                    ChatResponse.ToolCall dtoToolCall = response.getToolCalls().get(0);
                    assertEquals("call-123", dtoToolCall.getId());
                    assertEquals("function", dtoToolCall.getType());
                    assertEquals("get_weather", dtoToolCall.getName());
                    assertEquals("{\"city\":\"Beijing\"}", dtoToolCall.getArguments());
                })
                .verifyComplete();
    }

    // ==================== chatStream() 测试 ====================

    @Test
    @DisplayName("chatStream() 成功 - 返回 SSE 流")
    void chatStream_Success() {
        // Given
        ServerSentEvent<String> event1 = ServerSentEvent.<String>builder()
                .event("message")
                .data("{\"content\":\"牛顿第一定律\"}")
                .build();
        ServerSentEvent<String> event2 = ServerSentEvent.<String>builder()
                .event("done")
                .data("{}")
                .build();

        when(llmGateway.chatStream(any())).thenReturn(Flux.just(event1, event2));

        // When
        Flux<ServerSentEvent<String>> result = llmAppService.chatStream(dtoRequest);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(event -> "message".equals(event.event()))
                .expectNextMatches(event -> "done".equals(event.event()))
                .verifyComplete();

        verify(llmGateway, times(1)).chatStream(any());
    }

    // ==================== getAllowedModels() 测试 ====================

    @Test
    @DisplayName("getAllowedModels() 成功 - 验证模型列表转换")
    void getAllowedModels_Success() {
        // Given
        com.ai.edu.domain.llm.model.ModelInfo domainModelInfo = com.ai.edu.domain.llm.model.ModelInfo.builder()
                .provider("zhipu")
                .model("glm-4-flash")
                .fullName("zhipu/glm-4-flash")
                .displayName("GLM-4-Flash")
                .free(true)
                .supportsTools(true)
                .supportsVision(false)
                .description("智谱轻量模型")
                .build();

        com.ai.edu.domain.llm.model.AllowedModelsResponse domainResponse =
                com.ai.edu.domain.llm.model.AllowedModelsResponse.builder()
                        .allowedModels(List.of(domainModelInfo))
                        .defaultModel("zhipu/glm-4-flash")
                        .build();

        when(llmGateway.getAllowedModels()).thenReturn(Mono.just(domainResponse));

        // When
        Mono<AllowedModelsResponse> result = llmAppService.getAllowedModels();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("zhipu/glm-4-flash", response.getDefaultModel());
                    assertNotNull(response.getAllowedModels());
                    assertEquals(1, response.getAllowedModels().size());

                    ModelInfo modelInfo = response.getAllowedModels().get(0);
                    assertEquals("zhipu", modelInfo.getProvider());
                    assertEquals("glm-4-flash", modelInfo.getModel());
                    assertEquals("zhipu/glm-4-flash", modelInfo.getFullName());
                    assertEquals("GLM-4-Flash", modelInfo.getDisplayName());
                    assertTrue(modelInfo.getFree());
                    assertTrue(modelInfo.getSupportsTools());
                    assertFalse(modelInfo.getSupportsVision());
                    assertEquals("智谱轻量模型", modelInfo.getDescription());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getAllowedModels() 空列表处理")
    void getAllowedModels_EmptyList() {
        // Given
        com.ai.edu.domain.llm.model.AllowedModelsResponse domainResponse =
                com.ai.edu.domain.llm.model.AllowedModelsResponse.builder()
                        .allowedModels(Collections.emptyList())
                        .defaultModel(null)
                        .build();

        when(llmGateway.getAllowedModels()).thenReturn(Mono.just(domainResponse));

        // When
        Mono<AllowedModelsResponse> result = llmAppService.getAllowedModels();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response.getAllowedModels());
                    assertTrue(response.getAllowedModels().isEmpty());
                    assertNull(response.getDefaultModel());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getAllowedModels() null 列表处理")
    void getAllowedModels_NullList() {
        // Given
        com.ai.edu.domain.llm.model.AllowedModelsResponse domainResponse =
                com.ai.edu.domain.llm.model.AllowedModelsResponse.builder()
                        .allowedModels(null)
                        .defaultModel(null)
                        .build();

        when(llmGateway.getAllowedModels()).thenReturn(Mono.just(domainResponse));

        // When
        Mono<AllowedModelsResponse> result = llmAppService.getAllowedModels();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response.getAllowedModels());
                    assertTrue(response.getAllowedModels().isEmpty());
                })
                .verifyComplete();
    }

    // ==================== getModels() 测试 ====================

    @Test
    @DisplayName("getModels() 成功 - 验证提供商列表转换")
    void getModels_Success() {
        // Given
        com.ai.edu.domain.llm.model.ProviderInfo.ModelSummary modelSummary =
                com.ai.edu.domain.llm.model.ProviderInfo.ModelSummary.builder()
                        .model("glm-4-flash")
                        .displayName("GLM-4-Flash")
                        .free(true)
                        .build();

        com.ai.edu.domain.llm.model.ProviderInfo providerInfo =
                com.ai.edu.domain.llm.model.ProviderInfo.builder()
                        .name("zhipu")
                        .displayName("智谱 AI")
                        .models(List.of(modelSummary))
                        .build();

        com.ai.edu.domain.llm.model.ModelsResponse domainResponse =
                com.ai.edu.domain.llm.model.ModelsResponse.builder()
                        .providers(List.of(providerInfo))
                        .build();

        when(llmGateway.getModels()).thenReturn(Mono.just(domainResponse));

        // When
        Mono<ModelsResponse> result = llmAppService.getModels();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getProviders());
                    assertEquals(1, response.getProviders().size());

                    ModelsResponse.ProviderInfo provider = response.getProviders().get(0);
                    assertEquals("zhipu", provider.getName());
                    assertEquals("智谱 AI", provider.getDisplayName());
                    assertNotNull(provider.getModels());
                    assertEquals(1, provider.getModels().size());

                    ModelsResponse.ModelSummary model = provider.getModels().get(0);
                    assertEquals("glm-4-flash", model.getModel());
                    assertEquals("GLM-4-Flash", model.getDisplayName());
                    assertTrue(model.getFree());
                })
                .verifyComplete();
    }

    // ==================== getScenes() 测试 ====================

    @Test
    @DisplayName("getScenes() 成功 - 验证场景列表转换")
    void getScenes_Success() {
        // Given
        com.ai.edu.domain.llm.model.SceneInfo sceneInfo =
                com.ai.edu.domain.llm.model.SceneInfo.builder()
                        .code("homework_help")
                        .defaultProvider("zhipu")
                        .defaultModel("glm-4-flash")
                        .description("作业辅导场景")
                        .build();

        com.ai.edu.domain.llm.model.ScenesResponse domainResponse =
                com.ai.edu.domain.llm.model.ScenesResponse.builder()
                        .scenes(List.of(sceneInfo))
                        .build();

        when(llmGateway.getScenes()).thenReturn(Mono.just(domainResponse));

        // When
        Mono<ScenesResponse> result = llmAppService.getScenes();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getScenes());
                    assertEquals(1, response.getScenes().size());

                    ScenesResponse.SceneInfo scene = response.getScenes().get(0);
                    assertEquals("homework_help", scene.getCode());
                    assertEquals("zhipu", scene.getDefaultProvider());
                    assertEquals("glm-4-flash", scene.getDefaultModel());
                    assertEquals("作业辅导场景", scene.getDescription());
                })
                .verifyComplete();
    }

    // ==================== 错误处理测试 ====================

    @Test
    @DisplayName("chat() LlmGateway 返回错误 - 传递错误")
    void chat_GatewayError() {
        // Given
        when(llmGateway.chat(any()))
                .thenReturn(Mono.error(new RuntimeException("Gateway error")));

        // When
        Mono<ChatResponse> result = llmAppService.chat(dtoRequest);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof RuntimeException &&
                    "Gateway error".equals(throwable.getMessage()))
                .verify();
    }
}