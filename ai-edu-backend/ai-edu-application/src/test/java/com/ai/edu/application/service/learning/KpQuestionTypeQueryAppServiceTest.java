package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.PageDTO;
import com.ai.edu.application.dto.learning.QuestionTypeKpDTO;
import com.ai.edu.application.dto.learning.QuestionTypePageItemDTO;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 题型库查询应用服务测试（mock 仓储），覆盖 test.md QTP-001/003/004。
 */
class KpQuestionTypeQueryAppServiceTest {

    private KpQuestionTypeQueryAppService service;
    private QuestionTypeRepository questionTypeRepository;
    private QuestionTypeKpRepository questionTypeKpRepository;
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    @BeforeEach
    void setUp() {
        service = new KpQuestionTypeQueryAppService();
        questionTypeRepository = mock(QuestionTypeRepository.class);
        questionTypeKpRepository = mock(QuestionTypeKpRepository.class);
        kgKnowledgePointRepository = mock(KgKnowledgePointRepository.class);
        service.setQuestionTypeRepository(questionTypeRepository);
        service.setQuestionTypeKpRepository(questionTypeKpRepository);
        service.setKgKnowledgePointRepository(kgKnowledgePointRepository);
    }

    @Test
    @DisplayName("page — 分页列题型返回 items/total/page/size")
    void page_shouldReturnPagedTypes() {
        QuestionType qt = QuestionType.restore(1L, "鸡兔同笼", QuestionTypeStatus.STABLE, null, 10, 42,
                null, LocalDateTime.now(), LocalDateTime.now());
        when(questionTypeRepository.count()).thenReturn(128L);
        when(questionTypeRepository.findPage(0, 20)).thenReturn(List.of(qt));

        PageDTO<QuestionTypePageItemDTO> result = service.page(1, 20);

        assertEquals(128L, result.getTotal());
        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getItems().get(0).getId());
        assertEquals("鸡兔同笼", result.getItems().get(0).getTopicLabel());
        assertEquals("STABLE", result.getItems().get(0).getStatus());
        assertEquals(42, result.getItems().get(0).getHitCount());
        assertEquals(1, result.getPage());
        assertEquals(20, result.getSize());
    }

    @Test
    @DisplayName("listKnowledgePoints — 反查 kpLabel 并透传 gradeRange/ratio/hitCount")
    void listKnowledgePoints_shouldResolveKpLabel() {
        QuestionType qt = QuestionType.restore(1L, "鸡兔同笼", QuestionTypeStatus.STABLE, null, 10, 42,
                null, LocalDateTime.now(), LocalDateTime.now());
        QuestionTypeKp kp = QuestionTypeKp.restore(2L, 1L, "kp-uri-1", "7-8", 8, 34, 0.8,
                LocalDateTime.now(), LocalDateTime.now());
        when(questionTypeRepository.findById(1L)).thenReturn(Optional.of(qt));
        when(questionTypeKpRepository.findByQuestionTypeId(1L)).thenReturn(List.of(kp));
        when(kgKnowledgePointRepository.findByUris(List.of("kp-uri-1")))
                .thenReturn(List.of(KgKnowledgePoint.create("kp-uri-1", "二元一次方程组")));

        List<QuestionTypeKpDTO> result = service.listKnowledgePoints(1L);

        assertEquals(1, result.size());
        assertEquals("kp-uri-1", result.get(0).getKpUri());
        assertEquals("二元一次方程组", result.get(0).getKpLabel());
        assertEquals("7-8", result.get(0).getGradeRange());
        assertEquals(0.8, result.get(0).getRatio(), 0.0001);
        assertEquals(34, result.get(0).getHitCount());
    }

    @Test
    @DisplayName("listKnowledgePoints — 题型不存在抛 10002")
    void listKnowledgePoints_notFound_shouldThrow() {
        when(questionTypeRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.listKnowledgePoints(999L));
        assertEquals("10002", ex.getCode());
    }
}
