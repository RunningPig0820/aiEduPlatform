package com.ai.edu.domain.edukg.model.valueobject;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 值对象枚举测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgValueObjectTest {

    // ==================== KgImportance ====================

    @Test
    @Order(1)
    @DisplayName("KgImportance.getValue() 应返回正确的字符串值")
    void kgImportance_getValue_shouldReturnCorrectString() {
        assertEquals("low", KgImportance.LOW.getValue());
        assertEquals("medium", KgImportance.MEDIUM.getValue());
        assertEquals("high", KgImportance.HIGH.getValue());
    }

    @Test
    @Order(2)
    @DisplayName("KgImportance.getLabel() 应返回正确的中文标签")
    void kgImportance_getLabel_shouldReturnCorrectLabel() {
        assertEquals("低", KgImportance.LOW.getLabel());
        assertEquals("中", KgImportance.MEDIUM.getLabel());
        assertEquals("高", KgImportance.HIGH.getLabel());
    }

    @Test
    @Order(3)
    @DisplayName("KgImportance.fromValue() 应正确转换字符串为枚举")
    void kgImportance_fromValue_shouldReturnCorrectEnum() {
        assertEquals(KgImportance.LOW, KgImportance.fromValue("low"));
        assertEquals(KgImportance.MEDIUM, KgImportance.fromValue("medium"));
        assertEquals(KgImportance.HIGH, KgImportance.fromValue("high"));
    }

    @Test
    @Order(4)
    @DisplayName("KgImportance.fromValue() 对非法值应返回默认 MEDIUM")
    void kgImportance_fromValue_shouldReturnDefaultForInvalidValue() {
        assertEquals(KgImportance.MEDIUM, KgImportance.fromValue("invalid"));
    }

    // ==================== KgNodeStatus ====================

    @Test
    @Order(5)
    @DisplayName("KgNodeStatus.getValue() 应返回正确的字符串值")
    void kgNodeStatus_getValue_shouldReturnCorrectString() {
        assertEquals("active", KgNodeStatus.ACTIVE.getValue());
        assertEquals("deleted", KgNodeStatus.DELETED.getValue());
        assertEquals("merged", KgNodeStatus.MERGED.getValue());
    }

    @Test
    @Order(6)
    @DisplayName("KgNodeStatus.getLabel() 应返回正确的中文标签")
    void kgNodeStatus_getLabel_shouldReturnCorrectLabel() {
        assertEquals("正常", KgNodeStatus.ACTIVE.getLabel());
        assertEquals("已删除", KgNodeStatus.DELETED.getLabel());
        assertEquals("已合并", KgNodeStatus.MERGED.getLabel());
    }

    @Test
    @Order(7)
    @DisplayName("KgNodeStatus.fromValue() 应正确转换字符串为枚举")
    void kgNodeStatus_fromValue_shouldReturnCorrectEnum() {
        assertEquals(KgNodeStatus.ACTIVE, KgNodeStatus.fromValue("active"));
        assertEquals(KgNodeStatus.DELETED, KgNodeStatus.fromValue("deleted"));
        assertEquals(KgNodeStatus.MERGED, KgNodeStatus.fromValue("merged"));
    }

    @Test
    @Order(8)
    @DisplayName("KgNodeStatus.fromValue() 对非法值应返回默认 ACTIVE")
    void kgNodeStatus_fromValue_shouldReturnDefaultForInvalidValue() {
        assertEquals(KgNodeStatus.ACTIVE, KgNodeStatus.fromValue("invalid"));
    }
}
