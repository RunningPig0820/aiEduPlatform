package com.ai.edu.application.service;

import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 知识图谱错误码验证测试
 *
 * 测试目标：验证异常场景下返回正确的错误码
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgErrorCodeTest {

    // ==================== 6.13.1 教材不存在 ====================

    @Test
    @Order(1)
    @DisplayName("KG_TEXTBOOK_NOT_FOUND — code=70001")
    void kgTextbookNotFound_errorCode() {
        BusinessException ex = new BusinessException(ErrorCode.KG_TEXTBOOK_NOT_FOUND, "教材不存在: uri:xxx");

        assertEquals("70001", ex.getCode());
        assertTrue(ex.getMessage().contains("教材不存在"));
    }

    // ==================== 6.13.2 章节不存在 ====================

    @Test
    @Order(2)
    @DisplayName("KG_CHAPTER_NOT_FOUND — code=70002")
    void kgChapterNotFound_errorCode() {
        BusinessException ex = new BusinessException(ErrorCode.KG_CHAPTER_NOT_FOUND, "章节不存在: uri:xxx");

        assertEquals("70002", ex.getCode());
        assertTrue(ex.getMessage().contains("章节不存在"));
    }

    // ==================== 6.13.3 知识点不存在/已删除/已合并 ====================

    @Test
    @Order(3)
    @DisplayName("KG_KNOWLEDGE_POINT_NOT_FOUND — code=70003")
    void kgKnowledgePointNotFound_errorCode() {
        BusinessException ex = new BusinessException(ErrorCode.KG_KNOWLEDGE_POINT_NOT_FOUND, "知识点不存在: uri:xxx");

        assertEquals("70003", ex.getCode());
        assertTrue(ex.getMessage().contains("知识点不存在"));
    }

    @Test
    @Order(4)
    @DisplayName("KG_KNOWLEDGE_POINT_NOT_FOUND — 已删除/已合并场景")
    void kgKnowledgePointDeleted_errorCode() {
        // 已删除或已合并的知识点也返回 70003
        BusinessException ex1 = new BusinessException(ErrorCode.KG_KNOWLEDGE_POINT_NOT_FOUND, "知识点已删除");
        BusinessException ex2 = new BusinessException(ErrorCode.KG_KNOWLEDGE_POINT_NOT_FOUND, "知识点已合并到其他知识点");

        assertEquals("70003", ex1.getCode());
        assertEquals("70003", ex2.getCode());
    }

    // ==================== 6.13.4 小节不存在 ====================

    @Test
    @Order(5)
    @DisplayName("KG_SECTION_NOT_FOUND — code=70004")
    void kgSectionNotFound_errorCode() {
        BusinessException ex = new BusinessException(ErrorCode.KG_SECTION_NOT_FOUND, "小节不存在: uri:xxx");

        assertEquals("70004", ex.getCode());
        assertTrue(ex.getMessage().contains("小节不存在"));
    }

    // ==================== 6.13.5 Neo4j 查询失败 ====================

    @Test
    @Order(6)
    @DisplayName("KG_NEO4J_QUERY_FAILED — code=70005")
    void kgNeo4jQueryFailed_errorCode() {
        BusinessException ex = new BusinessException(ErrorCode.KG_NEO4J_QUERY_FAILED, "Neo4j 查询失败: connection timeout");

        assertEquals("70005", ex.getCode());
        assertTrue(ex.getMessage().contains("Neo4j"));
    }

    // ==================== 6.13.6 同步中重复触发 ====================

    @Test
    @Order(7)
    @DisplayName("KG_SYNC_IN_PROGRESS — code=70006")
    void kgSyncInProgress_errorCode() {
        BusinessException ex = new BusinessException(ErrorCode.KG_SYNC_IN_PROGRESS, "已有同步任务正在执行，请稍后再试");

        assertEquals("70006", ex.getCode());
        assertTrue(ex.getMessage().contains("同步任务"));
    }

    // ==================== 6.13.7 同步参数错误 ====================

    @Test
    @Order(8)
    @DisplayName("KG_SYNC_PARAM_ERROR — code=70007")
    void kgSyncParamError_errorCode() {
        BusinessException ex = new BusinessException(ErrorCode.KG_SYNC_PARAM_ERROR, "学科不能为空");

        assertEquals("70007", ex.getCode());
        assertTrue(ex.getMessage().contains("学科"));
    }

    // ==================== 错误码段验证 ====================

    @Test
    @Order(9)
    @DisplayName("所有知识图谱错误码均以 7 开头")
    void allKgErrorCodes_startWith7() {
        // 验证 ErrorCode 常量值
        assertTrue(ErrorCode.KG_TEXTBOOK_NOT_FOUND.startsWith("7"));
        assertTrue(ErrorCode.KG_CHAPTER_NOT_FOUND.startsWith("7"));
        assertTrue(ErrorCode.KG_KNOWLEDGE_POINT_NOT_FOUND.startsWith("7"));
        assertTrue(ErrorCode.KG_SECTION_NOT_FOUND.startsWith("7"));
        assertTrue(ErrorCode.KG_NEO4J_QUERY_FAILED.startsWith("7"));
        assertTrue(ErrorCode.KG_SYNC_IN_PROGRESS.startsWith("7"));
        assertTrue(ErrorCode.KG_SYNC_PARAM_ERROR.startsWith("7"));
    }

    @Test
    @Order(10)
    @DisplayName("错误码连续递增")
    void kgErrorCodes_sequential() {
        assertEquals("70001", ErrorCode.KG_TEXTBOOK_NOT_FOUND);
        assertEquals("70002", ErrorCode.KG_CHAPTER_NOT_FOUND);
        assertEquals("70003", ErrorCode.KG_KNOWLEDGE_POINT_NOT_FOUND);
        assertEquals("70004", ErrorCode.KG_SECTION_NOT_FOUND);
        assertEquals("70005", ErrorCode.KG_NEO4J_QUERY_FAILED);
        assertEquals("70006", ErrorCode.KG_SYNC_IN_PROGRESS);
        assertEquals("70007", ErrorCode.KG_SYNC_PARAM_ERROR);
    }
}
