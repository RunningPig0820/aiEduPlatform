package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.application.dto.learning.QuestionAnalysisKpDTO;
import com.ai.edu.application.service.batch.KpQuestionTypeAggregationService;
import com.ai.edu.application.service.learning.KpQuestionAnalysisAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * KpResolutionController#analyze-question 单元测试（mock AppService + MockHttpSession，test.md ANQ-006/007）。
 */
class KpQuestionAnalysisControllerTest {

    private static final Long STUDENT_ID = 501L;

    private KpResolutionController controller;
    private KpQuestionAnalysisAppService appService;
    private KpQuestionTypeAggregationService aggregationService;

    @BeforeEach
    void setUp() {
        appService = mock(KpQuestionAnalysisAppService.class);
        aggregationService = mock(KpQuestionTypeAggregationService.class);
        controller = new KpResolutionController();
        setField(controller, "kpQuestionAnalysisAppService", appService);
        setField(controller, "kpQuestionTypeAggregationService", aggregationService);
    }

    @Test
    @DisplayName("analyze-question：学生登录 + 文本 → 返回单题分析")
    void analyze_success() {
        QuestionAnalysisDTO dto = QuestionAnalysisDTO.resolved("鸡兔同笼", 80,
                List.of(QuestionAnalysisKpDTO.builder().kpUri("uri").kpLabel("鸡兔同笼").ratio(1.0).build()));
        when(appService.analyze("笼子里有鸡和兔", STUDENT_ID)).thenReturn(dto);

        ApiResponse<QuestionAnalysisDTO> response =
                controller.analyzeQuestion(req("笼子里有鸡和兔"), loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals("鸡兔同笼", response.getData().getTopicLabel());
        verify(appService).analyze("笼子里有鸡和兔", STUDENT_ID);
    }

    @Test
    @DisplayName("aggregation/run：触发题型库聚合（ADMIN，@PreAuthorize 由 HTTP 层拦截，单测直调绕过）")
    void aggregationRun_triggers() {
        ApiResponse<Void> response = controller.runAggregation();

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        verify(aggregationService).aggregate();
    }

    @Test
    @DisplayName("analyze-question：text 为空 → 10001 参数错误")
    void analyze_emptyText_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.analyzeQuestion(req("  "), loginSession()));
        assertEquals(ErrorCode.INVALID_PARAMS, ex.getCode());
        verify(appService, never()).analyze(anyString(), any());
    }

    @Test
    @DisplayName("analyze-question：未登录 → 10004")
    void analyze_notLoggedIn() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.analyzeQuestion(req("笼子里有鸡和兔"), new MockHttpSession()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        verify(appService, never()).analyze(anyString(), any());
    }

    @Test
    @DisplayName("analyze-question：非学生角色 → 20004 越权")
    void analyze_nonStudent_denied() {
        MockHttpSession teacher = new MockHttpSession();
        teacher.setAttribute("userId", 888L);
        teacher.setAttribute("role", "TEACHER");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.analyzeQuestion(req("笼子里有鸡和兔"), teacher));
        assertEquals(ErrorCode.PERMISSION_DENIED, ex.getCode());
        verify(appService, never()).analyze(anyString(), any());
    }

    private KpResolutionController.AnalyzeQuestionRequest req(String text) {
        KpResolutionController.AnalyzeQuestionRequest request =
                new KpResolutionController.AnalyzeQuestionRequest();
        request.setText(text);
        return request;
    }

    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", STUDENT_ID);
        session.setAttribute("role", "STUDENT");
        return session;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
