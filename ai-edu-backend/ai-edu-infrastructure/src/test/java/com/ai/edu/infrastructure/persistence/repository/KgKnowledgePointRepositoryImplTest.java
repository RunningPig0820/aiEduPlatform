package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.infrastructure.persistence.edukg.po.KgKnowledgePointPo;
import com.ai.edu.infrastructure.test.TestInfrastructureConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgKnowledgePointRepositoryImpl 集成测试
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
class KgKnowledgePointRepositoryImplTest {

    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    @Resource
    private com.ai.edu.infrastructure.persistence.edukg.mapper.KgKnowledgePointMapper kgKnowledgePointMapper;

    private static final String TEST_URI = "http://edukg.org/knowledge/3.1/kp/test_kp";
    private static final String TEST_URI_2 = "http://edukg.org/knowledge/3.1/kp/test_kp_2";
    private static final String TEST_URI_3 = "http://edukg.org/knowledge/3.1/kp/test_kp_3";

    /** 每个测试前清理测试数据 */
    @BeforeEach
    void setUp() {
        kgKnowledgePointMapper.selectByStatus("active").forEach(kp ->
                kgKnowledgePointMapper.deleteById(kp.getId()));
        kgKnowledgePointMapper.selectByStatus("deleted").forEach(kp ->
                kgKnowledgePointMapper.deleteById(kp.getId()));
    }

    // ==================== 6.6.4 save — insert new ====================

    @Test
    @Order(1)
    @DisplayName("save 新增知识点 — id 为 null 时应 INSERT")
    void save_newEntity_shouldInsert() {
        KgKnowledgePoint kp = KgKnowledgePoint.create(TEST_URI, "测试知识点");
        assertNull(kp.getId());

        KgKnowledgePoint saved = kgKnowledgePointRepository.save(kp);

        // 验证 id 被自动填充
        assertNotNull(saved.getId());
        // 验证真实写入 H2
        KgKnowledgePointPo found = kgKnowledgePointMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals(TEST_URI, found.getUri());
        assertEquals("测试知识点", found.getLabel());
        assertEquals("active", found.getStatus());
        assertFalse(found.getDeleted());
    }

    // ==================== 6.6.4 save — update existing ====================

    @Test
    @Order(2)
    @DisplayName("save 更新知识点 — id 已存在时应 UPDATE")
    void save_existingEntity_shouldUpdate() {
        // 先插入一条
        KgKnowledgePoint original = KgKnowledgePoint.create(TEST_URI, "旧标签");
        KgKnowledgePointPo insertedPo = KgKnowledgePointPo.from(original);
        kgKnowledgePointMapper.insert(insertedPo);
        original.setId(insertedPo.getId());
        assertNotNull(original.getId());

        // 修改属性
        original.updateAttributes("hard", "high", "analysis");
        KgKnowledgePoint updated = kgKnowledgePointRepository.save(original);

        // 验证更新
        assertEquals(original.getId(), updated.getId());
        KgKnowledgePointPo found = kgKnowledgePointMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals("hard", found.getDifficulty());
        assertEquals("high", found.getImportance());
        assertEquals("analysis", found.getCognitiveLevel());
    }

    // ==================== 6.6.4 findByUri — found ====================

    @Test
    @Order(3)
    @DisplayName("findByUri 查找已存在的知识点")
    void findByUri_shouldReturnPresent() {
        KgKnowledgePoint kp = KgKnowledgePoint.create(TEST_URI, "测试知识点");
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp));

        var result = kgKnowledgePointRepository.findByUri(TEST_URI);

        assertTrue(result.isPresent());
        assertEquals(TEST_URI, result.get().getUri());
        assertEquals("测试知识点", result.get().getLabel());
    }

    // ==================== 6.6.4 findByUri — not found ====================

    @Test
    @Order(4)
    @DisplayName("findByUri 查找不存在的知识点应返回 empty")
    void findByUri_notFound_shouldReturnEmpty() {
        var result = kgKnowledgePointRepository.findByUri("http://nonexistent/uri");

        assertTrue(result.isEmpty());
    }

    // ==================== 6.6.4 findByUris — batch query ====================

    @Test
    @Order(5)
    @DisplayName("findByUris 应批量返回匹配的知识点")
    void findByUris_shouldReturnMatching() {
        KgKnowledgePoint kp1 = KgKnowledgePoint.create(TEST_URI, "知识点1");
        KgKnowledgePoint kp2 = KgKnowledgePoint.create(TEST_URI_2, "知识点2");
        KgKnowledgePoint kp3 = KgKnowledgePoint.create(TEST_URI_3, "知识点3");
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp1));
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp2));
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp3));

        List<KgKnowledgePoint> result = kgKnowledgePointRepository.findByUris(
                List.of(TEST_URI, TEST_URI_2));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(kp -> TEST_URI.equals(kp.getUri())));
        assertTrue(result.stream().anyMatch(kp -> TEST_URI_2.equals(kp.getUri())));
    }

    @Test
    @Order(6)
    @DisplayName("findByUris 全部不存在时应返回空列表")
    void findByUris_noMatch_shouldReturnEmpty() {
        List<KgKnowledgePoint> result = kgKnowledgePointRepository.findByUris(
                List.of("http://nonexistent/1", "http://nonexistent/2"));

        assertTrue(result.isEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("findByUris 部分匹配应只返回存在的")
    void findByUris_partialMatch_shouldReturnExisting() {
        KgKnowledgePoint kp1 = KgKnowledgePoint.create(TEST_URI, "知识点1");
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp1));

        List<KgKnowledgePoint> result = kgKnowledgePointRepository.findByUris(
                List.of(TEST_URI, "http://nonexistent/uri"));

        assertEquals(1, result.size());
        assertEquals(TEST_URI, result.get(0).getUri());
    }

    // ==================== 6.6.4 findByStatus ====================

    @Test
    @Order(8)
    @DisplayName("findByStatus 应返回指定状态的知识点")
    void findByStatus_shouldReturnMatching() {
        KgKnowledgePoint kp1 = KgKnowledgePoint.create(TEST_URI, "知识点1");
        KgKnowledgePoint kp2 = KgKnowledgePoint.create(TEST_URI_2, "知识点2");
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp1));
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp2));

        List<KgKnowledgePoint> active = kgKnowledgePointRepository.findByStatus("active");

        assertEquals(2, active.size());
        assertTrue(active.stream().allMatch(kp -> "active".equals(kp.getStatus())));
    }

    @Test
    @Order(9)
    @DisplayName("findByStatus 无匹配时应返回空列表")
    void findByStatus_noMatch_shouldReturnEmpty() {
        List<KgKnowledgePoint> result = kgKnowledgePointRepository.findByStatus("deleted");

        assertTrue(result.isEmpty());
    }

    // ==================== 6.6.4 updateStatus ====================

    @Test
    @Order(10)
    @DisplayName("updateStatus 应更新 status 字段")
    void updateStatus_shouldChangeStatus() {
        KgKnowledgePoint kp = KgKnowledgePoint.create(TEST_URI, "知识点");
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp));

        kgKnowledgePointRepository.updateStatus(TEST_URI, "deleted");

        KgKnowledgePointPo found = kgKnowledgePointMapper.selectByUri(TEST_URI);
        assertNotNull(found);
        assertEquals("deleted", found.getStatus());
        // is_deleted 不会被 updateStatus 修改
        assertFalse(found.getDeleted());
    }

    // ==================== Soft-delete consistency ====================

    @Test
    @Order(11)
    @DisplayName("软删除后 findByUri 仍能查到（updateStatus 不改 is_deleted）")
    void softDelete_findByUri_shouldStillFind() {
        KgKnowledgePoint kp = KgKnowledgePoint.create(TEST_URI, "知识点");
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp));

        kgKnowledgePointRepository.updateStatus(TEST_URI, "deleted");

        var result = kgKnowledgePointRepository.findByUri(TEST_URI);
        assertTrue(result.isPresent());
        assertEquals("deleted", result.get().getStatus());
    }

    @Test
    @Order(12)
    @DisplayName("软删除后 findByUris 仍能查到（is_deleted 仍为 false）")
    void softDelete_findByUris_shouldStillFind() {
        KgKnowledgePoint kp1 = KgKnowledgePoint.create(TEST_URI, "知识点1");
        KgKnowledgePoint kp2 = KgKnowledgePoint.create(TEST_URI_2, "知识点2");
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp1));
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp2));

        kgKnowledgePointRepository.updateStatus(TEST_URI, "deleted");

        List<KgKnowledgePoint> result = kgKnowledgePointRepository.findByUris(
                List.of(TEST_URI, TEST_URI_2));

        assertEquals(2, result.size());
    }

    @Test
    @Order(13)
    @DisplayName("updateStatus 后 findByStatus 过滤应生效")
    void updateStatus_findByStatus_shouldReflectChange() {
        KgKnowledgePoint kp1 = KgKnowledgePoint.create(TEST_URI, "知识点1");
        KgKnowledgePoint kp2 = KgKnowledgePoint.create(TEST_URI_2, "知识点2");
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp1));
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp2));

        kgKnowledgePointRepository.updateStatus(TEST_URI, "deleted");

        // findByStatus("active") 应只返回未被标记为 deleted 的
        List<KgKnowledgePoint> active = kgKnowledgePointRepository.findByStatus("active");
        assertEquals(1, active.size());
        assertEquals(TEST_URI_2, active.get(0).getUri());

        // findByStatus("deleted") 应返回被标记的
        List<KgKnowledgePoint> deleted = kgKnowledgePointRepository.findByStatus("deleted");
        assertEquals(1, deleted.size());
        assertEquals(TEST_URI, deleted.get(0).getUri());
    }

    // ==================== findById ====================

    @Test
    @Order(14)
    @DisplayName("findById 应通过主键查找")
    void findById_shouldReturnPresent() {
        KgKnowledgePoint kp = KgKnowledgePoint.create(TEST_URI, "知识点");
        KgKnowledgePointPo insertedPo = KgKnowledgePointPo.from(kp);
        kgKnowledgePointMapper.insert(insertedPo);
        Long id = insertedPo.getId();

        var result = kgKnowledgePointRepository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(TEST_URI, result.get().getUri());
    }

    @Test
    @Order(15)
    @DisplayName("findById 不存在的主键应返回 empty")
    void findById_notFound_shouldReturnEmpty() {
        var result = kgKnowledgePointRepository.findById(9999L);

        assertTrue(result.isEmpty());
    }
}
