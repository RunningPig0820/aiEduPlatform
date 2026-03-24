package com.ai.edu.domain.llm.service;

import com.ai.edu.domain.llm.model.AllowedModelsResponse;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.model.ModelsResponse;
import com.ai.edu.domain.llm.model.ScenesResponse;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * LLM Gateway 接口
 * 定义与 Python LLM 服务通信的契约
 *
 * @author AI Edu Platform
 */
public interface LlmGateway {

    /**
     * 同步对话
     *
     * @param request 对话请求
     * @return 对话响应
     */
    Mono<AiEduChatResponse> chat(AiEduChatRequest request);

    /**
     * 流式对话 (SSE)
     *
     * @param request 对话请求
     * @return SSE 事件流
     */
    Flux<ServerSentEvent<String>> chatStream(AiEduChatRequest request);

    /**
     * 获取允许调用的模型列表
     *
     * @return 允许调用的模型列表
     */
    Mono<AllowedModelsResponse> getAllowedModels();

    /**
     * 获取所有模型列表
     *
     * @return 所有模型列表
     */
    Mono<ModelsResponse> getModels();

    /**
     * 获取场景列表
     *
     * @return 场景列表
     */
    Mono<ScenesResponse> getScenes();
}