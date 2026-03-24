package com.ai.edu.infrastructure.ai;

import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.LlmGatewayException;
import com.ai.edu.domain.llm.model.AllowedModelsResponse;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.model.LlmApiResponse;
import com.ai.edu.domain.llm.model.ModelsResponse;
import com.ai.edu.domain.llm.model.ScenesResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * LLM Gateway 实现
 * 使用 WebClient 与 Python LLM 服务通信
 *
 * @author AI Edu Platform
 */
@Slf4j
@Repository
public class LlmGatewayImpl implements LlmGateway {

    private static final String API_CHAT = "/api/llm/chat";
    private static final String API_CHAT_STREAM = "/api/llm/chat/stream";
    private static final String API_ALLOWED_MODELS = "/api/llm/allowed-models";
    private static final String API_MODELS = "/api/llm/models";
    private static final String API_SCENES = "/api/llm/scenes";


    @Resource
    private WebClient llmWebClient;

    /**
     * 设置 WebClient（用于测试）
     */
    void setLlmWebClient(WebClient llmWebClient) {
        this.llmWebClient = llmWebClient;
    }

    @Override
    public Mono<AiEduChatResponse> chat(AiEduChatRequest request) {
        log.debug("Sending chat request for userId: {}, scene: {}", request.getUserId(), request.getScene());

        // chat 接口直接返回 ChatResponse，不包装
        return llmWebClient.post()
                .uri(API_CHAT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiEduChatResponse.class)
                .doOnSuccess(response -> log.debug("Chat response received, modelUsed: {}",
                        response != null ? response.getModelUsed() : null))
                .doOnError(error -> log.error("Chat request failed: {}", error.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::handleResponseError)
                .onErrorMap(WebClientRequestException.class, this::handleRequestError);
    }

    @Override
    public Flux<ServerSentEvent<String>> chatStream(AiEduChatRequest request) {
        log.debug("Sending stream chat request for userId: {}, scene: {}",
                request.getUserId(), request.getScene());

        return llmWebClient.post()
                .uri(API_CHAT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .doOnNext(event -> log.trace("SSE event received: {}", event))
                .doOnError(error -> log.error("Stream chat request failed: {}", error.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::handleResponseError)
                .onErrorMap(WebClientRequestException.class, this::handleRequestError);
    }

    @Override
    public Mono<AllowedModelsResponse> getAllowedModels() {
        log.debug("Fetching allowed models");

        // GET 接口返回 {"code": "00000", "data": {...}} 格式
        return llmWebClient.get()
                .uri(API_ALLOWED_MODELS)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<LlmApiResponse<AllowedModelsResponse>>() {})
                .map(LlmApiResponse::getData)
                .doOnSuccess(response -> log.debug("Allowed models fetched, count: {}",
                        response != null && response.getAllowedModels() != null
                                ? response.getAllowedModels().size()
                                : 0))
                .doOnError(error -> log.error("Get allowed models failed: {}", error.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::handleResponseError)
                .onErrorMap(WebClientRequestException.class, this::handleRequestError);
    }

    @Override
    public Mono<ModelsResponse> getModels() {
        log.debug("Fetching all models");

        return llmWebClient.get()
                .uri(API_MODELS)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<LlmApiResponse<ModelsResponse>>() {})
                .map(LlmApiResponse::getData)
                .doOnSuccess(response -> log.debug("Models fetched, provider count: {}",
                        response != null && response.getProviders() != null
                                ? response.getProviders().size()
                                : 0))
                .doOnError(error -> log.error("Get models failed: {}", error.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::handleResponseError)
                .onErrorMap(WebClientRequestException.class, this::handleRequestError);
    }

    @Override
    public Mono<ScenesResponse> getScenes() {
        log.debug("Fetching scenes");

        return llmWebClient.get()
                .uri(API_SCENES)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<LlmApiResponse<ScenesResponse>>() {})
                .map(LlmApiResponse::getData)
                .doOnSuccess(response -> log.debug("Scenes fetched, count: {}",
                        response != null && response.getScenes() != null
                                ? response.getScenes().size()
                                : 0))
                .doOnError(error -> log.error("Get scenes failed: {}", error.getMessage()))
                .onErrorMap(WebClientResponseException.class, this::handleResponseError)
                .onErrorMap(WebClientRequestException.class, this::handleRequestError);
    }

    /**
     * 处理 HTTP 响应错误
     */
    private LlmGatewayException handleResponseError(WebClientResponseException ex) {
        int statusCode = ex.getStatusCode().value();
        String responseBody = ex.getResponseBodyAsString();

        log.error("LLM Gateway response error: status={}, body={}", statusCode, responseBody);

        return switch (statusCode) {
            case 400 -> new LlmGatewayException(ErrorCode.LLM_INVALID_PARAMS,
                    "LLM 请求参数无效: " + responseBody);
            case 403 -> new LlmGatewayException(ErrorCode.LLM_MODEL_NOT_ALLOWED,
                    "LLM 模型不允许调用: " + responseBody);
            case 408, 504 -> new LlmGatewayException(ErrorCode.LLM_TIMEOUT,
                    "LLM 服务超时: " + responseBody);
            case 429 -> new LlmGatewayException(ErrorCode.LLM_CALL_FAILED,
                    "LLM 请求频率超限: " + responseBody);
            default -> new LlmGatewayException(ErrorCode.LLM_SERVICE_UNAVAILABLE,
                    "LLM 服务不可用 (status=" + statusCode + "): " + responseBody);
        };
    }

    /**
     * 处理网络请求错误
     */
    private LlmGatewayException handleRequestError(WebClientRequestException ex) {
        log.error("LLM Gateway request error: {}", ex.getMessage());
        return new LlmGatewayException(ErrorCode.LLM_SERVICE_UNAVAILABLE,
                "LLM 服务不可用: " + ex.getMessage(), ex);
    }
}