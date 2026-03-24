package com.ai.edu.application.service;

import com.ai.edu.application.dto.llm.*;
import com.ai.edu.domain.shared.service.LlmGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LLM 应用服务
 * 职责：编排 LLM Gateway，处理 DTO 与领域模型的转换
 *
 * @author AI Edu Platform
 */
@Slf4j
@Service
public class LlmAppService {

    @Resource
    private LlmGateway llmGateway;

    // ==================== 对话接口 ====================

    /**
     * 同步对话
     *
     * @param request 对话请求 DTO
     * @return 对话响应 DTO
     */
    public Mono<ChatResponse> chat(ChatRequest request) {
        log.info("LLM 对话请求: userId={}, scene={}, message={}",
                request.getUserId(), request.getScene(), truncate(request.getMessage(), 100));

        // 转换 DTO -> Domain Model
        com.ai.edu.domain.shared.model.ChatRequest domainRequest = toDomainRequest(request);

        return llmGateway.chat(domainRequest)
                .map(this::toDtoResponse)
                .doOnSuccess(response -> log.info("LLM 对话响应: sessionId={}, modelUsed={}",
                        response.getSessionId(), response.getModelUsed()))
                .doOnError(e -> log.error("LLM 对话失败: {}", e.getMessage(), e));
    }

    /**
     * 流式对话 (SSE)
     *
     * @param request 对话请求 DTO
     * @return SSE 事件流
     */
    public Flux<ServerSentEvent<String>> chatStream(ChatRequest request) {
        log.info("LLM 流式对话请求: userId={}, scene={}, message={}",
                request.getUserId(), request.getScene(), truncate(request.getMessage(), 100));

        // 转换 DTO -> Domain Model
        com.ai.edu.domain.shared.model.ChatRequest domainRequest = toDomainRequest(request);

        return llmGateway.chatStream(domainRequest)
                .doOnComplete(() -> log.info("LLM 流式对话完成"))
                .doOnError(e -> log.error("LLM 流式对话失败: {}", e.getMessage(), e));
    }

    // ==================== 模型查询接口 ====================

    /**
     * 获取允许调用的模型列表
     *
     * @return 允许调用的模型列表 DTO
     */
    public Mono<AllowedModelsResponse> getAllowedModels() {
        log.info("获取允许调用的模型列表");

        return llmGateway.getAllowedModels()
                .map(this::toDtoAllowedModelsResponse)
                .doOnSuccess(response -> log.info("获取到 {} 个允许调用的模型",
                        response.getAllowedModels() != null ? response.getAllowedModels().size() : 0))
                .doOnError(e -> log.error("获取允许调用的模型列表失败: {}", e.getMessage(), e));
    }

    /**
     * 获取所有模型列表
     *
     * @return 所有模型列表 DTO
     */
    public Mono<ModelsResponse> getModels() {
        log.info("获取所有模型列表");

        return llmGateway.getModels()
                .map(this::toDtoModelsResponse)
                .doOnSuccess(response -> log.info("获取到 {} 个提供商",
                        response.getProviders() != null ? response.getProviders().size() : 0))
                .doOnError(e -> log.error("获取所有模型列表失败: {}", e.getMessage(), e));
    }

    /**
     * 获取场景列表
     *
     * @return 场景列表 DTO
     */
    public Mono<ScenesResponse> getScenes() {
        log.info("获取场景列表");

        return llmGateway.getScenes()
                .map(this::toDtoScenesResponse)
                .doOnSuccess(response -> log.info("获取到 {} 个场景",
                        response.getScenes() != null ? response.getScenes().size() : 0))
                .doOnError(e -> log.error("获取场景列表失败: {}", e.getMessage(), e));
    }

    // ==================== DTO <-> Domain 转换方法 ====================

    /**
     * 将 DTO 请求转换为领域模型请求
     */
    private com.ai.edu.domain.shared.model.ChatRequest toDomainRequest(ChatRequest dto) {
        return com.ai.edu.domain.shared.model.ChatRequest.builder()
                .message(dto.getMessage())
                .userId(dto.getUserId())
                .scene(dto.getScene())
                .provider(dto.getProvider())
                .model(dto.getModel())
                .sessionId(dto.getSessionId())
                .pageCode(dto.getPageCode())
                .context(dto.getContext())
                .build();
    }

    /**
     * 将领域模型响应转换为 DTO 响应
     */
    private ChatResponse toDtoResponse(com.ai.edu.domain.shared.model.ChatResponse domain) {
        ChatResponse.ChatResponseBuilder builder = ChatResponse.builder()
                .response(domain.getResponse())
                .sessionId(domain.getSessionId())
                .modelUsed(domain.getModelUsed());

        // 转换 Usage
        if (domain.getUsage() != null) {
            builder.usage(ChatResponse.Usage.builder()
                    .promptTokens(domain.getUsage().getPromptTokens() != null
                            ? domain.getUsage().getPromptTokens().intValue() : null)
                    .completionTokens(domain.getUsage().getCompletionTokens() != null
                            ? domain.getUsage().getCompletionTokens().intValue() : null)
                    .totalTokens(domain.getUsage().getTotalTokens() != null
                            ? domain.getUsage().getTotalTokens().intValue() : null)
                    .build());
        }

        // 转换 ToolCalls
        if (domain.getToolCalls() != null && !domain.getToolCalls().isEmpty()) {
            List<ChatResponse.ToolCall> toolCalls = domain.getToolCalls().stream()
                    .map(tc -> ChatResponse.ToolCall.builder()
                            .id(tc.getId())
                            .type(tc.getType())
                            .name(tc.getName())
                            .arguments(tc.getArguments())
                            .build())
                    .collect(Collectors.toList());
            builder.toolCalls(toolCalls);
        }

        return builder.build();
    }

    /**
     * 将领域模型允许模型响应转换为 DTO 响应
     */
    private AllowedModelsResponse toDtoAllowedModelsResponse(
            com.ai.edu.domain.shared.model.AllowedModelsResponse domain) {
        List<ModelInfo> allowedModels = Collections.emptyList();
        if (domain.getAllowedModels() != null) {
            allowedModels = domain.getAllowedModels().stream()
                    .map(this::toDtoModelInfo)
                    .collect(Collectors.toList());
        }

        return AllowedModelsResponse.builder()
                .allowedModels(allowedModels)
                .defaultModel(domain.getDefaultModel())
                .build();
    }

    /**
     * 将领域模型模型信息转换为 DTO 模型信息
     */
    private ModelInfo toDtoModelInfo(com.ai.edu.domain.shared.model.ModelInfo domain) {
        return ModelInfo.builder()
                .provider(domain.getProvider())
                .model(domain.getModel())
                .fullName(domain.getFullName())
                .displayName(domain.getDisplayName())
                .free(domain.getFree())
                .supportsTools(domain.getSupportsTools())
                .supportsVision(domain.getSupportsVision())
                .description(domain.getDescription())
                .build();
    }

    /**
     * 将领域模型所有模型响应转换为 DTO 响应
     */
    private ModelsResponse toDtoModelsResponse(com.ai.edu.domain.shared.model.ModelsResponse domain) {
        List<ModelsResponse.ProviderInfo> providers = Collections.emptyList();
        if (domain.getProviders() != null) {
            providers = domain.getProviders().stream()
                    .map(this::toDtoProviderInfo)
                    .collect(Collectors.toList());
        }

        return ModelsResponse.builder()
                .providers(providers)
                .build();
    }

    /**
     * 将领域模型提供商信息转换为 DTO 提供商信息
     */
    private ModelsResponse.ProviderInfo toDtoProviderInfo(
            com.ai.edu.domain.shared.model.ProviderInfo domain) {
        List<ModelsResponse.ModelSummary> models = Collections.emptyList();
        if (domain.getModels() != null) {
            models = domain.getModels().stream()
                    .map(ms -> ModelsResponse.ModelSummary.builder()
                            .model(ms.getModel())
                            .displayName(ms.getDisplayName())
                            .free(ms.getFree())
                            .build())
                    .collect(Collectors.toList());
        }

        return ModelsResponse.ProviderInfo.builder()
                .name(domain.getName())
                .displayName(domain.getDisplayName())
                .models(models)
                .build();
    }

    /**
     * 将领域模型场景响应转换为 DTO 响应
     */
    private ScenesResponse toDtoScenesResponse(com.ai.edu.domain.shared.model.ScenesResponse domain) {
        List<ScenesResponse.SceneInfo> scenes = Collections.emptyList();
        if (domain.getScenes() != null) {
            scenes = domain.getScenes().stream()
                    .map(this::toDtoSceneInfo)
                    .collect(Collectors.toList());
        }

        return ScenesResponse.builder()
                .scenes(scenes)
                .build();
    }

    /**
     * 将领域模型场景信息转换为 DTO 场景信息
     */
    private ScenesResponse.SceneInfo toDtoSceneInfo(
            com.ai.edu.domain.shared.model.SceneInfo domain) {
        return ScenesResponse.SceneInfo.builder()
                .code(domain.getCode())
                .defaultProvider(domain.getDefaultProvider())
                .defaultModel(domain.getDefaultModel())
                .description(domain.getDescription())
                .build();
    }

    // ==================== 工具方法 ====================

    /**
     * 截断字符串（用于日志输出）
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }
}