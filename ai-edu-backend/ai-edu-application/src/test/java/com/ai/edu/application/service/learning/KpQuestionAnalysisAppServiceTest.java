package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.learning.model.contract.QuestionUnderstandResult;
import com.ai.edu.domain.learning.service.QuestionUnderstandingPort;
import com.ai.edu.domain.learning.service.TutoringLlmPort;
import com.ai.edu.domain.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题型分析纯 Python 直通测试（小工具：文本/图片 → Python 识别 → 题型名展示，不题库命中/不落库/无业务功能）。
 */
class KpQuestionAnalysisAppServiceTest {

    private static final Long STUDENT_ID = 1001L;

    private KpQuestionAnalysisAppService service;
    private QuestionUnderstandingPort understandingPort;
    private TutoringLlmPort llmPort;
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        service = new KpQuestionAnalysisAppService();
        understandingPort = mock(QuestionUnderstandingPort.class);
        llmPort = mock(TutoringLlmPort.class);
        fileStorageService = mock(FileStorageService.class);
        inject(service, "questionUnderstandingPort", understandingPort);
        inject(service, "tutoringLlmPort", llmPort);
        inject(service, "fileStorageService", fileStorageService);
        when(fileStorageService.generatePresignedUrl(anyString(), anyInt())).thenReturn("https://cos/xxx");
    }

    // ---------- 文本直通 ----------

    @Test
    @DisplayName("文本：understand 识别出题型 → 直接返回题型名（不题库命中/不落库）")
    void analyze_understandHit() {
        when(understandingPort.understand(anyString(), any())).thenReturn(List.of("鸡兔同笼"));

        QuestionAnalysisDTO dto = service.analyze("笼子里有鸡和兔", STUDENT_ID);

        assertEquals("RESOLVED", dto.getStatus());
        assertEquals("鸡兔同笼", dto.getTopicLabel(), "返回 Python 识别的题型名");
        assertEquals(0, dto.getKnowledgePoints().size(), "小工具不带知识点分布");
        assertEquals(null, dto.getImageUrl(), "文本分析无原题图 URL");
    }

    @Test
    @DisplayName("文本：understand 空 → PENDING（不报错）")
    void analyze_understandEmpty() {
        when(understandingPort.understand(anyString(), any())).thenReturn(List.of());

        QuestionAnalysisDTO dto = service.analyze("一段识别不出的文本", STUDENT_ID);

        assertEquals("PENDING", dto.getStatus());
    }

    // ---------- 图片直通 ----------

    @Test
    @DisplayName("图片：视觉模型识别出题型 → 直接返回题型名（上传 COS → Python 看图，不落库）")
    void analyzeImage_visualHit() {
        when(llmPort.understandQuestion(anyString(), any(), any()))
                .thenReturn(QuestionUnderstandResult.builder().topicLabels(List.of("鸡兔同笼")).build());

        QuestionAnalysisDTO dto = service.analyzeImage(new byte[]{1, 2, 3}, "q.png", STUDENT_ID);

        assertEquals("RESOLVED", dto.getStatus());
        assertEquals("鸡兔同笼", dto.getTopicLabel());
        assertEquals("https://cos/xxx", dto.getImageUrl(), "返回原题图 URL 供前端展示");
        verify(fileStorageService).uploadToObjectKey(anyString(), any(), anyString());
        verify(llmPort).understandQuestion(anyString(), any(), any());
    }

    @Test
    @DisplayName("图片：视觉模型识别失败 → PENDING（不报错）")
    void analyzeImage_visualFailed() {
        when(llmPort.understandQuestion(anyString(), any(), any()))
                .thenReturn(QuestionUnderstandResult.builder().topicLabels(List.of()).build());

        QuestionAnalysisDTO dto = service.analyzeImage(new byte[]{1}, "q.png", STUDENT_ID);

        assertEquals("PENDING", dto.getStatus());
    }

    @Test
    @DisplayName("图片：非白名单格式 → 50006")
    void analyzeImage_invalidFormat() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.analyzeImage(new byte[]{1}, "q.gif", STUDENT_ID));
        assertEquals(ErrorCode.TUTORING_OCR_INVALID, ex.getCode());
    }

    @Test
    @DisplayName("图片：文件存储未配置 → 10001")
    void analyzeImage_noStorage() {
        KpQuestionAnalysisAppService noStorage = new KpQuestionAnalysisAppService();
        inject(noStorage, "questionUnderstandingPort", understandingPort);
        inject(noStorage, "tutoringLlmPort", llmPort);
        // fileStorageService 不注入（null）

        BusinessException ex = assertThrows(BusinessException.class,
                () -> noStorage.analyzeImage(new byte[]{1}, "q.png", STUDENT_ID));
        assertEquals(ErrorCode.INVALID_PARAMS, ex.getCode());
    }

    private void inject(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
