package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.KpCoverageDTO;
import com.ai.edu.application.dto.learning.KpCoverageItemDTO;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.entity.StudentKpMastery;
import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpSource;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
import com.ai.edu.domain.learning.model.valueobject.KpKey;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.repository.StudentKpMasteryRepository;
import com.ai.edu.domain.learning.repository.StudentTopicMasteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 知识点派生覆盖度应用服务测试（mock 仓储），覆盖 test.md COV-001/002/003 + clamp/discretize。
 */
class KpCoverageAppServiceTest {

    private static final Long STUDENT_ID = 501L;

    private KpCoverageAppService service;
    private StudentTopicMasteryRepository topicMasteryRepository;
    private DerivedKpObsRepository derivedKpObsRepository;
    private QuestionTypeRepository questionTypeRepository;
    private QuestionTypeKpRepository questionTypeKpRepository;
    private StudentKpMasteryRepository kpMasteryRepository;
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    @BeforeEach
    void setUp() {
        service = new KpCoverageAppService();
        topicMasteryRepository = mock(StudentTopicMasteryRepository.class);
        derivedKpObsRepository = mock(DerivedKpObsRepository.class);
        questionTypeRepository = mock(QuestionTypeRepository.class);
        questionTypeKpRepository = mock(QuestionTypeKpRepository.class);
        kpMasteryRepository = mock(StudentKpMasteryRepository.class);
        kgKnowledgePointRepository = mock(KgKnowledgePointRepository.class);

        service.setTopicMasteryRepository(topicMasteryRepository);
        service.setDerivedKpObsRepository(derivedKpObsRepository);
        service.setQuestionTypeRepository(questionTypeRepository);
        service.setQuestionTypeKpRepository(questionTypeKpRepository);
        service.setKpMasteryRepository(kpMasteryRepository);
        service.setKgKnowledgePointRepository(kgKnowledgePointRepository);

        when(topicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of());
        when(derivedKpObsRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of());
        when(kpMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of());
        when(kgKnowledgePointRepository.findByUris(anyList())).thenReturn(List.of());
        when(kgKnowledgePointRepository.findPlacementByUris(anyList())).thenReturn(List.of());
    }

    @Test
    @DisplayName("聚合题型按 ratio 派生：coverage = 题型掌握度 × ratio")
    void coverage_aggregatedRatio() {
        StudentTopicMastery topic = StudentTopicMastery.create(STUDENT_ID, TopicKey.of("鸡兔同笼"), "鸡兔同笼");
        topic.applySignal(MasterySignal.of("鸡兔同笼", MasterySignal.Level.MASTERED)); // 75
        when(topicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(topic));

        QuestionType qt = QuestionType.create("鸡兔同笼", QuestionTypeStatus.CANDIDATE, null);
        qt.setId(10L);
        when(questionTypeRepository.findByTopicLabel("鸡兔同笼")).thenReturn(Optional.of(qt));
        QuestionTypeKp kp1 = QuestionTypeKp.create(10L, "kp-1", "7-8");
        kp1.updateStats(0, 0, 0.8, "7-8");
        QuestionTypeKp kp2 = QuestionTypeKp.create(10L, "kp-2", "4-6");
        kp2.updateStats(0, 0, 0.2, "4-6");
        when(questionTypeKpRepository.findByQuestionTypeId(10L)).thenReturn(List.of(kp1, kp2));

        Map<String, KpCoverageItemDTO> items = byUri(service.getKpCoverage(STUDENT_ID));

        assertEquals(2, items.size());
        assertEquals(60, items.get("kp-1").getCoverage());  // 75 × 0.8
        assertEquals(50, items.get("kp-1").getMasteryLevel());
        assertEquals(15, items.get("kp-2").getCoverage());  // 75 × 0.2
        assertEquals(0, items.get("kp-2").getMasteryLevel());
    }

    @Test
    @DisplayName("未聚合题型按单观测派生（ratio 隐式 1）")
    void coverage_singleObs() {
        StudentTopicMastery topic = StudentTopicMastery.create(STUDENT_ID, TopicKey.of("相遇问题"), "相遇问题");
        topic.applySignal(MasterySignal.of("相遇问题", MasterySignal.Level.PRACTICING)); // 50
        when(topicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(topic));
        when(questionTypeRepository.findByTopicLabel("相遇问题")).thenReturn(Optional.empty());

        DerivedKpObs obs = DerivedKpObs.create(STUDENT_ID, "相遇问题", "kp-3", 7, 60,
                DerivedKpSource.MIRROR, DerivedKpStatus.RESOLVED);
        when(derivedKpObsRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(obs));

        Map<String, KpCoverageItemDTO> items = byUri(service.getKpCoverage(STUDENT_ID));

        assertEquals(50, items.get("kp-3").getCoverage());
        assertEquals(50, items.get("kp-3").getMasteryLevel());
    }

    @Test
    @DisplayName("无题型映射的知识点回退旧 KP 掌握度")
    void coverage_legacyFallback() {
        StudentKpMastery legacy = StudentKpMastery.create(STUDENT_ID, KpKey.of("kp-4"), "二元一次方程组");
        legacy.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.PRACTICING)); // 50
        when(kpMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(legacy));

        Map<String, KpCoverageItemDTO> items = byUri(service.getKpCoverage(STUDENT_ID));

        assertEquals(50, items.get("kp-4").getCoverage());
        assertEquals(50, items.get("kp-4").getMasteryLevel());
    }

    @Test
    @DisplayName("多题型叠加同 kp 溢出 → clamp 到 75")
    void coverage_clamped() {
        StudentTopicMastery t1 = StudentTopicMastery.create(STUDENT_ID, TopicKey.of("鸡兔同笼"), "鸡兔同笼");
        t1.applySignal(MasterySignal.of("鸡兔同笼", MasterySignal.Level.MASTERED)); // 75
        StudentTopicMastery t2 = StudentTopicMastery.create(STUDENT_ID, TopicKey.of("假设法"), "假设法");
        t2.applySignal(MasterySignal.of("假设法", MasterySignal.Level.MASTERED)); // 75
        when(topicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(t1, t2));
        when(questionTypeRepository.findByTopicLabel(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        DerivedKpObs o1 = DerivedKpObs.create(STUDENT_ID, "鸡兔同笼", "kp-5", 7, 90, DerivedKpSource.LLM, DerivedKpStatus.RESOLVED);
        DerivedKpObs o2 = DerivedKpObs.create(STUDENT_ID, "假设法", "kp-5", 7, 80, DerivedKpSource.LLM, DerivedKpStatus.RESOLVED);
        when(derivedKpObsRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(o1, o2));

        Map<String, KpCoverageItemDTO> items = byUri(service.getKpCoverage(STUDENT_ID));

        assertEquals(75, items.get("kp-5").getCoverage());  // 150 → clamp 75
        assertEquals(75, items.get("kp-5").getMasteryLevel());
    }

    private Map<String, KpCoverageItemDTO> byUri(KpCoverageDTO dto) {
        return dto.getItems().stream()
                .collect(Collectors.toMap(KpCoverageItemDTO::getKpUri, i -> i));
    }
}
