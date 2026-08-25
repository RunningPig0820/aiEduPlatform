package com.ai.edu.infrastructure.ai.rag;

import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import com.ai.edu.domain.learning.service.RagAssistantPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * RAG 助手 Python 引擎桥（M1 桩替实现，Python 未接入前返回固定 done）。
 *
 * <p>M1 阶段：不调 Python，返回固定桩替 done（permission 由 Java 网关发，本桥只回 intent 之后的事件）。
 * 后续里程碑（M2 起）替换为 WebClient 调 Python `/api/rag/assistant/ask`（复用 llm-gateway internalToken）。
 */
@Slf4j
@Repository
public class RagAssistantBridgeImpl implements RagAssistantPort {

    @Override
    public Flux<ServerSentEvent<String>> ask(RagAskRequest request) {
        log.info("[rag-assistant] M1 桩替 ask, question={}, sessionId={}, traceId={}",
                request.getQuestion(), request.getSessionId(), request.getTraceId());
        String stubDone = "{\"answer\":\"（桩替）RAG 项目介绍助手链路已通，等待 Python 白盒引擎接入。\"," +
                "\"quotedKeys\":[],\"tokensUsage\":{\"promptTokens\":0,\"completionTokens\":0,\"cacheHitTokens\":0,\"totalTokens\":0}," +
                "\"traceId\":\"" + request.getTraceId() + "\",\"suggestions\":[],\"reason\":null}";
        return Flux.just(ServerSentEvent.<String>builder().event("done").data(stubDone).build());
    }
}
