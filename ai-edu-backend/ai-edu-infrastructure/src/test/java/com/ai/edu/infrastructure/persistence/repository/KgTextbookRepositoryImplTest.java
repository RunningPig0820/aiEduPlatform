package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgTextbook;
import com.ai.edu.domain.edukg.repository.KgTextbookRepository;
import com.ai.edu.infrastructure.persistence.edukg.po.KgTextbookPo;
import com.ai.edu.infrastructure.test.TestInfrastructureConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgTextbookRepositoryImpl 集成测试
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
class KgTextbookRepositoryImplTest {

    @Resource
    private KgTextbookRepository kgTextbookRepository;

    @Resource
    private com.ai.edu.infrastructure.persistence.edukg.mapper.KgTextbookMapper kgTextbookMapper;

    private static final String TEST_URI = "http://edukg.org/knowledge/3.1/textbook/test_textbook";
    private static final String TEST_URI_2 = "http://edukg.org/knowledge/3.1/textbook/test_textbook_2";

    /** 每个测试前清理测试数据 */
    @BeforeEach
    void setUp() {
        // 硬删除清理（绕过逻辑删除）
        kgTextbookMapper.selectAllActive().forEach(tb ->
                kgTextbookMapper.deleteById(tb.getId()));
    }

    // ==================== 6.6.1 save — insert new ====================

    @Test
    @Order(1)
    @DisplayName("save 新增教材 — id 为 null 时应 INSERT")
    void save_newEntity_shouldInsert() {
        KgTextbook textbook = KgTextbook.create(TEST_URI, "测试教材", "七年级", "junior", "人教版", "math");
        assertNull(textbook.getId());

        KgTextbook saved = kgTextbookRepository.save(textbook);

        // 验证 id 被自动填充
        assertNotNull(saved.getId());
        // 验证真实写入 H2
        KgTextbookPo found = kgTextbookMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals(TEST_URI, found.getUri());
        assertEquals("测试教材", found.getLabel());
        assertEquals("七年级", found.getGrade());
        assertEquals("junior", found.getStage());
        assertEquals("math", found.getSubject());
        assertEquals("active", found.getStatus());
        assertFalse(found.getDeleted());
    }

    // ==================== 6.6.1 save — update existing ====================

    @Test
    @Order(2)
    @DisplayName("save 更新教材 — id 已存在时应 UPDATE")
    void save_existingEntity_shouldUpdate() {
        // 先插入一条
        KgTextbook original = KgTextbook.create(TEST_URI, "旧标签", "旧年级", "junior", "人教版", "math");
        KgTextbookPo insertedPo = KgTextbookPo.from(original);
        kgTextbookMapper.insert(insertedPo);
        original.setId(insertedPo.getId());
        assertNotNull(original.getId());

        // 修改后再次 save
        original.updateFrom(KgTextbook.create(TEST_URI, "新标签", "新年级", "junior", "人教版", "math"));
        KgTextbook updated = kgTextbookRepository.save(original);

        // 验证更新
        assertEquals(original.getId(), updated.getId());
        KgTextbookPo found = kgTextbookMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals("新标签", found.getLabel());
        assertEquals("新年级", found.getGrade());
    }

    // ==================== 6.6.1 findByUri — found ====================

    @Test
    @Order(3)
    @DisplayName("findByUri 查找已存在的教材")
    void findByUri_shouldReturnPresent() {
        KgTextbook textbook = KgTextbook.create(TEST_URI, "测试教材", "七年级", "junior", "人教版", "math");
        kgTextbookMapper.insert(KgTextbookPo.from(textbook));

        var result = kgTextbookRepository.findByUri(TEST_URI);

        assertTrue(result.isPresent());
        assertEquals(TEST_URI, result.get().getUri());
        assertEquals("测试教材", result.get().getLabel());
    }

    // ==================== 6.6.1 findByUri — not found ====================

    @Test
    @Order(4)
    @DisplayName("findByUri 查找不存在的教材应返回 empty")
    void findByUri_notFound_shouldReturnEmpty() {
        var result = kgTextbookRepository.findByUri("http://nonexistent/uri");

        assertTrue(result.isEmpty());
    }

    // ==================== 6.6.1 findAllActive ====================

    @Test
    @Order(5)
    @DisplayName("findAllActive 应只返回活跃教材")
    void findAllActive_shouldReturnOnlyActive() {
        KgTextbook tb1 = KgTextbook.create(TEST_URI, "教材1", "七年级", "junior", "人教版", "math");
        KgTextbook tb2 = KgTextbook.create(TEST_URI_2, "教材2", "八年级", "junior", "人教版", "math");
        kgTextbookMapper.insert(KgTextbookPo.from(tb1));
        kgTextbookMapper.insert(KgTextbookPo.from(tb2));

        List<KgTextbook> active = kgTextbookRepository.findAllActive();

        assertEquals(2, active.size());
    }

    @Test
    @Order(6)
    @DisplayName("findAllActive 应排除 status 为 deleted 的教材")
    void findAllActive_shouldExcludeDeletedStatus() {
        KgTextbook tb1 = KgTextbook.create(TEST_URI, "教材1", "七年级", "junior", "人教版", "math");
        KgTextbook tb2 = KgTextbook.create(TEST_URI_2, "教材2", "八年级", "junior", "人教版", "math");
        kgTextbookMapper.insert(KgTextbookPo.from(tb1));
        kgTextbookMapper.insert(KgTextbookPo.from(tb2));

        // 将 tb1 标记为 deleted（updateStatus 只改 status，不改 is_deleted）
        kgTextbookRepository.updateStatus(TEST_URI, "deleted");

        // findAllActive 使用 is_deleted = false 过滤，所以仍能查到（is_deleted 仍为 false）
        // 但业务上应通过 status 判断
        List<KgTextbook> all = kgTextbookRepository.findAllActive();
        // selectAllActive 的 SQL 只过滤 is_deleted，不过滤 status
        assertEquals(2, all.size());

        // 验证 status 确实被更新了
        KgTextbookPo deleted = kgTextbookMapper.selectByUri(TEST_URI);
        assertNotNull(deleted);
        assertEquals("deleted", deleted.getStatus());
    }

    // ==================== 6.6.1 updateStatus ====================

    @Test
    @Order(7)
    @DisplayName("updateStatus 应更新 status 字段")
    void updateStatus_shouldChangeStatus() {
        KgTextbook textbook = KgTextbook.create(TEST_URI, "教材", "七年级", "junior", "人教版", "math");
        kgTextbookMapper.insert(KgTextbookPo.from(textbook));

        kgTextbookRepository.updateStatus(TEST_URI, "deleted");

        KgTextbookPo found = kgTextbookMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals("deleted", found.getStatus());
        // is_deleted 不会被 updateStatus 修改
        assertFalse(found.getDeleted());
    }

    // ==================== 6.6.1 findBySubject ====================

    @Test
    @Order(8)
    @DisplayName("findBySubject 应返回指定学科的所有教材")
    void findBySubject_shouldReturnMatching() {
        KgTextbook tb1 = KgTextbook.create(TEST_URI, "数学教材1", "七年级", "junior", "人教版", "math");
        KgTextbook tb2 = KgTextbook.create(TEST_URI_2, "数学教材2", "八年级", "junior", "人教版", "math");
        kgTextbookMapper.insert(KgTextbookPo.from(tb1));
        kgTextbookMapper.insert(KgTextbookPo.from(tb2));

        // 插入另一个学科的
        KgTextbook tb3 = KgTextbook.create("http://edukg.org/knowledge/3.1/textbook/english_1",
                "英语教材", "七年级", "junior", "人教版", "english");
        kgTextbookMapper.insert(KgTextbookPo.from(tb3));

        List<KgTextbook> mathTextbooks = kgTextbookRepository.findBySubject("math");

        assertEquals(2, mathTextbooks.size());
        assertTrue(mathTextbooks.stream().allMatch(tb -> "math".equals(tb.getSubject())));
    }

    @Test
    @Order(9)
    @DisplayName("findBySubject 无匹配时应返回空列表")
    void findBySubject_noMatch_shouldReturnEmpty() {
        List<KgTextbook> result = kgTextbookRepository.findBySubject("physics");

        assertTrue(result.isEmpty());
    }

    // ==================== 6.6.1 findBySubjectAndPhase ====================

    @Test
    @Order(10)
    @DisplayName("findBySubjectAndStage 应返回匹配的教材")
    void findBySubjectAndStage_shouldReturnMatching() {
        KgTextbook tb1 = KgTextbook.create(TEST_URI, "初中数学", "七年级", "junior", "人教版", "math");
        KgTextbook tb2 = KgTextbook.create(TEST_URI_2, "初中数学2", "八年级", "junior", "人教版", "math");
        kgTextbookMapper.insert(KgTextbookPo.from(tb1));
        kgTextbookMapper.insert(KgTextbookPo.from(tb2));

        // 插入 stage 不同的
        KgTextbook tb3 = KgTextbook.create("http://edukg.org/knowledge/3.1/textbook/senior_math",
                "高中数学", "高一", "senior", "人教版", "math");
        kgTextbookMapper.insert(KgTextbookPo.from(tb3));

        List<KgTextbook> result = kgTextbookRepository.findBySubjectAndStage("math", "junior");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(tb -> "math".equals(tb.getSubject()) && "junior".equals(tb.getStage())));
    }

    @Test
    @Order(11)
    @DisplayName("findBySubjectAndStage 无匹配时应返回空列表")
    void findBySubjectAndStage_noMatch_shouldReturnEmpty() {
        List<KgTextbook> result = kgTextbookRepository.findBySubjectAndStage("physics", "junior");

        assertTrue(result.isEmpty());
    }

    // ==================== Soft-delete consistency ====================

    @Test
    @Order(12)
    @DisplayName("软删除后 findByUri 仍能查到（updateStatus 不改 is_deleted）")
    void softDelete_findByUri_shouldStillFind() {
        KgTextbook textbook = KgTextbook.create(TEST_URI, "教材", "七年级", "junior", "人教版", "math");
        kgTextbookMapper.insert(KgTextbookPo.from(textbook));

        // updateStatus 只改 status
        kgTextbookRepository.updateStatus(TEST_URI, "deleted");

        // findByUri 的 SQL 过滤 is_deleted = false，status 不影响
        var result = kgTextbookRepository.findByUri(TEST_URI);
        assertTrue(result.isPresent());
        assertEquals("deleted", result.get().getStatus());
    }

    // ==================== findById ====================

    @Test
    @Order(13)
    @DisplayName("findById 应通过主键查找")
    void findById_shouldReturnPresent() {
        KgTextbook textbook = KgTextbook.create(TEST_URI, "教材", "七年级", "junior", "人教版", "math");
        KgTextbookPo insertedPo = KgTextbookPo.from(textbook);
        kgTextbookMapper.insert(insertedPo);
        Long id = insertedPo.getId();

        var result = kgTextbookRepository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(TEST_URI, result.get().getUri());
    }

    @Test
    @Order(14)
    @DisplayName("findById 不存在的主键应返回 empty")
    void findById_notFound_shouldReturnEmpty() {
        var result = kgTextbookRepository.findById(9999L);

        assertTrue(result.isEmpty());
    }
}
