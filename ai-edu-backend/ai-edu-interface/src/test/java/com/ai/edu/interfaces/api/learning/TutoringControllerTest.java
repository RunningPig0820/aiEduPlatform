package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.TutoringConfigDTO;
import com.ai.edu.application.dto.learning.TutoringSessionDTO;
import com.ai.edu.application.dto.learning.TutoringSessionListItemDTO;
import com.ai.edu.application.dto.learning.command.SendMessageCommand;
import com.ai.edu.application.dto.learning.command.StartTutoringCommand;
import com.ai.edu.application.service.learning.TutoringAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.learning.model.contract.OcrResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TutoringController 单元测试（mock AppService + MockHttpSession，不启动 Spring 上下文）。
 *
 * <p>覆盖：SSE 端点的认证/越权/透传、同步端点的 ApiResponse 包装与业务异常。
 */
class TutoringControllerTest {

    private static final Long STUDENT_ID = 501L;
    private static final Long SESSION_ID = 1001L;

    private TutoringController controller;
    private TutoringAppService appService;

    @BeforeEach
    void setUp() {
        appService = mock(TutoringAppService.class);
        controller = new TutoringController();
        setField(controller, "tutoringAppService", appService);
    }

    // ==================== start（SSE） ====================

    @Test
    @DisplayName("start：登录 STUDENT → 委托服务并透传 SSE 流")
    void start_delegates() {
        when(appService.start(eq(STUDENT_ID), eq("鸡兔同笼")))
                .thenReturn(Flux.just(sse("meta", "{}"), sse("token", "{\"content\":\"先找已知条件\"}"), sse("done", "{}")));

        Flux<ServerSentEvent<String>> result = controller.start(
                StartTutoringCommand.builder().message("鸡兔同笼").build(), loginSession());

        StepVerifier.create(result)
                .expectNextMatches(ev -> "meta".equals(ev.event()))
                .expectNextMatches(ev -> "token".equals(ev.event()))
                .expectNextMatches(ev -> "done".equals(ev.event()))
                .verifyComplete();
        verify(appService).start(STUDENT_ID, "鸡兔同笼");
    }

    @Test
    @DisplayName("start：未登录 → SSE error 事件(10004)，不开流")
    void start_notLoggedIn() {
        Flux<ServerSentEvent<String>> result = controller.start(
                StartTutoringCommand.builder().message("鸡兔同笼").build(), new MockHttpSession());

        StepVerifier.create(result)
                .assertNext(ev -> {
                    assertEquals("error", ev.event());
                    assertTrue(ev.data().contains(ErrorCode.UNAUTHORIZED), ev.data());
                })
                .verifyComplete();
        verify(appService, never()).start(anyLong(), anyString());
    }

    @Test
    @DisplayName("start：非 STUDENT 角色 → SSE error 事件(20004)")
    void start_notStudentRole() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 999L);
        session.setAttribute("role", "TEACHER");

        Flux<ServerSentEvent<String>> result = controller.start(
                StartTutoringCommand.builder().message("鸡兔同笼").build(), session);

        StepVerifier.create(result)
                .assertNext(ev -> {
                    assertEquals("error", ev.event());
                    assertTrue(ev.data().contains(ErrorCode.PERMISSION_DENIED), ev.data());
                })
                .verifyComplete();
        verify(appService, never()).start(anyLong(), anyString());
    }

    // ==================== sendMessage（SSE） ====================

    @Test
    @DisplayName("sendMessage：路径 sessionId + body content 委托服务")
    void sendMessage_delegates() {
        when(appService.sendMessage(eq(STUDENT_ID), eq(SESSION_ID), eq("2x+4(35-x)=94")))
                .thenReturn(Flux.just(sse("meta", "{}"), sse("done", "{}")));
        SendMessageCommand cmd = SendMessageCommand.builder().content("2x+4(35-x)=94").build();

        Flux<ServerSentEvent<String>> result = controller.sendMessage(SESSION_ID, cmd, loginSession());

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
        verify(appService).sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94");
    }

    @Test
    @DisplayName("sendMessage：空 content → SSE error 事件(10001)")
    void sendMessage_blankContent() {
        Flux<ServerSentEvent<String>> result = controller.sendMessage(
                SESSION_ID, SendMessageCommand.builder().content("  ").build(), loginSession());

        StepVerifier.create(result)
                .assertNext(ev -> {
                    assertEquals("error", ev.event());
                    assertTrue(ev.data().contains(ErrorCode.INVALID_PARAMS), ev.data());
                })
                .verifyComplete();
        verify(appService, never()).sendMessage(anyLong(), anyLong(), anyString());
    }

    // ==================== requestAnswer（SSE） ====================

    @Test
    @DisplayName("requestAnswer：委托服务")
    void requestAnswer_delegates() {
        when(appService.requestAnswer(eq(STUDENT_ID), eq(SESSION_ID))).thenReturn(Flux.just(sse("meta", "{}")));

        Flux<ServerSentEvent<String>> result = controller.requestAnswer(SESSION_ID, loginSession());

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
        verify(appService).requestAnswer(STUDENT_ID, SESSION_ID);
    }

    // ==================== getConfig（同步） ====================

    @Test
    @DisplayName("getConfig：返回 ocrEnabled 开关")
    void getConfig_returnsOcrEnabled() {
        when(appService.getTutoringConfig())
                .thenReturn(TutoringConfigDTO.builder().ocrEnabled(true).build());

        ApiResponse<TutoringConfigDTO> response = controller.getConfig(loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals(true, response.getData().getOcrEnabled());
        verify(appService).getTutoringConfig();
    }

    @Test
    @DisplayName("getConfig：未登录 → 抛 10004")
    void getConfig_notLoggedIn() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.getConfig(new MockHttpSession()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    // ==================== getSession（同步） ====================

    @Test
    @DisplayName("getSession：登录 → ApiResponse 包装")
    void getSession_success() {
        when(appService.getSession(eq(STUDENT_ID), eq(SESSION_ID)))
                .thenReturn(TutoringSessionDTO.builder().sessionId(SESSION_ID).build());

        ApiResponse<TutoringSessionDTO> response = controller.getSession(SESSION_ID, loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals(SESSION_ID, response.getData().getSessionId());
        verify(appService).getSession(STUDENT_ID, SESSION_ID);
    }

    @Test
    @DisplayName("getSession：未登录 → 抛 10004")
    void getSession_notLoggedIn() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.getSession(SESSION_ID, new MockHttpSession()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    // ==================== listSessions / deleteSession（同步） ====================

    @Test
    @DisplayName("listSessions：登录 STUDENT → 委托服务并包装列表")
    void listSessions_delegates() {
        when(appService.listSessions(eq(STUDENT_ID))).thenReturn(List.of(
                TutoringSessionListItemDTO.builder().sessionId(SESSION_ID).title("鸡兔同笼怎么做").status("ACTIVE").build()));

        ApiResponse<List<TutoringSessionListItemDTO>> response = controller.listSessions(loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals(1, response.getData().size());
        assertEquals(SESSION_ID, response.getData().get(0).getSessionId());
        verify(appService).listSessions(STUDENT_ID);
    }

    @Test
    @DisplayName("listSessions：未登录 → 抛 10004")
    void listSessions_notLoggedIn() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.listSessions(new MockHttpSession()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        verify(appService, never()).listSessions(any());
    }

    @Test
    @DisplayName("listSessions：非 STUDENT → 抛 20004")
    void listSessions_notStudentRole() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 999L);
        session.setAttribute("role", "TEACHER");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.listSessions(session));
        assertEquals(ErrorCode.PERMISSION_DENIED, ex.getCode());
    }

    @Test
    @DisplayName("deleteSession：登录 → 委托服务，返回空 data")
    void deleteSession_delegates() {
        ApiResponse<Void> response = controller.deleteSession(SESSION_ID, loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertNull(response.getData());
        verify(appService).deleteSession(STUDENT_ID, SESSION_ID);
    }

    @Test
    @DisplayName("deleteSession：未登录 → 抛 10004")
    void deleteSession_notLoggedIn() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.deleteSession(SESSION_ID, new MockHttpSession()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        verify(appService, never()).deleteSession(anyLong(), anyLong());
    }

    // ==================== archive（同步） ====================

    @Test
    @DisplayName("archive：委托服务并包装")
    void archive_delegates() {
        when(appService.archive(eq(STUDENT_ID), eq(SESSION_ID)))
                .thenReturn(TutoringSessionDTO.builder().sessionId(SESSION_ID).build());

        ApiResponse<TutoringSessionDTO> response = controller.archive(SESSION_ID, loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        verify(appService).archive(STUDENT_ID, SESSION_ID);
    }

    // ==================== ocr（同步，multipart） ====================

    @Test
    @DisplayName("ocr：委托服务并包装识别结果")
    void ocr_delegates() throws Exception {
        when(appService.ocr(any(), eq("q.png"))).thenReturn(
                OcrResult.builder().text("鸡兔同笼，共35头94脚").confidence(0.92).build());
        MockMultipartFile file = new MockMultipartFile("file", "q.png", "image/png", new byte[]{1, 2, 3});

        ApiResponse<OcrResult> response = controller.ocr(file, loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals("鸡兔同笼，共35头94脚", response.getData().getText());
        verify(appService).ocr(any(), eq("q.png"));
    }

    @Test
    @DisplayName("ocr：未登录 → 抛 10004")
    void ocr_notLoggedIn() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "q.png", "image/png", new byte[]{1});

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.ocr(file, new MockHttpSession()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    // ==================== helpers ====================

    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", STUDENT_ID);
        session.setAttribute("role", "STUDENT");
        return session;
    }

    private ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.<String>builder().event(event).data(data).build();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = TutoringController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
