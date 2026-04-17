package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgChapter;
import com.ai.edu.domain.edukg.repository.KgChapterRepository;
import com.ai.edu.infrastructure.test.TestInfrastructureConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgChapterRepositoryImpl 集成测试
 *
 * - 使用 H2 内存数据库执行真实 SQL
 * - 验证 MyBatis-Plus Mapper 的真实数据库操作
 */
@SpringBootTest(
        classes = TestInfrastructureConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("h2")
@TestMethodOrder(OrderAnnotation.class)
class KgChapterRepositoryImplTest {

    @Resource
    private KgChapterRepository kgChapterRepository;

    @Resource
    private com.ai.edu.infrastructure.persistence.edukg.mapper.KgChapterMapper kgChapterMapper;

    private static final String TEST_URI = "http://edukg.org/knowledge/3.1/chapter/test_chapter";
    private static final String TEST_URI_2 = "http://edukg.org/knowledge/3.1/chapter/test_chapter_2";
    private static final String TEST_URI_3 = "http://edukg.org/knowledge/3.1/chapter/test_chapter_3";

    /** 每个测试前清理测试数据 */
    @BeforeEach
    void setUp() {
        kgChapterMapper.selectByStatus("active").forEach(ch ->
                kgChapterMapper.deleteById(ch.getId()));
        // 也清理非 active 状态的（确保完全清理）
        kgChapterMapper.selectByStatus("deleted").forEach(ch ->
                kgChapterMapper.deleteById(ch.getId()));
    }

    // ==================== 6.6.2 save — insert new ====================

    @Test
    @Order(1)
    @DisplayName("save 新增章节 — id 为 null 时应 INSERT")
    void save_newEntity_shouldInsert() {
        KgChapter chapter = KgChapter.create(TEST_URI, "测试章节");
        assertNull(chapter.getId());

        KgChapter saved = kgChapterRepository.save(chapter);

        // 验证 id 被自动填充
        assertNotNull(saved.getId());
        // 验证真实写入 H2
        KgChapter found = kgChapterMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals(TEST_URI, found.getUri());
        assertEquals("测试章节", found.getLabel());
        assertEquals("active", found.getStatus());
        assertFalse(found.getDeleted());
    }

    // ==================== 6.6.2 save — update existing ====================

    @Test
    @Order(2)
    @DisplayName("save 更新章节 — id 已存在时应 UPDATE")
    void save_existingEntity_shouldUpdate() {
        // 先插入一条
        KgChapter original = KgChapter.create(TEST_URI, "旧标签");
        kgChapterMapper.insert(original);
        assertNotNull(original.getId());

        // 修改 label
        original.updateTopic("新主题");
        KgChapter updated = kgChapterRepository.save(original);

        // 验证更新
        assertEquals(original.getId(), updated.getId());
        KgChapter found = kgChapterMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals("新主题", found.getTopic());
    }

    // ==================== 6.6.2 findByUri — found ====================

    @Test
    @Order(3)
    @DisplayName("findByUri 查找已存在的章节")
    void findByUri_shouldReturnPresent() {
        KgChapter chapter = KgChapter.create(TEST_URI, "测试章节");
        kgChapterMapper.insert(chapter);

        var result = kgChapterRepository.findByUri(TEST_URI);

        assertTrue(result.isPresent());
        assertEquals(TEST_URI, result.get().getUri());
        assertEquals("测试章节", result.get().getLabel());
    }

    // ==================== 6.6.2 findByUri — not found ====================

    @Test
    @Order(4)
    @DisplayName("findByUri 查找不存在的章节应返回 empty")
    void findByUri_notFound_shouldReturnEmpty() {
        var result = kgChapterRepository.findByUri("http://nonexistent/uri");

        assertTrue(result.isEmpty());
    }

    // ==================== 6.6.2 findByUris — batch query ====================

    @Test
    @Order(5)
    @DisplayName("findByUris 应批量返回匹配的章节")
    void findByUris_shouldReturnMatching() {
        KgChapter ch1 = KgChapter.create(TEST_URI, "章节1");
        KgChapter ch2 = KgChapter.create(TEST_URI_2, "章节2");
        KgChapter ch3 = KgChapter.create(TEST_URI_3, "章节3");
        kgChapterMapper.insert(ch1);
        kgChapterMapper.insert(ch2);
        kgChapterMapper.insert(ch3);

        List<KgChapter> result = kgChapterRepository.findByUris(
                List.of(TEST_URI, TEST_URI_2));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(ch -> TEST_URI.equals(ch.getUri())));
        assertTrue(result.stream().anyMatch(ch -> TEST_URI_2.equals(ch.getUri())));
    }

    @Test
    @Order(6)
    @DisplayName("findByUris 全部不存在时应返回空列表")
    void findByUris_noMatch_shouldReturnEmpty() {
        List<KgChapter> result = kgChapterRepository.findByUris(
                List.of("http://nonexistent/1", "http://nonexistent/2"));

        assertTrue(result.isEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("findByUris 部分匹配应只返回存在的")
    void findByUris_partialMatch_shouldReturnExisting() {
        KgChapter ch1 = KgChapter.create(TEST_URI, "章节1");
        kgChapterMapper.insert(ch1);

        List<KgChapter> result = kgChapterRepository.findByUris(
                List.of(TEST_URI, "http://nonexistent/uri"));

        assertEquals(1, result.size());
        assertEquals(TEST_URI, result.get(0).getUri());
    }

    // ==================== 6.6.2 updateStatus ====================

    @Test
    @Order(8)
    @DisplayName("updateStatus 应更新 status 字段")
    void updateStatus_shouldChangeStatus() {
        KgChapter chapter = KgChapter.create(TEST_URI, "章节");
        kgChapterMapper.insert(chapter);

        kgChapterRepository.updateStatus(TEST_URI, "deleted");

        KgChapter found = kgChapterMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals("deleted", found.getStatus());
        // is_deleted 不会被 updateStatus 修改
        assertFalse(found.getDeleted());
    }

    // ==================== Soft-delete consistency ====================

    @Test
    @Order(9)
    @DisplayName("软删除后 findByUri 仍能查到（updateStatus 不改 is_deleted）")
    void softDelete_findByUri_shouldStillFind() {
        KgChapter chapter = KgChapter.create(TEST_URI, "章节");
        kgChapterMapper.insert(chapter);

        kgChapterRepository.updateStatus(TEST_URI, "deleted");

        var result = kgChapterRepository.findByUri(TEST_URI);
        assertTrue(result.isPresent());
        assertEquals("deleted", result.get().getStatus());
    }

    @Test
    @Order(10)
    @DisplayName("软删除后 findByUris 仍能查到（is_deleted 仍为 false）")
    void softDelete_findByUris_shouldStillFind() {
        KgChapter ch1 = KgChapter.create(TEST_URI, "章节1");
        KgChapter ch2 = KgChapter.create(TEST_URI_2, "章节2");
        kgChapterMapper.insert(ch1);
        kgChapterMapper.insert(ch2);

        kgChapterRepository.updateStatus(TEST_URI, "deleted");

        List<KgChapter> result = kgChapterRepository.findByUris(
                List.of(TEST_URI, TEST_URI_2));

        assertEquals(2, result.size());
    }

    // ==================== findById ====================

    @Test
    @Order(11)
    @DisplayName("findById 应通过主键查找")
    void findById_shouldReturnPresent() {
        KgChapter chapter = KgChapter.create(TEST_URI, "章节");
        kgChapterMapper.insert(chapter);
        Long id = chapter.getId();

        var result = kgChapterRepository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(TEST_URI, result.get().getUri());
    }

    @Test
    @Order(12)
    @DisplayName("findById 不存在的主键应返回 empty")
    void findById_notFound_shouldReturnEmpty() {
        var result = kgChapterRepository.findById(9999L);

        assertTrue(result.isEmpty());
    }
}
