package com.ai.edu.domain.edukg.model.entity;

import com.ai.edu.domain.edukg.model.valueobject.KgImportance;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgKnowledgePoint 实体测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgKnowledgePointTest {

    private static final String TEST_URI = "http://edukg.org/knowledge/3.1/kp/加法";
    private static final String TEST_LABEL = "加法";

    @Test
    @Order(1)
    @DisplayName("create() 应正确赋值并默认 status=active")
    void create_shouldSetAllFieldsAndDefaultStatus() {
        KgKnowledgePoint kp = KgKnowledgePoint.create(TEST_URI, TEST_LABEL);

        assertEquals(TEST_URI, kp.getUri());
        assertEquals(TEST_LABEL, kp.getLabel());
        assertEquals("active", kp.getStatus());
        assertNull(kp.getDifficulty());
        assertNull(kp.getImportance());
        assertNull(kp.getCognitiveLevel());
        assertNull(kp.getMergedToUri());
        assertEquals(0L, kp.getCreatedBy());
        assertEquals(0L, kp.getModifiedBy());
        assertFalse(kp.getDeleted());
    }

    @Test
    @Order(2)
    @DisplayName("updateAttributes() 应更新 difficulty/importance/cognitiveLevel")
    void updateAttributes_shouldSetAllAttributes() {
        KgKnowledgePoint kp = KgKnowledgePoint.create(TEST_URI, TEST_LABEL);

        kp.updateAttributes("easy", "high", "理解");

        assertEquals("easy", kp.getDifficulty());
        assertEquals("high", kp.getImportance());
        assertEquals("理解", kp.getCognitiveLevel());
    }

    @Test
    @Order(3)
    @DisplayName("isHighImport() 应在 importance=high 时返回 true")
    void isHighImport_shouldReturnTrueWhenHigh() {
        KgKnowledgePoint kp = KgKnowledgePoint.create(TEST_URI, TEST_LABEL);
        kp.updateAttributes("easy", KgImportance.HIGH.getValue(), "理解");

        assertTrue(kp.isHighImport());
    }

    @Test
    @Order(4)
    @DisplayName("isHighImport() 应在 importance=medium 时返回 false")
    void isHighImport_shouldReturnFalseWhenMedium() {
        KgKnowledgePoint kp = KgKnowledgePoint.create(TEST_URI, TEST_LABEL);
        kp.updateAttributes("easy", KgImportance.MEDIUM.getValue(), "理解");

        assertFalse(kp.isHighImport());
    }

    @Test
    @Order(5)
    @DisplayName("isHighImport() 应在 importance 为 null 时返回 false")
    void isHighImport_shouldReturnFalseWhenNull() {
        KgKnowledgePoint kp = KgKnowledgePoint.create(TEST_URI, TEST_LABEL);

        assertFalse(kp.isHighImport());
    }
}
