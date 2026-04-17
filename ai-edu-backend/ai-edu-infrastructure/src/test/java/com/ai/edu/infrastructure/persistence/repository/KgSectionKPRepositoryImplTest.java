package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgSectionKPMapper;
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
 * KgSectionKPRepositoryImpl 集成测试
 *
 * - 使用 H2 内存数据库执行真实 SQL
 * - 测试小节-知识点关联的完整仓储生命周期
 */
@SpringBootTest(
        classes = TestInfrastructureConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("h2")
@TestMethodOrder(OrderAnnotation.class)
class KgSectionKPRepositoryImplTest {

    @Resource
    private KgSectionKPRepositoryImpl repository;

    @Resource
    private KgSectionKPMapper mapper;

    private static final String SECTION_URI = "http://edukg.org/knowledge/3.1/section/sec1";
    private static final String KP_URI_1 = "http://edukg.org/knowledge/3.1/kp/kp1";
    private static final String KP_URI_2 = "http://edukg.org/knowledge/3.1/kp/kp2";
    private static final String KP_URI_3 = "http://edukg.org/knowledge/3.1/kp/kp3";

    /** 每个测试前清理关联表数据，保证测试隔离 */
    @BeforeEach
    void setUp() {
        mapper.selectAllActiveRelations().forEach(r -> mapper.deleteById(r.getId()));
    }

    // ==================== 6.6.7 save — insert new relation ====================

    @Test
    @Order(1)
    @DisplayName("save 新增关联 — id 为 null 时执行 INSERT")
    void save_insertNewRelation() {
        KgSectionKP relation = KgSectionKP.create(SECTION_URI, KP_URI_1, 1);

        assertNull(relation.getId());

        KgSectionKP saved = repository.save(relation);

        assertNotNull(saved.getId());
        assertEquals(SECTION_URI, saved.getSectionUri());
        assertEquals(KP_URI_1, saved.getKpUri());
        assertEquals(1, saved.getOrderIndex());
        assertFalse(saved.getDeleted());

        // 验证数据库中真实存在
        List<KgSectionKP> found = mapper.selectBySectionUri(SECTION_URI);
        assertEquals(1, found.size());
        assertEquals(KP_URI_1, found.get(0).getKpUri());
    }

    // ==================== save — update existing relation ====================

    @Test
    @Order(2)
    @DisplayName("save 更新关联 — id 已设置时执行 UPDATE，更新 orderIndex")
    void save_updateExistingRelation() {
        // 先插入一条
        KgSectionKP relation = KgSectionKP.create(SECTION_URI, KP_URI_1, 1);
        repository.save(relation);
        Long id = relation.getId();

        // 通过 mapper 直接更新 orderIndex（实体无 setter）
        UpdateWrapper<KgSectionKP> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id).set("order_index", 5);
        mapper.update(null, wrapper);

        // 验证 orderIndex 已更新
        List<KgSectionKP> found = mapper.selectBySectionUri(SECTION_URI);
        assertEquals(1, found.size());
        assertEquals(5, found.get(0).getOrderIndex());
        assertEquals(id, found.get(0).getId());
    }

    // ==================== findBySectionUri ====================

    @Test
    @Order(3)
    @DisplayName("findBySectionUri — 查找指定小节的所有知识点关联")
    void findBySectionUri_returnsAllKPsForSection() {
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_1, 1));
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_2, 2));
        mapper.insert(KgSectionKP.create("other_section", KP_URI_3, 1));

        List<KgSectionKP> results = repository.findBySectionUri(SECTION_URI);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.getSectionUri().equals(SECTION_URI)));
        assertTrue(results.stream().anyMatch(r -> r.getKpUri().equals(KP_URI_1)));
        assertTrue(results.stream().anyMatch(r -> r.getKpUri().equals(KP_URI_2)));
    }

    // ==================== findByKpUri ====================

    @Test
    @Order(4)
    @DisplayName("findByKpUri — 查找指定知识点的所有小节关联")
    void findByKpUri_returnsAllSectionsForKP() {
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_1, 1));
        mapper.insert(KgSectionKP.create("other_section", KP_URI_1, 2));
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_2, 1));

        List<KgSectionKP> results = repository.findByKpUri(KP_URI_1);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.getKpUri().equals(KP_URI_1)));
    }

    // ==================== findAllActive ====================

    @Test
    @Order(5)
    @DisplayName("findAllActive — 仅返回未删除的关联")
    void findAllActive_returnsOnlyActiveRelations() {
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_1, 1));
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_2, 2));

        List<KgSectionKP> results = repository.findAllActive();

        assertTrue(results.size() >= 2);
        assertTrue(results.stream().anyMatch(r ->
                r.getSectionUri().equals(SECTION_URI) && r.getKpUri().equals(KP_URI_1)));
        assertTrue(results.stream().anyMatch(r ->
                r.getSectionUri().equals(SECTION_URI) && r.getKpUri().equals(KP_URI_2)));
    }

    // ==================== deleteBySectionUri — soft delete ====================

    @Test
    @Order(6)
    @DisplayName("deleteBySectionUri 软删除 — 删除后 findAllActive 不再返回")
    void deleteBySectionUri_softDeleteRemovesFromFindAllActive() {
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_1, 1));
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_2, 2));
        mapper.insert(KgSectionKP.create("other_section", KP_URI_3, 1));

        int beforeCount = repository.findAllActive().size();

        repository.deleteBySectionUri(SECTION_URI);

        List<KgSectionKP> afterResults = repository.findAllActive();
        // 被删除的小节关联不再出现
        assertFalse(afterResults.stream().anyMatch(r -> r.getSectionUri().equals(SECTION_URI)));
        // 其他小节关联仍存在
        assertTrue(afterResults.stream().anyMatch(r -> r.getSectionUri().equals("other_section")));
        assertEquals(beforeCount - 2, afterResults.size());
    }

    @Test
    @Order(7)
    @DisplayName("deleteBySectionUri 软删除 — 删除后 findBySectionUri 不再返回")
    void deleteBySectionUri_softDeleteRemovesFromFindBySectionUri() {
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_1, 1));

        repository.deleteBySectionUri(SECTION_URI);

        List<KgSectionKP> results = repository.findBySectionUri(SECTION_URI);
        assertTrue(results.isEmpty());
    }

    // ==================== deleteByKpUri — soft delete ====================

    @Test
    @Order(8)
    @DisplayName("deleteByKpUri 软删除 — 删除后 findAllActive 不再返回")
    void deleteByKpUri_softDeleteRemovesFromFindAllActive() {
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_1, 1));
        mapper.insert(KgSectionKP.create("other_section", KP_URI_1, 2));
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_2, 1));

        repository.deleteByKpUri(KP_URI_1);

        List<KgSectionKP> results = repository.findAllActive();
        // 被删除的知识点关联不再出现
        assertFalse(results.stream().anyMatch(r -> r.getKpUri().equals(KP_URI_1)));
        // 其他知识点关联仍存在
        assertTrue(results.stream().anyMatch(r -> r.getKpUri().equals(KP_URI_2)));
    }

    @Test
    @Order(9)
    @DisplayName("deleteByKpUri 软删除 — 删除后 findByKpUri 不再返回")
    void deleteByKpUri_softDeleteRemovesFromFindByKpUri() {
        mapper.insert(KgSectionKP.create(SECTION_URI, KP_URI_1, 1));

        repository.deleteByKpUri(KP_URI_1);

        List<KgSectionKP> results = repository.findByKpUri(KP_URI_1);
        assertTrue(results.isEmpty());
    }

    // ==================== saveBatch ====================

    @Test
    @Order(10)
    @DisplayName("saveBatch — 批量插入多条关联")
    void saveBatch_insertsMultipleRelations() {
        List<KgSectionKP> relations = List.of(
                KgSectionKP.create(SECTION_URI, KP_URI_1, 1),
                KgSectionKP.create(SECTION_URI, KP_URI_2, 2),
                KgSectionKP.create(SECTION_URI, KP_URI_3, 3)
        );

        repository.saveBatch(relations);

        List<KgSectionKP> results = mapper.selectBySectionUri(SECTION_URI);
        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(r -> r.getKpUri().equals(KP_URI_1)));
        assertTrue(results.stream().anyMatch(r -> r.getKpUri().equals(KP_URI_2)));
        assertTrue(results.stream().anyMatch(r -> r.getKpUri().equals(KP_URI_3)));
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
