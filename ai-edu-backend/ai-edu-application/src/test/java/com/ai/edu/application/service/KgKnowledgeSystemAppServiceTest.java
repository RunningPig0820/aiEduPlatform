package com.ai.edu.application.service;

import com.ai.edu.application.dto.kg.KgGradeSystemDTO;
import com.ai.edu.application.dto.kg.StatsDTO;
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
 * KgKnowledgeSystemAppService 单元测试
 *
 * 测试目标：Mock Repository，验证知识体系构建
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class KgKnowledgeSystemAppServiceTest {

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
    private KgKnowledgeSystemAppService kgKnowledgeSystemAppService;

    // ==================== 6.9.1 getGradeSystem ====================

    @Test
    @Order(1)
    @DisplayName("getGradeSystem 按 subject 分组 — 应返回正确的知识体系结构")
    void getGradeSystem_bySubject_shouldReturnGroupedBySubject() {
        String grade = "七年级";

        // 两本数学教材
        List<KgTextbook> allTextbooks = List.of(
                KgTextbook.create("uri:tb1", "数学上册", grade, "junior", "人教版", "math"),
                KgTextbook.create("uri:tb2", "数学下册", grade, "junior", "人教版", "math")
        );
        when(kgTextbookRepository.findAllActive()).thenReturn(allTextbooks);

        // 教材-章节关系
        when(kgTextbookChapterRepository.findByTextbookUri("uri:tb1")).thenReturn(List.of(
                KgTextbookChapter.create("uri:tb1", "uri:ch1", 1)
        ));
        when(kgTextbookChapterRepository.findByTextbookUri("uri:tb2")).thenReturn(List.of(
                KgTextbookChapter.create("uri:tb2", "uri:ch2", 1)
        ));

        // 章节实体
        when(kgChapterRepository.findByUris(anyList())).thenReturn(List.of(
                KgChapter.create("uri:ch1", "第一章"),
                KgChapter.create("uri:ch2", "第二章")
        ));

        // 章节-小节关系
        when(kgChapterSectionRepository.findAllActive()).thenReturn(List.of(
                KgChapterSection.create("uri:ch1", "uri:sec1", 1),
                KgChapterSection.create("uri:ch2", "uri:sec2", 1)
        ));

        // 小节实体
        when(kgSectionRepository.findByUris(anyList())).thenReturn(List.of(
                KgSection.create("uri:sec1", "第一节"),
                KgSection.create("uri:sec2", "第二节")
        ));

        // 小节-知识点关系
        KgKnowledgePoint kp1 = KgKnowledgePoint.create("uri:kp1", "知识点1");
        KgKnowledgePoint kp2 = KgKnowledgePoint.create("uri:kp2", "知识点2");
        when(kgSectionKPRepository.findAllActive()).thenReturn(List.of(
                KgSectionKP.create("uri:sec1", "uri:kp1", 1),
                KgSectionKP.create("uri:sec2", "uri:kp2", 1)
        ));
        when(kgKnowledgePointRepository.findByUri("uri:kp1")).thenReturn(Optional.of(kp1));
        when(kgKnowledgePointRepository.findByUri("uri:kp2")).thenReturn(Optional.of(kp2));

        KgGradeSystemDTO result = kgKnowledgeSystemAppService.getGradeSystem(grade, "subject");

        assertNotNull(result);
        assertEquals(grade, result.getGrade());
        assertEquals("subject", result.getGroupBy());
        assertEquals(1, result.getGroups().size());

        KgGradeSystemDTO.GroupDTO mathGroup = result.getGroups().get(0);
        assertEquals("math", mathGroup.getKey());
        assertEquals("数学", mathGroup.getLabel());
        assertEquals(2, mathGroup.getKnowledgePointCount());
        assertEquals(2, mathGroup.getChapters().size());
    }

    @Test
    @Order(2)
    @DisplayName("getGradeSystem 按 stage 分组 — 应返回 stage 分组结构")
    void getGradeSystem_byStage_shouldReturnGroupedByStage() {
        String grade = "七年级";
        List<KgTextbook> allTextbooks = List.of(
                KgTextbook.create("uri:tb1", "数学教材", grade, "middle", "人教版", "math"),
                KgTextbook.create("uri:tb2", "英语教材", grade, "middle", "人教版", "english")
        );
        when(kgTextbookRepository.findAllActive()).thenReturn(allTextbooks);

        // 教材-章节关系（必须有章节，否则 group 会被 continue 跳过）
        when(kgTextbookChapterRepository.findByTextbookUri("uri:tb1")).thenReturn(List.of(
                KgTextbookChapter.create("uri:tb1", "uri:ch1", 1)
        ));
        when(kgTextbookChapterRepository.findByTextbookUri("uri:tb2")).thenReturn(List.of(
                KgTextbookChapter.create("uri:tb2", "uri:ch2", 1)
        ));
        when(kgChapterRepository.findByUris(anyList())).thenReturn(List.of(
                KgChapter.create("uri:ch1", "第一章"),
                KgChapter.create("uri:ch2", "第二章")
        ));
        when(kgChapterSectionRepository.findAllActive()).thenReturn(List.of());
        when(kgSectionKPRepository.findAllActive()).thenReturn(List.of());

        KgGradeSystemDTO result = kgKnowledgeSystemAppService.getGradeSystem(grade, "stage");

        assertNotNull(result);
        assertEquals("stage", result.getGroupBy());
        assertEquals(1, result.getGroups().size());
        assertEquals("middle", result.getGroups().get(0).getKey());
        assertEquals("初中", result.getGroups().get(0).getLabel());
        assertEquals(2, result.getGroups().get(0).getChapters().size());
    }

    @Test
    @Order(3)
    @DisplayName("getGradeSystem 年级不存在 — 应返回空结构")
    void getGradeSystem_gradeNotFound_shouldReturnEmptyStructure() {
        when(kgTextbookRepository.findAllActive()).thenReturn(List.of());

        KgGradeSystemDTO result = kgKnowledgeSystemAppService.getGradeSystem("九年级", null);

        assertNotNull(result);
        assertEquals("九年级", result.getGrade());
        assertEquals("subject", result.getGroupBy());
        assertNotNull(result.getGroups());
        assertTrue(result.getGroups().isEmpty());
        assertEquals(0, result.getTotalKnowledgePoints());
    }

    @Test
    @Order(4)
    @DisplayName("getGradeSystem groupBy 为空 — 应默认 subject")
    void getGradeSystem_nullGroupBy_shouldDefaultToSubject() {
        when(kgTextbookRepository.findAllActive()).thenReturn(List.of());

        KgGradeSystemDTO result = kgKnowledgeSystemAppService.getGradeSystem("七年级", null);

        assertEquals("subject", result.getGroupBy());
    }

    @Test
    @Order(5)
    @DisplayName("getGradeSystem groupBy 为空字符串 — 应默认 subject")
    void getGradeSystem_blankGroupBy_shouldDefaultToSubject() {
        String grade = "七年级";
        // 需要提供非空教材列表，使代码走到 effectiveGroupBy 的逻辑（而非早期返回）
        when(kgTextbookRepository.findAllActive()).thenReturn(List.of(
                KgTextbook.create("uri:tb1", "教材", grade, "junior", "人教版", "math")
        ));
        when(kgTextbookChapterRepository.findByTextbookUri("uri:tb1")).thenReturn(List.of());
        when(kgChapterSectionRepository.findAllActive()).thenReturn(List.of());
        when(kgSectionKPRepository.findAllActive()).thenReturn(List.of());

        KgGradeSystemDTO result = kgKnowledgeSystemAppService.getGradeSystem(grade, "  ");

        assertEquals("subject", result.getGroupBy());
    }

    @Test
    @Order(6)
    @DisplayName("getGradeSystem 教材无章节 — 分组被跳过，返回 0 groups")
    void getGradeSystem_textbooksWithoutChapters_shouldHaveZeroKpCount() {
        String grade = "七年级";
        when(kgTextbookRepository.findAllActive()).thenReturn(List.of(
                KgTextbook.create("uri:tb1", "教材", grade, "junior", "人教版", "math")
        ));
        when(kgTextbookChapterRepository.findByTextbookUri("uri:tb1")).thenReturn(List.of());
        when(kgChapterSectionRepository.findAllActive()).thenReturn(List.of());
        when(kgSectionKPRepository.findAllActive()).thenReturn(List.of());

        KgGradeSystemDTO result = kgKnowledgeSystemAppService.getGradeSystem(grade, "subject");

        assertNotNull(result);
        // 无章节时 group 被 continue 跳过，groups 为空列表
        assertEquals(0, result.getGroups().size());
        assertEquals(0, result.getTotalKnowledgePoints());
    }

    // ==================== 6.9.2 getGradeStats ====================

    @Test
    @Order(7)
    @DisplayName("getGradeStats 应返回正确的统计数据")
    void getGradeStats_shouldReturnCorrectStats() {
        String grade = "七年级";

        // 两本教材
        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:tb1", "数学上册", grade, "junior", "人教版", "math"),
                KgTextbook.create("uri:tb2", "数学下册", grade, "junior", "人教版", "math")
        );
        when(kgTextbookRepository.findAllActive()).thenReturn(textbooks);

        // 教材-章节: 2 个章节
        when(kgTextbookChapterRepository.findAllActive()).thenReturn(List.of(
                KgTextbookChapter.create("uri:tb1", "uri:ch1", 1),
                KgTextbookChapter.create("uri:tb2", "uri:ch2", 1)
        ));

        // 章节-小节: 2 个小节
        when(kgChapterSectionRepository.findAllActive()).thenReturn(List.of(
                KgChapterSection.create("uri:ch1", "uri:sec1", 1),
                KgChapterSection.create("uri:ch2", "uri:sec2", 1)
        ));

        // 小节-知识点: 3 个知识点
        when(kgSectionKPRepository.findAllActive()).thenReturn(List.of(
                KgSectionKP.create("uri:sec1", "uri:kp1", 1),
                KgSectionKP.create("uri:sec1", "uri:kp2", 2),
                KgSectionKP.create("uri:sec2", "uri:kp3", 1)
        ));

        // 知识点实体（不同难度）
        KgKnowledgePoint kp1 = KgKnowledgePoint.create("uri:kp1", "知识点1");
        setField(kp1, "difficulty", "easy");
        KgKnowledgePoint kp2 = KgKnowledgePoint.create("uri:kp2", "知识点2");
        setField(kp2, "difficulty", "medium");
        KgKnowledgePoint kp3 = KgKnowledgePoint.create("uri:kp3", "知识点3");
        setField(kp3, "difficulty", "easy");

        when(kgKnowledgePointRepository.findByUris(anyList())).thenReturn(List.of(kp1, kp2, kp3));

        StatsDTO result = kgKnowledgeSystemAppService.getGradeStats(grade);

        assertNotNull(result);
        assertEquals(grade, result.getGrade());
        assertEquals(3, result.getTotalKnowledgePoints());
        assertEquals(2, result.getTotalTextbooks());
        assertEquals(2, result.getTotalChapters());
        assertEquals(2, result.getTotalSections());

        // 难度分布: easy=2, medium=1
        Map<String, Integer> diffDist = result.getDifficultyDistribution();
        assertEquals(2, diffDist.get("easy"));
        assertEquals(1, diffDist.get("medium"));
    }

    @Test
    @Order(8)
    @DisplayName("getGradeStats 年级不存在 — 应返回空结构")
    void getGradeStats_gradeNotFound_shouldReturnEmptyStructure() {
        when(kgTextbookRepository.findAllActive()).thenReturn(List.of());

        StatsDTO result = kgKnowledgeSystemAppService.getGradeStats("九年级");

        assertNotNull(result);
        assertEquals("九年级", result.getGrade());
        assertEquals(0, result.getTotalKnowledgePoints());
        assertEquals(0, result.getTotalTextbooks());
        assertEquals(0, result.getTotalChapters());
        assertEquals(0, result.getTotalSections());
        assertNotNull(result.getDifficultyDistribution());
        assertTrue(result.getDifficultyDistribution().isEmpty());
    }

    @Test
    @Order(9)
    @DisplayName("getGradeStats 难度为 null — 应归类为 unknown")
    void getGradeStats_nullDifficulty_shouldBeUnknown() {
        String grade = "七年级";
        when(kgTextbookRepository.findAllActive()).thenReturn(List.of(
                KgTextbook.create("uri:tb1", "教材", grade, "junior", "人教版", "math")
        ));
        when(kgTextbookChapterRepository.findAllActive()).thenReturn(List.of(
                KgTextbookChapter.create("uri:tb1", "uri:ch1", 1)
        ));
        when(kgChapterSectionRepository.findAllActive()).thenReturn(List.of(
                KgChapterSection.create("uri:ch1", "uri:sec1", 1)
        ));

        // 难度为 null 的知识点
        KgKnowledgePoint kp = KgKnowledgePoint.create("uri:kp1", "知识点1");
        // difficulty 默认为 null
        when(kgSectionKPRepository.findAllActive()).thenReturn(List.of(
                KgSectionKP.create("uri:sec1", "uri:kp1", 1)
        ));
        when(kgKnowledgePointRepository.findByUris(anyList())).thenReturn(List.of(kp));

        StatsDTO result = kgKnowledgeSystemAppService.getGradeStats(grade);

        Map<String, Integer> diffDist = result.getDifficultyDistribution();
        assertEquals(1, diffDist.get("unknown"));
    }

    // ==================== 6.9.3 空数据场景 ====================

    @Test
    @Order(10)
    @DisplayName("getGradeSystem 无数据 — total=0, groups 空列表")
    void getGradeSystem_emptyData_shouldReturnEmptyStructure() {
        when(kgTextbookRepository.findAllActive()).thenReturn(List.of());

        KgGradeSystemDTO result = kgKnowledgeSystemAppService.getGradeSystem("七年级", "subject");

        assertEquals(0, result.getTotalKnowledgePoints());
        assertTrue(result.getGroups().isEmpty());
    }

    @Test
    @Order(11)
    @DisplayName("getGradeStats 无数据 — total=0, 空列表")
    void getGradeStats_emptyData_shouldReturnEmptyStructure() {
        when(kgTextbookRepository.findAllActive()).thenReturn(List.of());

        StatsDTO result = kgKnowledgeSystemAppService.getGradeStats("七年级");

        assertEquals(0, result.getTotalKnowledgePoints());
        assertTrue(result.getDifficultyDistribution().isEmpty());
    }

    // ==================== Helper ====================

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
