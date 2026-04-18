package com.ai.edu.application.service;

import com.ai.edu.application.dto.kg.BatchRelationsDTO;
import com.ai.edu.application.dto.kg.HealthDTO;
import com.ai.edu.common.dto.kg.HealthCheckResult;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.repository.KgKnowledgeGraphQueryRepository;
import com.ai.edu.domain.edukg.service.KgSyncRecordService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * KgNeo4jService 单元测试
 *
 * 测试目标：Mock 领域服务和基础设施服务，验证 Neo4j 查询编排
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class KgNeo4jServiceTest {

    @Mock
    private KgSyncRecordService recordService;
    @Mock
    private KgKnowledgeGraphQueryRepository kgKnowledgeGraphQueryRepository;

    @InjectMocks
    private KgNeo4jService kgNeo4jService;

    // ==================== 6.10.1 getNeo4jHealth ====================

    @Test
    @Order(1)
    @DisplayName("getNeo4jHealth 健康 — 应返回 available=true")
    void getNeo4jHealth_healthy_shouldReturnAvailable() {
        when(recordService.checkNeo4jHealth())
                .thenReturn(new HealthCheckResult(true, 15, "Connected"));

        HealthDTO result = kgNeo4jService.getNeo4jHealth();

        assertNotNull(result);
        assertTrue(result.isAvailable());
        assertEquals(15, result.getResponseTimeMs());
        assertEquals("Connected", result.getMessage());
    }

    @Test
    @Order(2)
    @DisplayName("getNeo4jHealth 异常 — 应返回 available=false")
    void getNeo4jHealth_unhealthy_shouldReturnUnavailable() {
        when(recordService.checkNeo4jHealth())
                .thenReturn(new HealthCheckResult(false, 0, "Connection refused"));

        HealthDTO result = kgNeo4jService.getNeo4jHealth();

        assertNotNull(result);
        assertFalse(result.isAvailable());
        assertEquals(0, result.getResponseTimeMs());
        assertEquals("Connection refused", result.getMessage());
    }

    // ==================== 6.10.2 batchGetConceptRelations ====================

    @Test
    @Order(3)
    @DisplayName("batchGetConceptRelations 正常返回 — 应合并三类关联")
    void batchGetConceptRelations_normal_shouldMergeAllRelations() {
        List<String> uris = List.of("uri:tb1");

        when(kgKnowledgeGraphQueryRepository.getTextbookChapterRelations("uri:tb1"))
                .thenReturn(List.of(
                        KgTextbookChapter.create("uri:tb1", "uri:ch1", 1),
                        KgTextbookChapter.create("uri:tb1", "uri:ch2", 2)
                ));
        when(kgKnowledgeGraphQueryRepository.getChapterSectionRelations("uri:tb1"))
                .thenReturn(List.of(
                        KgChapterSection.create("uri:tb1", "uri:sec1", 1)
                ));
        when(kgKnowledgeGraphQueryRepository.getSectionKPRelations("uri:tb1"))
                .thenReturn(List.of(
                        KgSectionKP.create("uri:tb1", "uri:kp1", 1),
                        KgSectionKP.create("uri:tb1", "uri:kp2", 2)
                ));

        BatchRelationsDTO result = kgNeo4jService.batchGetConceptRelations(uris);

        assertNotNull(result);
        assertEquals(1, result.getRelations().size());
        BatchRelationsDTO.RelationEntry entry = result.getRelations().get(0);
        assertEquals("uri:tb1", entry.getUri());
        assertEquals(5, entry.getRelatedUris().size());
        assertTrue(entry.getRelatedUris().contains("uri:ch1"));
        assertTrue(entry.getRelatedUris().contains("uri:ch2"));
        assertTrue(entry.getRelatedUris().contains("uri:sec1"));
        assertTrue(entry.getRelatedUris().contains("uri:kp1"));
        assertTrue(entry.getRelatedUris().contains("uri:kp2"));
    }

    @Test
    @Order(4)
    @DisplayName("batchGetConceptRelations Neo4j 不可用 — 应降级返回空列表")
    void batchGetConceptRelations_neo4jUnavailable_shouldFallbackToEmpty() {
        List<String> uris = List.of("uri:tb1");

        when(kgKnowledgeGraphQueryRepository.getTextbookChapterRelations("uri:tb1"))
                .thenThrow(new RuntimeException("Neo4j connection timeout"));

        BatchRelationsDTO result = kgNeo4jService.batchGetConceptRelations(uris);

        assertNotNull(result);
        assertEquals(1, result.getRelations().size());
        assertEquals("uri:tb1", result.getRelations().get(0).getUri());
        assertTrue(result.getRelations().get(0).getRelatedUris().isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("batchGetConceptRelations 部分 URI 无关联 — 应返回空列表")
    void batchGetConceptRelations_partialUriNoRelations_shouldReturnEmptyForThose() {
        List<String> uris = List.of("uri:tb1", "uri:tb2");

        when(kgKnowledgeGraphQueryRepository.getTextbookChapterRelations("uri:tb1"))
                .thenReturn(List.of(KgTextbookChapter.create("uri:tb1", "uri:ch1", 1)));
        when(kgKnowledgeGraphQueryRepository.getChapterSectionRelations("uri:tb1"))
                .thenReturn(List.of());
        when(kgKnowledgeGraphQueryRepository.getSectionKPRelations("uri:tb1"))
                .thenReturn(List.of());

        when(kgKnowledgeGraphQueryRepository.getTextbookChapterRelations("uri:tb2"))
                .thenReturn(List.of());
        when(kgKnowledgeGraphQueryRepository.getChapterSectionRelations("uri:tb2"))
                .thenReturn(List.of());
        when(kgKnowledgeGraphQueryRepository.getSectionKPRelations("uri:tb2"))
                .thenReturn(List.of());

        BatchRelationsDTO result = kgNeo4jService.batchGetConceptRelations(uris);

        assertNotNull(result);
        assertEquals(2, result.getRelations().size());

        BatchRelationsDTO.RelationEntry entry1 = result.getRelations().get(0);
        assertEquals("uri:tb1", entry1.getUri());
        assertEquals(1, entry1.getRelatedUris().size());

        BatchRelationsDTO.RelationEntry entry2 = result.getRelations().get(1);
        assertEquals("uri:tb2", entry2.getUri());
        assertTrue(entry2.getRelatedUris().isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("batchGetConceptRelations 空列表 — 应返回空 relations")
    void batchGetConceptRelations_emptyUris_shouldReturnEmpty() {
        BatchRelationsDTO result = kgNeo4jService.batchGetConceptRelations(List.of());

        assertNotNull(result);
        assertNotNull(result.getRelations());
        assertTrue(result.getRelations().isEmpty());
    }
}
