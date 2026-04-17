package com.ai.edu.application.service;

import com.ai.edu.application.dto.kg.ChapterTreeNode;
import com.ai.edu.application.dto.kg.KgKnowledgePointDetailDTO;
import com.ai.edu.application.dto.kg.KgTextbookDTO;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KgNavigationAppService 单元测试
 *
 * 测试目标：Mock Repository，验证导航查询逻辑
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class KgNavigationAppServiceTest {

    @Mock
    private KgTextbookRepository kgTextbookRepository;
    @Mock
    private KgChapterRepository kgChapterRepository;
    @Mock
    private KgSectionRepository kgSectionRepository;
    @Mock
    private KgKnowledgePointRepository kgKnowledgePointRepository;
    @Mock
    private KgTextbookChapterRepository kgTextbookChapterRepository;
    @Mock
    private KgChapterSectionRepository kgChapterSectionRepository;
    @Mock
    private KgSectionKPRepository kgSectionKPRepository;

    @InjectMocks
    private KgNavigationAppService kgNavigationAppService;

    // ==================== 6.8.1 getTextbooks ====================

    @Test
    @Order(1)
    @DisplayName("getTextbooks 全部查询 — 无参数时应返回所有教材")
    void getTextbooks_noFilter_shouldReturnAll() {
        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:tb1", "教材1", "七年级", "junior", "math"),
                KgTextbook.create("uri:tb2", "教材2", "八年级", "junior", "english")
        );
        when(kgTextbookRepository.findAllActive()).thenReturn(textbooks);

        List<KgTextbookDTO> result = kgNavigationAppService.getTextbooks(null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("uri:tb1", result.get(0).getUri());
        assertEquals("教材1", result.get(0).getLabel());
        assertEquals("七年级", result.get(0).getGrade());
        assertEquals("junior", result.get(0).getPhase());
        assertEquals("math", result.get(0).getSubject());
        assertEquals("active", result.get(0).getStatus());
        verify(kgTextbookRepository).findAllActive();
    }

    @Test
    @Order(2)
    @DisplayName("getTextbooks 按 subject 过滤")
    void getTextbooks_bySubject_shouldFilter() {
        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:tb1", "数学教材1", "七年级", "junior", "math")
        );
        when(kgTextbookRepository.findBySubject("math")).thenReturn(textbooks);

        List<KgTextbookDTO> result = kgNavigationAppService.getTextbooks("math", null);

        assertEquals(1, result.size());
        assertEquals("math", result.get(0).getSubject());
        verify(kgTextbookRepository).findBySubject("math");
    }

    @Test
    @Order(3)
    @DisplayName("getTextbooks 按 subject+phase 过滤")
    void getTextbooks_bySubjectAndPhase_shouldFilter() {
        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:tb1", "初中数学", "七年级", "junior", "math")
        );
        when(kgTextbookRepository.findBySubjectAndPhase("math", "junior")).thenReturn(textbooks);

        List<KgTextbookDTO> result = kgNavigationAppService.getTextbooks("math", "junior");

        assertEquals(1, result.size());
        assertEquals("math", result.get(0).getSubject());
        assertEquals("junior", result.get(0).getPhase());
        verify(kgTextbookRepository).findBySubjectAndPhase("math", "junior");
    }

    @Test
    @Order(4)
    @DisplayName("getTextbooks 无匹配数据应返回空列表")
    void getTextbooks_noMatch_shouldReturnEmpty() {
        when(kgTextbookRepository.findBySubject("physics")).thenReturn(List.of());

        List<KgTextbookDTO> result = kgNavigationAppService.getTextbooks("physics", null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 6.8.2 getChaptersByTextbook ====================

    @Test
    @Order(5)
    @DisplayName("getChaptersByTextbook 教材存在 — 应返回章节树含小节和知识点计数")
    void getChaptersByTextbook_textbookExists_shouldReturnChapterTree() {
        String tbUri = "uri:tb1";
        KgTextbook textbook = KgTextbook.create(tbUri, "数学教材", "七年级", "junior", "math");
        when(kgTextbookRepository.findByUri(tbUri)).thenReturn(Optional.of(textbook));

        // 教材-章节关系
        List<KgTextbookChapter> tbChRels = List.of(
                KgTextbookChapter.create(tbUri, "uri:ch1", 1),
                KgTextbookChapter.create(tbUri, "uri:ch2", 2)
        );
        when(kgTextbookChapterRepository.findByTextbookUri(tbUri)).thenReturn(tbChRels);

        // 章节实体
        List<KgChapter> chapters = List.of(
                KgChapter.create("uri:ch1", "第一章"),
                KgChapter.create("uri:ch2", "第二章")
        );
        when(kgChapterRepository.findByUris(anyList())).thenReturn(chapters);

        // 章节-小节关系
        List<KgChapterSection> chSecRels = List.of(
                KgChapterSection.create("uri:ch1", "uri:sec1", 1),
                KgChapterSection.create("uri:ch1", "uri:sec2", 2),
                KgChapterSection.create("uri:ch2", "uri:sec3", 1)
        );
        when(kgChapterSectionRepository.findAllActive()).thenReturn(chSecRels);

        // 小节实体
        List<KgSection> sections = List.of(
                KgSection.create("uri:sec1", "第一节"),
                KgSection.create("uri:sec2", "第二节"),
                KgSection.create("uri:sec3", "第三节")
        );
        when(kgSectionRepository.findByUris(anyList())).thenReturn(sections);

        // 小节-知识点关系
        List<KgSectionKP> secKpRels = List.of(
                KgSectionKP.create("uri:sec1", "uri:kp1", 1),
                KgSectionKP.create("uri:sec1", "uri:kp2", 2),
                KgSectionKP.create("uri:sec2", "uri:kp3", 1),
                KgSectionKP.create("uri:sec3", "uri:kp4", 1),
                KgSectionKP.create("uri:sec3", "uri:kp5", 2),
                KgSectionKP.create("uri:sec3", "uri:kp6", 3)
        );
        when(kgSectionKPRepository.findAllActive()).thenReturn(secKpRels);

        List<ChapterTreeNode> result = kgNavigationAppService.getChaptersByTextbook(tbUri);

        assertNotNull(result);
        assertEquals(2, result.size());

        // 第一章
        ChapterTreeNode ch1 = result.get(0);
        assertEquals("uri:ch1", ch1.getUri());
        assertEquals("第一章", ch1.getLabel());
        assertEquals(1, ch1.getOrderIndex());
        assertNotNull(ch1.getSections());
        assertEquals(2, ch1.getSections().size());
        assertEquals("uri:sec1", ch1.getSections().get(0).getUri());
        assertEquals(2, ch1.getSections().get(0).getKnowledgePointCount());
        assertEquals("uri:sec2", ch1.getSections().get(1).getUri());
        assertEquals(1, ch1.getSections().get(1).getKnowledgePointCount());

        // 第二章
        ChapterTreeNode ch2 = result.get(1);
        assertEquals("uri:ch2", ch2.getUri());
        assertEquals("第二章", ch2.getLabel());
        assertEquals(2, ch2.getOrderIndex());
        assertEquals(1, ch2.getSections().size());
        assertEquals(3, ch2.getSections().get(0).getKnowledgePointCount());
    }

    @Test
    @Order(6)
    @DisplayName("getChaptersByTextbook 教材不存在 — 应抛 KG_TEXTBOOK_NOT_FOUND")
    void getChaptersByTextbook_textbookNotFound_shouldThrow() {
        when(kgTextbookRepository.findByUri("uri:notexist")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgNavigationAppService.getChaptersByTextbook("uri:notexist")
        );
        assertEquals("70001", ex.getCode());
        assertTrue(ex.getMessage().contains("教材不存在"));
    }

    @Test
    @Order(7)
    @DisplayName("getChaptersByTextbook 无章节关系 — 应返回空列表")
    void getChaptersByTextbook_noChapters_shouldReturnEmpty() {
        String tbUri = "uri:tb1";
        when(kgTextbookRepository.findByUri(tbUri)).thenReturn(Optional.of(KgTextbook.create(tbUri, "教材", "七年级", "junior", "math")));
        when(kgTextbookChapterRepository.findByTextbookUri(tbUri)).thenReturn(List.of());

        List<ChapterTreeNode> result = kgNavigationAppService.getChaptersByTextbook(tbUri);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @Order(8)
    @DisplayName("getChaptersByTextbook 空章节自动过滤 — 章节无小节时应被排除")
    void getChaptersByTextbook_emptyChapter_shouldBeFiltered() {
        String tbUri = "uri:tb1";
        when(kgTextbookRepository.findByUri(tbUri)).thenReturn(Optional.of(KgTextbook.create(tbUri, "教材", "七年级", "junior", "math")));

        // 两个章节
        when(kgTextbookChapterRepository.findByTextbookUri(tbUri)).thenReturn(List.of(
                KgTextbookChapter.create(tbUri, "uri:ch1", 1),
                KgTextbookChapter.create(tbUri, "uri:ch2", 2)
        ));
        when(kgChapterRepository.findByUris(anyList())).thenReturn(List.of(
                KgChapter.create("uri:ch1", "有内容的章"),
                KgChapter.create("uri:ch2", "空章")
        ));

        // 只有 ch1 有小节
        when(kgChapterSectionRepository.findAllActive()).thenReturn(List.of(
                KgChapterSection.create("uri:ch1", "uri:sec1", 1)
        ));
        when(kgSectionRepository.findByUris(anyList())).thenReturn(List.of(
                KgSection.create("uri:sec1", "第一节")
        ));
        when(kgSectionKPRepository.findAllActive()).thenReturn(List.of());

        List<ChapterTreeNode> result = kgNavigationAppService.getChaptersByTextbook(tbUri);

        assertEquals(1, result.size());
        assertEquals("uri:ch1", result.get(0).getUri());
    }

    // ==================== 6.8.3 getKnowledgePointsBySection ====================

    @Test
    @Order(9)
    @DisplayName("getKnowledgePointsBySection 小节存在 — 应返回知识点列表含父级信息")
    void getKnowledgePointsBySection_sectionExists_shouldReturnKpListWithParents() {
        String secUri = "uri:sec1";

        // 小节-知识点关系
        when(kgSectionKPRepository.findBySectionUri(secUri)).thenReturn(List.of(
                KgSectionKP.create(secUri, "uri:kp1", 1),
                KgSectionKP.create(secUri, "uri:kp2", 2)
        ));

        // 知识点实体
        List<KgKnowledgePoint> kps = List.of(
                KgKnowledgePoint.create("uri:kp1", "知识点1"),
                KgKnowledgePoint.create("uri:kp2", "知识点2")
        );
        when(kgKnowledgePointRepository.findByUris(anyList())).thenReturn(kps);

        // 小节实体
        KgSection section = KgSection.create(secUri, "第一节");
        when(kgSectionRepository.findByUri(secUri)).thenReturn(Optional.of(section));

        // 父级章节
        List<KgChapterSection> chSecRels = List.of(
                KgChapterSection.create("uri:ch1", secUri, 1)
        );
        when(kgChapterSectionRepository.findBySectionUri(secUri)).thenReturn(chSecRels);
        KgChapter chapter = KgChapter.create("uri:ch1", "第一章");
        when(kgChapterRepository.findByUri("uri:ch1")).thenReturn(Optional.of(chapter));

        List<KgKnowledgePointDetailDTO> result = kgNavigationAppService.getKnowledgePointsBySection(secUri);

        assertNotNull(result);
        assertEquals(2, result.size());

        KgKnowledgePointDetailDTO kp1 = result.get(0);
        assertEquals("uri:kp1", kp1.getUri());
        assertEquals("知识点1", kp1.getLabel());
        // 验证 2 层父级
        assertEquals(secUri, kp1.getSectionUri());
        assertEquals("第一节", kp1.getSectionLabel());
        assertEquals("uri:ch1", kp1.getChapterUri());
        assertEquals("第一章", kp1.getChapterLabel());
    }

    @Test
    @Order(10)
    @DisplayName("getKnowledgePointsBySection 小节不存在 — 应返回空列表")
    void getKnowledgePointsBySection_noRelations_shouldReturnEmpty() {
        when(kgSectionKPRepository.findBySectionUri("uri:empty")).thenReturn(List.of());

        List<KgKnowledgePointDetailDTO> result = kgNavigationAppService.getKnowledgePointsBySection("uri:empty");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 6.8.4 getKnowledgePointDetail ====================

    @Test
    @Order(11)
    @DisplayName("getKnowledgePointDetail 知识点存在 — 应返回含 2 层父级")
    void getKnowledgePointDetail_exists_shouldReturnWithTwoParentLevels() {
        String kpUri = "uri:kp1";
        String secUri = "uri:sec1";
        String chUri = "uri:ch1";

        // 知识点
        KgKnowledgePoint kp = KgKnowledgePoint.create(kpUri, "勾股定理");
        when(kgKnowledgePointRepository.findByUri(kpUri)).thenReturn(Optional.of(kp));

        // 父级小节
        when(kgSectionKPRepository.findByKpUri(kpUri)).thenReturn(List.of(
                KgSectionKP.create(secUri, kpUri, 1)
        ));
        KgSection section = KgSection.create(secUri, "几何基础");
        when(kgSectionRepository.findByUri(secUri)).thenReturn(Optional.of(section));

        // 爷爷级章节
        when(kgChapterSectionRepository.findBySectionUri(secUri)).thenReturn(List.of(
                KgChapterSection.create(chUri, secUri, 1)
        ));
        KgChapter chapter = KgChapter.create(chUri, "几何");
        when(kgChapterRepository.findByUri(chUri)).thenReturn(Optional.of(chapter));

        KgKnowledgePointDetailDTO result = kgNavigationAppService.getKnowledgePointDetail(kpUri);

        assertNotNull(result);
        assertEquals(kpUri, result.getUri());
        assertEquals("勾股定理", result.getLabel());
        // 2 层父级
        assertEquals(secUri, result.getSectionUri());
        assertEquals("几何基础", result.getSectionLabel());
        assertEquals(chUri, result.getChapterUri());
        assertEquals("几何", result.getChapterLabel());
    }

    @Test
    @Order(12)
    @DisplayName("getKnowledgePointDetail 知识点不存在 — 应抛 KG_KNOWLEDGE_POINT_NOT_FOUND")
    void getKnowledgePointDetail_notFound_shouldThrow() {
        when(kgKnowledgePointRepository.findByUri("uri:notexist")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgNavigationAppService.getKnowledgePointDetail("uri:notexist")
        );
        assertEquals("70003", ex.getCode());
        assertTrue(ex.getMessage().contains("知识点不存在"));
    }

    @Test
    @Order(13)
    @DisplayName("getKnowledgePointDetail 无父级关系 — 应返回知识点但父级为 null")
    void getKnowledgePointDetail_noParents_shouldReturnWithNullParents() {
        String kpUri = "uri:kp1";
        when(kgKnowledgePointRepository.findByUri(kpUri)).thenReturn(Optional.of(
                KgKnowledgePoint.create(kpUri, "孤立知识点")
        ));
        when(kgSectionKPRepository.findByKpUri(kpUri)).thenReturn(List.of());

        KgKnowledgePointDetailDTO result = kgNavigationAppService.getKnowledgePointDetail(kpUri);

        assertNotNull(result);
        assertEquals(kpUri, result.getUri());
        assertNull(result.getSectionUri());
        assertNull(result.getSectionLabel());
        assertNull(result.getChapterUri());
        assertNull(result.getChapterLabel());
    }

    @Test
    @Order(14)
    @DisplayName("getKnowledgePointDetail 有小节但无章节 — 应返回小节但章节为 null")
    void getKnowledgePointDetail_sectionButNoChapter_shouldReturnSectionOnly() {
        String kpUri = "uri:kp1";
        String secUri = "uri:sec1";

        when(kgKnowledgePointRepository.findByUri(kpUri)).thenReturn(Optional.of(
                KgKnowledgePoint.create(kpUri, "知识点")
        ));
        when(kgSectionKPRepository.findByKpUri(kpUri)).thenReturn(List.of(
                KgSectionKP.create(secUri, kpUri, 1)
        ));
        when(kgSectionRepository.findByUri(secUri)).thenReturn(Optional.of(
                KgSection.create(secUri, "小节")
        ));
        // 无章节关联
        when(kgChapterSectionRepository.findBySectionUri(secUri)).thenReturn(List.of());

        KgKnowledgePointDetailDTO result = kgNavigationAppService.getKnowledgePointDetail(kpUri);

        assertNotNull(result);
        assertEquals(secUri, result.getSectionUri());
        assertEquals("小节", result.getSectionLabel());
        assertNull(result.getChapterUri());
        assertNull(result.getChapterLabel());
    }

    // ==================== 6.8.5 DTO 转换验证 ====================

    @Test
    @Order(15)
    @DisplayName("getTextbookDetail 应验证 Entity → DTO 字段映射")
    void getTextbookDetail_shouldMapEntityToDto() {
        KgTextbook textbook = KgTextbook.create("uri:tb1", "测试教材", "七年级", "junior", "math");
        when(kgTextbookRepository.findByUri("uri:tb1")).thenReturn(Optional.of(textbook));

        KgTextbookDTO result = kgNavigationAppService.getTextbookDetail("uri:tb1");

        assertEquals("uri:tb1", result.getUri());
        assertEquals("测试教材", result.getLabel());
        assertEquals("七年级", result.getGrade());
        assertEquals("junior", result.getPhase());
        assertEquals("math", result.getSubject());
        assertEquals("active", result.getStatus());
    }

    @Test
    @Order(16)
    @DisplayName("getTextbookDetail 教材不存在 — 应抛 KG_TEXTBOOK_NOT_FOUND")
    void getTextbookDetail_notFound_shouldThrow() {
        when(kgTextbookRepository.findByUri("uri:notexist")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgNavigationAppService.getTextbookDetail("uri:notexist")
        );
        assertEquals("70001", ex.getCode());
        assertTrue(ex.getMessage().contains("教材不存在"));
    }

    @Test
    @Order(17)
    @DisplayName("getKnowledgePointsBySection 小节实体不存在 — 应跳过该知识点")
    void getKnowledgePointsBySection_sectionEntityMissing_shouldSkipKp() {
        String secUri = "uri:sec1";

        when(kgSectionKPRepository.findBySectionUri(secUri)).thenReturn(List.of(
                KgSectionKP.create(secUri, "uri:kp1", 1),
                KgSectionKP.create(secUri, "uri:kp2", 2)
        ));
        // 只返回一个知识点
        when(kgKnowledgePointRepository.findByUris(anyList())).thenReturn(List.of(
                KgKnowledgePoint.create("uri:kp1", "知识点1")
        ));
        when(kgSectionRepository.findByUri(secUri)).thenReturn(Optional.of(KgSection.create(secUri, "小节")));
        when(kgChapterSectionRepository.findBySectionUri(secUri)).thenReturn(List.of());

        List<KgKnowledgePointDetailDTO> result = kgNavigationAppService.getKnowledgePointsBySection(secUri);

        // kp2 不存在于 kpMap 中，应被跳过
        assertEquals(1, result.size());
        assertEquals("uri:kp1", result.get(0).getUri());
    }

    @Test
    @Order(18)
    @DisplayName("getKnowledgePointsBySection 小节和章节都为 null — 父级字段应为 null")
    void getKnowledgePointsBySection_noSectionOrChapter_shouldHaveNullParents() {
        String secUri = "uri:sec1";

        when(kgSectionKPRepository.findBySectionUri(secUri)).thenReturn(List.of(
                KgSectionKP.create(secUri, "uri:kp1", 1)
        ));
        when(kgKnowledgePointRepository.findByUris(anyList())).thenReturn(List.of(
                KgKnowledgePoint.create("uri:kp1", "知识点1")
        ));
        // 小节和章节都不存在
        when(kgSectionRepository.findByUri(secUri)).thenReturn(Optional.empty());
        when(kgChapterSectionRepository.findBySectionUri(secUri)).thenReturn(List.of());

        List<KgKnowledgePointDetailDTO> result = kgNavigationAppService.getKnowledgePointsBySection(secUri);

        assertEquals(1, result.size());
        assertNull(result.get(0).getSectionUri());
        assertNull(result.get(0).getSectionLabel());
        assertNull(result.get(0).getChapterUri());
        assertNull(result.get(0).getChapterLabel());
    }
}
