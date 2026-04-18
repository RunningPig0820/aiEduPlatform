package com.ai.edu.infrastructure.persistence.neo4j;

import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.infrastructure.persistence.edukg.mapper.*;
import com.ai.edu.infrastructure.persistence.edukg.po.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import com.ai.edu.infrastructure.test.TestInfrastructureConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Neo4jKgSyncService 集成测试
 *
 * - MySQL: H2 内存数据库（真实 SQL 执行）
 * - Neo4j: 直连生产（只读，真实查询节点/关系）
 */
@SpringBootTest(
        classes = TestInfrastructureConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("h2")
@TestMethodOrder(OrderAnnotation.class)
class Neo4jKgSyncServiceTest {

    @Autowired
    private Neo4jKgSyncService syncService;

    @Autowired
    private KgTextbookMapper kgTextbookMapper;
    @Autowired
    private KgChapterMapper kgChapterMapper;
    @Autowired
    private KgSectionMapper kgSectionMapper;
    @Autowired
    private KgKnowledgePointMapper kgKnowledgePointMapper;
    @Autowired
    private KgTextbookChapterMapper kgTextbookChapterMapper;
    @Autowired
    private KgChapterSectionMapper kgChapterSectionMapper;
    @Autowired
    private KgSectionKPMapper kgSectionKPMapper;
    @Autowired
    private KgSyncRecordMapper kgSyncRecordMapper;

    private static final String TEST_URI = "http://edukg.org/knowledge/3.1/textbook/test_unit";

    /** 每个测试前硬删除清理测试数据 */
    @BeforeEach
    void setUp() {
        // 硬删除关联表（绕过逻辑删除）
        kgTextbookChapterMapper.selectAllActiveRelations().forEach(r ->
                kgTextbookChapterMapper.deleteById(r.getId()));
        kgChapterSectionMapper.selectAllActiveRelations().forEach(r ->
                kgChapterSectionMapper.deleteById(r.getId()));
        kgSectionKPMapper.selectAllActiveRelations().forEach(r ->
                kgSectionKPMapper.deleteById(r.getId()));
        // 硬删除节点表
        kgTextbookMapper.selectAllActive().forEach(tb ->
                kgTextbookMapper.deleteById(tb.getId()));
        kgChapterMapper.selectByStatus("active").forEach(ch ->
                kgChapterMapper.deleteById(ch.getId()));
        kgSectionMapper.selectByStatus("active").forEach(sec ->
                kgSectionMapper.deleteById(sec.getId()));
        kgKnowledgePointMapper.selectByStatus("active").forEach(kp ->
                kgKnowledgePointMapper.deleteById(kp.getId()));
        // 清理同步记录
        kgSyncRecordMapper.selectRecent(100).forEach(r ->
                kgSyncRecordMapper.deleteById(r.getId()));
    }

    // ==================== 6.3.4 upsertTextbooks ====================

    @Test
    @Order(1)
    @DisplayName("upsertTextbooks 新增场景 — H2 中不存在应 INSERT")
    void upsertTextbooks_insertNew() {
        // H2 是空的，selectByUri 应返回 null
        assertNull(kgTextbookMapper.selectByUri(TEST_URI));

        KgTextbook tb = KgTextbook.create(TEST_URI, "测试教材", "一年级", "primary", "人教版", "math");
        int count = syncService.upsertTextbooks(List.of(tb));

        assertEquals(1, count);
        // 验证真实写入 H2
        KgTextbookPo saved = kgTextbookMapper.selectByUri(TEST_URI);
        assertNotNull(saved);
        assertEquals(TEST_URI, saved.getUri());
        assertEquals("测试教材", saved.getLabel());
        assertEquals("一年级", saved.getGrade());
    }

    @Test
    @Order(2)
    @DisplayName("upsertTextbooks 更新场景 — H2 中已存在应用 Neo4j 新数据更新")
    void upsertTextbooks_updateExisting() {
        // 先插入一条旧数据
        KgTextbook old = KgTextbook.create(TEST_URI, "旧标签", "旧年级", "primary", "人教版", "旧学科");
        kgTextbookMapper.insert(KgTextbookPo.from(old));

        // 模拟从 Neo4j 来的新数据
        KgTextbook neo = KgTextbook.create(TEST_URI, "新标签", "新年级", "primary", "人教版", "新学科");
        int count = syncService.upsertTextbooks(List.of(neo));

        assertEquals(0, count); // 更新不计入 insert 计数
        // 验证 H2 数据被更新
        KgTextbookPo saved = kgTextbookMapper.selectByUri(TEST_URI);
        assertNotNull(saved);
        assertEquals("新标签", saved.getLabel());
        assertEquals("新年级", saved.getGrade());
        assertEquals("新学科", saved.getSubject());
    }

    @Test
    @Order(3)
    @DisplayName("upsertTextbooks 空列表应返回 0")
    void upsertTextbooks_emptyList() {
        assertEquals(0, syncService.upsertTextbooks(Collections.emptyList()));
    }

    @Test
    @Order(4)
    @DisplayName("upsertTextbooks null 应返回 0")
    void upsertTextbooks_null() {
        assertEquals(0, syncService.upsertTextbooks(null));
    }

    // ==================== 6.3.5 upsertChapters ====================

    @Test
    @Order(5)
    @DisplayName("upsertChapters 仅新增，已存在的不插入")
    void upsertChapters_onlyInsertNew() {
        String uri1 = "http://edukg.org/knowledge/3.1/chapter/ch1";
        String uri2 = "http://edukg.org/knowledge/3.1/chapter/ch2";

        // 预插入已存在的章节
        kgChapterMapper.insert(KgChapterPo.from(KgChapter.create(uri1, "已存在")));

        KgChapter ch1 = KgChapter.create(uri1, "已存在");
        KgChapter ch2 = KgChapter.create(uri2, "新章节");
        int count = syncService.upsertChapters(List.of(ch1, ch2));

        assertEquals(1, count);
        assertNotNull(kgChapterMapper.selectByUri(uri1));       // ch1 仍存在
        assertNotNull(kgChapterMapper.selectByUri(uri2));       // ch2 插入了
    }

    @Test
    @Order(6)
    @DisplayName("upsertChapters 已存在的不更新")
    void upsertChapters_shouldNotUpdateExisting() {
        String uri = "http://edukg.org/knowledge/3.1/chapter/ch1";
        kgChapterMapper.insert(KgChapterPo.from(KgChapter.create(uri, "旧标签")));

        KgChapter ch = KgChapter.create(uri, "新标签");
        int count = syncService.upsertChapters(List.of(ch));

        assertEquals(0, count);
        assertEquals("旧标签", kgChapterMapper.selectByUri(uri).getLabel());
    }

    @Test
    @Order(7)
    @DisplayName("upsertSections 仅新增")
    void upsertSections_onlyInsertNew() {
        String uri = "http://edukg.org/knowledge/3.1/section/sec1";
        KgSection sec = KgSection.create(uri, "小节1");
        int count = syncService.upsertSections(List.of(sec));

        assertEquals(1, count);
        assertNotNull(kgSectionMapper.selectByUri(uri));
    }

    @Test
    @Order(8)
    @DisplayName("upsertKnowledgePoints 仅新增")
    void upsertKnowledgePoints_onlyInsertNew() {
        String uri = "http://edukg.org/knowledge/3.1/kp/kp1";
        KgKnowledgePoint kp = KgKnowledgePoint.create(uri, "知识点1");
        int count = syncService.upsertKnowledgePoints(List.of(kp));

        assertEquals(1, count);
        assertNotNull(kgKnowledgePointMapper.selectByUri(uri));
    }

    // ==================== 6.3.6 rebuildTextbookChapterRelations ====================

    @Test
    @Order(9)
    @DisplayName("rebuildTextbookChapterRelations 新增关联 — 真实写入 H2")
    void rebuildTextbookChapterRelations_insertNew() {
        String tbUri = TEST_URI;
        String chUri = "http://edukg.org/knowledge/3.1/chapter/ch1";

        // H2 为空，应执行 INSERT
        KgTextbookChapter rel = KgTextbookChapter.create(tbUri, chUri, 1);
        int ops = syncService.rebuildTextbookChapterRelations(List.of(rel));

        assertEquals(1, ops);
        List<KgTextbookChapterPo> relations = kgTextbookChapterMapper.selectByTextbookUri(tbUri);
        assertEquals(1, relations.size());
        assertEquals(chUri, relations.get(0).getChapterUri());
        assertEquals(1, relations.get(0).getOrderIndex());
    }

    @Test
    @Order(10)
    @DisplayName("rebuildTextbookChapterRelations 更新 orderIndex")
    void rebuildTextbookChapterRelations_updateOrderIndex() {
        String tbUri = TEST_URI;
        String chUri = "http://edukg.org/knowledge/3.1/chapter/ch1";

        // 预插入旧 orderIndex=1
        kgTextbookChapterMapper.insert(KgTextbookChapterPo.from(KgTextbookChapter.create(tbUri, chUri, 1)));

        // Neo4j 返回 orderIndex=3
        KgTextbookChapter neo4jRel = KgTextbookChapter.create(tbUri, chUri, 3);
        int ops = syncService.rebuildTextbookChapterRelations(List.of(neo4jRel));

        assertEquals(1, ops);
        List<KgTextbookChapterPo> relations = kgTextbookChapterMapper.selectByTextbookUri(tbUri);
        assertEquals(3, relations.get(0).getOrderIndex());
    }

    @Test
    @Order(11)
    @DisplayName("rebuildTextbookChapterRelations 软删除不存在关联")
    void rebuildTextbookChapterRelations_softDeleteMissing() {
        String tbUri = TEST_URI;
        String chUri1 = "http://edukg.org/knowledge/3.1/chapter/ch1";
        String chUri2 = "http://edukg.org/knowledge/3.1/chapter/ch2";

        // H2 有两条关联
        kgTextbookChapterMapper.insert(KgTextbookChapterPo.from(KgTextbookChapter.create(tbUri, chUri1, 1)));
        kgTextbookChapterMapper.insert(KgTextbookChapterPo.from(KgTextbookChapter.create(tbUri, chUri2, 2)));

        // Neo4j 只有 chUri1
        KgTextbookChapter neo4jRel = KgTextbookChapter.create(tbUri, chUri1, 1);
        int ops = syncService.rebuildTextbookChapterRelations(List.of(neo4jRel));

        assertEquals(1, ops);
        // chUri2 应被软删除
        List<KgTextbookChapterPo> relations = kgTextbookChapterMapper.selectByTextbookUri(tbUri);
        assertEquals(1, relations.size());
        assertEquals(chUri1, relations.get(0).getChapterUri());
    }

    @Test
    @Order(12)
    @DisplayName("rebuildTextbookChapterRelations 空列表应全量软删除")
    void rebuildTextbookChapterRelations_emptyList() {
        // 预插入 3 条
        kgTextbookChapterMapper.insert(KgTextbookChapterPo.from(KgTextbookChapter.create(TEST_URI, "ch1", 1)));
        kgTextbookChapterMapper.insert(KgTextbookChapterPo.from(KgTextbookChapter.create(TEST_URI, "ch2", 2)));
        kgTextbookChapterMapper.insert(KgTextbookChapterPo.from(KgTextbookChapter.create(TEST_URI, "ch3", 3)));

        assertEquals(3, kgTextbookChapterMapper.selectAllActiveRelations().size());

        int ops = syncService.rebuildTextbookChapterRelations(Collections.emptyList());

        assertEquals(0, ops);
        assertEquals(0, kgTextbookChapterMapper.selectAllActiveRelations().size());
    }

    // ==================== 6.3.7 rebuildChapterSectionRelations / rebuildSectionKPRelations ====================

    @Test
    @Order(13)
    @DisplayName("rebuildChapterSectionRelations 新增关联")
    void rebuildChapterSectionRelations_insertNew() {
        String chUri = "http://edukg.org/knowledge/3.1/chapter/ch1";
        String secUri = "http://edukg.org/knowledge/3.1/section/sec1";

        KgChapterSection rel = KgChapterSection.create(chUri, secUri, 1);
        int ops = syncService.rebuildChapterSectionRelations(List.of(rel));

        assertEquals(1, ops);
        assertEquals(1, kgChapterSectionMapper.selectByChapterUri(chUri).size());
    }

    @Test
    @Order(14)
    @DisplayName("rebuildSectionKPRelations 新增关联")
    void rebuildSectionKPRelations_insertNew() {
        String secUri = "http://edukg.org/knowledge/3.1/section/sec1";
        String kpUri = "http://edukg.org/knowledge/3.1/kp/kp1";

        KgSectionKP rel = KgSectionKP.create(secUri, kpUri, 1);
        int ops = syncService.rebuildSectionKPRelations(List.of(rel));

        assertEquals(1, ops);
        assertEquals(1, kgSectionKPMapper.selectBySectionUri(secUri).size());
    }

    @Test
    @Order(15)
    @DisplayName("rebuildChapterSectionRelations 空列表全量软删除")
    void rebuildChapterSectionRelations_emptyList() {
        kgChapterSectionMapper.insert(KgChapterSectionPo.from(KgChapterSection.create("ch1", "sec1", 1)));
        kgChapterSectionMapper.insert(KgChapterSectionPo.from(KgChapterSection.create("ch1", "sec2", 2)));

        int ops = syncService.rebuildChapterSectionRelations(Collections.emptyList());
        assertEquals(0, ops);
        assertEquals(0, kgChapterSectionMapper.selectAllActiveRelations().size());
    }

    @Test
    @Order(16)
    @DisplayName("rebuildSectionKPRelations 空列表全量软删除")
    void rebuildSectionKPRelations_emptyList() {
        kgSectionKPMapper.insert(KgSectionKPPo.from(KgSectionKP.create("sec1", "kp1", 1)));

        int ops = syncService.rebuildSectionKPRelations(Collections.emptyList());
        assertEquals(0, ops);
        assertEquals(0, kgSectionKPMapper.selectAllActiveRelations().size());
    }

    // ==================== 6.3.8 markDeletedNodes ====================

    @Test
    @Order(17)
    @DisplayName("markDeletedNodes Textbook — H2 有但 Neo4j 无应标记 deleted")
    void markDeletedNodes_textbook() {
        kgTextbookMapper.insert(KgTextbookPo.from(KgTextbook.create(TEST_URI, "教材", "一年级", "primary", "人教版", "math")));

        int count = syncService.markDeletedNodes("Textbook", Set.of());

        assertEquals(1, count);
        // updateStatus 只改 status 字段，不改 is_deleted
        // 验证 status 被更新为 "deleted"
        List<KgTextbookPo> all = kgTextbookMapper.selectAllActive();
        KgTextbookPo tb = all.stream().filter(t -> t.getUri().equals(TEST_URI)).findFirst().orElse(null);
        assertNotNull(tb);
        assertEquals("deleted", tb.getStatus());
    }

    @Test
    @Order(18)
    @DisplayName("markDeletedNodes Chapter")
    void markDeletedNodes_chapter() {
        String uri = "http://edukg.org/knowledge/3.1/chapter/ch1";
        kgChapterMapper.insert(KgChapterPo.from(KgChapter.create(uri, "章节1")));

        int count = syncService.markDeletedNodes("Chapter", Set.of());

        assertEquals(1, count);
        assertNotNull(kgChapterMapper.selectByUri(uri));
    }

    @Test
    @Order(19)
    @DisplayName("markDeletedNodes Section")
    void markDeletedNodes_section() {
        String uri = "http://edukg.org/knowledge/3.1/section/sec1";
        kgSectionMapper.insert(KgSectionPo.from(KgSection.create(uri, "小节1")));

        int count = syncService.markDeletedNodes("Section", Set.of());

        assertEquals(1, count);
        assertNotNull(kgSectionMapper.selectByUri(uri));
    }

    @Test
    @Order(20)
    @DisplayName("markDeletedNodes KnowledgePoint")
    void markDeletedNodes_knowledgePoint() {
        String uri = "http://edukg.org/knowledge/3.1/kp/kp1";
        kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(KgKnowledgePoint.create(uri, "知识点1")));

        int count = syncService.markDeletedNodes("KnowledgePoint", Set.of());

        assertEquals(1, count);
        assertNotNull(kgKnowledgePointMapper.selectByUri(uri));
    }

    @Test
    @Order(21)
    @DisplayName("markDeletedNodes Neo4j 中有该 URI 不标记删除")
    void markDeletedNodes_shouldNotDeleteIfInNeo4j() {
        kgTextbookMapper.insert(KgTextbookPo.from(KgTextbook.create(TEST_URI, "教材", "一年级", "primary", "人教版", "math")));

        int count = syncService.markDeletedNodes("Textbook", Set.of(TEST_URI));

        assertEquals(0, count);
        assertNotNull(kgTextbookMapper.selectByUri(TEST_URI));
    }

    // ==================== 6.3.9 validateUris ====================

    @Test
    @Order(22)
    @DisplayName("validateUris 正常 URI 应通过")
    void validateUris_validUris() {
        var result = syncService.validateUris(
                List.of("http://edukg.org/knowledge/3.1/textbook/一年级上册"), "Textbook");
        assertTrue(result.valid);
        assertTrue(result.errors.isEmpty());
    }

    @Test
    @Order(23)
    @DisplayName("validateUris 空 URI 应报错")
    void validateUris_nullUri() {
        var result = syncService.validateUris(Arrays.asList(null, ""), "Textbook");
        assertFalse(result.valid);
        assertTrue(result.errors.stream().anyMatch(e -> e.contains("null or blank")));
    }

    @Test
    @Order(24)
    @DisplayName("validateUris 非法格式应报错")
    void validateUris_invalidFormat() {
        var result = syncService.validateUris(List.of("short"), "Textbook");
        assertFalse(result.valid);
        assertTrue(result.errors.stream().anyMatch(e -> e.contains("Invalid URI format")));
    }

    @Test
    @Order(25)
    @DisplayName("validateUris 重复 URI 应报错")
    void validateUris_duplicateUri() {
        String uri = "http://edukg.org/knowledge/3.1/textbook/一年级上册";
        var result = syncService.validateUris(List.of(uri, uri), "Textbook");
        assertFalse(result.valid);
        assertTrue(result.errors.stream().anyMatch(e -> e.contains("Duplicate")));
    }

    @Test
    @Order(26)
    @DisplayName("validateUris 空白 URI 应报错")
    void validateUris_blankUri() {
        var result = syncService.validateUris(List.of("   "), "Textbook");
        assertFalse(result.valid);
    }

    // ==================== 6.3.10 validateAllUris ====================

    @Test
    @Order(27)
    @DisplayName("validateAllUris 全部合法应通过")
    void validateAllUris_allValid() {
        String base = "http://edukg.org/knowledge/3.1/";
        var result = syncService.validateAllUris(
                List.of(KgTextbook.create(base + "textbook/1", "教材1", "一年级", "primary", "人教版", "math")),
                List.of(KgChapter.create(base + "chapter/1", "章节1")),
                List.of(KgSection.create(base + "section/1", "小节1")),
                List.of(KgKnowledgePoint.create(base + "kp/1", "知识点1")));
        assertTrue(result.valid);
        assertTrue(result.errors.isEmpty());
    }

    @Test
    @Order(28)
    @DisplayName("validateAllUris 部分非法应报错")
    void validateAllUris_partialInvalid() {
        var result = syncService.validateAllUris(
                List.of(KgTextbook.create("short", "非法", "一年级", "primary", "人教版", "math")),
                List.of(KgChapter.create("http://edukg.org/knowledge/3.1/chapter/1", "章节1")),
                Collections.emptyList(), Collections.emptyList());
        assertFalse(result.valid);
        assertTrue(result.errors.stream().anyMatch(e -> e.contains("Invalid URI format")));
    }

    // ==================== 6.3.11 reconcile ====================

    @Test
    @Order(29)
    @DisplayName("reconcile 数量一致应 matched — 基于真实 H2 数据")
    void reconcile_countsMatch() {
        // H2 为空，Neo4j 也传空
        var result = syncService.reconcile(
                Set.of(), Set.of(), Set.of(), Set.of(),
                List.of(), List.of(), List.of());

        assertTrue(result.matched);
        assertEquals(0, result.mysqlTextbookCount);
        assertEquals(0, result.neo4jTextbookCount);
    }

    @Test
    @Order(30)
    @DisplayName("reconcile 数量不一致应 mismatched")
    void reconcile_countsMismatch() {
        // H2 插入 2 条教材，Neo4j 传 1 条
        kgTextbookMapper.insert(KgTextbookPo.from(KgTextbook.create(TEST_URI + "1", "教材1", "一年级", "primary", "人教版", "math")));
        kgTextbookMapper.insert(KgTextbookPo.from(KgTextbook.create(TEST_URI + "2", "教材2", "一年级", "primary", "人教版", "math")));

        var result = syncService.reconcile(
                Set.of("uri1"), Set.of(), Set.of(), Set.of(),
                List.of(), List.of(), List.of());

        assertFalse(result.matched);
        assertEquals(2, result.mysqlTextbookCount);
        assertEquals(1, result.neo4jTextbookCount);
        assertTrue(result.differences.get(0).contains("Textbook count mismatch"));
    }

    // ==================== 6.3.12 checkNeo4jHealth ====================

    @Test
    @Order(31)
    @DisplayName("checkNeo4jHealth 真实 Neo4j 连接应健康")
    void checkNeo4jHealth_healthy() {
        var result = syncService.checkNeo4jHealth();

        assertNotNull(result);
        assertTrue(result.healthy, "Neo4j 应连接正常: " + result.message);
        assertTrue(result.responseTimeMs < 10000, "响应时间应 < 10s: " + result.responseTimeMs + "ms");
    }

    // ==================== Sync Record Methods ====================

    @Test
    @Order(32)
    @DisplayName("createSyncRecord 应真实插入 H2")
    void createSyncRecord_shouldInsert() {
        KgSyncRecord record = syncService.createSyncRecord("full", "grade=一年级", 100L);

        assertNotNull(record);
        assertNotNull(record.getId()); // H2 AUTO_INCREMENT 应生成 id
        assertEquals("full", record.getSyncType());
        assertEquals("grade=一年级", record.getScope());
        assertEquals("running", record.getStatus());
    }

    @Test
    @Order(33)
    @DisplayName("completeSyncRecord 应更新 H2 记录")
    void completeSyncRecord_shouldUpdate() {
        KgSyncRecord record = syncService.createSyncRecord("full", null, 100L);
        Long id = record.getId();

        syncService.completeSyncRecord(id, 10, 5, 2, "matched", "All good");

        KgSyncRecordPo saved = kgSyncRecordMapper.selectById(id);
        assertEquals("success", saved.getStatus());
        assertEquals(10, saved.getInsertedCount());
        assertEquals(5, saved.getUpdatedCount());
        assertEquals(2, saved.getStatusChangedCount());
        assertEquals("matched", saved.getReconciliationStatus());
    }

    @Test
    @Order(34)
    @DisplayName("failSyncRecord 应更新 H2 记录为失败")
    void failSyncRecord_shouldUpdate() {
        KgSyncRecord record = syncService.createSyncRecord("full", null, 100L);
        Long id = record.getId();

        syncService.failSyncRecord(id, "Neo4j timeout");

        KgSyncRecordPo saved = kgSyncRecordMapper.selectById(id);
        assertEquals("failed", saved.getStatus());
        assertEquals("Neo4j timeout", saved.getErrorMessage());
    }

    @Test
    @Order(35)
    @DisplayName("completeSyncRecord 记录不存在应无操作")
    void completeSyncRecord_recordNotFound() {
        assertDoesNotThrow(() ->
                syncService.completeSyncRecord(999L, 10, 5, 2, "matched", "All good"));
    }

    @Test
    @Order(36)
    @DisplayName("getSyncRecords 应返回 H2 中的记录")
    void getSyncRecords_shouldReturnRecords() {
        // 先插入
        syncService.createSyncRecord("full", "scope1", 100L);
        syncService.createSyncRecord("partial", "scope2", 100L);

        var result = syncService.getSyncRecords(5);
        assertTrue(result.size() >= 2);
    }

    @Test
    @Order(37)
    @DisplayName("getLatestSyncRecord 应返回最新记录")
    void getLatestSyncRecord_withRecords() {
        syncService.createSyncRecord("full", null, 100L);

        var result = syncService.getLatestSyncRecord();
        assertNotNull(result);
        assertEquals("full", result.getSyncType());
    }

    @Test
    @Order(38)
    @DisplayName("getLatestSyncRecord 无记录应返回 null")
    void getLatestSyncRecord_noRecords() {
        // 清理所有同步记录
        kgSyncRecordMapper.selectRecent(100).forEach(r -> kgSyncRecordMapper.deleteById(r.getId()));

        var result = syncService.getLatestSyncRecord();
        assertNull(result);
    }

    // ==================== Neo4j 真实查询测试 ====================

    @Test
    @Order(39)
    @DisplayName("syncTextbookNodes 真实 Neo4j 查询应返回节点列表")
    void syncTextbookNodes_fromRealNeo4j() {
        List<KgTextbook> textbooks = syncService.syncTextbookNodes();

        // Neo4j 可能有数据，只要不抛异常就说明连接正常
        assertNotNull(textbooks);
        if (textbooks.isEmpty()) {
            System.out.println("[Neo4j] Textbook nodes returned 0 — Neo4j may have no data");
        } else {
            System.out.println("[Neo4j] Textbook nodes: " + textbooks.size());
            // 验证返回的实体字段正确映射
            KgTextbook first = textbooks.get(0);
            assertNotNull(first.getUri());
            assertNotNull(first.getLabel());
        }
    }

    @Test
    @Order(40)
    @DisplayName("syncChapterNodes 真实 Neo4j 查询应返回节点列表")
    void syncChapterNodes_fromRealNeo4j() {
        List<KgChapter> chapters = syncService.syncChapterNodes();
        assertNotNull(chapters);
        System.out.println("[Neo4j] Chapter nodes: " + chapters.size());
    }

    @Test
    @Order(41)
    @DisplayName("syncSectionNodes 真实 Neo4j 查询应返回节点列表")
    void syncSectionNodes_fromRealNeo4j() {
        List<KgSection> sections = syncService.syncSectionNodes();
        assertNotNull(sections);
        System.out.println("[Neo4j] Section nodes: " + sections.size());
    }

    @Test
    @Order(42)
    @DisplayName("syncKnowledgePointNodes 真实 Neo4j 查询应返回节点列表")
    void syncKnowledgePointNodes_fromRealNeo4j() {
        List<KgKnowledgePoint> kps = syncService.syncKnowledgePointNodes();
        assertNotNull(kps);
        System.out.println("[Neo4j] KnowledgePoint nodes: " + kps.size());
    }

    @Test
    @Order(43)
    @DisplayName("syncTextbookChapterRelations 真实 Neo4j 查询应返回关系列表")
    void syncTextbookChapterRelations_fromRealNeo4j() {
        List<KgTextbookChapter> relations = syncService.syncTextbookChapterRelations();
        assertNotNull(relations);
        System.out.println("[Neo4j] Textbook-Chapter relations: " + relations.size());
    }

    @Test
    @Order(44)
    @DisplayName("syncChapterSectionRelations 真实 Neo4j 查询应返回关系列表")
    void syncChapterSectionRelations_fromRealNeo4j() {
        List<KgChapterSection> relations = syncService.syncChapterSectionRelations();
        assertNotNull(relations);
        System.out.println("[Neo4j] Chapter-Section relations: " + relations.size());
    }

    @Test
    @Order(45)
    @DisplayName("syncSectionKPRelations 真实 Neo4j 查询应返回关系列表")
    void syncSectionKPRelations_fromRealNeo4j() {
        List<KgSectionKP> relations = syncService.syncSectionKPRelations();
        assertNotNull(relations);
        System.out.println("[Neo4j] Section-KP relations: " + relations.size());
    }
}
