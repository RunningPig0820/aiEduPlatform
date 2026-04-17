package com.ai.edu.infrastructure.cache;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.mockito.Mockito;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Neo4jRelationCacheService 单元测试
 *
 * 测试目标：Redis 缓存读写、TTL、序列化/反序列化
 *
 * 全 Mock 方式：Mock RedissonClient + RBucket
 */
@TestMethodOrder(OrderAnnotation.class)
class Neo4jRelationCacheServiceTest {

    private Neo4jRelationCacheService cacheService;
    private RedissonClient redissonClient;
    @SuppressWarnings("rawtypes")
    private RBucket mockBucket;

    private static final String TEST_TB_URI = "http://edukg.org/knowledge/3.1/textbook/test_cache";
    private static final String TEST_CH_URI = "http://edukg.org/knowledge/3.1/chapter/ch1";
    private static final String TEST_SEC_URI = "http://edukg.org/knowledge/3.1/section/sec1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        redissonClient = Mockito.mock(RedissonClient.class);
        mockBucket = Mockito.mock(RBucket.class);

        cacheService = new Neo4jRelationCacheService();
        setField(cacheService, "redissonClient", redissonClient);

        // 默认：每次调用 getBucket 返回同一个 mockBucket
        when(redissonClient.getBucket(anyString())).thenReturn(mockBucket);
    }

    // ==================== 6.5.1 Textbook-Chapter 读写链路 ====================

    @Test
    @Order(1)
    @DisplayName("setTextbookChapterRelations 应序列化并写入 Redis，带 TTL")
    void setTextbookChapterRelations_shouldSerializeAndSetWithTTL() throws Exception {
        List<KgTextbookChapter> relations = List.of(
                KgTextbookChapter.create(TEST_TB_URI, TEST_CH_URI, 1),
                KgTextbookChapter.create(TEST_TB_URI, "ch2", 2)
        );

        cacheService.setTextbookChapterRelations(TEST_TB_URI, relations);

        // 验证 getBucket 被调用，key 正确
        verify(redissonClient).getBucket("kg:neo4j:relation:textbook:" + TEST_TB_URI);
        // 验证 bucket.set 被调用，带 TTL
        verify(mockBucket).set(anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @Order(2)
    @DisplayName("getTextbookChapterRelations 应从 Redis 读取并反序列化")
    void getTextbookChapterRelations_shouldReadAndDeserialize() throws Exception {
        List<KgTextbookChapter> relations = List.of(
                KgTextbookChapter.create(TEST_TB_URI, TEST_CH_URI, 1)
        );
        String json = objectMapper.writeValueAsString(relations);
        when(mockBucket.get()).thenReturn(json);

        List<KgTextbookChapter> result = cacheService.getTextbookChapterRelations(TEST_TB_URI);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_CH_URI, result.get(0).getChapterUri());
        assertEquals(1, result.get(0).getOrderIndex());
    }

    // ==================== 6.5.2 Chapter-Section 读写链路 ====================

    @Test
    @Order(3)
    @DisplayName("setChapterSectionRelations 应序列化并写入 Redis")
    void setChapterSectionRelations_shouldSerializeAndSetWithTTL() {
        List<KgChapterSection> relations = List.of(
                KgChapterSection.create(TEST_CH_URI, TEST_SEC_URI, 1)
        );

        cacheService.setChapterSectionRelations(TEST_CH_URI, relations);

        verify(redissonClient).getBucket("kg:neo4j:relation:chapter:" + TEST_CH_URI);
        verify(mockBucket).set(anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @Order(4)
    @DisplayName("getChapterSectionRelations 应从 Redis 读取并反序列化")
    void getChapterSectionRelations_shouldReadAndDeserialize() throws Exception {
        List<KgChapterSection> relations = List.of(
                KgChapterSection.create(TEST_CH_URI, TEST_SEC_URI, 1)
        );
        String json = objectMapper.writeValueAsString(relations);
        when(mockBucket.get()).thenReturn(json);

        List<KgChapterSection> result = cacheService.getChapterSectionRelations(TEST_CH_URI);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_SEC_URI, result.get(0).getSectionUri());
    }

    // ==================== 6.5.3 Section-KP 读写链路 ====================

    @Test
    @Order(5)
    @DisplayName("setSectionKPRelations 应序列化并写入 Redis")
    void setSectionKPRelations_shouldSerializeAndSetWithTTL() {
        String kpUri = "http://edukg.org/knowledge/3.1/kp/kp1";
        List<KgSectionKP> relations = List.of(
                KgSectionKP.create(TEST_SEC_URI, kpUri, 1)
        );

        cacheService.setSectionKPRelations(TEST_SEC_URI, relations);

        verify(redissonClient).getBucket("kg:neo4j:relation:section:" + TEST_SEC_URI);
        verify(mockBucket).set(anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @Order(6)
    @DisplayName("getSectionKPRelations 应从 Redis 读取并反序列化")
    void getSectionKPRelations_shouldReadAndDeserialize() throws Exception {
        String kpUri = "http://edukg.org/knowledge/3.1/kp/kp1";
        List<KgSectionKP> relations = List.of(
                KgSectionKP.create(TEST_SEC_URI, kpUri, 1)
        );
        String json = objectMapper.writeValueAsString(relations);
        when(mockBucket.get()).thenReturn(json);

        List<KgSectionKP> result = cacheService.getSectionKPRelations(TEST_SEC_URI);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(kpUri, result.get(0).getKpUri());
    }

    // ==================== 6.5.4 缓存未命中 ====================

    @Test
    @Order(7)
    @DisplayName("缓存未命中（key 不存在）应返回 null")
    void cacheMiss_shouldReturnNull() {
        when(mockBucket.get()).thenReturn(null);

        List<KgTextbookChapter> result = cacheService.getTextbookChapterRelations(TEST_TB_URI);

        assertNull(result);
    }

    // ==================== 6.5.5 反序列化异常 ====================

    @Test
    @Order(8)
    @DisplayName("反序列化异常（损坏的 JSON）应返回空列表")
    void deserializeFailure_shouldReturnEmptyList() {
        when(mockBucket.get()).thenReturn("{invalid json");

        List<KgTextbookChapter> result = cacheService.getTextbookChapterRelations(TEST_TB_URI);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 6.5.6 evict 方法 ====================

    @Test
    @Order(9)
    @DisplayName("evictTextbook 应删除正确的 key")
    void evictTextbook_shouldDeleteCorrectKey() {
        cacheService.evictTextbook(TEST_TB_URI);

        verify(redissonClient).getBucket("kg:neo4j:relation:textbook:" + TEST_TB_URI);
        verify(mockBucket).delete();
    }

    @Test
    @Order(10)
    @DisplayName("evictChapter 应删除正确的 key")
    void evictChapter_shouldDeleteCorrectKey() {
        cacheService.evictChapter(TEST_CH_URI);

        verify(redissonClient).getBucket("kg:neo4j:relation:chapter:" + TEST_CH_URI);
        verify(mockBucket).delete();
    }

    @Test
    @Order(11)
    @DisplayName("evictSection 应删除正确的 key")
    void evictSection_shouldDeleteCorrectKey() {
        cacheService.evictSection(TEST_SEC_URI);

        verify(redissonClient).getBucket("kg:neo4j:relation:section:" + TEST_SEC_URI);
        verify(mockBucket).delete();
    }

    // ==================== Helper ====================

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = Neo4jRelationCacheService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
