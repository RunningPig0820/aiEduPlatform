package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgChapterSectionMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgChapterSectionPo;
import com.ai.edu.infrastructure.test.TestInfrastructureConfig;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgChapterSectionRepositoryImpl 集成测试
 *
 * - 使用 H2 内存数据库执行真实 SQL
 * - 测试章节-小节关联的完整仓储生命周期
 */
@SpringBootTest(
        classes = TestInfrastructureConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("h2")
@TestMethodOrder(OrderAnnotation.class)
class KgChapterSectionRepositoryImplTest {

    @Resource
    private KgChapterSectionRepositoryImpl repository;

    @Resource
    private KgChapterSectionMapper mapper;

    private static final String CHAPTER_URI = "http://edukg.org/knowledge/3.1/chapter/ch1";
    private static final String SECTION_URI_1 = "http://edukg.org/knowledge/3.1/section/sec1";
    private static final String SECTION_URI_2 = "http://edukg.org/knowledge/3.1/section/sec2";
    private static final String SECTION_URI_3 = "http://edukg.org/knowledge/3.1/section/sec3";

    /** 每个测试前清理关联表数据，保证测试隔离 */
    @BeforeEach
    void setUp() {
        mapper.selectAllActiveRelations().forEach(r -> mapper.deleteById(r.getId()));
    }

    // ==================== 6.6.6 save — insert new relation ====================

    @Test
    @Order(1)
    @DisplayName("save 新增关联 — id 为 null 时执行 INSERT")
    void save_insertNewRelation() {
        KgChapterSection relation = KgChapterSection.create(CHAPTER_URI, SECTION_URI_1, 1);

        assertNull(relation.getId());

        KgChapterSection saved = repository.save(relation);

        assertNotNull(saved.getId());
        assertEquals(CHAPTER_URI, saved.getChapterUri());
        assertEquals(SECTION_URI_1, saved.getSectionUri());
        assertEquals(1, saved.getOrderIndex());
        assertFalse(saved.getDeleted());

        // 验证数据库中真实存在
        List<KgChapterSectionPo> found = mapper.selectByChapterUri(CHAPTER_URI);
        assertEquals(1, found.size());
        assertEquals(SECTION_URI_1, found.get(0).getSectionUri());
    }

    // ==================== save — update existing relation ====================

    @Test
    @Order(2)
    @DisplayName("save 更新关联 — id 已设置时执行 UPDATE，更新 orderIndex")
    void save_updateExistingRelation() {
        // 先插入一条
        KgChapterSection relation = KgChapterSection.create(CHAPTER_URI, SECTION_URI_1, 1);
        repository.save(relation);
        Long id = relation.getId();

        // 通过 mapper 直接更新 orderIndex（实体无 setter）
        UpdateWrapper<KgChapterSectionPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id).set("order_index", 5);
        mapper.update(null, wrapper);

        // 验证 orderIndex 已更新
        List<KgChapterSectionPo> found = mapper.selectByChapterUri(CHAPTER_URI);
        assertEquals(1, found.size());
        assertEquals(5, found.get(0).getOrderIndex());
        assertEquals(id, found.get(0).getId());
    }

    // ==================== findByChapterUri ====================

    @Test
    @Order(3)
    @DisplayName("findByChapterUri — 查找指定章节的所有小节关联")
    void findByChapterUri_returnsAllSectionsForChapter() {
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_1, 1)));
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_2, 2)));
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create("other_chapter", SECTION_URI_3, 1)));

        List<KgChapterSection> results = repository.findByChapterUri(CHAPTER_URI);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.getChapterUri().equals(CHAPTER_URI)));
        assertTrue(results.stream().anyMatch(r -> r.getSectionUri().equals(SECTION_URI_1)));
        assertTrue(results.stream().anyMatch(r -> r.getSectionUri().equals(SECTION_URI_2)));
    }

    // ==================== findBySectionUri ====================

    @Test
    @Order(4)
    @DisplayName("findBySectionUri — 查找指定小节的所有章节关联")
    void findBySectionUri_returnsAllChaptersForSection() {
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_1, 1)));
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create("other_chapter", SECTION_URI_1, 2)));
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_2, 1)));

        List<KgChapterSection> results = repository.findBySectionUri(SECTION_URI_1);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.getSectionUri().equals(SECTION_URI_1)));
    }

    // ==================== findAllActive ====================

    @Test
    @Order(5)
    @DisplayName("findAllActive — 仅返回未删除的关联")
    void findAllActive_returnsOnlyActiveRelations() {
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_1, 1)));
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_2, 2)));

        List<KgChapterSection> results = repository.findAllActive();

        assertTrue(results.size() >= 2);
        assertTrue(results.stream().anyMatch(r ->
                r.getChapterUri().equals(CHAPTER_URI) && r.getSectionUri().equals(SECTION_URI_1)));
        assertTrue(results.stream().anyMatch(r ->
                r.getChapterUri().equals(CHAPTER_URI) && r.getSectionUri().equals(SECTION_URI_2)));
    }

    // ==================== deleteByChapterUri — soft delete ====================

    @Test
    @Order(6)
    @DisplayName("deleteByChapterUri 软删除 — 删除后 findAllActive 不再返回")
    void deleteByChapterUri_softDeleteRemovesFromFindAllActive() {
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_1, 1)));
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_2, 2)));
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create("other_chapter", SECTION_URI_3, 1)));

        int beforeCount = repository.findAllActive().size();

        repository.deleteByChapterUri(CHAPTER_URI);

        List<KgChapterSection> afterResults = repository.findAllActive();
        // 被删除的章节关联不再出现
        assertFalse(afterResults.stream().anyMatch(r -> r.getChapterUri().equals(CHAPTER_URI)));
        // 其他章节关联仍存在
        assertTrue(afterResults.stream().anyMatch(r -> r.getChapterUri().equals("other_chapter")));
        assertEquals(beforeCount - 2, afterResults.size());
    }

    @Test
    @Order(7)
    @DisplayName("deleteByChapterUri 软删除 — 删除后 findByChapterUri 不再返回")
    void deleteByChapterUri_softDeleteRemovesFromFindByChapterUri() {
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_1, 1)));

        repository.deleteByChapterUri(CHAPTER_URI);

        List<KgChapterSection> results = repository.findByChapterUri(CHAPTER_URI);
        assertTrue(results.isEmpty());
    }

    // ==================== deleteBySectionUri — soft delete ====================

    @Test
    @Order(8)
    @DisplayName("deleteBySectionUri 软删除 — 删除后 findAllActive 不再返回")
    void deleteBySectionUri_softDeleteRemovesFromFindAllActive() {
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_1, 1)));
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create("other_chapter", SECTION_URI_1, 2)));
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_2, 1)));

        repository.deleteBySectionUri(SECTION_URI_1);

        List<KgChapterSection> results = repository.findAllActive();
        // 被删除的小节关联不再出现
        assertFalse(results.stream().anyMatch(r -> r.getSectionUri().equals(SECTION_URI_1)));
        // 其他小节关联仍存在
        assertTrue(results.stream().anyMatch(r -> r.getSectionUri().equals(SECTION_URI_2)));
    }

    @Test
    @Order(9)
    @DisplayName("deleteBySectionUri 软删除 — 删除后 findBySectionUri 不再返回")
    void deleteBySectionUri_softDeleteRemovesFromFindBySectionUri() {
        mapper.insert(KgChapterSectionPo.from(KgChapterSection.create(CHAPTER_URI, SECTION_URI_1, 1)));

        repository.deleteBySectionUri(SECTION_URI_1);

        List<KgChapterSection> results = repository.findBySectionUri(SECTION_URI_1);
        assertTrue(results.isEmpty());
    }

    // ==================== saveBatch ====================

    @Test
    @Order(10)
    @DisplayName("saveBatch — 批量插入多条关联")
    void saveBatch_insertsMultipleRelations() {
        List<KgChapterSection> relations = List.of(
                KgChapterSection.create(CHAPTER_URI, SECTION_URI_1, 1),
                KgChapterSection.create(CHAPTER_URI, SECTION_URI_2, 2),
                KgChapterSection.create(CHAPTER_URI, SECTION_URI_3, 3)
        );

        repository.saveBatch(relations);

        List<KgChapterSectionPo> results = mapper.selectByChapterUri(CHAPTER_URI);
        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(r -> r.getSectionUri().equals(SECTION_URI_1)));
        assertTrue(results.stream().anyMatch(r -> r.getSectionUri().equals(SECTION_URI_2)));
        assertTrue(results.stream().anyMatch(r -> r.getSectionUri().equals(SECTION_URI_3)));
    }

    @Test
    @Order(11)
    @DisplayName("saveBatch 空列表应无操作")
    void saveBatch_emptyList() {
        assertDoesNotThrow(() -> repository.saveBatch(List.of()));
        assertEquals(0, repository.findAllActive().size());
    }

    @Test
    @Order(12)
    @DisplayName("saveBatch null 应无操作")
    void saveBatch_null() {
        assertDoesNotThrow(() -> repository.saveBatch(null));
    }
}
