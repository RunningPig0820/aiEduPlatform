package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.command.RagAskCommand;
import com.ai.edu.application.service.learning.RagAssistantAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RagAssistantController 单元测试（mock AppService + MockHttpSession，不启动 Spring 上下文）。
 *
 * <p>覆盖角色硬门 RAG-GATE-001~004：学生放行（SSE permission{allowed:true} → 桥事件流）/
 * 非学生 403（ResponseStatusException，GlobalExceptionHandler 转 HTTP 403）/ 角色缺失 403 /
 * body 传 role 被忽略（角色只认 session）。
 */
class RagAssistantControllerTest {

    private static final Long STUDENT_ID = 1001L;
    private static final Long TEACHER_ID = 2001L;

    private RagAssistantController controller;
    private RagAssistantAppService appService;

    @BeforeEach
    void setUp() {
        appService = mock(RagAssistantAppService.class);
        controller = new RagAssistantController();
        setField(controller, "ragAssistantAppService", appService);
    }

    // ==================== RAG-GATE-001 学生放行 ====================

    @Test
    @DisplayName("RAG-GATE-001：学生放行 → Flux(permission{allowed:true} → 桥事件流)")
    void gate_studentPasses() {
        Flux<ServerSentEvent<String>> stubFlow = Flux.just(
                sse("permission", "{\"role\":\"STUDENT\",\"allowed\":true,\"traceId\":\"trc-abc\"}"),
                sse("done", "{\"answer\":\"（桩替）...\"}"));
        when(appService.ask(any())).thenReturn(stubFlow);

        Flux<ServerSentEvent<String>> result = controller.ask(
                RagAskCommand.builder().question("这个项目的整体架构是什么？")
                        .sessionId("sess-001").currentProject("ai-tutoring").stream(true).build(),
                loginSession(STUDENT_ID, "STUDENT"));

        StepVerifier.create(result)
                .assertNext(ev -> {
                    assertEquals("permission", ev.event());
                    assertEquals(true, ev.data().contains("\"allowed\":true"), ev.data());
                })
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        verify(appService).ask(any());
    }

    // ==================== RAG-GATE-002 非学生拒绝 ====================

    @Test
    @DisplayName("RAG-GATE-002：非学生（TEACHER）→ 403，不调 appService")
    void gate_teacherRejected() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.ask(
                RagAskCommand.builder().question("这个项目的整体架构是什么？").sessionId("sess-001").stream(true).build(),
                loginSession(TEACHER_ID, "TEACHER")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("仅学生可访问此助手"), String.valueOf(ex.getReason()));
        verify(appService, never()).ask(any());
        verify(appService, never()).askStages(any());
    }

    // ==================== RAG-GATE-003 角色缺失 ====================

    @Test
    @DisplayName("RAG-GATE-003：角色缺失（无 session/无 role）→ 403，不进 RAG 流程")
    void gate_missingRole() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.ask(
                RagAskCommand.builder().question("这个项目的整体架构是什么？").sessionId("sess-001").stream(true).build(),
                new MockHttpSession()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(appService, never()).ask(any());
        verify(appService, never()).askStages(any());
    }

    // ==================== RAG-GATE-004 body 传 role 被忽略 ====================

    @Test
    @DisplayName("RAG-GATE-004：TEACHER session + body 带 role=STUDENT → 仍 403（角色只认 session）")
    void gate_bodyRoleIgnored() {
        // 命令无 role 字段（前端传 role 由 FAIL_ON_UNKNOWN_PROPERTIES=false 忽略）；
        // 控制器只读 session role=TEACHER → 仍 403，证明不信任前端传参
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.ask(
                RagAskCommand.builder().question("这个项目的整体架构是什么？").sessionId("sess-001")
                        .currentProject("ai-tutoring").stream(true).build(),
                loginSession(TEACHER_ID, "TEACHER")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(appService, never()).ask(any());
    }

    // ==================== askSync 非流式 ====================

    @Test
    @DisplayName("askSync：学生 → ApiResponse(done 摘要)；非学生 → 403")
    void askSync_studentOk_teacher403() {
        when(appService.askStages(any())).thenReturn(Map.of("answer", "（桩替）...", "stages", java.util.List.of()));

        ApiResponse<Map<String, Object>> ok = controller.askSync(
                RagAskCommand.builder().question("这个项目的整体架构是什么？").sessionId("sess-001").stream(false).build(),
                loginSession(STUDENT_ID, "STUDENT"));
        assertTrue(ok.getData().containsKey("answer"));

        assertThrows(ResponseStatusException.class, () -> controller.askSync(
                RagAskCommand.builder().question("这个项目的整体架构是什么？").sessionId("sess-001").build(),
                loginSession(TEACHER_ID, "TEACHER")));
    }

    // ==================== source 查看原文 ====================

    @Test
    @DisplayName("source：学生 → 代理返回原文；非学生 → 403 且不调 appService")
    void source_studentOk_teacher403() {
        when(appService.source(any())).thenReturn(reactor.core.publisher.Mono.just("# 原文"));
        clearInvocations(appService); // 清掉桩调用计数，verify 只看真实调用
        assertTrue(controller.source("4.完善文档/02-…md", loginSession(STUDENT_ID, "STUDENT"))
                .block().getData().contains("原文"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.source("4.完善文档/02-…md", loginSession(TEACHER_ID, "TEACHER")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(appService, times(1)).source(any()); // 仅学生路径 1 次，教师路径 0 次
    }

    // ==================== eval/report 评估报告 ====================

    @Test
    @DisplayName("evalReport：学生 → ApiResponse(报告)；非学生 → 403 且不调 appService")
    void evalReport_studentOk_teacher403() {
        com.ai.edu.application.dto.learning.rag.RagEvalReportDTO report =
                com.ai.edu.application.dto.learning.rag.RagEvalReportDTO.builder()
                        .version("v1").count(15).hitAt3(0.8).build();
        when(appService.evalReport()).thenReturn(reactor.core.publisher.Mono.just(report));
        clearInvocations(appService);

        assertTrue(controller.evalReport(loginSession(STUDENT_ID, "STUDENT"))
                .block().getData().getHitAt3() == 0.8);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.evalReport(loginSession(TEACHER_ID, "TEACHER")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(appService, times(1)).evalReport();
    }

    // ==================== eval/run 触发重评测 ====================

    @Test
    @DisplayName("evalRun：学生 → ApiResponse(running)；非学生 → 403 且不调 appService")
    void evalRun_studentOk_teacher403() {
        when(appService.evalRun()).thenReturn(reactor.core.publisher.Mono.just(
                com.ai.edu.application.dto.learning.rag.RagEvalRunDTO.builder().running(true).build()));
        clearInvocations(appService);

        assertTrue(controller.evalRun(loginSession(STUDENT_ID, "STUDENT"))
                .block().getData().getRunning());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.evalRun(loginSession(TEACHER_ID, "TEACHER")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(appService, times(1)).evalRun();
    }

    // ==================== helpers ====================

    private MockHttpSession loginSession(Long userId, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        session.setAttribute("role", role);
        return session;
    }

    private ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.<String>builder().event(event).data(data).build();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = RagAssistantController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
