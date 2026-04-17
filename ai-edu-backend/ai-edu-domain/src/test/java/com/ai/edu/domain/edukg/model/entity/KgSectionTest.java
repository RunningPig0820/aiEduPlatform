package com.ai.edu.domain.edukg.model.entity;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgSection 实体测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgSectionTest {

    private static final String TEST_URI = "http://edukg.org/knowledge/3.1/section/10以内数的认识";
    private static final String TEST_LABEL = "10以内数的认识";

    @Test
    @Order(1)
    @DisplayName("create() 应正确赋值并默认 status=active")
    void create_shouldSetAllFieldsAndDefaultStatus() {
        KgSection section = KgSection.create(TEST_URI, TEST_LABEL);

        assertEquals(TEST_URI, section.getUri());
        assertEquals(TEST_LABEL, section.getLabel());
        assertEquals("active", section.getStatus());
        assertNull(section.getMergedToUri());
        assertEquals(0L, section.getCreatedBy());
        assertEquals(0L, section.getModifiedBy());
        assertFalse(section.getDeleted());
    }
}
