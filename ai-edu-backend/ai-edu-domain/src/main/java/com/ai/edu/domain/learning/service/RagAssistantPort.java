package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * RAG 项目介绍助手端口（Java → Python 白盒链路引擎的调用契约）。
 *
 * <p>基础设施层 {@code RagAssistantBridgeImpl} 以 WebClient 实现（复用 llm-gateway internalToken 模式，
 * 后续里程碑接真实 Python 端点）。<b>permission 由 Java 网关发</b>，桥返回的 SSE 流从 intent 事件开始；
 * meta/done 由网关重建，不透传 Python 原始事件。流式不可重试，失败由编排层降级。
 */
public interface RagAssistantPort {

    /**
     * 发起一轮问答（SSE 流）：返回 Python 白盒事件流（intent → (clarify|switch) → rewrite → rerank → (boundary|token*) → done）。
     * 实现内部<b>不重试</b>（流式不可重试，失败由编排层降级）。
     *
     * @param request ask 请求（history/traceId 由 Java 组装传入）
     */
    Flux<ServerSentEvent<String>> ask(RagAskRequest request);

    /**
     * 发起一轮问答（非流式，RAG-A-17）：请求 stream=false → Python 返回 done+stages 摘要 JSON
     * （snake_case，{answer, quoted_keys, tokens_usage, trace_id, suggestions, reason, stages:{intent,rewrite,rerank}}）。
     * 返回 Python 原始 JSON 字符串，由应用层 snake→camel 重建。失败 → TutoringAgentException。
     *
     * @param request ask 请求（history/traceId 由 Java 组装传入，stream=false）
     */
    Mono<String> askSync(RagAskRequest request);

    /**
     * 查看原文（非流式）：转发 Python 源文件静态服务，返回文件内容（markdown/text）。
     * 文件不存在 → EntityNotFoundException（10002，HTTP 404）。
     *
     * @param filePath rerank 块的 filePath（如 "4.完善文档/02-…md"，含目录分隔）
     */
    Mono<String> source(String filePath);

    /**
     * 评估报告（非流式）：转发 Python baseline 报告白盒（hit@3/质量分/耗时/成本/版本）。
     * 返回 Python 原始 snake_case JSON 字符串，由应用层反序列化为 camelCase DTO。
     * 暂无报告 → EntityNotFoundException（10002，HTTP 404）。
     */
    Mono<String> evalReport();

    /**
     * 触发重评测（非流式，异步模型）：转发 Python {@code POST /api/rag/assistant/eval/run}，
     * Python 后台跑一轮真实评估（几分钟），立即返回 {running:true}。
     * 已有一轮在跑 → 幂等返回 {running:true, already_running:true}。
     * 返回 Python 原始 JSON 字符串，由应用层反序列化。
     */
    Mono<String> evalRun();

    /**
     * 开始引导（非流式）：转发 Python {@code GET /api/rag/assistant/guide}，返回模块引导底座池
     * 出题（1~3 条，必含 ≥1 条 RAG 方向）。0 token、非 SSE、不占冻结时序。
     * currentProject 为可选模块锚点（缺省由 Python 兜底默认模块）。
     * 返回 Python 原始 JSON 字符串，由应用层反序列化。
     */
    Mono<String> guide(String currentProject);
}
