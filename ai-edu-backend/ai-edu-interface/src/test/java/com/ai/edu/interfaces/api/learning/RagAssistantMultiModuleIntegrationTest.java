package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.learning.command.RagAskCommand;
import com.ai.edu.application.dto.learning.rag.RagGuideDTO;
import com.ai.edu.application.dto.learning.rag.SseDoneDTO;
import com.ai.edu.application.dto.learning.rag.SseIntentDTO;
import com.ai.edu.application.dto.learning.rag.SseRerankBlock;
import com.ai.edu.application.dto.learning.rag.SseRerankDTO;
import com.ai.edu.application.dto.learning.rag.SseTokensUsageDTO;
import com.ai.edu.application.service.learning.RagAssistantAppService;
import com.ai.edu.domain.learning.model.contract.RagHistoryItem;
import com.ai.edu.infrastructure.ai.LlmGatewayProperties;
import com.ai.edu.infrastructure.ai.rag.RagAssistantBridgeImpl;
import com.ai.edu.infrastructure.ai.rag.RagWebClientConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * RAG 助手<b>多模块联调</b>集成测试（Java 网关 → 真实 Python 引擎）。
 *
 * <p>前置：Python 多模块检索打通（2026-08-29，四模块闭集
 * {@code ai-tutoring/knowledge-graph/question-analysis/rag-system}，各模块独立语料池，
 * 自测 11 问隔离正确 0 泄漏）。本套验证 Java 网关对全模块的白盒链路转发正确：
 *
 * <ol>
 *   <li><b>四模块 SSE 白盒链</b>：permission→intent→rewrite→rerank→(token*|boundary)→done 经 Java
 *       重建为 camelCase，无 snake 泄漏；done traceId 与 permission 一致。</li>
 *   <li><b>模块隔离</b>：rerank 块 filePath 全来自请求模块语料池（0 跨模块泄漏）。</li>
 *   <li><b>追问展开</b>：带 history 的 follow-up 锚定原模块（方案A 前端传）。</li>
 *   <li><b>边界拒答</b>：模块语料外问题 → boundary + done(reason=low_confidence)，0 token。</li>
 *   <li><b>缺省 currentProject</b>：不传 → Python 兜底 rag-system（契约"缺省 rag-system"）。</li>
 *   <li><b>引导 / 原文代理</b>：guide 各模块池出题；source 跨模块 COS key 可读。</li>
 * </ol>
 *
 * 非 mock：构造真实 ragWebClient + 桥 + 应用服务，调本地 Python {@code /api/rag/assistant/ask}。
 * 事件断言用 {@code collectList().block()} 收全流再按序列断言（StepVerifier 的
 * recordWith/thenConsumeWhile(true) 组合在长 token 流上会丢尾段，改此更稳）。
 * <b>Python 服务不在线自动跳过</b>（Assumptions），不破坏常规测试套件。
 */
class RagAssistantMultiModuleIntegrationTest {

    private static final String BASE_URL = "http://127.0.0.1:9527";
    private static final String INTERNAL_TOKEN = "my-secret-token-123";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** 四模块闭集 + 各模块"应命中"代表问题（Python rag_test_multi 自测 3 命中 0 泄漏） */
    private static final String[][] HIT_CASES = {
            {"ai-tutoring", "怎么防止学生套答案"},
            {"knowledge-graph", "知识点匹配率怎么从17%提高到97.1%"},
            {"question-analysis", "题型怎么动态聚类的"},
            {"rag-system", "RAG问答系统是干什么的"},
    };

    private RagAssistantAppService appService;
    private RagAssistantBridgeImpl bridge;

    @BeforeEach
    void setUp() {
        assumeTrue(isPortOpen("127.0.0.1", 9527), "Python 服务未启动，跳过真实多模块联调");
        LlmGatewayProperties props = new LlmGatewayProperties();
        props.setBaseUrl(BASE_URL);
        props.setInternalToken(INTERNAL_TOKEN);
        var ragWebClient = new RagWebClientConfig().ragWebClient(props);
        bridge = new RagAssistantBridgeImpl();
        ReflectionTestUtils.setField(bridge, "ragWebClient", ragWebClient);
        ReflectionTestUtils.setField(bridge, "llmGatewayProperties", props);
        appService = new RagAssistantAppService();
        ReflectionTestUtils.setField(appService, "ragAssistantPort", bridge);
    }

    // ==================== 1. 四模块 SSE 白盒链 + 模块隔离 ====================

    @Test
    @DisplayName("模块 ai-tutoring：白盒链 camel 重建 + rerank 块全来自本模块 + done traceId 一致")
    void ask_aiTutoringChain() {
        ChainResult r = runHitChain(HIT_CASES[0][0], HIT_CASES[0][1], null);
        assertGenerated(r, "ai-tutoring");
    }

    @Test
    @DisplayName("模块 knowledge-graph：白盒链 camel 重建 + rerank 块全来自本模块 + done traceId 一致")
    void ask_knowledgeGraphChain() {
        ChainResult r = runHitChain(HIT_CASES[1][0], HIT_CASES[1][1], null);
        assertGenerated(r, "knowledge-graph");
    }

    @Test
    @DisplayName("模块 question-analysis：白盒链 camel 重建 + rerank 块全来自本模块 + done traceId 一致")
    void ask_questionAnalysisChain() {
        ChainResult r = runHitChain(HIT_CASES[2][0], HIT_CASES[2][1], null);
        assertGenerated(r, "question-analysis");
    }

    @Test
    @DisplayName("模块 rag-system：白盒链 camel 重建 + rerank 块全来自本模块 + done traceId 一致")
    void ask_ragSystemChain() {
        ChainResult r = runHitChain(HIT_CASES[3][0], HIT_CASES[3][1], null);
        assertGenerated(r, "rag-system");
    }

    // ==================== 2. 追问展开（history，方案A 前端传） ====================

    @Test
    @DisplayName("追问展开：省略主语的 follow-up 带 history → 锚定原模块不跳到其他模块")
    void ask_followUp_withHistory_staysInModule() {
        // 第一轮：ai-tutoring 流程图（确立会话锚点）
        ChainResult round1 = runHitChain("ai-tutoring", "流程图是什么样的", null);

        // 第二轮：省略主语追问 + history（最近 3 轮），应锚定 ai-tutoring 且召回命中
        RagAskCommand followUp = cmd("能说的详细一点吗", "ai-tutoring", List.of(
                RagHistoryItem.builder()
                        .question("流程图是什么样的")
                        .answer(round1.answer())
                        .anchor("ai-tutoring")
                        .build()));
        ChainResult round2 = runHitChain("ai-tutoring", followUp);
        assertGenerated(round2, "ai-tutoring");
        assertFalse(round2.filePaths().isEmpty(), "追问应命中原模块语料");
    }

    // ==================== 3. 边界拒答（语料外问题 → low_confidence） ====================

    @Test
    @DisplayName("边界拒答：模块语料外问题（辞职信）→ boundary + done(reason=low_confidence)，0 token 固定话术")
    void ask_boundary_lowConfidence() {
        List<ServerSentEvent<String>> evs = collect(appService.ask(cmd("帮我写一封辞职信。", "ai-tutoring", null)));

        // 事件序：permission → intent → rewrite → rerank → boundary → done
        assertEvent(evs.get(0), "permission");
        String traceId = extractTraceId(evs.get(0).data());
        assertNotNull(traceId);

        assertEvent(evs.get(1), "intent");
        SseIntentDTO intent = parse(evs.get(1).data(), SseIntentDTO.class);
        assertEquals("ai-tutoring", intent.getAnchor(), "语料外问题应锚定 current_project（权威消歧）");

        assertEvent(evs.get(2), "rewrite");
        assertEvent(evs.get(3), "rerank");

        assertEvent(evs.get(4), "boundary", "无命中应先发 boundary");
        assertTrue(evs.get(4).data().contains("\"reason\":\"low_confidence\""), evs.get(4).data());

        assertEvent(evs.get(5), "done", "boundary 后立即 done");
        SseDoneDTO done = parse(evs.get(5).data(), SseDoneDTO.class);
        assertEquals("low_confidence", done.getReason(), "边界拒答 reason=low_confidence");
        assertEquals("未找到关联文档，我尚未掌握。", done.getAnswer(), "拒答为固定话术（非空）");
        assertEquals(traceId, done.getTraceId(), "done traceId 应与 permission 一致");
        assertNotNull(done.getTokensUsage(), "done 应含 tokensUsage");
        assertEquals(0, done.getTokensUsage().getTotalTokens(), "拒答 0 token（不调 generate）");
        assertEquals(6, evs.size(), "边界轮应恰好 6 个事件，无多余 token");
    }

    // ==================== 4. 缺省 current_project 兜底（Python 缺省 rag-system） ====================

    @Test
    @DisplayName("缺省 currentProject：前端不传 → Python 兜底 rag-system，链路不应被 null 阻断")
    void ask_defaultCurrentProject_fallsBack() {
        // 契约"缺省 rag-system"：前端可不传 currentProject。若 Java 序列化 current_project=null
        // 而 Pydantic 拒 null → 422 → 桥 TutoringAgentException（见 RagWebClientConfig NON_NULL 修复）。
        RagAskCommand command = RagAskCommand.builder()
                .question("RAG问答系统是干什么的")
                .sessionId("sess-default-" + System.currentTimeMillis())
                .stream(true)
                .topK(3)
                .build(); // currentProject = null

        List<ServerSentEvent<String>> evs = collect(appService.ask(command));
        assertEvent(evs.get(0), "permission", "首事件应 permission，不应被 422 打断");
        assertEvent(evs.get(1), "intent");
        SseIntentDTO intent = parse(evs.get(1).data(), SseIntentDTO.class);
        assertEquals("rag-system", intent.getAnchor(), "缺省 currentProject 应兜底 rag-system");
        assertEvent(evs.get(2), "rewrite");
        assertEvent(evs.get(3), "rerank");
        assertEvent(evs.get(evs.size() - 1), "done", "链路应正常走完到 done");
    }

    // ==================== 5. 引导（guide 各模块池） ====================

    // ==================== RAG-A-17 非流式 askSync（真实 Python） ====================

    @Test
    @DisplayName("RAG-A-17 非流式：askStages 走真实 Python，返回 camel done+stages（rerank 块本模块）")
    void askSync_nonStreamingReal() {
        java.util.Map<String, Object> result = appService.askStages(cmd("RAG问答系统是干什么的", "rag-system", null));
        assertNotNull(result, "非流式应返回 done+stages");
        assertTrue(result.containsKey("answer"), "应含 answer");
        assertNotNull(result.get("answer"));
        assertFalse(((String) result.get("answer")).isBlank(), "answer 不应为空");
        assertTrue(result.containsKey("quotedKeys"), "应含 quotedKeys");
        assertTrue(result.containsKey("tokensUsage"), "应含 tokensUsage");
        assertTrue(result.containsKey("traceId"), "应含 traceId");
        assertNull(result.get("reason"), "命中轮 reason 应为 null");
        assertTrue(result.containsKey("stages"), "应含 stages 摘要");
        java.util.Map<?, ?> stages = (java.util.Map<?, ?>) result.get("stages");
        assertTrue(stages.containsKey("intent"), "stages 应含 intent");
        assertTrue(stages.containsKey("rewrite"), "stages 应含 rewrite");
        assertTrue(stages.containsKey("rerank"), "stages 应含 rerank");
        java.util.List<?> rerank = (java.util.List<?>) stages.get("rerank");
        assertFalse(rerank.isEmpty(), "非流式 rerank 块不应为空");
        java.util.Map<?, ?> block = (java.util.Map<?, ?>) rerank.get(0);
        assertTrue(block.containsKey("blockId"), "rerank 块应 camel 化(blockId)");
        assertTrue(block.containsKey("filePath"), "rerank 块应 camel 化(filePath)");
        assertTrue(((String) block.get("filePath")).contains("rag-system"),
                "非流式 rerank 块应来自请求模块, got=" + block.get("filePath"));
    }

    @Test
    @DisplayName("guide：4 个 currentProject 各返回模块引导池（1~3 条，非空）")
    void guide_perModulePool() {
        for (String[] hit : HIT_CASES) {
            RagGuideDTO guide = appService.guide(hit[0]).block();
            assertNotNull(guide, "guide 响应不应为 null: " + hit[0]);
            assertTrue(guide.getSuggestions() != null && !guide.getSuggestions().isEmpty(),
                    "模块 " + hit[0] + " 引导池应出题");
            assertTrue(guide.getSuggestions().size() <= 3, "引导最多 3 条");
            for (var s : guide.getSuggestions()) {
                assertTrue(s.getTitle() != null && !s.getTitle().isBlank(), "引导题 title 非空");
                assertTrue(s.getDirection() != null && !s.getDirection().isBlank(), "引导题 direction 非空");
            }
        }
    }

    @Test
    @DisplayName("guide：currentProject 缺省 → Python 兜底默认模块（ai-tutoring 入口池）")
    void guide_defaultProject_fallsBack() {
        RagGuideDTO guide = appService.guide(null).block();
        assertNotNull(guide);
        assertTrue(guide.getSuggestions() != null && !guide.getSuggestions().isEmpty(),
                "缺省 currentProject 引导应兜底默认模块出题");
    }

    // ==================== 6. 原文代理（source 跨模块 COS key） ====================

    @Test
    @DisplayName("source：rag-system 命中块 COS key → 原文代理读回非空内容（跨模块原文可看）")
    void source_crossModuleCosKey() {
        // 自包含：先跑一轮 rag-system 命中链取真实 rerank 块 filePath（COS key），再读原文
        ChainResult r = runHitChain("rag-system", "RAG问答系统是干什么的", null);
        assertFalse(r.filePaths().isEmpty(), "应取到 rag-system 命中块 COS key");

        String key = r.filePaths().get(0);
        assertTrue(key.startsWith("rag-source/") || key.startsWith("rag-slices/"),
                "COS key 应为 rag-source|rag-slices 前缀，got=" + key);

        String content = bridge.source(key).block();
        assertNotNull(content, "原文内容不应为 null: " + key);
        assertFalse(content.isBlank(), "原文不应为空: " + key);
    }

    // ==================== helpers ====================

    /** 一轮"应命中"模块链结果：traceId + rerank 块 filePath（COS key）+ done 答案/reason。 */
    private record ChainResult(String traceId, List<String> filePaths, String answer, String reason) {
    }

    private RagAskCommand cmd(String question, String module, List<RagHistoryItem> history) {
        return RagAskCommand.builder()
                .question(question)
                .sessionId("sess-" + module + "-" + System.currentTimeMillis())
                .currentProject(module)
                .topK(3)
                .stream(true)
                .history(history)
                .build();
    }

    /** 收全 ask 流事件（block 到流结束；异常 → 直接抛，暴露联调问题）。 */
    private List<ServerSentEvent<String>> collect(reactor.core.publisher.Flux<ServerSentEvent<String>> flux) {
        List<ServerSentEvent<String>> evs = flux.collectList().block();
        assertNotNull(evs, "ask 流应完整返回事件列表");
        return evs;
    }

    /**
     * 执行一轮应命中模块链并断言 Java 网关契约：
     * 事件序 permission→intent→rewrite→rerank→(token*|boundary)→done；camel 重建无 snake 泄漏；
     * intent anchor=请求模块；rerank 非空且块 filePath 全来自本模块（0 泄漏）；done traceId 与 permission 一致。
     */
    private ChainResult runHitChain(String module, String question, List<RagHistoryItem> history) {
        return runHitChain(module, cmd(question, module, history));
    }

    private ChainResult runHitChain(String module, RagAskCommand command) {
        List<ServerSentEvent<String>> evs = collect(appService.ask(command));

        // 1. permission
        assertEvent(evs.get(0), "permission");
        assertTrue(evs.get(0).data().contains("\"allowed\":true"), evs.get(0).data());
        String traceId = extractTraceId(evs.get(0).data());
        assertNotNull(traceId, "permission 应携带 traceId");

        // 2. intent：anchor=请求模块 + camel（无 snake）
        assertEvent(evs.get(1), "intent");
        SseIntentDTO intent = parse(evs.get(1).data(), SseIntentDTO.class);
        assertEquals(module, intent.getAnchor(), "intent anchor 应为请求模块");
        assertFalse(evs.get(1).data().contains("switch_detected"), "intent 应 camel 重建, got=" + evs.get(1).data());
        assertFalse(evs.get(1).data().contains("locked_sections"), "intent 应 camel 重建, got=" + evs.get(1).data());

        // 3. rewrite：camel
        assertEvent(evs.get(2), "rewrite");
        assertTrue(evs.get(2).data().contains("\"originalQuestion\""), evs.get(2).data());
        assertTrue(evs.get(2).data().contains("\"rewrittenQuery\""), evs.get(2).data());
        assertFalse(evs.get(2).data().contains("original_question"), "rewrite 应 camel 重建, got=" + evs.get(2).data());

        // 4. rerank：非空 + 模块隔离（块 filePath 全来自请求模块，0 泄漏）+ camel
        assertEvent(evs.get(3), "rerank");
        SseRerankDTO rerank = parse(evs.get(3).data(), SseRerankDTO.class);
        assertNotNull(rerank.getBlocks(), "rerank blocks 不应为 null");
        assertFalse(rerank.getBlocks().isEmpty(), "模块 " + module + " 命中为空: " + evs.get(3).data());
        assertFalse(evs.get(3).data().contains("block_id"), "rerank 应 camel 重建, got=" + evs.get(3).data());
        assertFalse(evs.get(3).data().contains("file_path"), "rerank 应 camel 重建, got=" + evs.get(3).data());
        List<String> filePaths = rerank.getBlocks().stream().map(SseRerankBlock::getFilePath).toList();
        for (String fp : filePaths) {
            assertTrue(fp != null && fp.contains(module),
                    "模块隔离泄漏: filePath=" + fp + " 不属于模块 " + module);
        }

        // 5. 尾段：(token*|boundary) → done；traceId 一致；done 结构齐全
        StringBuilder answer = new StringBuilder();
        String reason = null;
        String finalTraceId = null;
        SseTokensUsageDTO usage = null;
        for (int i = 4; i < evs.size(); i++) {
            ServerSentEvent<String> ev = evs.get(i);
            switch (ev.event()) {
                case "token" -> answer.append(parse(ev.data(), com.ai.edu.application.dto.learning.rag.SseTokenDTO.class).getText());
                case "boundary" -> { /* 低置信标记，继续到 done */ }
                case "done" -> {
                    SseDoneDTO done = parse(ev.data(), SseDoneDTO.class);
                    finalTraceId = done.getTraceId();
                    reason = done.getReason();
                    usage = done.getTokensUsage();
                    if (done.getAnswer() != null) {
                        answer.setLength(0);
                        answer.append(done.getAnswer());
                    }
                }
                default -> fail("未知事件类型: " + ev.event() + " | " + ev.data());
            }
        }
        assertNotNull(finalTraceId, "应收到 done 事件");
        assertEquals(traceId, finalTraceId, "done traceId 应与 permission 一致");
        assertNotNull(usage, "done 应含 tokensUsage");

        return new ChainResult(traceId, filePaths, answer.toString(), reason);
    }

    /** 命中轮应真实生成（非 boundary 拒答）：answer 非空、reason 为 null。 */
    private void assertGenerated(ChainResult r, String module) {
        assertTrue(r.answer() != null && !r.answer().isBlank(),
                "模块 " + module + " 命中轮 answer 不应为空");
        assertNull(r.reason(), "命中轮 reason 应为 null");
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void assertEvent(ServerSentEvent<String> ev, String event) {
        assertEvent(ev, event, event);
    }

    private void assertEvent(ServerSentEvent<String> ev, String event, String msg) {
        assertTrue(ev != null, "事件不应为 null");
        assertEquals(event, ev.event(), msg);
    }

    private String extractTraceId(String data) {
        int idx = data.indexOf("\"traceId\":\"");
        if (idx < 0) {
            return null;
        }
        int start = idx + "\"traceId\":\"".length();
        int end = data.indexOf('"', start);
        return end < 0 ? null : data.substring(start, end);
    }

    private <T> T parse(String data, Class<T> type) {
        try {
            return MAPPER.readValue(data, type);
        } catch (JsonProcessingException e) {
            throw new AssertionError("解析事件失败: " + data, e);
        }
    }
}
