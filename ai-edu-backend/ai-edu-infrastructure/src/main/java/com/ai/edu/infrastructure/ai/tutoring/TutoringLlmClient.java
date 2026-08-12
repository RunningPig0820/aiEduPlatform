package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.DecideContext;
import com.ai.edu.domain.learning.model.contract.GenerateContext;
import com.ai.edu.domain.learning.model.contract.OcrResult;
import com.ai.edu.domain.learning.service.TutoringConfig;
import com.ai.edu.domain.learning.service.TutoringLlmPort;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 答疑 Python agent 客户端（WebClient，复用 llm-gateway internalToken 模式）。
 *
 * <p>类型先行流式：{@code decide}（SSE 流，thinking/agent/meta/done 全透传，由编排层消费）→ {@code generate}（流式正文）。
 * <b>decide/generate 均不可重试</b>（流式，重试会重发已透传事件），失败由编排层降级。
 */
@Slf4j
@Repository
public class TutoringLlmClient implements TutoringLlmPort {

    @Resource
    private WebClient tutoringWebClient;

    @Resource
    private TutoringConfig tutoringConfig;

    @Override
    public Flux<ServerSentEvent<String>> decideStream(DecideContext context) {
        log.info("[tutoring] decideStream 调用, history={}, round={}, answerReq={}, isNewQuestion={}",
                context.getHistory() == null ? 0 : context.getHistory().size(),
                context.getRoundCount(), context.getAnswerRequestCount(), context.isNewQuestion());
        // 流式不可重试：重试会重发已透传事件，失败由编排层降级（与 generate 一致）；
        // 超时按配置（2026-08 关思考后 decide ~1.5s，配置 10s 防挂起；TimeoutException 由 onErrorResume 包装）
        return tutoringWebClient.post()
                .uri(config().decidePath())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(context)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .doOnNext(event -> log.trace("[tutoring] decide SSE: {}", event.data()))
                .timeout(config().decideTimeout())
                .onErrorResume(e -> {
                    log.error("[tutoring] decide 调用失败: {}", e.getMessage(), e);
                    return Flux.error(new TutoringAgentException("答疑决策服务暂不可用", e));
                });
    }

    @Override
    public Flux<ServerSentEvent<String>> generate(GenerateContext context) {
        log.info("[tutoring] generate 调用, actionType={}, history={}",
                context.getActionType(), context.getHistory() == null ? 0 : context.getHistory().size());
        return tutoringWebClient.post()
                .uri(config().generatePath())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(context)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .doOnNext(event -> log.trace("[tutoring] generate SSE: {}", event.data()))
                .onErrorResume(e -> {
                    log.error("[tutoring] generate 调用失败: {}", e.getMessage(), e);
                    return Flux.error(new TutoringAgentException("答疑生成服务暂不可用", e));
                });
    }

    @Override
    public OcrResult recognize(byte[] imageData, String originalFilename) {
        log.info("[tutoring] OCR 调用, file={}", originalFilename);
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(imageData) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            });
            return Mono.defer(() -> tutoringWebClient.post()
                    .uri(config().ocrPath())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(OcrResult.class))
                    .retry(config().agentRetry())
                    .block(config().ocrTimeout());
        } catch (Exception e) {
            log.error("[tutoring] OCR 调用失败: {}", e.getMessage(), e);
            throw new TutoringAgentException("题目识别服务暂不可用", e);
        }
    }

    /** 答疑配置（未注入时回退默认值，保持测试/默认行为一致）。 */
    private TutoringConfig config() {
        return tutoringConfig == null ? TutoringConfig.defaults() : tutoringConfig;
    }
}
