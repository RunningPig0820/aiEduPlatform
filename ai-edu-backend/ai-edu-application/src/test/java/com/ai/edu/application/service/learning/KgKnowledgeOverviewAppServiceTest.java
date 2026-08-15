package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.KgKnowledgePointPageItemDTO;
import com.ai.edu.application.dto.learning.PageDTO;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.valueobject.KgKpPlacement;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 知识点总览应用服务测试（mock 仓储），覆盖 test.md OVW-001/004。
 */
class KgKnowledgeOverviewAppServiceTest {

    private KgKnowledgeOverviewAppService service;
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    @BeforeEach
    void setUp() {
        service = new KgKnowledgeOverviewAppService();
        kgKnowledgePointRepository = mock(KgKnowledgePointRepository.class);
        service.setKgKnowledgePointRepository(kgKnowledgePointRepository);
    }

    @Test
    @DisplayName("page — 按学段分页返回 kpUri/kpLabel/stage/chapter/section + total")
    void page_shouldReturnPagedPoints() {
        KgKpPlacement p = KgKpPlacement.builder()
                .kpUri("kp-uri-1").kpLabel("二元一次方程组").stage("middle")
                .chapterLabel("二元一次方程组").sectionLabel("8.1 二元一次方程组").build();
        when(kgKnowledgePointRepository.countByStage("middle")).thenReturn(532L);
        when(kgKnowledgePointRepository.findPageByStage("middle", 0, 20)).thenReturn(List.of(p));

        PageDTO<KgKnowledgePointPageItemDTO> result = service.page("middle", 1, 20);

        assertEquals(532L, result.getTotal());
        assertEquals(1, result.getItems().size());
        assertEquals("kp-uri-1", result.getItems().get(0).getKpUri());
        assertEquals("二元一次方程组", result.getItems().get(0).getKpLabel());
        assertEquals("middle", result.getItems().get(0).getStage());
        assertEquals("二元一次方程组", result.getItems().get(0).getChapterLabel());
        assertEquals("8.1 二元一次方程组", result.getItems().get(0).getSectionLabel());
    }

    @Test
    @DisplayName("page — 非法 stage 抛 10003")
    void invalidStage_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.page("invalid", 1, 20));
        assertEquals("10003", ex.getCode());
    }

    @Test
    @DisplayName("page — 空 stage 抛 10003")
    void nullStage_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.page(null, 1, 20));
        assertEquals("10003", ex.getCode());
    }
}
