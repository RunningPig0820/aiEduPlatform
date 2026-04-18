package com.ai.edu.application.assembler;

import com.ai.edu.application.dto.llm.*;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LLM DTO 与领域模型转换器
 *
 * @author AI Edu Platform
 */
public final class LlmConvert {

    private LlmConvert() {
        // 工具类，禁止实例化
    }

    // ==================== DTO -> Domain ====================

    /**
     * 将 DTO 请求转换为领域模型请求
     */
    public static AiEduChatRequest toDomain(ChatRequest dto) {
        if (dto == null) {
            return null;
        }
        return AiEduChatRequest.builder()
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

    // ==================== Domain -> DTO ====================

    /**
     * 将领域模型响应转换为 DTO 响应
     */
    public static ChatResponse toDto(AiEduChatResponse domain) {
        if (domain == null) {
            return null;
        }

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
    public static AllowedModelsResponse toDto(com.ai.edu.domain.llm.model.AllowedModelsResponse domain) {
        if (domain == null) {
            return null;
        }

        List<ModelInfo> allowedModels = Collections.emptyList();
        if (domain.getAllowedModels() != null) {
            allowedModels = domain.getAllowedModels().stream()
                    .map(LlmConvert::toDto)
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
    public static ModelInfo toDto(com.ai.edu.domain.llm.model.ModelInfo domain) {
        if (domain == null) {
            return null;
        }
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
    public static ModelsResponse toDto(com.ai.edu.domain.llm.model.ModelsResponse domain) {
        if (domain == null) {
            return null;
        }

        List<ModelsResponse.ProviderInfo> providers = Collections.emptyList();
        if (domain.getProviders() != null) {
            providers = domain.getProviders().stream()
                    .map(LlmConvert::toDto)
                    .collect(Collectors.toList());
        }

        return ModelsResponse.builder()
                .providers(providers)
                .build();
    }

    /**
     * 将领域模型提供商信息转换为 DTO 提供商信息
     */
    public static ModelsResponse.ProviderInfo toDto(com.ai.edu.domain.llm.model.ProviderInfo domain) {
        if (domain == null) {
            return null;
        }

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
    public static ScenesResponse toDto(com.ai.edu.domain.llm.model.ScenesResponse domain) {
        if (domain == null) {
            return null;
        }

        List<ScenesResponse.SceneInfo> scenes = Collections.emptyList();
        if (domain.getScenes() != null) {
            scenes = domain.getScenes().stream()
                    .map(LlmConvert::toDto)
                    .collect(Collectors.toList());
        }

        return ScenesResponse.builder()
                .scenes(scenes)
                .build();
    }

    /**
     * 将领域模型场景信息转换为 DTO 场景信息
     */
    public static ScenesResponse.SceneInfo toDto(com.ai.edu.domain.llm.model.SceneInfo domain) {
        if (domain == null) {
            return null;
        }
        return ScenesResponse.SceneInfo.builder()
                .code(domain.getCode())
                .defaultProvider(domain.getDefaultProvider())
                .defaultModel(domain.getDefaultModel())
                .description(domain.getDescription())
                .build();
    }
}