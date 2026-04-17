package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgSection;
import com.ai.edu.domain.edukg.repository.KgSectionRepository;
import com.ai.edu.infrastructure.test.TestInfrastructureConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgSectionRepositoryImpl 集成测试
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
class KgSectionRepositoryImplTest {

    @Resource
    private KgSectionRepository kgSectionRepository;

    @Resource
    private com.ai.edu.infrastructure.persistence.edukg.mapper.KgSectionMapper kgSectionMapper;

    private static final String TEST_URI = "http://edukg.org/knowledge/3.1/section/test_section";
    private static final String TEST_URI_2 = "http://edukg.org/knowledge/3.1/section/test_section_2";
    private static final String TEST_URI_3 = "http://edukg.org/knowledge/3.1/section/test_section_3";

    /** 每个测试前清理测试数据 */
    @BeforeEach
    void setUp() {
        kgSectionMapper.selectByStatus("active").forEach(sec ->
                kgSectionMapper.deleteById(sec.getId()));
        kgSectionMapper.selectByStatus("deleted").forEach(sec ->
                kgSectionMapper.deleteById(sec.getId()));
    }

    // ==================== 6.6.3 save — insert new ====================

    @Test
    @Order(1)
    @DisplayName("save 新增小节 — id 为 null 时应 INSERT")
    void save_newEntity_shouldInsert() {
        KgSection section = KgSection.create(TEST_URI, "测试小节");
        assertNull(section.getId());

        KgSection saved = kgSectionRepository.save(section);

        // 验证 id 被自动填充
        assertNotNull(saved.getId());
        // 验证真实写入 H2
        KgSection found = kgSectionMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals(TEST_URI, found.getUri());
        assertEquals("测试小节", found.getLabel());
        assertEquals("active", found.getStatus());
        assertFalse(found.getDeleted());
    }

    // ==================== 6.6.3 save — update existing ====================

    @Test
    @Order(2)
    @DisplayName("save 更新小节 — id 已存在时应 UPDATE")
    void save_existingEntity_shouldUpdate() {
        // 先插入一条
        KgSection original = KgSection.create(TEST_URI, "旧标签");
        kgSectionMapper.insert(original);
        Long savedId = original.getId();
        assertNotNull(savedId);

        // 使用 MyBatis-Plus 直接更新来验证 save(id 非 null) 走 updateById 路径
        KgSection toUpdate = KgSection.create(TEST_URI, "新标签");
        // 通过反射设置 id（实体只有 @Getter，没有 @Setter）
        try {
            var idField = KgSection.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(toUpdate, savedId);
        } catch (Exception e) {
            fail("Failed to set id via reflection: " + e.getMessage());
        }
        KgSection updated = kgSectionRepository.save(toUpdate);

        // 验证更新
        assertEquals(savedId, updated.getId());
        KgSection found = kgSectionMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals("新标签", found.getLabel());
    }

    // ==================== 6.6.3 findByUri — found ====================

    @Test
    @Order(3)
    @DisplayName("findByUri 查找已存在的小节")
    void findByUri_shouldReturnPresent() {
        KgSection section = KgSection.create(TEST_URI, "测试小节");
        kgSectionMapper.insert(section);

        var result = kgSectionRepository.findByUri(TEST_URI);

        assertTrue(result.isPresent());
        assertEquals(TEST_URI, result.get().getUri());
        assertEquals("测试小节", result.get().getLabel());
    }

    // ==================== 6.6.3 findByUri — not found ====================

    @Test
    @Order(4)
    @DisplayName("findByUri 查找不存在的小节应返回 empty")
    void findByUri_notFound_shouldReturnEmpty() {
        var result = kgSectionRepository.findByUri("http://nonexistent/uri");

        assertTrue(result.isEmpty());
    }

    // ==================== 6.6.3 findByUris — batch query ====================

    @Test
    @Order(5)
    @DisplayName("findByUris 应批量返回匹配的小节")
    void findByUris_shouldReturnMatching() {
        KgSection sec1 = KgSection.create(TEST_URI, "小节1");
        KgSection sec2 = KgSection.create(TEST_URI_2, "小节2");
        KgSection sec3 = KgSection.create(TEST_URI_3, "小节3");
        kgSectionMapper.insert(sec1);
        kgSectionMapper.insert(sec2);
        kgSectionMapper.insert(sec3);

        List<KgSection> result = kgSectionRepository.findByUris(
                List.of(TEST_URI, TEST_URI_2));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(sec -> TEST_URI.equals(sec.getUri())));
        assertTrue(result.stream().anyMatch(sec -> TEST_URI_2.equals(sec.getUri())));
    }

    @Test
    @Order(6)
    @DisplayName("findByUris 全部不存在时应返回空列表")
    void findByUris_noMatch_shouldReturnEmpty() {
        List<KgSection> result = kgSectionRepository.findByUris(
                List.of("http://nonexistent/1", "http://nonexistent/2"));

        assertTrue(result.isEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("findByUris 部分匹配应只返回存在的")
    void findByUris_partialMatch_shouldReturnExisting() {
        KgSection sec1 = KgSection.create(TEST_URI, "小节1");
        kgSectionMapper.insert(sec1);

        List<KgSection> result = kgSectionRepository.findByUris(
                List.of(TEST_URI, "http://nonexistent/uri"));

        assertEquals(1, result.size());
        assertEquals(TEST_URI, result.get(0).getUri());
    }

    // ==================== 6.6.3 updateStatus ====================

    @Test
    @Order(8)
    @DisplayName("updateStatus 应更新 status 字段")
    void updateStatus_shouldChangeStatus() {
        KgSection section = KgSection.create(TEST_URI, "小节");
        kgSectionMapper.insert(section);

        kgSectionRepository.updateStatus(TEST_URI, "deleted");

        KgSection found = kgSectionMapper.selectByUri(TEST_URI);
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
        KgSection section = KgSection.create(TEST_URI, "小节");
        kgSectionMapper.insert(section);

        kgSectionRepository.updateStatus(TEST_URI, "deleted");

        var result = kgSectionRepository.findByUri(TEST_URI);
        assertTrue(result.isPresent());
        assertEquals("deleted", result.get().getStatus());
    }

    @Test
    @Order(10)
    @DisplayName("软删除后 findByUris 仍能查到（is_deleted 仍为 false）")
    void softDelete_findByUris_shouldStillFind() {
        KgSection sec1 = KgSection.create(TEST_URI, "小节1");
        KgSection sec2 = KgSection.create(TEST_URI_2, "小节2");
        kgSectionMapper.insert(sec1);
        kgSectionMapper.insert(sec2);

        kgSectionRepository.updateStatus(TEST_URI, "deleted");

        List<KgSection> result = kgSectionRepository.findByUris(
                List.of(TEST_URI, TEST_URI_2));

        assertEquals(2, result.size());
    }

    // ==================== findById ====================

    @Test
    @Order(11)
    @DisplayName("findById 应通过主键查找")
    void findById_shouldReturnPresent() {
        KgSection section = KgSection.create(TEST_URI, "小节");
        kgSectionMapper.insert(section);
        Long id = section.getId();

        var result = kgSectionRepository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(TEST_URI, result.get().getUri());
    }

    @Test
    @Order(12)
    @DisplayName("findById 不存在的主键应返回 empty")
    void findById_notFound_shouldReturnEmpty() {
        var result = kgSectionRepository.findById(9999L);

        assertTrue(result.isEmpty());
    }
}
