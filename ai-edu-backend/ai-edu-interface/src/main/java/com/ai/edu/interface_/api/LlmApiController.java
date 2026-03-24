package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.llm.AllowedModelsResponse;
import com.ai.edu.application.dto.llm.ChatRequest;
import com.ai.edu.application.dto.llm.ChatResponse;
import com.ai.edu.application.dto.llm.ModelsResponse;
import com.ai.edu.application.dto.llm.ScenesResponse;
import com.ai.edu.application.service.LlmAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * LLM API 控制器
 * 提供 LLM 对话相关接口
 *
 * @author AI Edu Platform
 */
@Slf4j
@RestController
@RequestMapping("/api/llm")
public class LlmApiController {

    @Resource
    private LlmAppService llmAppService;

    /**
     * 同步对话接口
     * 需要登录验证
     *
     * @param request 对话请求
     * @param session HTTP Session
     * @return 对话响应
     */
    @PostMapping("/chat")
    public Mono<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request,
            HttpSession session) {
        // 从 Session 获取用户 ID
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Mono.error(new BusinessException(ErrorCode.UNAUTHORIZED, "未登录"));
        }
        request.setUserId(userId);

        log.info("LLM 同步对话请求: userId={}, scene={}", userId, request.getScene());

        return llmAppService.chat(request)
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("LLM 同步对话成功: userId={}", userId))
                .doOnError(e -> log.error("LLM 同步对话失败: userId={}, error={}", userId, e.getMessage()));
    }

    /**
     * 流式对话接口 (SSE)
     * 需要登录验证
     *
     * @param request 对话请求
     * @param session HTTP Session
     * @return SSE 事件流
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @Valid @RequestBody ChatRequest request,
            HttpSession session) {
        // 从 Session 获取用户 ID
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            // 对于 SSE，返回错误事件
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"code\":\"" + ErrorCode.UNAUTHORIZED + "\",\"message\":\"未登录\"}")
                    .build());
        }
        request.setUserId(userId);

        log.info("LLM 流式对话请求: userId={}, scene={}", userId, request.getScene());

        return llmAppService.chatStream(request)
                .doOnComplete(() -> log.info("LLM 流式对话完成: userId={}", userId))
                .doOnError(e -> log.error("LLM 流式对话失败: userId={}, error={}", userId, e.getMessage()));
    }

    /**
     * 获取允许调用的模型列表
     * 公开接口，无需登录
     *
     * @return 允许调用的模型列表
     */
    @GetMapping("/allowed-models")
    public Mono<ApiResponse<AllowedModelsResponse>> getAllowedModels() {
        log.info("获取允许调用的模型列表");

        return llmAppService.getAllowedModels()
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("获取允许调用的模型列表成功"))
                .doOnError(e -> log.error("获取允许调用的模型列表失败: {}", e.getMessage()));
    }

    /**
     * 获取所有模型列表
     * 公开接口，无需登录
     *
     * @return 所有模型列表
     */
    @GetMapping("/models")
    public Mono<ApiResponse<ModelsResponse>> getModels() {
        log.info("获取所有模型列表");

        return llmAppService.getModels()
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("获取所有模型列表成功"))
                .doOnError(e -> log.error("获取所有模型列表失败: {}", e.getMessage()));
    }

    /**
     * 获取场景列表
     * 公开接口，无需登录
     *
     * @return 场景列表
     */
    @GetMapping("/scenes")
    public Mono<ApiResponse<ScenesResponse>> getScenes() {
        log.info("获取场景列表");

        return llmAppService.getScenes()
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("获取场景列表成功"))
                .doOnError(e -> log.error("获取场景列表失败: {}", e.getMessage()));
    }
}