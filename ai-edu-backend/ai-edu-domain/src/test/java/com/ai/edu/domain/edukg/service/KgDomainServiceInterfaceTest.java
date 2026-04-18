package com.ai.edu.domain.edukg.service;

import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 领域服务接口契约验证测试
 * 通过反射验证接口方法签名完整、返回类型正确。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgDomainServiceInterfaceTest {

    // ==================== 6.2.1 KgSyncDomainService ====================

    @Test
    @Order(1)
    @DisplayName("KgSyncDomainService 应包含所有节点同步方法")
    void kgSyncDomainService_shouldHaveAllNodeSyncMethods() throws Exception {
        Class<?> clazz = KgSyncDomainService.class;

        // 验证 4 个节点同步方法
        Method textbookMethod = clazz.getMethod("syncTextbookNodes");
        assertEquals(List.class, textbookMethod.getReturnType());

        Method chapterMethod = clazz.getMethod("syncChapterNodes");
        assertEquals(List.class, chapterMethod.getReturnType());

        Method sectionMethod = clazz.getMethod("syncSectionNodes");
        assertEquals(List.class, sectionMethod.getReturnType());

        Method kpMethod = clazz.getMethod("syncKnowledgePointNodes");
        assertEquals(List.class, kpMethod.getReturnType());
    }

    @Test
    @Order(2)
    @DisplayName("KgSyncDomainService 应包含所有关系同步方法")
    void kgSyncDomainService_shouldHaveAllRelationSyncMethods() throws Exception {
        Class<?> clazz = KgSyncDomainService.class;

        Method tbChMethod = clazz.getMethod("syncTextbookChapterRelations");
        assertEquals(List.class, tbChMethod.getReturnType());

        Method chSecMethod = clazz.getMethod("syncChapterSectionRelations");
        assertEquals(List.class, chSecMethod.getReturnType());

        Method secKpMethod = clazz.getMethod("syncSectionKPRelations");
        assertEquals(List.class, secKpMethod.getReturnType());
    }

    @Test
    @Order(3)
    @DisplayName("KgSyncDomainService 应包含所有 upsert 方法")
    void kgSyncDomainService_shouldHaveAllUpsertMethods() throws Exception {
        Class<?> clazz = KgSyncDomainService.class;

        Method upsertTextbooks = clazz.getMethod("upsertTextbooks", List.class);
        assertEquals(int.class, upsertTextbooks.getReturnType());

        Method upsertChapters = clazz.getMethod("upsertChapters", List.class);
        assertEquals(int.class, upsertChapters.getReturnType());

        Method upsertSections = clazz.getMethod("upsertSections", List.class);
        assertEquals(int.class, upsertSections.getReturnType());

        Method upsertKps = clazz.getMethod("upsertKnowledgePoints", List.class);
        assertEquals(int.class, upsertKps.getReturnType());
    }

    @Test
    @Order(4)
    @DisplayName("KgSyncDomainService 应包含所有 rebuild 关系方法")
    void kgSyncDomainService_shouldHaveAllRebuildMethods() throws Exception {
        Class<?> clazz = KgSyncDomainService.class;

        Method rebuildTbCh = clazz.getMethod("rebuildTextbookChapterRelations", List.class);
        assertEquals(int.class, rebuildTbCh.getReturnType());

        Method rebuildChSec = clazz.getMethod("rebuildChapterSectionRelations", List.class);
        assertEquals(int.class, rebuildChSec.getReturnType());

        Method rebuildSecKp = clazz.getMethod("rebuildSectionKPRelations", List.class);
        assertEquals(int.class, rebuildSecKp.getReturnType());
    }

    @Test
    @Order(5)
    @DisplayName("KgSyncDomainService 应包含 markDeletedNodes 方法")
    void kgSyncDomainService_shouldHaveMarkDeletedNodesMethod() throws Exception {
        Class<?> clazz = KgSyncDomainService.class;

        Method method = clazz.getMethod("markDeletedNodes", String.class, Set.class);
        assertEquals(int.class, method.getReturnType());
    }

    @Test
    @Order(6)
    @DisplayName("KgSyncDomainService 应包含同步记录方法")
    void kgSyncDomainService_shouldHaveSyncRecordMethods() throws Exception {
        Class<?> clazz = KgSyncDomainService.class;

        Method createRecord = clazz.getMethod("createSyncRecord", String.class, String.class, Long.class);
        assertEquals(Class.forName("com.ai.edu.domain.edukg.model.entity.KgSyncRecord"), createRecord.getReturnType());

        Method completeRecord = clazz.getMethod("completeSyncRecord", Long.class, int.class, int.class,
                int.class, String.class, String.class);
        assertEquals(void.class, completeRecord.getReturnType());

        Method failRecord = clazz.getMethod("failSyncRecord", Long.class, String.class);
        assertEquals(void.class, failRecord.getReturnType());

        Method getLatest = clazz.getMethod("getLatestSyncRecord");
        assertEquals(Class.forName("com.ai.edu.domain.edukg.model.entity.KgSyncRecord"), getLatest.getReturnType());

        Method getRecords = clazz.getMethod("getSyncRecords", int.class);
        assertEquals(List.class, getRecords.getReturnType());
    }

    @Test
    @Order(7)
    @DisplayName("KgSyncDomainService 应包含 validateAllUris 方法")
    void kgSyncDomainService_shouldHaveValidateAllUrisMethod() throws Exception {
        Class<?> clazz = KgSyncDomainService.class;

        Method method = clazz.getMethod("validateAllUris", List.class, List.class, List.class, List.class);
        assertNotNull(method);
    }

    @Test
    @Order(8)
    @DisplayName("KgSyncDomainService 应包含 reconcile 方法")
    void kgSyncDomainService_shouldHaveReconcileMethod() throws Exception {
        Class<?> clazz = KgSyncDomainService.class;

        Method method = clazz.getMethod("reconcile", Set.class, Set.class, Set.class, Set.class,
                List.class, List.class, List.class);
        assertNotNull(method);
    }

    @Test
    @Order(9)
    @DisplayName("KgSyncDomainService 应包含 checkNeo4jHealth 方法")
    void kgSyncDomainService_shouldHaveCheckNeo4jHealthMethod() throws Exception {
        Class<?> clazz = KgSyncDomainService.class;

        Method method = clazz.getMethod("checkNeo4jHealth");
        assertNotNull(method);
    }

    @Test
    @Order(10)
    @DisplayName("KgSyncDomainService 应包含内部结果类")
    void kgSyncDomainService_shouldHaveResultClasses() {
        Class<?> clazz = KgSyncDomainService.class;
        Class<?>[] innerClasses = clazz.getClasses();

        Set<String> innerClassNames = Arrays.stream(innerClasses)
                .map(Class::getSimpleName)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(innerClassNames.contains("UriValidationResult"), "应包含 UriValidationResult");
        assertTrue(innerClassNames.contains("ReconciliationResult"), "应包含 ReconciliationResult");
        assertTrue(innerClassNames.contains("HealthCheckResult"), "应包含 HealthCheckResult");
    }

    @Test
    @Order(11)
    @DisplayName("UriValidationResult 应包含 valid 和 errors 字段")
    void uriValidationResult_shouldHaveCorrectFields() throws Exception {
        Class<?> clazz = KgSyncDomainService.UriValidationResult.class;

        assertNotNull(clazz.getField("valid"));
        assertNotNull(clazz.getField("errors"));

        // 验证构造函数可用
        var constructor = clazz.getConstructor(boolean.class, List.class);
        assertNotNull(constructor);
    }

    @Test
    @Order(12)
    @DisplayName("ReconciliationResult 应包含 matched 和 counts 字段")
    void reconciliationResult_shouldHaveCorrectFields() throws Exception {
        Class<?> clazz = KgSyncDomainService.ReconciliationResult.class;

        assertNotNull(clazz.getField("matched"));
        assertNotNull(clazz.getField("mysqlTextbookCount"));
        assertNotNull(clazz.getField("neo4jTextbookCount"));
        assertNotNull(clazz.getField("mysqlChapterCount"));
        assertNotNull(clazz.getField("neo4jChapterCount"));
        assertNotNull(clazz.getField("mysqlSectionCount"));
        assertNotNull(clazz.getField("neo4jSectionCount"));
        assertNotNull(clazz.getField("mysqlKpCount"));
        assertNotNull(clazz.getField("neo4jKpCount"));
        assertNotNull(clazz.getField("differences"));
    }

    @Test
    @Order(13)
    @DisplayName("HealthCheckResult 应包含 healthy/responseTimeMs/message 字段")
    void healthCheckResult_shouldHaveCorrectFields() throws Exception {
        Class<?> clazz = KgSyncDomainService.HealthCheckResult.class;

        assertNotNull(clazz.getField("healthy"));
        assertNotNull(clazz.getField("responseTimeMs"));
        assertNotNull(clazz.getField("message"));
    }

    // ==================== 6.2.2 KgRelationQueryDomainService ====================

    @Test
    @Order(14)
    @DisplayName("KgRelationQueryDomainService 应包含 getTextbookChapterRelations 方法")
    void kgRelationQueryDomainService_shouldHaveGetTextbookChapterRelations() throws Exception {
        Class<?> clazz = KgRelationQueryDomainService.class;

        Method method = clazz.getMethod("getTextbookChapterRelations", String.class);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    @Order(15)
    @DisplayName("KgRelationQueryDomainService 应包含 getChapterSectionRelations 方法")
    void kgRelationQueryDomainService_shouldHaveGetChapterSectionRelations() throws Exception {
        Class<?> clazz = KgRelationQueryDomainService.class;

        Method method = clazz.getMethod("getChapterSectionRelations", String.class);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    @Order(16)
    @DisplayName("KgRelationQueryDomainService 应包含 getSectionKPRelations 方法")
    void kgRelationQueryDomainService_shouldHaveGetSectionKPRelations() throws Exception {
        Class<?> clazz = KgRelationQueryDomainService.class;

        Method method = clazz.getMethod("getSectionKPRelations", String.class);
        assertEquals(List.class, method.getReturnType());
    }

    // ==================== 6.2.3 Repository 接口 ====================

    @Test
    @Order(17)
    @DisplayName("KgTextbookRepository 应包含完整方法")
    void kgTextbookRepository_shouldHaveCompleteMethods() throws Exception {
        Class<?> clazz = com.ai.edu.domain.edukg.repository.KgTextbookRepository.class;

        assertNotNull(clazz.getMethod("save", com.ai.edu.domain.edukg.model.entity.KgTextbook.class));
        assertNotNull(clazz.getMethod("findById", Long.class));
        assertNotNull(clazz.getMethod("findByUri", String.class));
        assertNotNull(clazz.getMethod("findBySubject", String.class));
        assertNotNull(clazz.getMethod("findBySubjectAndStage", String.class, String.class));
        assertNotNull(clazz.getMethod("findAllActive"));
        assertNotNull(clazz.getMethod("updateStatus", String.class, String.class));
    }

    @Test
    @Order(18)
    @DisplayName("KgChapterRepository 应包含完整方法")
    void kgChapterRepository_shouldHaveCompleteMethods() throws Exception {
        Class<?> clazz = com.ai.edu.domain.edukg.repository.KgChapterRepository.class;

        assertNotNull(clazz.getMethod("save", com.ai.edu.domain.edukg.model.entity.KgChapter.class));
        assertNotNull(clazz.getMethod("findById", Long.class));
        assertNotNull(clazz.getMethod("findByUri", String.class));
        assertNotNull(clazz.getMethod("findByUris", List.class));
        assertNotNull(clazz.getMethod("updateStatus", String.class, String.class));
    }

    @Test
    @Order(19)
    @DisplayName("KgSectionRepository 应包含完整方法")
    void kgSectionRepository_shouldHaveCompleteMethods() throws Exception {
        Class<?> clazz = com.ai.edu.domain.edukg.repository.KgSectionRepository.class;

        assertNotNull(clazz.getMethod("save", com.ai.edu.domain.edukg.model.entity.KgSection.class));
        assertNotNull(clazz.getMethod("findById", Long.class));
        assertNotNull(clazz.getMethod("findByUri", String.class));
        assertNotNull(clazz.getMethod("findByUris", List.class));
        assertNotNull(clazz.getMethod("updateStatus", String.class, String.class));
    }

    @Test
    @Order(20)
    @DisplayName("KgKnowledgePointRepository 应包含完整方法")
    void kgKnowledgePointRepository_shouldHaveCompleteMethods() throws Exception {
        Class<?> clazz = com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository.class;

        assertNotNull(clazz.getMethod("save", com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint.class));
        assertNotNull(clazz.getMethod("findById", Long.class));
        assertNotNull(clazz.getMethod("findByUri", String.class));
        assertNotNull(clazz.getMethod("findByUris", List.class));
        assertNotNull(clazz.getMethod("findByStatus", String.class));
        assertNotNull(clazz.getMethod("updateStatus", String.class, String.class));
    }

    @Test
    @Order(21)
    @DisplayName("KgTextbookChapterRepository 应包含完整方法")
    void kgTextbookChapterRepository_shouldHaveCompleteMethods() throws Exception {
        Class<?> clazz = com.ai.edu.domain.edukg.repository.KgTextbookChapterRepository.class;

        assertNotNull(clazz.getMethod("save", com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter.class));
        assertNotNull(clazz.getMethod("saveBatch", List.class));
        assertNotNull(clazz.getMethod("deleteByTextbookUri", String.class));
        assertNotNull(clazz.getMethod("deleteByChapterUri", String.class));
        assertNotNull(clazz.getMethod("findByTextbookUri", String.class));
        assertNotNull(clazz.getMethod("findByChapterUri", String.class));
        assertNotNull(clazz.getMethod("findAllActive"));
    }

    @Test
    @Order(22)
    @DisplayName("KgChapterSectionRepository 应包含完整方法")
    void kgChapterSectionRepository_shouldHaveCompleteMethods() throws Exception {
        Class<?> clazz = com.ai.edu.domain.edukg.repository.KgChapterSectionRepository.class;

        assertNotNull(clazz.getMethod("save", com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection.class));
        assertNotNull(clazz.getMethod("saveBatch", List.class));
        assertNotNull(clazz.getMethod("deleteByChapterUri", String.class));
        assertNotNull(clazz.getMethod("deleteBySectionUri", String.class));
        assertNotNull(clazz.getMethod("findByChapterUri", String.class));
        assertNotNull(clazz.getMethod("findBySectionUri", String.class));
        assertNotNull(clazz.getMethod("findAllActive"));
    }

    @Test
    @Order(23)
    @DisplayName("KgSectionKPRepository 应包含完整方法")
    void kgSectionKPRepository_shouldHaveCompleteMethods() throws Exception {
        Class<?> clazz = com.ai.edu.domain.edukg.repository.KgSectionKPRepository.class;

        assertNotNull(clazz.getMethod("save", com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP.class));
        assertNotNull(clazz.getMethod("saveBatch", List.class));
        assertNotNull(clazz.getMethod("deleteBySectionUri", String.class));
        assertNotNull(clazz.getMethod("deleteByKpUri", String.class));
        assertNotNull(clazz.getMethod("findBySectionUri", String.class));
        assertNotNull(clazz.getMethod("findByKpUri", String.class));
        assertNotNull(clazz.getMethod("findAllActive"));
    }
}
