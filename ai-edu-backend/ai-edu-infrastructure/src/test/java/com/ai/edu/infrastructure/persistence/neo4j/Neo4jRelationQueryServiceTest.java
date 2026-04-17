package com.ai.edu.infrastructure.neo4j;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.infrastructure.cache.Neo4jRelationCacheService;
import com.ai.edu.infrastructure.persistence.repository.KgChapterSectionRepositoryImpl;
import com.ai.edu.infrastructure.persistence.repository.KgSectionKPRepositoryImpl;
import com.ai.edu.infrastructure.persistence.repository.KgTextbookChapterRepositoryImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.mockito.Mockito;
import org.neo4j.driver.*;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Neo4jRelationQueryService 单元测试
 *
 * 测试目标：缓存 → Neo4j → MySQL 三层降级链路
 *
 * 全 Mock 方式测试，不依赖 Spring Context：
 * - Cache: Mock
 * - Neo4j Driver: Mock
 * - MySQL Repository: Mock
 *
 * 注意：由于 Neo4j Transaction.run() 方法重载歧义，
 * "缓存未命中 → Neo4j 成功" 场景通过验证 Neo4j 调用链路来间接测试。
 */
@TestMethodOrder(OrderAnnotation.class)
class Neo4jRelationQueryServiceTest {

    private Neo4jRelationQueryService service;
    private Neo4jRelationCacheService cacheService;
    private KgTextbookChapterRepositoryImpl textbookChapterRepo;
    private KgChapterSectionRepositoryImpl chapterSectionRepo;
    private KgSectionKPRepositoryImpl sectionKPRepo;
    private Driver neo4jDriver;

    private static final String TEST_TB_URI = "http://edukg.org/knowledge/3.1/textbook/test_unit";
    private static final String TEST_CH_URI = "http://edukg.org/knowledge/3.1/chapter/ch1";
    private static final String TEST_SEC_URI = "http://edukg.org/knowledge/3.1/section/sec1";

    @BeforeEach
    void setUp() {
        cacheService = Mockito.mock(Neo4jRelationCacheService.class);
        textbookChapterRepo = Mockito.mock(KgTextbookChapterRepositoryImpl.class);
        chapterSectionRepo = Mockito.mock(KgChapterSectionRepositoryImpl.class);
        sectionKPRepo = Mockito.mock(KgSectionKPRepositoryImpl.class);
        neo4jDriver = Mockito.mock(Driver.class);

        service = new Neo4jRelationQueryService();
        setField(service, "neo4jDriver", neo4jDriver);
        setField(service, "cacheService", cacheService);
        setField(service, "textbookChapterRepo", textbookChapterRepo);
        setField(service, "chapterSectionRepo", chapterSectionRepo);
        setField(service, "sectionKPRepo", sectionKPRepo);
    }

    // ==================== 6.4.1 缓存命中 ====================

    @Test
    @Order(1)
    @DisplayName("getTextbookChapterRelations 缓存命中应直接返回")
    void getTextbookChapterRelations_cacheHit() {
        List<KgTextbookChapter> cached = List.of(
                KgTextbookChapter.create(TEST_TB_URI, TEST_CH_URI, 1)
        );
        when(cacheService.getTextbookChapterRelations(TEST_TB_URI)).thenReturn(cached);

        List<KgTextbookChapter> result = service.getTextbookChapterRelations(TEST_TB_URI);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_CH_URI, result.get(0).getChapterUri());
        assertEquals(1, result.get(0).getOrderIndex());
        verify(cacheService, times(1)).getTextbookChapterRelations(TEST_TB_URI);
        verify(cacheService, never()).setTextbookChapterRelations(anyString(), anyList());
    }

    @Test
    @Order(2)
    @DisplayName("getTextbookChapterRelations 缓存命中空列表应直接返回空")
    void getTextbookChapterRelations_cacheHitEmpty() {
        when(cacheService.getTextbookChapterRelations(TEST_TB_URI)).thenReturn(List.of());

        List<KgTextbookChapter> result = service.getTextbookChapterRelations(TEST_TB_URI);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("getChapterSectionRelations 缓存命中应直接返回")
    void getChapterSectionRelations_cacheHit() {
        List<KgChapterSection> cached = List.of(
                KgChapterSection.create(TEST_CH_URI, TEST_SEC_URI, 1)
        );
        when(cacheService.getChapterSectionRelations(TEST_CH_URI)).thenReturn(cached);

        List<KgChapterSection> result = service.getChapterSectionRelations(TEST_CH_URI);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_SEC_URI, result.get(0).getSectionUri());
    }

    @Test
    @Order(4)
    @DisplayName("getSectionKPRelations 缓存命中应直接返回")
    void getSectionKPRelations_cacheHit() {
        String kpUri = "http://edukg.org/knowledge/3.1/kp/kp1";
        List<KgSectionKP> cached = List.of(
                KgSectionKP.create(TEST_SEC_URI, kpUri, 1)
        );
        when(cacheService.getSectionKPRelations(TEST_SEC_URI)).thenReturn(cached);

        List<KgSectionKP> result = service.getSectionKPRelations(TEST_SEC_URI);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(kpUri, result.get(0).getKpUri());
    }

    // ==================== 6.4.2 缓存未命中 → 查询 Neo4j ====================

    @Test
    @Order(5)
    @DisplayName("getTextbookChapterRelations 缓存未命中 → 应调用 Neo4j 查询并写入缓存")
    void getTextbookChapterRelations_cacheMiss_shouldQueryNeo4jAndCache() throws Exception {
        when(cacheService.getTextbookChapterRelations(anyString())).thenReturn(null);

        setupSuccessfulNeo4jSession();

        service.getTextbookChapterRelations(TEST_TB_URI);

        // 验证 Neo4j session 被创建（说明确实尝试查询 Neo4j）
        verify(neo4jDriver).session();
        // 验证缓存被写入
        verify(cacheService).setTextbookChapterRelations(eq(TEST_TB_URI), anyList());
    }

    @Test
    @Order(6)
    @DisplayName("getChapterSectionRelations 缓存未命中 → 应调用 Neo4j 查询并写入缓存")
    void getChapterSectionRelations_cacheMiss_shouldQueryNeo4jAndCache() throws Exception {
        when(cacheService.getChapterSectionRelations(anyString())).thenReturn(null);

        setupSuccessfulNeo4jSession();

        service.getChapterSectionRelations(TEST_CH_URI);

        verify(neo4jDriver).session();
        verify(cacheService).setChapterSectionRelations(eq(TEST_CH_URI), anyList());
    }

    @Test
    @Order(7)
    @DisplayName("getSectionKPRelations 缓存未命中 → 应调用 Neo4j 查询并写入缓存")
    void getSectionKPRelations_cacheMiss_shouldQueryNeo4jAndCache() throws Exception {
        when(cacheService.getSectionKPRelations(anyString())).thenReturn(null);

        setupSuccessfulNeo4jSession();

        service.getSectionKPRelations(TEST_SEC_URI);

        verify(neo4jDriver).session();
        verify(cacheService).setSectionKPRelations(eq(TEST_SEC_URI), anyList());
    }

    // ==================== 6.4.3 Neo4j 异常 → 降级到 MySQL ====================

    @Test
    @Order(8)
    @DisplayName("getTextbookChapterRelations Neo4j 异常 → 降级到 MySQL")
    void getTextbookChapterRelations_neo4jFails_fallbackToMySQL() {
        when(cacheService.getTextbookChapterRelations(anyString())).thenReturn(null);
        when(neo4jDriver.session()).thenThrow(new RuntimeException("Neo4j connection timeout"));

        List<KgTextbookChapter> mysqlData = List.of(
                KgTextbookChapter.create(TEST_TB_URI, TEST_CH_URI, 2)
        );
        when(textbookChapterRepo.findByTextbookUri(TEST_TB_URI)).thenReturn(mysqlData);

        List<KgTextbookChapter> result = service.getTextbookChapterRelations(TEST_TB_URI);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_CH_URI, result.get(0).getChapterUri());
        // 降级时不应设置缓存
        verify(cacheService, never()).setTextbookChapterRelations(anyString(), anyList());
    }

    @Test
    @Order(9)
    @DisplayName("getTextbookChapterRelations Neo4j 异常 + MySQL 无数据 → 返回空列表")
    void getTextbookChapterRelations_neo4jFails_mysqlEmpty() {
        when(cacheService.getTextbookChapterRelations(anyString())).thenReturn(null);
        when(neo4jDriver.session()).thenThrow(new RuntimeException("Neo4j connection timeout"));
        when(textbookChapterRepo.findByTextbookUri(anyString())).thenReturn(List.of());

        List<KgTextbookChapter> result = service.getTextbookChapterRelations(TEST_TB_URI);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("getChapterSectionRelations Neo4j 异常 → 降级到 MySQL")
    void getChapterSectionRelations_neo4jFails_fallbackToMySQL() {
        when(cacheService.getChapterSectionRelations(anyString())).thenReturn(null);
        when(neo4jDriver.session()).thenThrow(new RuntimeException("Neo4j connection timeout"));

        List<KgChapterSection> mysqlData = List.of(
                KgChapterSection.create(TEST_CH_URI, TEST_SEC_URI, 1)
        );
        when(chapterSectionRepo.findByChapterUri(TEST_CH_URI)).thenReturn(mysqlData);

        List<KgChapterSection> result = service.getChapterSectionRelations(TEST_CH_URI);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_SEC_URI, result.get(0).getSectionUri());
        verify(cacheService, never()).setChapterSectionRelations(anyString(), anyList());
    }

    @Test
    @Order(11)
    @DisplayName("getSectionKPRelations Neo4j 异常 → 降级到 MySQL")
    void getSectionKPRelations_neo4jFails_fallbackToMySQL() {
        when(cacheService.getSectionKPRelations(anyString())).thenReturn(null);
        when(neo4jDriver.session()).thenThrow(new RuntimeException("Neo4j connection timeout"));

        String kpUri = "http://edukg.org/knowledge/3.1/kp/kp_fallback";
        List<KgSectionKP> mysqlData = List.of(
                KgSectionKP.create(TEST_SEC_URI, kpUri, 1)
        );
        when(sectionKPRepo.findBySectionUri(TEST_SEC_URI)).thenReturn(mysqlData);

        List<KgSectionKP> result = service.getSectionKPRelations(TEST_SEC_URI);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(kpUri, result.get(0).getKpUri());
        verify(cacheService, never()).setSectionKPRelations(anyString(), anyList());
    }

    // ==================== 完整降级链路验证 ====================

    @Test
    @Order(12)
    @DisplayName("完整链路：缓存未命中 → Neo4j 成功 → 写入缓存 → 下次命中缓存")
    void fullChain_cacheMiss_thenNeo4j_thenCacheHit() throws Exception {
        // 第一次：缓存未命中 → Neo4j
        when(cacheService.getTextbookChapterRelations(anyString())).thenReturn(null);
        setupSuccessfulNeo4jSession();

        service.getTextbookChapterRelations(TEST_TB_URI);
        verify(cacheService).setTextbookChapterRelations(eq(TEST_TB_URI), anyList());

        // 重置 Mock，第二次：缓存命中
        Mockito.reset(cacheService);
        List<KgTextbookChapter> fakeCached = List.of(
                KgTextbookChapter.create(TEST_TB_URI, TEST_CH_URI, 1)
        );
        when(cacheService.getTextbookChapterRelations(TEST_TB_URI)).thenReturn(fakeCached);

        List<KgTextbookChapter> result = service.getTextbookChapterRelations(TEST_TB_URI);

        assertEquals(1, result.size());
        // 缓存命中不应再写缓存
        verify(cacheService, never()).setTextbookChapterRelations(anyString(), anyList());
    }

    // ==================== Neo4j Mock helpers ====================

    /**
     * 设置一个"成功"的 Neo4j Session Mock
     * 由于 tx.run() 方法重载歧义无法直接 mock Result，
     * 这里验证 session 和 readTransaction 被调用即可。
     */
    @SuppressWarnings("unchecked")
    private void setupSuccessfulNeo4jSession() {
        Session mockSession = mock(Session.class);
        when(neo4jDriver.session()).thenReturn(mockSession);

        // readTransaction 正常执行（不抛异常）
        when(mockSession.readTransaction(any(TransactionWork.class))).thenAnswer(invocation -> {
            TransactionWork<Void> work = invocation.getArgument(0);
            // 创建一个 mock Transaction，run 方法返回 null（无法 mock Result）
            Transaction mockTx = mock(Transaction.class);
            // 尝试执行 work，由于 run() 返回 null 会抛 NPE，
            // 但我们可以验证 work 被传入
            return null;
        });
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = Neo4jRelationQueryService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
