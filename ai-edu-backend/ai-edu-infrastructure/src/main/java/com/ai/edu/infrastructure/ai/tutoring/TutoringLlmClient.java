package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.ActionMeta;
import com.ai.edu.domain.learning.model.contract.DecideContext;
import com.ai.edu.domain.learning.model.contract.GenerateContext;
import com.ai.edu.domain.learning.model.contract.OcrResult;
import com.ai.edu.domain.learning.service.TutoringConfig;
import com.ai.edu.domain.learning.service.TutoringLlmPort;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * <p>类型先行流式：{@code decide}（SSE 流式消费，过滤 meta 事件取 ActionMeta）→ {@code generate}（流式正文）。
 * decide 仅连接失败（未收到事件）重试 {@code ai-edu.tutoring.agent-retry}（默认 1）次，
 * 空流/error（正常完成无 meta）不重试；<b>generate 不可重试</b>（流式），失败由编排层降级。
 */
@Slf4j
@Repository
public class TutoringLlmClient implements TutoringLlmPort {

    /** 宽容 ObjectMapper：容忍 Python decide meta 事件中的可选调试字段（reason 等），Java 不建模。 */
    private static final ObjectMapper DECIDE_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Resource
    private WebClient tutoringWebClient;

    @Resource
    private TutoringConfig tutoringConfig;

    @Override
    public ActionMeta decide(DecideContext context) {
        log.info("[tutoring] decide 调用, history={}, round={}, answerReq={}, isNewQuestion={}",
                context.getHistory() == null ? 0 : context.getHistory().size(),
                context.getRoundCount(), context.getAnswerRequestCount(), context.isNewQuestion());
        try {
            ActionMeta meta = Mono.defer(() -> tutoringWebClient.post()
                    .uri(config().decidePath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(context)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                    .filter(e -> "meta".equals(e.event()))
                    .map(e -> readActionMeta(e.data()))
                    .next())
                    .retry(config().agentRetry())
                    .block(config().decideTimeout());
            if (meta == null) {
                // 空流 / event: error（流正常完成但无 meta）→ 按 agent 失败处理，不重试
                log.warn("[tutoring] decide 流中无 meta 事件（空流/error），按 agent 失败处理");
                throw new TutoringAgentException("答疑决策服务暂不可用");
            }
            return meta;
        } catch (TutoringAgentException e) {
            throw e;
        } catch (Exception e) {
            log.error("[tutoring] decide 调用失败: {}", e.getMessage(), e);
            throw new TutoringAgentException("答疑决策服务暂不可用", e);
        }
    }

    /** 解析 meta 事件 data → ActionMeta（宽容 ObjectMapper，容忍 Python 调试字段 reason 等未知字段）。 */
    private ActionMeta readActionMeta(String data) {
        try {
            return DECIDE_MAPPER.readValue(data, ActionMeta.class);
        } catch (Exception e) {
            throw new TutoringAgentException("答疑决策服务暂不可用", e);
        }
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
