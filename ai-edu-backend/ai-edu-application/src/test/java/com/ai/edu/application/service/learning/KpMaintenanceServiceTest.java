package com.ai.edu.application.service.learning;

import com.ai.edu.application.service.batch.KpMaintenanceService;
import com.ai.edu.application.service.batch.KpQuestionTypeAggregationService;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpSource;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.service.KpDisambiguationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 维护闭环服务单测（mock 依赖，验证 WEAK 共现转正 + CONFLICTED 重判）。
 */
class KpMaintenanceServiceTest {

    private static final String KP_URI = "http://edukg.org/knowledge/3.1/kp/math#jsfa";

    private KpMaintenanceService service;
    private DerivedKpObsRepository obsRepository;
    private KpDisambiguationPort disambiguationPort;
    private KpQuestionTypeAggregationService aggregationService;

    @BeforeEach
    void setUp() {
        obsRepository = mock(DerivedKpObsRepository.class);
        disambiguationPort = mock(KpDisambiguationPort.class);
        aggregationService = mock(KpQuestionTypeAggregationService.class);
        service = new KpMaintenanceService();
        setField(service, "derivedKpObsRepository", obsRepository);
        setField(service, "kpDisambiguationPort", disambiguationPort);
        setField(service, "aggregationService", aggregationService);
        setField(service, "confidenceThreshold", 60);
    }

    @Test
    @DisplayName("WEAK 共现（≥2 学生）→ 转 RESOLVED")
    void weakPromotesByCooccurrence() {
        when(obsRepository.findByStatus(DerivedKpStatus.WEAK))
                .thenReturn(List.of(obs(1L, DerivedKpStatus.WEAK)));
        when(obsRepository.countDistinctStudentsByTopicAndKp("鸡兔同笼", KP_URI)).thenReturn(2);

        service.maintain();

        verify(obsRepository).updateStatus(anyLong(), eq(DerivedKpStatus.RESOLVED));
    }

    @Test
    @DisplayName("CONFLICTED 高置信重判 → READJUDICATED")
    void conflictedRejudgedHighConfidence() {
        when(obsRepository.findByStatus(DerivedKpStatus.WEAK)).thenReturn(List.of());
        when(obsRepository.findByStatus(DerivedKpStatus.CONFLICTED))
                .thenReturn(List.of(obs(1L, DerivedKpStatus.CONFLICTED)));
        when(disambiguationPort.disambiguate(any(), any()))
                .thenReturn(KpResolution.resolved("鸡兔同笼", KP_URI, "假设法", 88));

        service.maintain();

        verify(obsRepository).updateStatus(anyLong(), eq(DerivedKpStatus.READJUDICATED));
    }

    @Test
    @DisplayName("CONFLICTED 低置信重判 → HUMAN_REVIEW")
    void conflictedRejudgedLowConfidence() {
        when(obsRepository.findByStatus(DerivedKpStatus.WEAK)).thenReturn(List.of());
        when(obsRepository.findByStatus(DerivedKpStatus.CONFLICTED))
                .thenReturn(List.of(obs(1L, DerivedKpStatus.CONFLICTED)));
        when(disambiguationPort.disambiguate(any(), any())).thenReturn(null);

        service.maintain();

        verify(obsRepository).updateStatus(anyLong(), eq(DerivedKpStatus.HUMAN_REVIEW));
    }

    @Test
    @DisplayName("存疑 PENDING 高置信重判 → 转 WEAK（待共现转正，不直接 RESOLVED 防幻觉污染）")
    void pendingRejudgedToWeak() {
        when(obsRepository.findByStatus(DerivedKpStatus.WEAK)).thenReturn(List.of());
        when(obsRepository.findByStatus(DerivedKpStatus.CONFLICTED)).thenReturn(List.of());
        when(obsRepository.findByStatus(DerivedKpStatus.PENDING))
                .thenReturn(List.of(obs(5L, DerivedKpStatus.PENDING)));
        when(disambiguationPort.disambiguate("鸡兔同笼", 4))
                .thenReturn(KpResolution.resolved("鸡兔同笼", KP_URI, "二元一次方程组", 80));

        service.maintain();

        verify(obsRepository).resolveWeakByMaintenance(5L, KP_URI, 80);
        verify(obsRepository, never()).updateStatus(anyLong(), eq(DerivedKpStatus.RESOLVED));
    }

    private DerivedKpObs obs(Long studentId, DerivedKpStatus status) {
        DerivedKpObs o = DerivedKpObs.create(studentId, "鸡兔同笼", KP_URI, 4, 80,
                DerivedKpSource.LLM, status);
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
