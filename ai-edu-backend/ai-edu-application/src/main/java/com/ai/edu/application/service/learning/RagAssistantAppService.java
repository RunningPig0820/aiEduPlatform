package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.command.RagAskCommand;
import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import com.ai.edu.domain.learning.service.RagAssistantPort;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RAG 项目介绍助手应用服务：编排一轮问答（角色门在控制器，本服务负责 trace_id 生成、permission 事件、
 * 桥调用与 SSE 事件流组装）。
 *
 * <p>M1 阶段：桥为桩替（返回固定 done），permission 由本服务生成（含 traceId，Java 入口生成）。
 * 后续里程碑（M2）补 history 组装与 Python 真实桥调用。
 */
@Slf4j
@Service
public class RagAssistantAppService {

    @Resource
    private RagAssistantPort ragAssistantPort;

    /**
     * 发起一轮问答（SSE 流式）：permission（Java 发）→ 桥事件流（intent 之后）。
     */
    public Flux<ServerSentEvent<String>> ask(RagAskCommand command) {
        String traceId = UUID.randomUUID().toString();
        ServerSentEvent<String> permission = ServerSentEvent.<String>builder()
                .event("permission")
                .data("{\"role\":\"STUDENT\",\"allowed\":true,\"traceId\":\"" + traceId + "\"}")
                .build();
        RagAskRequest request = RagAskRequest.builder()
                .question(command.getQuestion())
                .sessionId(command.getSessionId())
                .currentProject(command.getCurrentProject())
                .history(List.of()) // M2 起由网关组装最近 N 轮
                .traceId(traceId)
                .topK(command.getTopK() == null ? 3 : command.getTopK())
                .build();
        log.info("[rag-assistant] ask, traceId={}, sessionId={}, currentProject={}",
                traceId, command.getSessionId(), command.getCurrentProject());
        return Flux.concat(Flux.just(permission), ragAssistantPort.ask(request));
    }

    /**
     * 发起一轮问答（非流式，M1 桩替）：返回 done 结构 + stages 摘要（真实链路在 M2 起）。
     */
    public Map<String, Object> askStages(RagAskCommand command) {
        return Map.of(
                "answer", "（桩替）RAG 项目介绍助手链路已通，等待 Python 白盒引擎接入。",
                "quotedKeys", List.of(),
                "tokensUsage", Map.of("promptTokens", 0, "completionTokens", 0, "cacheHitTokens", 0, "totalTokens", 0),
                "traceId", UUID.randomUUID().toString(),
                "suggestions", List.of(),
                "reason", null,
                "stages", List.of("permission", "intent", "rewrite", "rerank", "done"));
    }
}
