package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.service.KpConstrainedAssociationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 封闭域池约束选择编排单测（「题库和知识点」独立迭代用，analyze 本期未接线）。
 */
class KpPoolAssociateServiceTest {

    private static final String TEXT = "笼子里有鸡和兔共 35 个头，94 只脚，鸡和兔各有多少只？";
    private static final Long STUDENT_ID = 1001L;

    private KpPoolAssociateService service;
    private KgKnowledgePointRepository kgRepository;
    private DerivedKpObsRepository obsRepository;
    private KpConstrainedAssociationPort associationPort;

    @BeforeEach
    void setUp() {
        kgRepository = mock(KgKnowledgePointRepository.class);
        obsRepository = mock(DerivedKpObsRepository.class);
        associationPort = mock(KpConstrainedAssociationPort.class);
        service = new KpPoolAssociateService();
        setField(service, "kgKnowledgePointRepository", kgRepository);
        setField(service, "derivedKpObsRepository", obsRepository);
        setField(service, "constrainedAssociationPort", associationPort);
    }

    @Test
    @DisplayName("学段池 → LLM 从池选 top-N → RESOLVED + top-1 落 RESOLVED obs（信任模型）")
    void poolAssociates_topNResolved() {
        when(kgRepository.findLabelsByStage("小学")).thenReturn(List.of("鸡兔同笼", "二元一次方程组", "假设法"));
        when(associationPort.associate(eq(TEXT), eq(4), any())).thenReturn(List.of("鸡兔同笼", "二元一次方程组"));
        when(kgRepository.findByLabel("鸡兔同笼")).thenReturn(Optional.of(kp("uri-jt", "鸡兔同笼")));
        when(kgRepository.findByLabel("二元一次方程组")).thenReturn(Optional.of(kp("uri-er", "二元一次方程组")));

        QuestionAnalysisDTO dto = service.associate(TEXT, STUDENT_ID, 4, List.of("鸡兔同笼"));

        assertEquals("RESOLVED", dto.getStatus());
        assertEquals("鸡兔同笼", dto.getTopicLabel());
        assertEquals(2, dto.getKnowledgePoints().size());
        assertEquals(0.6, dto.getKnowledgePoints().get(0).getRatio());
        verify(obsRepository).upsert(argThat(o -> "uri-jt".equals(o.getKpUri())
                && "RESOLVED".equals(o.getStatus().name())));
    }

    @Test
    @DisplayName("学段池空 → null（调用方走 PENDING 兜底）")
    void poolEmpty_returnsNull() {
        when(kgRepository.findLabelsByStage("小学")).thenReturn(List.of());

        assertNull(service.associate(TEXT, STUDENT_ID, 4, List.of("鸡兔同笼")));
        verify(associationPort, org.mockito.Mockito.never()).associate(any(), any(), any());
    }

    @Test
    @DisplayName("无年级 → null（无学段池）")
    void gradeNull_returnsNull() {
        assertNull(service.associate(TEXT, STUDENT_ID, null, List.of("鸡兔同笼")));
        verify(kgRepository, org.mockito.Mockito.never()).findLabelsByStage(any());
    }

    @Test
    @DisplayName("无题型名 → null（obs 无归属）")
    void noTopics_returnsNull() {
        assertNull(service.associate(TEXT, STUDENT_ID, 4, List.of()));
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
