package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.entity.StudentQuestionRecord;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.model.contract.QuestionUnderstandResult;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.repository.StudentQuestionRecordRepository;
import com.ai.edu.domain.learning.service.QuestionUnderstandingPort;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import com.ai.edu.domain.learning.service.TutoringLlmPort;
import com.ai.edu.domain.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * analyze-question 应用编排单测（D8 封闭域池约束选择：题型库命中权威 / 池约束恒非空 / 兜底挂起）。
 */
class KpQuestionAnalysisAppServiceTest {

    private static final String TEXT = "笼子里有鸡和兔共 35 个头，94 只脚，鸡和兔各有多少只？";
    private static final Long STUDENT_ID = 1001L;

    private KpQuestionAnalysisAppService service;
    private QuestionUnderstandingPort understandingPort;
    private TutoringKpResolver kpResolver;
    private QuestionTypeRepository questionTypeRepository;
    private QuestionTypeKpRepository questionTypeKpRepository;
    private KgKnowledgePointRepository kgRepository;
    private FileStorageService fileStorageService;
    private TutoringLlmPort tutoringLlmPort;
    private StudentQuestionRecordRepository questionRecordRepository;
    private TopicLabelAggregationService topicLabelAggregationService;

    @BeforeEach
    void setUp() {
        understandingPort = mock(QuestionUnderstandingPort.class);
        kpResolver = mock(TutoringKpResolver.class);
        questionTypeRepository = mock(QuestionTypeRepository.class);
        questionTypeKpRepository = mock(QuestionTypeKpRepository.class);
        kgRepository = mock(KgKnowledgePointRepository.class);
        fileStorageService = mock(FileStorageService.class);
        tutoringLlmPort = mock(TutoringLlmPort.class);
        questionRecordRepository = mock(StudentQuestionRecordRepository.class);
        topicLabelAggregationService = mock(TopicLabelAggregationService.class);
        service = new KpQuestionAnalysisAppService();
        setField(service, "questionUnderstandingPort", understandingPort);
        setField(service, "tutoringKpResolver", kpResolver);
        setField(service, "questionTypeRepository", questionTypeRepository);
        setField(service, "questionTypeKpRepository", questionTypeKpRepository);
        setField(service, "kgKnowledgePointRepository", kgRepository);
        setField(service, "fileStorageService", fileStorageService);
        setField(service, "tutoringLlmPort", tutoringLlmPort);
        setField(service, "questionRecordRepository", questionRecordRepository);
        setField(service, "topicLabelAggregationService", topicLabelAggregationService);
    }

    @Test
    @DisplayName("① 题型库命中 → 全分布（数据驱动权威，kpLabel 反查）")
    void catalogHit_returnsFullDistribution() {
        when(kpResolver.resolveStudentGrade(STUDENT_ID)).thenReturn(7);
        when(understandingPort.understand(TEXT, 7)).thenReturn(List.of("鸡兔同笼"));
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼"))
                .thenReturn(Optional.of(qt(5L, "鸡兔同笼问题")));
        when(questionTypeKpRepository.findByQuestionTypeId(5L)).thenReturn(List.of(
                bucket("uri-A", "4-6", 0.6),
                bucket("uri-B", "7-8", 0.4)));
        when(kgRepository.findByUri("uri-A")).thenReturn(Optional.of(kp("uri-A", "鸡兔同笼")));
        when(kgRepository.findByUri("uri-B")).thenReturn(Optional.of(kp("uri-B", "假设法")));

        QuestionAnalysisDTO dto = service.analyze(TEXT, STUDENT_ID);

        assertEquals("鸡兔同笼问题", dto.getTopicLabel());
        assertEquals("RESOLVED", dto.getStatus());
        assertEquals(60, dto.getConfidence());
        assertEquals(2, dto.getKnowledgePoints().size());
        // 2.7.1: 题目落库（source=ai，score=null 无信号，canonical=权威名）
        verify(questionRecordRepository).save(analyzeRecord(
                "ai", TEXT, "鸡兔同笼", "鸡兔同笼问题", null));
    }

    @Test
    @DisplayName("① 遍历候选题型名：首个 miss、第二个命中 → RESOLVED（顺序无关）")
    void iteration_catalogSecondHits() {
        when(kpResolver.resolveStudentGrade(STUDENT_ID)).thenReturn(7);
        when(understandingPort.understand(TEXT, 7)).thenReturn(List.of("鸡兔同笼问题", "鸡兔同笼"));
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼问题")).thenReturn(Optional.empty());
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼")).thenReturn(Optional.of(qt(5L, "鸡兔同笼")));
        when(questionTypeKpRepository.findByQuestionTypeId(5L)).thenReturn(List.of(bucket("uri-A", null, 1.0)));
        when(kgRepository.findByUri("uri-A")).thenReturn(Optional.of(kp("uri-A", "鸡兔同笼")));

        QuestionAnalysisDTO dto = service.analyze(TEXT, STUDENT_ID);

        assertEquals("RESOLVED", dto.getStatus());
        assertEquals("鸡兔同笼", dto.getTopicLabel());
    }

    @Test
    @DisplayName("② 题库 miss → 仅题型 RESOLVED + 空知识点（域 B 独立化：不挂起、不写 obs）")
    void catalogMiss_returnsResolvedNoKp() {
        when(kpResolver.resolveStudentGrade(STUDENT_ID)).thenReturn(4);
        when(understandingPort.understand(TEXT, 4)).thenReturn(List.of("鸡兔同笼"));
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼")).thenReturn(Optional.empty());
        when(topicLabelAggregationService.aggregate("鸡兔同笼", STUDENT_ID)).thenReturn("鸡兔同笼");

        QuestionAnalysisDTO dto = service.analyze(TEXT, STUDENT_ID);

        assertEquals("RESOLVED", dto.getStatus());
        assertEquals("鸡兔同笼", dto.getTopicLabel());
        assertEquals(0, dto.getConfidence());
        assertTrue(dto.getKnowledgePoints().isEmpty());
        // 2.7.2: 返回 canonical 过聚集；2.7.1: 题目落库（source=ai，score=null 无信号）
        verify(questionRecordRepository).save(analyzeRecord("ai", TEXT, "鸡兔同笼", "鸡兔同笼", null));
    }

    @Test
    @DisplayName("题目理解失败 → PENDING 无候选，不抛错")
    void understandEmpty_returnsPending() {
        when(kpResolver.resolveStudentGrade(STUDENT_ID)).thenReturn(null);
        when(understandingPort.understand(TEXT, null)).thenReturn(List.of());

        QuestionAnalysisDTO dto = service.analyze(TEXT, STUDENT_ID);

        assertEquals("PENDING", dto.getStatus());
        assertNull(dto.getTopicLabel());
        assertTrue(dto.getKnowledgePoints().isEmpty());
        verify(questionRecordRepository, never()).save(any()); // 识别失败不落库
    }

    @Test
    @DisplayName("图片：Python 看图识别题型 → 题型库命中 → 权威分布")
    void image_catalogHit() {
        when(kpResolver.resolveStudentGrade(STUDENT_ID)).thenReturn(4);
        when(fileStorageService.generatePresignedUrl(anyString(), anyInt())).thenReturn("http://cos/signed");
        when(questionTypeRepository.findTopTopicLabels(20)).thenReturn(List.of("鸡兔同笼"));
        when(tutoringLlmPort.understandQuestion(eq("http://cos/signed"), any(), eq(4)))
                .thenReturn(QuestionUnderstandResult.builder()
                        .topicLabels(List.of("鸡兔同笼")).questionKps(List.of("二元一次方程组")).build());
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼")).thenReturn(Optional.of(qt(5L, "鸡兔同笼")));
        when(questionTypeKpRepository.findByQuestionTypeId(5L)).thenReturn(List.of(bucket("uri-A", null, 1.0)));
        when(kgRepository.findByUri("uri-A")).thenReturn(Optional.of(kp("uri-A", "鸡兔同笼")));

        QuestionAnalysisDTO dto = service.analyzeImage(new byte[]{1}, "q.png", STUDENT_ID);

        assertEquals("RESOLVED", dto.getStatus());
        assertEquals("鸡兔同笼", dto.getTopicLabel());
        verify(fileStorageService).uploadToObjectKey(anyString(), any(), anyString());
        // 2.7.1: 图片题目也落库（source=ai，score=null 无信号，canonical=权威名）
        verify(questionRecordRepository).save(analyzeRecord("ai", "[图片题目]", "鸡兔同笼", "鸡兔同笼", null));
    }

    @Test
    @DisplayName("图片：题库 miss → 仅题型 RESOLVED + 空知识点（域 B 独立化：不顺带 kps、不挂起）")
    void image_catalogMiss_returnsResolvedNoKp() {
        when(kpResolver.resolveStudentGrade(STUDENT_ID)).thenReturn(4);
        when(fileStorageService.generatePresignedUrl(anyString(), anyInt())).thenReturn("http://cos/signed");
        when(questionTypeRepository.findTopTopicLabels(20)).thenReturn(List.of("鸡兔同笼"));
        when(tutoringLlmPort.understandQuestion(anyString(), any(), any()))
                .thenReturn(QuestionUnderstandResult.builder()
                        .topicLabels(List.of("鸡兔同笼")).questionKps(List.of("二元一次方程组")).build());
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼")).thenReturn(Optional.empty());
        when(topicLabelAggregationService.aggregate("鸡兔同笼", STUDENT_ID)).thenReturn("鸡兔同笼");

        QuestionAnalysisDTO dto = service.analyzeImage(new byte[]{1}, "q.png", STUDENT_ID);

        assertEquals("RESOLVED", dto.getStatus());
        assertEquals("鸡兔同笼", dto.getTopicLabel());
        assertEquals(0, dto.getConfidence());
        assertTrue(dto.getKnowledgePoints().isEmpty(), "顺带知识点不再展示（域 B 独立化）");
        verify(kgRepository, never()).findByLabel(anyString());
        // 2.7.2: 图片 miss 也返回 canonical 过聚集 + 落库
        verify(questionRecordRepository).save(analyzeRecord("ai", "[图片题目]", "鸡兔同笼", "鸡兔同笼", null));
    }

    @Test
    @DisplayName("图片：Python 识别失败（空 topicLabels）→ PENDING 不报错")
    void image_failed_pending() {
        when(kpResolver.resolveStudentGrade(STUDENT_ID)).thenReturn(4);
        when(fileStorageService.generatePresignedUrl(anyString(), anyInt())).thenReturn("http://cos/signed");
        when(questionTypeRepository.findTopTopicLabels(20)).thenReturn(List.of());
        when(tutoringLlmPort.understandQuestion(anyString(), any(), any()))
                .thenReturn(QuestionUnderstandResult.builder().topicLabels(List.of()).questionKps(List.of()).build());

        QuestionAnalysisDTO dto = service.analyzeImage(new byte[]{1}, "q.png", STUDENT_ID);

        assertEquals("PENDING", dto.getStatus());
        assertNull(dto.getTopicLabel());
        verify(questionRecordRepository, never()).save(any()); // 识别失败不落库
    }

    @Test
    @DisplayName("图片：非法格式 → 抛 TUTORING_OCR_INVALID")
    void image_invalidFormat() {
        BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
                () -> service.analyzeImage(new byte[]{1}, "q.gif", STUDENT_ID));
        assertEquals(ErrorCode.TUTORING_OCR_INVALID, ex.getCode());
    }

    /** 落库断言 helper：source/content/topicLabel/canonical/score 逐字段校验。 */
    private StudentQuestionRecord analyzeRecord(String source, String content, String topicLabel,
                                                String canonical, BigDecimal score) {
        return argThat(r -> r instanceof StudentQuestionRecord rec
                && source.equals(rec.getSource())
                && content.equals(rec.getContent())
                && topicLabel.equals(rec.getTopicLabel())
                && java.util.Objects.equals(canonical, rec.getCanonicalLabel())
                && java.util.Objects.equals(score, rec.getScore()));
    }

    private QuestionType qt(Long id, String label) {
        QuestionType qt = QuestionType.create(label, QuestionTypeStatus.CANDIDATE, 1001L);
        qt.setId(id);
        return qt;
    }

    private QuestionTypeKp bucket(String uri, String gradeRange, double ratio) {
        QuestionTypeKp kp = QuestionTypeKp.create(5L, uri, gradeRange);
        kp.updateStats(2, 3, ratio, gradeRange);
        return kp;
    }

    private KgKnowledgePoint kp(String uri, String label) {
        return KgKnowledgePoint.create(uri, label);
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
