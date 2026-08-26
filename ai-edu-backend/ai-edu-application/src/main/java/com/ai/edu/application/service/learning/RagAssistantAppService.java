package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.command.RagAskCommand;
import com.ai.edu.application.dto.learning.rag.RagCloseDTO;
import com.ai.edu.application.dto.learning.rag.RagEvalReportDTO;
import com.ai.edu.application.dto.learning.rag.RagEvalRunDTO;
import com.ai.edu.application.dto.learning.rag.RagGuideDTO;
import com.ai.edu.application.dto.learning.rag.RagRealConversationDTO;
import com.ai.edu.application.dto.learning.rag.RagSessionUsageDTO;
import com.ai.edu.application.dto.learning.rag.SseBoundaryDTO;
import com.ai.edu.application.dto.learning.rag.SseRerankBlock;
import com.ai.edu.application.dto.learning.rag.SseRerankDTO;
import com.ai.edu.application.dto.learning.rag.SseClarifyDTO;
import com.ai.edu.application.dto.learning.rag.SseDoneDTO;
import com.ai.edu.application.dto.learning.rag.SseIntentDTO;
import com.ai.edu.application.dto.learning.rag.SsePermissionDTO;
import com.ai.edu.application.dto.learning.rag.SseRerankDTO;
import com.ai.edu.application.dto.learning.rag.SseRewriteDTO;
import com.ai.edu.application.dto.learning.rag.SseSwitchDTO;
import com.ai.edu.application.dto.learning.rag.SseTokenDTO;
import com.ai.edu.application.dto.learning.rag.SseTokensUsageDTO;
import com.ai.edu.common.exception.EntityNotFoundException;
import com.ai.edu.domain.learning.model.contract.RagAskRequest;
import com.ai.edu.domain.learning.model.contract.RagQualityScore;
import com.ai.edu.domain.learning.service.RagAssistantPort;
import com.ai.edu.domain.learning.service.RagQualityGrader;
import com.ai.edu.domain.shared.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

    /** 真实对话质量统计 Redis key（每轮 LLM 打分累计，TTL 24h） */
    private static final String EVAL_RECENT_KEY = "rag:assistant:eval:recent";

    /** M7 会话收尾 Redis 键/常量（对齐 tutoring：Java 聚合点，Python 无状态） */
    private static final String SESSION_KEY_PREFIX = "rag:assistant:session:";
    private static final String SESSION_USAGE_SUFFIX = ":usage";
    private static final String SESSION_CLOSED_SUFFIX = ":closed";
    private static final String TRACE_KEY_PREFIX = "rag:assistant:trace:";
    private static final long SESSION_TTL_HOURS = 24;
    /** 会话已关闭后再 ask → 固定话术（0 token，不调 Python；对齐 Python CLOSED_MSG） */
    private static final String CLOSED_MSG = "本轮对话已结束，可开启新对话。";

    @Resource
    private RagAssistantPort ragAssistantPort;

    @Resource
    private RagQualityGrader ragQualityGrader;

    @Resource
    private RedisService redisService;

    /**
     * 发起一轮问答（SSE 流式）：permission（Java 发，含 traceId）→ 桥事件流（snake→camel 重建）。
     */
    public Flux<ServerSentEvent<String>> ask(RagAskCommand command) {
        String traceId = UUID.randomUUID().toString();
        SsePermissionDTO permission = SsePermissionDTO.builder()
                .role("STUDENT").allowed(true).traceId(traceId).build();
        // M7 7.2：会话已关闭 → 短路固定话术（0 token，不调 Python）
        if (isSessionClosed(command.getSessionId())) {
            log.info("[rag-assistant] ask 会话已关闭, 短路话术 sessionId={}", command.getSessionId());
            return closedSessionStream(permission, traceId);
        }
        RagAskRequest request = RagAskRequest.builder()
                .question(command.getQuestion())
                .sessionId(command.getSessionId())
                .currentProject(command.getCurrentProject())
                // M6 方案A：history 由前端传（最近 3 轮，追问展开用）；不落库，刷新后为空=新会话
                .history(command.getHistory() == null ? List.of() : command.getHistory())
                .traceId(traceId)
                .topK(command.getTopK() == null ? 3 : command.getTopK())
                .stream(Boolean.TRUE) // 桥恒以 SSE 流式调 Python（非流式走 Java 侧 askStages）
                .build();
        log.info("[rag-assistant] ask, traceId={}, sessionId={}, currentProject={}",
                traceId, command.getSessionId(), command.getCurrentProject());
        long startNanos = System.nanoTime();
        AtomicReference<List<SseRerankBlock>> blocksRef = new AtomicReference<>(List.of());
        AtomicReference<String> categoryRef = new AtomicReference<>();
        return Flux.concat(
                Flux.just(sse("permission", writeCamel(permission))),
                ragAssistantPort.ask(request)
                        .map(ev -> rebuildEvent(ev, traceId))
                        // 捕获 rerank 块（title/summary 供忠实度打分），done 前 ready
                        .doOnNext(ev -> captureRerankBlocks(ev, blocksRef))
                        // 捕获 intent category（问候轮识别，避免欢迎语被误打分进评估）
                        .doOnNext(ev -> captureIntentCategory(ev, categoryRef))
                        // M7 7.1/7.3：每轮 done 落库（会话累计 token + trace 补查快照）
                        .doOnNext(ev -> persistRound(command, ev, traceId))
                        // 每轮真实问答 done 后：后台异步 LLM 打分 → 累计进评估报告（不阻塞 SSE）
                        .doOnNext(ev -> scheduleGradeOnDone(command, ev, blocksRef.get(), startNanos,
                                categoryRef.get())));
    }

    /**
     * 关闭对话（非流式）：置 session closed（Redis）+ 读回会话累计 token/轮数。
     * 幂等：已关闭也返回 closed=true；未知 session（无累计且未关闭）→ EntityNotFoundException（10002）。
     */
    public Mono<RagCloseDTO> close(String sessionId) {
        return Mono.fromCallable(() -> {
            String usageKey = SESSION_KEY_PREFIX + sessionId + SESSION_USAGE_SUFFIX;
            String closedKey = SESSION_KEY_PREFIX + sessionId + SESSION_CLOSED_SUFFIX;
            String closed = redisService.get(closedKey);
            String usageJson = redisService.get(usageKey);
            if (closed == null && usageJson == null) {
                throw new EntityNotFoundException("会话不存在");
            }
            if (closed == null) {
                redisService.set(closedKey, "1", SESSION_TTL_HOURS, TimeUnit.HOURS);
            }
            int rounds = 0;
            int prompt = 0, completion = 0, cacheHit = 0, total = 0;
            if (usageJson != null) {
                JsonNode agg = CAMEL_MAPPER.readTree(usageJson);
                rounds = agg.path("rounds").asInt(0);
                prompt = agg.path("promptTokens").asInt(0);
                completion = agg.path("completionTokens").asInt(0);
                cacheHit = agg.path("cacheHitTokens").asInt(0);
                total = agg.path("totalTokens").asInt(0);
            }
            return RagCloseDTO.builder()
                    .sessionId(sessionId).closed(true).rounds(rounds)
                    .sessionUsage(RagSessionUsageDTO.builder()
                            .promptTokens(prompt).completionTokens(completion)
                            .cacheHitTokens(cacheHit).totalTokens(total).build())
                    .build();
        });
    }

    /**
     * 断线补查单轮结果（非流式）：读 Redis trace 快照（每轮 done 落库），不存在 → 10002。
     */
    public Mono<SseDoneDTO> turn(String traceId) {
        return Mono.fromCallable(() -> {
            String json = redisService.get(TRACE_KEY_PREFIX + traceId);
            if (json == null) {
                throw new EntityNotFoundException("trace 不存在");
            }
            return CAMEL_MAPPER.readValue(json, SseDoneDTO.class);
        });
    }

    /**
     * 查看原文（非流式）：委托桥转发 Python 源文件静态服务，返回文件内容。
     * 文件不存在 → EntityNotFoundException（10002，HTTP 404）。
     */
    public Mono<String> source(String filePath) {
        return ragAssistantPort.source(filePath);
    }

    /**
     * 评估报告（非流式）：桥返回 Python snake_case 报告 JSON → SNAKE_MAPPER 解析为 camelCase DTO。
     * 暂无报告 → EntityNotFoundException（10002，HTTP 404）。
     */
    public Mono<RagEvalReportDTO> evalReport() {
        return ragAssistantPort.evalReport().map(json -> {
            try {
                RagEvalReportDTO dto = SNAKE_MAPPER.readValue(json, RagEvalReportDTO.class);
                dto.setRealConversation(readRealConversation()); // Java 侧真实对话质量统计并入
                return dto;
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("评估报告解析失败: " + e.getMessage(), e);
            }
        });
    }

    /** 读真实对话质量聚合（Redis），无数据/解析失败 → null（前端不展示该区段）。 */
    private RagRealConversationDTO readRealConversation() {
        try {
            String current = redisService.get(EVAL_RECENT_KEY);
            if (current == null) {
                return null;
            }
            JsonNode agg = CAMEL_MAPPER.readTree(current);
            int count = agg.path("count").asInt(0);
            if (count == 0) {
                return null;
            }
            double quality = agg.path("sum_quality").asDouble(0.0) / count;
            double quotedRatio = (double) agg.path("quoted_count").asInt(0) / count;
            double latency = agg.path("sum_latency_ms").asLong(0) / (double) count;
            return RagRealConversationDTO.builder()
                    .count(count)
                    .avgQuality(Math.round(quality * 100) / 100.0)
                    .quotedRatio(Math.round(quotedRatio * 100) / 100.0)
                    .avgLatencyMs(Math.round(latency))
                    .build();
        } catch (Exception e) {
            log.warn("[rag-quality] 读真实对话统计失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 触发重评测（异步）：桥调 Python {@code POST /eval/run} 后台跑一轮，立即返回 running 状态。
     * 已有一轮在跑 → 幂等返回 {running:true, alreadyRunning:true}，非错误。
     */
    public Mono<RagEvalRunDTO> evalRun() {
        return ragAssistantPort.evalRun().map(json -> {
            try {
                return SNAKE_MAPPER.readValue(json, RagEvalRunDTO.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("重评测响应解析失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 开始引导（非流式）：桥调 Python {@code GET /guide}，返回模块引导底座池出题
     * （1~3 条，必含 ≥1 条 RAG 方向）。0 token、非 SSE、不占冻结时序。
     * currentProject 可选（缺省 Python 兜底默认模块）。
     */
    public Mono<RagGuideDTO> guide(String currentProject) {
        return ragAssistantPort.guide(currentProject).map(json -> {
            try {
                return SNAKE_MAPPER.readValue(json, RagGuideDTO.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("引导响应解析失败: " + e.getMessage(), e);
            }
        });
    }

    /** rerank 事件（camel）→ 捕获精排块（供忠实度打分喂片段摘要）。 */
    private void captureRerankBlocks(ServerSentEvent<String> ev, AtomicReference<List<SseRerankBlock>> ref) {
        if (!"rerank".equals(ev.event()) || ev.data() == null) {
            return;
        }
        try {
            SseRerankDTO dto = CAMEL_MAPPER.readValue(ev.data(), SseRerankDTO.class);
            if (dto.getBlocks() != null) {
                ref.set(dto.getBlocks());
            }
        } catch (JsonProcessingException e) {
            log.warn("[rag-quality] rerank 块解析失败: {}", e.getMessage());
        }
    }

    /** intent 事件（camel）→ 捕获 category（问候轮识别，M6 ④：category=问候 不触发质量打分）。 */
    private void captureIntentCategory(ServerSentEvent<String> ev, AtomicReference<String> ref) {
        if (!"intent".equals(ev.event()) || ev.data() == null) {
            return;
        }
        try {
            SseIntentDTO dto = CAMEL_MAPPER.readValue(ev.data(), SseIntentDTO.class);
            if (dto.getCategory() != null) {
                ref.set(dto.getCategory());
            }
        } catch (JsonProcessingException e) {
            log.warn("[rag-quality] intent category 解析失败: {}", e.getMessage());
        }
    }

    /** 把精排块格式化为 "【标题】摘要" 片段列表（优先命中引用的块，最多 5 条）。 */
    private List<String> formatBlockSummaries(List<SseRerankBlock> blocks, List<String> quotedKeys) {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }
        List<SseRerankBlock> quoted = blocks.stream()
                .filter(b -> quotedKeys != null && quotedKeys.contains(b.getBlockId()))
                .toList();
        List<SseRerankBlock> source = quoted.isEmpty() ? blocks : quoted;
        return source.stream().limit(5)
                .map(b -> "【" + (b.getTitle() == null ? "块" : b.getTitle()) + "】"
                        + (b.getSummary() == null ? "" : b.getSummary()))
                .collect(Collectors.toList());
    }

    /**
     * done 后异步触发真实对话 LLM 质量打分（不阻塞 SSE）。仅对非空答案且正常完成的轮次评分
     * （boundary=low_confidence / 澄清轮 answer 空或 reason 非 null 跳过）。
     */
    private void scheduleGradeOnDone(RagAskCommand command, ServerSentEvent<String> ev,
                                     List<SseRerankBlock> blocks, long startNanos, String category) {
        if (!"done".equals(ev.event()) || ev.data() == null) {
            return;
        }
        try {
            SseDoneDTO done = CAMEL_MAPPER.readValue(ev.data(), SseDoneDTO.class);
            if (done.getAnswer() == null || done.getAnswer().isBlank() || done.getReason() != null) {
                return; // 非生成轮不评分
            }
            if ("问候".equals(category)) {
                // 问候轮：固定欢迎语（0 token），非真实答案，不入评估聚合（M6 ④）
                log.debug("[rag-quality] 问候轮，跳过评分");
                return;
            }
            if (ragQualityGrader == null) {
                // 打分是尽力而为的旁路：无打分器（非 Spring 装配/单测）时不打断回答链路
                log.debug("[rag-quality] 无打分器注入，跳过本轮评分");
                return;
            }
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            List<String> quotedKeys = done.getQuotedKeys() == null ? List.of() : done.getQuotedKeys();
            List<String> summaries = formatBlockSummaries(blocks, quotedKeys);
            ragQualityGrader.grade(command.getQuestion(), done.getAnswer(), quotedKeys, summaries)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(score -> accumulateGrade(score, quotedKeys, latencyMs),
                            err -> log.warn("[rag-quality] 打分订阅异常: {}", err.getMessage()));
        } catch (JsonProcessingException e) {
            log.warn("[rag-quality] done 解析失败，跳过评分: {}", e.getMessage());
        }
    }

    /** 把一轮质量分累计进 Redis 聚合（rag:assistant:eval:recent，TTL 24h），读-改-写。 */
    private void accumulateGrade(RagQualityScore score, List<String> quotedKeys, long latencyMs) {
        try {
            String current = redisService.get(EVAL_RECENT_KEY);
            JsonNode agg = current == null ? CAMEL_MAPPER.createObjectNode() : CAMEL_MAPPER.readTree(current);
            int count = agg.path("count").asInt(0) + 1;
            double sumQuality = agg.path("sum_quality").asDouble(0.0) + score.getScore();
            int quotedCount = agg.path("quoted_count").asInt(0) + (quotedKeys.isEmpty() ? 0 : 1);
            long sumLatency = agg.path("sum_latency_ms").asLong(0) + latencyMs;
            var node = CAMEL_MAPPER.createObjectNode();
            node.put("count", count);
            node.put("sum_quality", sumQuality);
            node.put("quoted_count", quotedCount);
            node.put("sum_latency_ms", sumLatency);
            redisService.set(EVAL_RECENT_KEY, node.toString(), 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("[rag-quality] 累计失败: {}", e.getMessage());
        }
    }

    /** 会话是否已 closed（Redis closed 标志；Redis 异常 → 按未关闭处理，不阻断）。 */
    private boolean isSessionClosed(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        try {
            return "1".equals(redisService.get(SESSION_KEY_PREFIX + sessionId + SESSION_CLOSED_SUFFIX));
        } catch (Exception e) {
            log.warn("[rag-assistant] 读会话 closed 标志失败: {}", e.getMessage());
            return false;
        }
    }

    /** 会话已关闭 → permission + done(固定话术)，0 token、不调 Python、不落库不评分。 */
    private Flux<ServerSentEvent<String>> closedSessionStream(SsePermissionDTO permission, String traceId) {
        SseDoneDTO done = SseDoneDTO.builder()
                .answer(CLOSED_MSG).quotedKeys(List.of())
                .tokensUsage(SseTokensUsageDTO.builder()
                        .promptTokens(0).completionTokens(0).cacheHitTokens(0).totalTokens(0).build())
                .traceId(traceId).suggestions(List.of()).reason(null).build();
        return Flux.concat(
                Flux.just(sse("permission", writeCamel(permission))),
                Flux.just(sse("done", writeCamel(done))));
    }

    /**
     * M7 7.1/7.3：每轮 done 后落库——①会话累计 token（close 结算用）②trace 快照（断线补查用）。
     * 落库失败不阻断回答链路（尽力而为，只告警）。
     */
    private void persistRound(RagAskCommand command, ServerSentEvent<String> ev, String traceId) {
        if (!"done".equals(ev.event()) || ev.data() == null) {
            return;
        }
        try {
            SseDoneDTO done = CAMEL_MAPPER.readValue(ev.data(), SseDoneDTO.class);
            // 7.3 turns 补查：done 的 camel JSON 直接落 trace 键（TTL 24h）
            redisService.set(TRACE_KEY_PREFIX + traceId, ev.data(), SESSION_TTL_HOURS, TimeUnit.HOURS);
            // 7.1 会话累计 token + 轮数
            accumulateSessionUsage(command.getSessionId(), done.getTokensUsage());
        } catch (Exception e) {
            log.warn("[rag-assistant] 会话落库失败: {}", e.getMessage());
        }
    }

    /** 每轮 done 的 tokens_usage 累加进会话键（读-改-写，TTL 24h）。 */
    private void accumulateSessionUsage(String sessionId, SseTokensUsageDTO usage) {
        try {
            String key = SESSION_KEY_PREFIX + sessionId + SESSION_USAGE_SUFFIX;
            String current = redisService.get(key);
            JsonNode agg = current == null ? CAMEL_MAPPER.createObjectNode() : CAMEL_MAPPER.readTree(current);
            var node = CAMEL_MAPPER.createObjectNode();
            node.put("promptTokens", agg.path("promptTokens").asInt(0) + nvl(usage == null ? null : usage.getPromptTokens()));
            node.put("completionTokens", agg.path("completionTokens").asInt(0) + nvl(usage == null ? null : usage.getCompletionTokens()));
            node.put("cacheHitTokens", agg.path("cacheHitTokens").asInt(0) + nvl(usage == null ? null : usage.getCacheHitTokens()));
            node.put("totalTokens", agg.path("totalTokens").asInt(0) + nvl(usage == null ? null : usage.getTotalTokens()));
            node.put("rounds", agg.path("rounds").asInt(0) + 1);
            redisService.set(key, node.toString(), SESSION_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("[rag-assistant] 会话累计 token 失败: {}", e.getMessage());
        }
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
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
