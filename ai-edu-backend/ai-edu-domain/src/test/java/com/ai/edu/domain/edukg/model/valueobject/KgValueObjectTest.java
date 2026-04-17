package com.ai.edu.domain.edukg.model.valueobject;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 值对象枚举测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgValueObjectTest {

    // ==================== KgDifficulty ====================

    @Test
    @Order(1)
    @DisplayName("KgDifficulty.getValue() 应返回正确的字符串值")
    void kgDifficulty_getValue_shouldReturnCorrectString() {
        assertEquals("easy", KgDifficulty.EASY.getValue());
        assertEquals("medium", KgDifficulty.MEDIUM.getValue());
        assertEquals("hard", KgDifficulty.HARD.getValue());
    }

    @Test
    @Order(2)
    @DisplayName("KgDifficulty.getLabel() 应返回正确的中文标签")
    void kgDifficulty_getLabel_shouldReturnCorrectLabel() {
        assertEquals("简单", KgDifficulty.EASY.getLabel());
        assertEquals("中等", KgDifficulty.MEDIUM.getLabel());
        assertEquals("困难", KgDifficulty.HARD.getLabel());
    }

    @Test
    @Order(3)
    @DisplayName("KgDifficulty.fromValue() 应正确转换字符串为枚举")
    void kgDifficulty_fromValue_shouldReturnCorrectEnum() {
        assertEquals(KgDifficulty.EASY, KgDifficulty.fromValue("easy"));
        assertEquals(KgDifficulty.MEDIUM, KgDifficulty.fromValue("medium"));
        assertEquals(KgDifficulty.HARD, KgDifficulty.fromValue("hard"));
    }

    @Test
    @Order(4)
    @DisplayName("KgDifficulty.fromValue() 对非法值应返回默认 MEDIUM")
    void kgDifficulty_fromValue_shouldReturnDefaultForInvalidValue() {
        assertEquals(KgDifficulty.MEDIUM, KgDifficulty.fromValue("invalid"));
        assertEquals(KgDifficulty.MEDIUM, KgDifficulty.fromValue(null));
    }

    // ==================== KgImportance ====================

    @Test
    @Order(5)
    @DisplayName("KgImportance.getValue() 应返回正确的字符串值")
    void kgImportance_getValue_shouldReturnCorrectString() {
        assertEquals("low", KgImportance.LOW.getValue());
        assertEquals("medium", KgImportance.MEDIUM.getValue());
        assertEquals("high", KgImportance.HIGH.getValue());
    }

    @Test
    @Order(6)
    @DisplayName("KgImportance.getLabel() 应返回正确的中文标签")
    void kgImportance_getLabel_shouldReturnCorrectLabel() {
        assertEquals("低", KgImportance.LOW.getLabel());
        assertEquals("中", KgImportance.MEDIUM.getLabel());
        assertEquals("高", KgImportance.HIGH.getLabel());
    }

    @Test
    @Order(7)
    @DisplayName("KgImportance.fromValue() 应正确转换字符串为枚举")
    void kgImportance_fromValue_shouldReturnCorrectEnum() {
        assertEquals(KgImportance.LOW, KgImportance.fromValue("low"));
        assertEquals(KgImportance.MEDIUM, KgImportance.fromValue("medium"));
        assertEquals(KgImportance.HIGH, KgImportance.fromValue("high"));
    }

    @Test
    @Order(8)
    @DisplayName("KgImportance.fromValue() 对非法值应返回默认 MEDIUM")
    void kgImportance_fromValue_shouldReturnDefaultForInvalidValue() {
        assertEquals(KgImportance.MEDIUM, KgImportance.fromValue("invalid"));
    }

    // ==================== KgNodeStatus ====================

    @Test
    @Order(9)
    @DisplayName("KgNodeStatus.getValue() 应返回正确的字符串值")
    void kgNodeStatus_getValue_shouldReturnCorrectString() {
        assertEquals("active", KgNodeStatus.ACTIVE.getValue());
        assertEquals("deleted", KgNodeStatus.DELETED.getValue());
        assertEquals("merged", KgNodeStatus.MERGED.getValue());
    }

    @Test
    @Order(10)
    @DisplayName("KgNodeStatus.getLabel() 应返回正确的中文标签")
    void kgNodeStatus_getLabel_shouldReturnCorrectLabel() {
        assertEquals("正常", KgNodeStatus.ACTIVE.getLabel());
        assertEquals("已删除", KgNodeStatus.DELETED.getLabel());
        assertEquals("已合并", KgNodeStatus.MERGED.getLabel());
    }

    @Test
    @Order(11)
    @DisplayName("KgNodeStatus.fromValue() 应正确转换字符串为枚举")
    void kgNodeStatus_fromValue_shouldReturnCorrectEnum() {
        assertEquals(KgNodeStatus.ACTIVE, KgNodeStatus.fromValue("active"));
        assertEquals(KgNodeStatus.DELETED, KgNodeStatus.fromValue("deleted"));
        assertEquals(KgNodeStatus.MERGED, KgNodeStatus.fromValue("merged"));
    }

    @Test
    @Order(12)
    @DisplayName("KgNodeStatus.fromValue() 对非法值应返回默认 ACTIVE")
    void kgNodeStatus_fromValue_shouldReturnDefaultForInvalidValue() {
        assertEquals(KgNodeStatus.ACTIVE, KgNodeStatus.fromValue("invalid"));
    }

    // ==================== KgCognitiveLevel ====================

    @Test
    @Order(13)
    @DisplayName("KgCognitiveLevel.getValue() 应返回正确的字符串值")
    void kgCognitiveLevel_getValue_shouldReturnCorrectString() {
        assertEquals("记忆", KgCognitiveLevel.REMEMBER.getValue());
        assertEquals("理解", KgCognitiveLevel.UNDERSTAND.getValue());
        assertEquals("应用", KgCognitiveLevel.APPLY.getValue());
        assertEquals("分析", KgCognitiveLevel.ANALYZE.getValue());
    }

    @Test
    @Order(14)
    @DisplayName("KgCognitiveLevel.getLabel() 应返回正确的中文标签")
    void kgCognitiveLevel_getLabel_shouldReturnCorrectLabel() {
        assertEquals("记忆", KgCognitiveLevel.REMEMBER.getLabel());
        assertEquals("理解", KgCognitiveLevel.UNDERSTAND.getLabel());
        assertEquals("应用", KgCognitiveLevel.APPLY.getLabel());
        assertEquals("分析", KgCognitiveLevel.ANALYZE.getLabel());
    }

    @Test
    @Order(15)
    @DisplayName("KgCognitiveLevel.fromValue() 应正确转换字符串为枚举")
    void kgCognitiveLevel_fromValue_shouldReturnCorrectEnum() {
        assertEquals(KgCognitiveLevel.REMEMBER, KgCognitiveLevel.fromValue("记忆"));
        assertEquals(KgCognitiveLevel.UNDERSTAND, KgCognitiveLevel.fromValue("理解"));
        assertEquals(KgCognitiveLevel.APPLY, KgCognitiveLevel.fromValue("应用"));
        assertEquals(KgCognitiveLevel.ANALYZE, KgCognitiveLevel.fromValue("分析"));
    }

    @Test
    @Order(16)
    @DisplayName("KgCognitiveLevel.fromValue() 对非法值应返回默认 UNDERSTAND")
    void kgCognitiveLevel_fromValue_shouldReturnDefaultForInvalidValue() {
        assertEquals(KgCognitiveLevel.UNDERSTAND, KgCognitiveLevel.fromValue("invalid"));
    }
}
