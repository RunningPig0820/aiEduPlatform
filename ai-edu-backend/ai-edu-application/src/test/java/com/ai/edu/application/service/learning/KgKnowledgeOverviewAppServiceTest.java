package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.KgTreeNodeDTO;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.valueobject.KgTreeNode;
import com.ai.edu.domain.edukg.repository.KgOverviewTreeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识点总览下钻应用服务测试（mock 仓储）——点击式教材树：学段→课本→章节→小节→知识点，每次单层查询。
 */
class KgKnowledgeOverviewAppServiceTest {

    private KgKnowledgeOverviewAppService service;
    private KgOverviewTreeRepository treeRepository;

    @BeforeEach
    void setUp() {
        service = new KgKnowledgeOverviewAppService();
        treeRepository = mock(KgOverviewTreeRepository.class);
        service.setKgOverviewTreeRepository(treeRepository);
    }

    private KgTreeNode node(String uri, String label, int orderIndex) {
        return KgTreeNode.builder().uri(uri).label(label).orderIndex(orderIndex).build();
    }

    @Test
    @DisplayName("gradesByStage — code→中文查库（stage 映射），返回年级节点")
    void gradesByStage() {
        when(treeRepository.findGradesByStage("小学")).thenReturn(List.of(
                node("一年级", "一年级", 0),
                node("二年级", "二年级", 1)));

        List<KgTreeNodeDTO> items = service.gradesByStage("primary");

        assertEquals(2, items.size());
        assertEquals("一年级", items.get(0).getUri());
        assertEquals("一年级", items.get(0).getLabel());
        verify(treeRepository).findGradesByStage("小学");
    }

    @Test
    @DisplayName("textbooksByStage — 学段+年级→课本列表（stage 映射），返回课本节点")
    void textbooksByStage_shouldMapStage() {
        when(treeRepository.findTextbooksByStage("小学", "一年级")).thenReturn(List.of(
                node("tb-1", "人教版小学数学一年级上册", 0),
                node("tb-2", "人教版小学数学一年级下册", 1)));

        List<KgTreeNodeDTO> items = service.textbooksByStage("primary", "一年级");

        assertEquals(2, items.size());
        assertEquals("tb-1", items.get(0).getUri());
        assertEquals("人教版小学数学一年级上册", items.get(0).getLabel());
        assertEquals(0, items.get(0).getOrderIndex());
        verify(treeRepository).findTextbooksByStage("小学", "一年级");
    }

    @Test
    @DisplayName("chaptersByTextbook — 透传章节节点（课本→章节 单层查询）")
    void chaptersByTextbook() {
        when(treeRepository.findChaptersByTextbookUri("tb-1")).thenReturn(List.of(
                node("ch-1", "一 数一数", 0)));

        List<KgTreeNodeDTO> items = service.chaptersByTextbook("tb-1");

        assertEquals(1, items.size());
        assertEquals("ch-1", items.get(0).getUri());
        assertEquals("一 数一数", items.get(0).getLabel());
        verify(treeRepository).findChaptersByTextbookUri("tb-1");
    }

    @Test
    @DisplayName("sectionsByChapter — 透传小节节点（章节→小节 单层查询）")
    void sectionsByChapter() {
        when(treeRepository.findSectionsByChapterUri("ch-1")).thenReturn(List.of(
                node("sec-1", "1.1 位置", 0)));

        List<KgTreeNodeDTO> items = service.sectionsByChapter("ch-1");

        assertEquals(1, items.size());
        assertEquals("sec-1", items.get(0).getUri());
        assertEquals("1.1 位置", items.get(0).getLabel());
        verify(treeRepository).findSectionsByChapterUri("ch-1");
    }

    @Test
    @DisplayName("knowledgePointsBySection — 透传知识点节点（小节→知识点 单层查询）")
    void knowledgePointsBySection() {
        when(treeRepository.findKnowledgePointsBySectionUri("sec-1")).thenReturn(List.of(
                node("kp-1", "位置的认识", 0)));

        List<KgTreeNodeDTO> items = service.knowledgePointsBySection("sec-1");

        assertEquals(1, items.size());
        assertEquals("kp-1", items.get(0).getUri());
        assertEquals("位置的认识", items.get(0).getLabel());
        verify(treeRepository).findKnowledgePointsBySectionUri("sec-1");
    }

    @Test
    @DisplayName("textbooksByStage — 非法 stage 抛 10003")
    void invalidStage_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.textbooksByStage("invalid", "一年级"));
        assertEquals("10003", ex.getCode());
    }
}
