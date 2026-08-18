package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.application.dto.learning.QuestionAnalysisKpDTO;
import com.ai.edu.application.service.learning.KpQuestionAnalysisAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * KpResolutionController#analyze-question 单元测试（mock AppService + MockHttpSession，test.md ANQ-006/007）。
 */
class KpQuestionAnalysisControllerTest {

    private static final Long STUDENT_ID = 501L;

    private KpResolutionController controller;
    private KpQuestionAnalysisAppService appService;

    @BeforeEach
    void setUp() {
        appService = mock(KpQuestionAnalysisAppService.class);
        controller = new KpResolutionController();
        setField(controller, "kpQuestionAnalysisAppService", appService);
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
    @DisplayName("aggregation/run：已停用（域 B 独立化）——返回提示，不再执行 obs 共现聚合")
    void aggregationRun_stopped() {
        ApiResponse<String> response = controller.runAggregation();

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertTrue(response.getData().contains("已停用"));
    }

    @Test
    @DisplayName("analyze-question/image：学生 + 图片 → 返回单题分析")
    void analyzeImage_success() {
        QuestionAnalysisDTO dto = QuestionAnalysisDTO.resolved("鸡兔同笼", 60, List.of());
        when(appService.analyzeImage(any(), anyString(), eq(STUDENT_ID))).thenReturn(dto);
        MockMultipartFile file = new MockMultipartFile("file", "q.png", "image/png", new byte[]{1});

        ApiResponse<QuestionAnalysisDTO> response = controller.analyzeQuestionImage(file, loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        verify(appService).analyzeImage(any(), anyString(), eq(STUDENT_ID));
    }

    @Test
    @DisplayName("analyze-question/image：file 为空 → 10001")
    void analyzeImage_emptyFile_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.analyzeQuestionImage(new MockMultipartFile("file", "", "", new byte[0]), loginSession()));
        assertEquals(ErrorCode.INVALID_PARAMS, ex.getCode());
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
