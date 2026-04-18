package com.ai.edu.domain.edukg.service;

import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 领域服务接口契约验证测试
 * 通过反射验证接口方法签名完整、返回类型正确。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgDomainServiceInterfaceTest {

    // ==================== 6.2.1 KgKnowledgeGraphQueryRepository ====================

    @Test
    @Order(1)
    @DisplayName("KgKnowledgeGraphQueryRepository 应包含 getTextbookChapterRelations 方法")
    void kgKnowledgeGraphQueryRepository_shouldHaveGetTextbookChapterRelations() throws Exception {
        Class<?> clazz = com.ai.edu.domain.edukg.repository.KgKnowledgeGraphQueryRepository.class;

        Method method = clazz.getMethod("getTextbookChapterRelations", String.class);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    @Order(2)
    @DisplayName("KgKnowledgeGraphQueryRepository 应包含 getChapterSectionRelations 方法")
    void kgKnowledgeGraphQueryRepository_shouldHaveGetChapterSectionRelations() throws Exception {
        Class<?> clazz = com.ai.edu.domain.edukg.repository.KgKnowledgeGraphQueryRepository.class;

        Method method = clazz.getMethod("getChapterSectionRelations", String.class);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    @Order(3)
    @DisplayName("KgKnowledgeGraphQueryRepository 应包含 getSectionKPRelations 方法")
    void kgKnowledgeGraphQueryRepository_shouldHaveGetSectionKPRelations() throws Exception {
        Class<?> clazz = com.ai.edu.domain.edukg.repository.KgKnowledgeGraphQueryRepository.class;

        Method method = clazz.getMethod("getSectionKPRelations", String.class);
        assertEquals(List.class, method.getReturnType());
    }

    // ==================== 6.2.3 Repository 接口 ====================

    @Test
    @Order(4)
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
