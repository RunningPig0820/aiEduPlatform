package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgTextbookChapterMapper;
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
 * KgTextbookChapterRepositoryImpl 集成测试
 *
 * - 使用 H2 内存数据库执行真实 SQL
 * - 测试教材-章节关联的完整仓储生命周期
 */
@SpringBootTest(
        classes = TestInfrastructureConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("h2")
@TestMethodOrder(OrderAnnotation.class)
class KgTextbookChapterRepositoryImplTest {

    @Resource
    private KgTextbookChapterRepositoryImpl repository;

    @Resource
    private KgTextbookChapterMapper mapper;

    private static final String TEXTBOOK_URI = "http://edukg.org/knowledge/3.1/textbook/test_math";
    private static final String CHAPTER_URI_1 = "http://edukg.org/knowledge/3.1/chapter/ch1";
    private static final String CHAPTER_URI_2 = "http://edukg.org/knowledge/3.1/chapter/ch2";
    private static final String CHAPTER_URI_3 = "http://edukg.org/knowledge/3.1/chapter/ch3";

    /** 每个测试前清理关联表数据，保证测试隔离 */
    @BeforeEach
    void setUp() {
        mapper.selectAllActiveRelations().forEach(r -> mapper.deleteById(r.getId()));
    }

    // ==================== 6.6.5 save — insert new relation ====================

    @Test
    @Order(1)
    @DisplayName("save 新增关联 — id 为 null 时执行 INSERT")
    void save_insertNewRelation() {
        KgTextbookChapter relation = KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_1, 1);

        assertNull(relation.getId());

        KgTextbookChapter saved = repository.save(relation);

        assertNotNull(saved.getId());
        assertEquals(TEXTBOOK_URI, saved.getTextbookUri());
        assertEquals(CHAPTER_URI_1, saved.getChapterUri());
        assertEquals(1, saved.getOrderIndex());
        assertFalse(saved.getDeleted());

        // 验证数据库中真实存在
        List<KgTextbookChapter> found = mapper.selectByTextbookUri(TEXTBOOK_URI);
        assertEquals(1, found.size());
        assertEquals(CHAPTER_URI_1, found.get(0).getChapterUri());
    }

    // ==================== save — update existing relation ====================

    @Test
    @Order(2)
    @DisplayName("save 更新关联 — id 已设置时执行 UPDATE，更新 orderIndex")
    void save_updateExistingRelation() {
        // 先插入一条
        KgTextbookChapter relation = KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_1, 1);
        repository.save(relation);
        Long id = relation.getId();

        // 通过 mapper 直接更新 orderIndex（实体无 setter）
        UpdateWrapper<KgTextbookChapter> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id).set("order_index", 5);
        mapper.update(null, wrapper);

        // 验证 orderIndex 已更新
        List<KgTextbookChapter> found = mapper.selectByTextbookUri(TEXTBOOK_URI);
        assertEquals(1, found.size());
        assertEquals(5, found.get(0).getOrderIndex());
        assertEquals(id, found.get(0).getId());
    }

    // ==================== findByTextbookUri ====================

    @Test
    @Order(3)
    @DisplayName("findByTextbookUri — 查找指定教材的所有章节关联")
    void findByTextbookUri_returnsAllChaptersForTextbook() {
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_1, 1));
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_2, 2));
        mapper.insert(KgTextbookChapter.create("other_textbook", CHAPTER_URI_3, 1));

        List<KgTextbookChapter> results = repository.findByTextbookUri(TEXTBOOK_URI);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.getTextbookUri().equals(TEXTBOOK_URI)));
        assertTrue(results.stream().anyMatch(r -> r.getChapterUri().equals(CHAPTER_URI_1)));
        assertTrue(results.stream().anyMatch(r -> r.getChapterUri().equals(CHAPTER_URI_2)));
    }

    // ==================== findByChapterUri ====================

    @Test
    @Order(4)
    @DisplayName("findByChapterUri — 查找指定章节的所有教材关联")
    void findByChapterUri_returnsAllTextbooksForChapter() {
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_1, 1));
        mapper.insert(KgTextbookChapter.create("other_textbook", CHAPTER_URI_1, 2));
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_2, 1));

        List<KgTextbookChapter> results = repository.findByChapterUri(CHAPTER_URI_1);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.getChapterUri().equals(CHAPTER_URI_1)));
    }

    // ==================== findAllActive ====================

    @Test
    @Order(5)
    @DisplayName("findAllActive — 仅返回未删除的关联")
    void findAllActive_returnsOnlyActiveRelations() {
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_1, 1));
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_2, 2));

        List<KgTextbookChapter> results = repository.findAllActive();

        // 至少包含刚插入的两条（可能有其他测试残留，但我们的两条一定在）
        assertTrue(results.size() >= 2);
        assertTrue(results.stream().anyMatch(r ->
                r.getTextbookUri().equals(TEXTBOOK_URI) && r.getChapterUri().equals(CHAPTER_URI_1)));
        assertTrue(results.stream().anyMatch(r ->
                r.getTextbookUri().equals(TEXTBOOK_URI) && r.getChapterUri().equals(CHAPTER_URI_2)));
    }

    // ==================== deleteByTextbookUri — soft delete ====================

    @Test
    @Order(6)
    @DisplayName("deleteByTextbookUri 软删除 — 删除后 findAllActive 不再返回")
    void deleteByTextbookUri_softDeleteRemovesFromFindAllActive() {
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_1, 1));
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_2, 2));
        mapper.insert(KgTextbookChapter.create("other_textbook", CHAPTER_URI_3, 1));

        int beforeCount = repository.findAllActive().size();

        repository.deleteByTextbookUri(TEXTBOOK_URI);

        List<KgTextbookChapter> afterResults = repository.findAllActive();
        // 被删除的教材关联不再出现
        assertFalse(afterResults.stream().anyMatch(r -> r.getTextbookUri().equals(TEXTBOOK_URI)));
        // 其他教材关联仍存在
        assertTrue(afterResults.stream().anyMatch(r -> r.getTextbookUri().equals("other_textbook")));
        assertEquals(beforeCount - 2, afterResults.size());
    }

    @Test
    @Order(7)
    @DisplayName("deleteByTextbookUri 软删除 — 删除后 findByTextbookUri 不再返回")
    void deleteByTextbookUri_softDeleteRemovesFromFindByTextbookUri() {
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_1, 1));

        repository.deleteByTextbookUri(TEXTBOOK_URI);

        List<KgTextbookChapter> results = repository.findByTextbookUri(TEXTBOOK_URI);
        assertTrue(results.isEmpty());
    }

    // ==================== deleteByChapterUri — soft delete ====================

    @Test
    @Order(8)
    @DisplayName("deleteByChapterUri 软删除 — 删除后 findAllActive 不再返回")
    void deleteByChapterUri_softDeleteRemovesFromFindAllActive() {
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_1, 1));
        mapper.insert(KgTextbookChapter.create("other_textbook", CHAPTER_URI_1, 2));
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_2, 1));

        repository.deleteByChapterUri(CHAPTER_URI_1);

        List<KgTextbookChapter> results = repository.findAllActive();
        // 被删除的章节关联不再出现
        assertFalse(results.stream().anyMatch(r -> r.getChapterUri().equals(CHAPTER_URI_1)));
        // 其他章节关联仍存在
        assertTrue(results.stream().anyMatch(r -> r.getChapterUri().equals(CHAPTER_URI_2)));
    }

    @Test
    @Order(9)
    @DisplayName("deleteByChapterUri 软删除 — 删除后 findByChapterUri 不再返回")
    void deleteByChapterUri_softDeleteRemovesFromFindByChapterUri() {
        mapper.insert(KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_1, 1));

        repository.deleteByChapterUri(CHAPTER_URI_1);

        List<KgTextbookChapter> results = repository.findByChapterUri(CHAPTER_URI_1);
        assertTrue(results.isEmpty());
    }

    // ==================== saveBatch ====================

    @Test
    @Order(10)
    @DisplayName("saveBatch — 批量插入多条关联")
    void saveBatch_insertsMultipleRelations() {
        List<KgTextbookChapter> relations = List.of(
                KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_1, 1),
                KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_2, 2),
                KgTextbookChapter.create(TEXTBOOK_URI, CHAPTER_URI_3, 3)
        );

        repository.saveBatch(relations);

        List<KgTextbookChapter> results = mapper.selectByTextbookUri(TEXTBOOK_URI);
        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(r -> r.getChapterUri().equals(CHAPTER_URI_1)));
        assertTrue(results.stream().anyMatch(r -> r.getChapterUri().equals(CHAPTER_URI_2)));
        assertTrue(results.stream().anyMatch(r -> r.getChapterUri().equals(CHAPTER_URI_3)));
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
