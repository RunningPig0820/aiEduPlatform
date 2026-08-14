package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.service.KpDisambiguationPort;
import com.ai.edu.domain.organization.repository.ClassRepository;
import com.ai.edu.domain.organization.repository.StudentClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 知识点解析管线单测（mock 依赖，验证 ① 镜像精确/LIKE → ② 题型库 → ③ LLM 消歧 → ④ 挂起）。
 */
class TutoringKpResolverImplTest {

    private static final String KP_URI = "http://edukg.org/knowledge/3.1/kp/math#jsfa";
    private static final Long STUDENT_ID = 101L;

    private TutoringKpResolverImpl resolver;
    private KgKnowledgePointRepository kgRepository;
    private QuestionTypeRepository questionTypeRepository;
    private QuestionTypeKpRepository questionTypeKpRepository;
    private StudentClassRepository studentClassRepository;
    private ClassRepository classRepository;
    private DerivedKpObsRepository obsRepository;
    private KpDisambiguationPort disambiguationPort;

    @BeforeEach
    void setUp() {
        kgRepository = mock(KgKnowledgePointRepository.class);
        questionTypeRepository = mock(QuestionTypeRepository.class);
        questionTypeKpRepository = mock(QuestionTypeKpRepository.class);
        studentClassRepository = mock(StudentClassRepository.class);
        classRepository = mock(ClassRepository.class);
        obsRepository = mock(DerivedKpObsRepository.class);
        disambiguationPort = mock(KpDisambiguationPort.class);
        resolver = new TutoringKpResolverImpl();
        setField(resolver, "kgKnowledgePointRepository", kgRepository);
        setField(resolver, "questionTypeRepository", questionTypeRepository);
        setField(resolver, "questionTypeKpRepository", questionTypeKpRepository);
        setField(resolver, "studentClassRepository", studentClassRepository);
        setField(resolver, "classRepository", classRepository);
        setField(resolver, "derivedKpObsRepository", obsRepository);
        setField(resolver, "kpDisambiguationPort", disambiguationPort);
        setField(resolver, "confidenceThreshold", 60);
    }

    @Test
    @DisplayName("镜像精确命中 → RESOLVED，confidence=100，不调 LLM")
    void exactMatch() {
        when(kgRepository.findByLabel("二元一次方程组")).thenReturn(Optional.of(kp(KP_URI, "二元一次方程组")));

        KpResolution r = resolver.resolve("二元一次方程组", STUDENT_ID);

        assertEquals(KpResolution.STATUS_RESOLVED, r.getStatus());
        assertEquals(KP_URI, r.getUri());
        assertEquals(100, r.getConfidence());
        verify(disambiguationPort, never()).disambiguate(anyString(), any());
    }

    @Test
    @DisplayName("镜像 LIKE 命中 → RESOLVED，confidence=80")
    void likeMatchFallback() {
        when(kgRepository.findByLabel("二元一次方程组")).thenReturn(Optional.empty());
        when(kgRepository.findByLabelLike("二元一次方程组")).thenReturn(Optional.of(kp(KP_URI, "二元一次方程组")));

        KpResolution r = resolver.resolve("二元一次方程组", STUDENT_ID);

        assertEquals(KpResolution.STATUS_RESOLVED, r.getStatus());
        assertEquals(KP_URI, r.getUri());
        assertEquals(80, r.getConfidence());
    }

    @Test
    @DisplayName("镜像未命中 → LLM 消歧高置信 → 冷启动 WEAK")
    void llmDisambiguateColdStart() {
        when(kgRepository.findByLabel(anyString())).thenReturn(Optional.empty());
        when(kgRepository.findByLabelLike(anyString())).thenReturn(Optional.empty());
        when(disambiguationPort.disambiguate("鸡兔同笼", null))
                .thenReturn(KpResolution.resolved("鸡兔同笼", KP_URI, "假设法", 88));

        KpResolution r = resolver.resolve("鸡兔同笼", STUDENT_ID);

        assertEquals(KpResolution.STATUS_RESOLVED, r.getStatus());
        assertEquals(KP_URI, r.getUri());
        assertEquals(88, r.getConfidence());
        // 冷启动（题型库无先验）→ 观测标 WEAK
        verify(obsRepository).upsert(argThat(o -> "WEAK".equals(o.getStatus().name())));
    }

    @Test
    @DisplayName("LLM 低置信/无候选 → PENDING，携带候选")
    void lowConfidencePending() {
        when(kgRepository.findByLabel(anyString())).thenReturn(Optional.empty());
        when(kgRepository.findByLabelLike(anyString())).thenReturn(Optional.empty());
        when(kgRepository.findByLabelLikeList("牛吃草")).thenReturn(List.of(kp(KP_URI, "假设法")));
        when(disambiguationPort.disambiguate("牛吃草", null)).thenReturn(null);

        KpResolution r = resolver.resolve("牛吃草", STUDENT_ID);

        assertEquals(KpResolution.STATUS_PENDING, r.getStatus());
        assertNull(r.getUri());
        assertFalse(r.getCandidates().isEmpty());
        verify(obsRepository).upsert(argThat(o -> "PENDING".equals(o.getStatus().name())));
    }

    @Test
    @DisplayName("空 label → PENDING，不查库")
    void blankLabel() {
        KpResolution r = resolver.resolve("  ", STUDENT_ID);

        assertEquals(KpResolution.STATUS_PENDING, r.getStatus());
        verify(kgRepository, never()).findByLabel(anyString());
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
