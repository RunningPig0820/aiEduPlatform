package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.command.RagAskCommand;
import com.ai.edu.application.dto.learning.rag.SseBoundaryDTO;
import com.ai.edu.application.dto.learning.rag.SseClarifyDTO;
import com.ai.edu.application.dto.learning.rag.SseDoneDTO;
import com.ai.edu.application.dto.learning.rag.SseIntentDTO;
import com.ai.edu.application.dto.learning.rag.SsePermissionDTO;
import com.ai.edu.application.dto.learning.rag.SseRerankDTO;
import com.ai.edu.application.dto.learning.rag.SseRewriteDTO;
import com.ai.edu.application.dto.learning.rag.SseSwitchDTO;
import com.ai.edu.application.dto.learning.rag.SseTokenDTO;
import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import com.ai.edu.domain.learning.service.RagAssistantPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RAG 项目介绍助手应用服务：编排一轮问答（角色门在控制器，本服务负责 trace_id 生成、permission 事件、
 * 桥调用与 SSE 事件流**重建**）。
 *
 * <p><b>SSE 中继（阶段 1）</b>：桥返回 Python 原始 snake_case 事件流，本服务逐事件重建为前端 camelCase
 * 契约（intent/rewrite/rerank/boundary/clarify/switch/token/done），permission 由本服务前置（含 traceId）。
 * <b>done traceId 一致性校验</b>：Python done 回显 traceId 与 Java 生成值不一致 → 仅告警不阻断（契约定稿）。
 * 重建失败的事件透传原始（不阻断链路）。
 */
@Slf4j
@Service
public class RagAssistantAppService {

    /** 读 Python snake_case 事件数据 → camelCase DTO */
    private static final ObjectMapper SNAKE_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** 写前端 camelCase 事件 */
    private static final ObjectMapper CAMEL_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Resource
    private RagAssistantPort ragAssistantPort;

    /**
     * 发起一轮问答（SSE 流式）：permission（Java 发，含 traceId）→ 桥事件流（snake→camel 重建）。
     */
    public Flux<ServerSentEvent<String>> ask(RagAskCommand command) {
        String traceId = UUID.randomUUID().toString();
        SsePermissionDTO permission = SsePermissionDTO.builder()
                .role("STUDENT").allowed(true).traceId(traceId).build();
        RagAskRequest request = RagAskRequest.builder()
                .question(command.getQuestion())
                .sessionId(command.getSessionId())
                .currentProject(command.getCurrentProject())
                .history(List.of()) // M2 起由网关组装最近 N 轮
                .traceId(traceId)
                .topK(command.getTopK() == null ? 3 : command.getTopK())
                .stream(Boolean.TRUE) // 桥恒以 SSE 流式调 Python（非流式走 Java 侧 askStages）
                .build();
        log.info("[rag-assistant] ask, traceId={}, sessionId={}, currentProject={}",
                traceId, command.getSessionId(), command.getCurrentProject());
        return Flux.concat(
                Flux.just(sse("permission", writeCamel(permission))),
                ragAssistantPort.ask(request).map(ev -> rebuildEvent(ev, traceId)));
    }

    /**
     * 发起一轮问答（非流式，M1 桩替）：返回 done 结构 + stages 摘要（真实链路在 M2 起）。
     */
    public Map<String, Object> askStages(RagAskCommand command) {
        // 用 LinkedHashMap（Map.of 不允许 null 值，"reason":null 会抛 NPE）
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", "（桩替）RAG 项目介绍助手链路已通，等待 Python 白盒引擎接入。");
        result.put("quotedKeys", List.of());
        result.put("tokensUsage", Map.of("promptTokens", 0, "completionTokens", 0, "cacheHitTokens", 0, "totalTokens", 0));
        result.put("traceId", UUID.randomUUID().toString());
        result.put("suggestions", List.of());
        result.put("reason", null);
        result.put("stages", List.of("permission", "intent", "rewrite", "rerank", "done"));
        return result;
    }

    /** 重建 Python snake 事件 → 前端 camel 事件；done 做 traceId 一致性校验（对不上告警，不阻断）。 */
    private ServerSentEvent<String> rebuildEvent(ServerSentEvent<String> pyEvent, String traceId) {
        if (pyEvent == null || pyEvent.event() == null || pyEvent.data() == null) {
            return pyEvent;
        }
        String event = pyEvent.event();
        Object dto;
        try {
            dto = switch (event) {
                case "intent" -> SNAKE_MAPPER.readValue(pyEvent.data(), SseIntentDTO.class);
                case "rewrite" -> SNAKE_MAPPER.readValue(pyEvent.data(), SseRewriteDTO.class);
                case "rerank" -> SNAKE_MAPPER.readValue(pyEvent.data(), SseRerankDTO.class);
                case "boundary" -> SNAKE_MAPPER.readValue(pyEvent.data(), SseBoundaryDTO.class);
                case "clarify" -> SNAKE_MAPPER.readValue(pyEvent.data(), SseClarifyDTO.class);
                case "switch" -> SNAKE_MAPPER.readValue(pyEvent.data(), SseSwitchDTO.class);
                case "token" -> SNAKE_MAPPER.readValue(pyEvent.data(), SseTokenDTO.class);
                case "done" -> {
                    SseDoneDTO done = SNAKE_MAPPER.readValue(pyEvent.data(), SseDoneDTO.class);
                    if (done.getTraceId() != null && !traceId.equals(done.getTraceId())) {
                        log.warn("[rag-assistant] done traceId 不一致: 期望={}, 实际={}", traceId, done.getTraceId());
                    }
                    yield done;
                }
                default -> null;
            };
        } catch (JsonProcessingException e) {
            log.warn("[rag-assistant] 事件重建失败, event={}: {}", event, e.getMessage());
            return pyEvent; // 重建失败透传原始（不阻断）
        }
        if (dto == null) {
            return pyEvent;
        }
        return ServerSentEvent.<String>builder().event(event).data(writeCamel(dto)).build();
    }

    private String writeCamel(Object dto) {
        try {
            return CAMEL_MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("SSE 事件序列化失败: " + dto.getClass().getSimpleName(), e);
        }
    }

    private ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.<String>builder().event(event).data(data).build();
    }
}
