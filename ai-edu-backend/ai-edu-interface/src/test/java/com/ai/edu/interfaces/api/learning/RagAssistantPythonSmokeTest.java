package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.learning.command.RagAskCommand;
import com.ai.edu.application.service.learning.RagAssistantAppService;
import com.ai.edu.infrastructure.ai.LlmGatewayProperties;
import com.ai.edu.infrastructure.ai.rag.RagAssistantBridgeImpl;
import com.ai.edu.infrastructure.ai.rag.RagWebClientConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * RAG 助手 真实端到端冒烟（Java 网关 → 真实 Python 引擎）。
 *
 * <p>非 mock：构造真实 ragWebClient + 桥 + 应用服务，调本地 Python {@code /api/rag/assistant/ask}，
 * 验证整条白盒链路（permission→intent→rewrite→rerank→token*→done）经 Java 重建为 camelCase。
 * <b>Python 服务不在线自动跳过</b>（Assumptions），不破坏常规测试套件。
 */
class RagAssistantPythonSmokeTest {

    private static final String BASE_URL = "http://127.0.0.1:9527";
    private static final String INTERNAL_TOKEN = "my-secret-token-123";

    @Test
    @DisplayName("真实链路：Java 网关 → Python 引擎，白盒事件全链路 camel 重建")
    void fullChain_smoke() {
        assumeTrue(isPortOpen("127.0.0.1", 9527), "Python 服务未启动，跳过真实链路冒烟");

        // 构造真实链：ragWebClient → 桥 → 应用服务
        LlmGatewayProperties props = new LlmGatewayProperties();
        props.setBaseUrl(BASE_URL);
        props.setInternalToken(INTERNAL_TOKEN);
        var ragWebClient = new RagWebClientConfig().ragWebClient(props);

        RagAssistantBridgeImpl bridge = new RagAssistantBridgeImpl();
        ReflectionTestUtils.setField(bridge, "ragWebClient", ragWebClient);
        ReflectionTestUtils.setField(bridge, "llmGatewayProperties", props);

        RagAssistantAppService appService = new RagAssistantAppService();
        ReflectionTestUtils.setField(appService, "ragAssistantPort", bridge);

        // AI答疑模块问题（有语料，走真实生成）
        RagAskCommand command = RagAskCommand.builder()
                .question("AI答疑的意图识别流程是怎样的")
                .sessionId("sess-smoke-" + System.currentTimeMillis())
                .currentProject("ai-tutoring")
                .topK(3)
                .stream(true)
                .build();

        AtomicReference<String> permissionTraceId = new AtomicReference<>();

        // U4 后 source 按 COS key 读普通桶（file_path 形如 rag-source/<模块>/<路径>.md）；用真实 key 验证中文目录/COS 路径
        var sourceMono = bridge.source("rag-source/ai-tutoring/语雀/语雀-AI答疑.md");
        try {
            String content = sourceMono.block();
            assertTrue(content != null && content.contains("AI答疑"), "source 应返回原文, got=" + (content == null ? "null" : content.substring(0, Math.min(50, content.length()))));
        } catch (Exception e) {
            throw new AssertionError("source 复现失败: " + e, e);
        }

        Flux<ServerSentEvent<String>> stream = appService.ask(command);
        StepVerifier.create(stream)
                // permission：camel + traceId
                .assertNext(ev -> {
                    assertTrue("permission".equals(ev.event()), ev.event());
                    assertTrue(ev.data().contains("\"allowed\":true"), ev.data());
                    assertTrue(ev.data().contains("\"traceId\""), ev.data());
                    assertTrue(ev.data().contains("traceId"), ev.data());
                    permissionTraceId.set(extractTraceId(ev.data()));
                    assertNotNull(permissionTraceId.get(), "permission 应携带 traceId");
                })
                // intent：camel，anchor=ai-tutoring（AI答疑语料）
                .assertNext(ev -> {
                    assertTrue("intent".equals(ev.event()), ev.event());
                    assertTrue(ev.data().contains("\"anchor\":\"ai-tutoring\""), ev.data());
                    assertTrue(ev.data().contains("\"switchDetected\""), ev.data());
                })
                // rewrite：camel originalQuestion
                .assertNext(ev -> {
                    assertTrue("rewrite".equals(ev.event()), ev.event());
                    assertTrue(ev.data().contains("\"originalQuestion\""), ev.data());
                    assertTrue(ev.data().contains("\"rewrittenQuery\""), ev.data());
                })
                // rerank：camel blocks + filePath
                .assertNext(ev -> {
                    assertTrue("rerank".equals(ev.event()), ev.event());
                    assertTrue(ev.data().contains("\"blocks\""), ev.data());
                    assertTrue(ev.data().contains("\"filePath\""), ev.data());
                })
                // token*（可 0..N）
                .thenConsumeWhile(ev -> "token".equals(ev.event()))
                // done：camel quotedKeys/tokensUsage/traceId（与 permission 一致）
                .assertNext(ev -> {
                    assertTrue("done".equals(ev.event()), ev.event());
                    assertTrue(ev.data().contains("\"quotedKeys\""), ev.data());
                    assertTrue(ev.data().contains("\"tokensUsage\""), ev.data());
                    assertTrue(ev.data().contains("\"traceId\":\"" + permissionTraceId.get() + "\""), ev.data());
                })
                .verifyComplete();
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
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
}
