package com.ai.edu.application.service.llm;

import com.ai.edu.application.dto.llm.*;
import com.ai.edu.application.assembler.LlmConvert;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.service.LlmGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * LLM 应用服务
 * 职责：编排 LLM Gateway，处理业务逻辑
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
        AiEduChatRequest domainRequest = LlmConvert.toDomain(request);

        return llmGateway.chat(domainRequest)
                .map(LlmConvert::toDto)
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
        AiEduChatRequest domainRequest = LlmConvert.toDomain(request);

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
                .map(LlmConvert::toDto)
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
                .map(LlmConvert::toDto)
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
                .map(LlmConvert::toDto)
                .doOnSuccess(response -> log.info("获取到 {} 个场景",
                        response.getScenes() != null ? response.getScenes().size() : 0))
                .doOnError(e -> log.error("获取场景列表失败: {}", e.getMessage(), e));
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