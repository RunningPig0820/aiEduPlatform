package com.ai.edu.application.service.learning;

import com.ai.edu.application.service.batch.KpQuestionTypeAggregationService;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpSource;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 题型库聚合服务单测（mock 仓储，验证聚合阈值 + 升 STABLE 门槛）。
 */
class KpQuestionTypeAggregationServiceTest {

    private KpQuestionTypeAggregationService service;
    private DerivedKpObsRepository obsRepository;
    private QuestionTypeRepository questionTypeRepository;
    private QuestionTypeKpRepository questionTypeKpRepository;

    @BeforeEach
    void setUp() {
        obsRepository = mock(DerivedKpObsRepository.class);
        questionTypeRepository = mock(QuestionTypeRepository.class);
        questionTypeKpRepository = mock(QuestionTypeKpRepository.class);
        service = new KpQuestionTypeAggregationService();
        setField(service, "derivedKpObsRepository", obsRepository);
        setField(service, "questionTypeRepository", questionTypeRepository);
        setField(service, "questionTypeKpRepository", questionTypeKpRepository);
        setField(service, "candidateStudents", 3);
        setField(service, "candidateHits", 5);
        setField(service, "stableStudents", 10);
    }

    @Test
    @DisplayName("达阈值（≥3 学生且 ≥5 命中）→ 建 CANDIDATE + 分布桶")
    void aggregateReachesThreshold() {
        when(obsRepository.findResolved()).thenReturn(List.of(
                obs(1L, 4), obs(2L, 4), obs(3L, 5), obs(4L, 5), obs(5L, 6)));
        when(questionTypeRepository.findByTopicLabel("鸡兔同笼")).thenReturn(Optional.empty());
        QuestionType qt = QuestionType.create("鸡兔同笼", QuestionTypeStatus.CANDIDATE, 1L);
        qt.setId(100L);
        when(questionTypeRepository.upsert(any())).thenReturn(qt);

        service.aggregate();

        verify(questionTypeRepository).upsert(any(QuestionType.class));
        verify(questionTypeKpRepository, atLeastOnce()).upsert(any());
    }

    @Test
    @DisplayName("未达阈值（仅 1 学生）→ 不聚合")
    void aggregateBelowThreshold() {
        when(obsRepository.findResolved()).thenReturn(List.of(obs(1L, 4)));

        service.aggregate();

        verify(questionTypeRepository, never()).upsert(any());
        verify(questionTypeKpRepository, never()).upsert(any());
    }

    @Test
    @DisplayName("升 STABLE：<10 学生 → 拒绝")
    void promoteToStableRejected() {
        QuestionType qt = QuestionType.create("鸡兔同笼", QuestionTypeStatus.CANDIDATE, 1L);
        qt.updateStats(5, 10);
        when(questionTypeRepository.findByTopicLabel("鸡兔同笼")).thenReturn(Optional.of(qt));

        assertThrows(IllegalStateException.class, () -> service.promoteToStable("鸡兔同笼", null));
        verify(questionTypeRepository, never()).upsert(any());
    }

    @Test
    @DisplayName("升 STABLE：≥10 学生 → 转 STABLE")
    void promoteToStableSucceeds() {
        QuestionType qt = QuestionType.create("鸡兔同笼", QuestionTypeStatus.CANDIDATE, 1L);
        qt.updateStats(12, 30);
        when(questionTypeRepository.findByTopicLabel("鸡兔同笼")).thenReturn(Optional.of(qt));

        service.promoteToStable("鸡兔同笼", "已知鸡兔总数与脚数求各多少");

        verify(questionTypeRepository).upsert(argThat(t -> t.getStatus() == QuestionTypeStatus.STABLE));
    }

    private DerivedKpObs obs(Long studentId, int grade) {
        DerivedKpObs o = DerivedKpObs.create(studentId, "鸡兔同笼",
                "http://edukg.org/knowledge/3.1/kp/math#jsfa", grade, 80,
                DerivedKpSource.LLM, DerivedKpStatus.RESOLVED);
        o.setId(studentId);
        return o;
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
