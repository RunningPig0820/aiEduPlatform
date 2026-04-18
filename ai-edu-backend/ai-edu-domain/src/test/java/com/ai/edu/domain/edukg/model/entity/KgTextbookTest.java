package com.ai.edu.domain.edukg.model.entity;

import com.ai.edu.domain.edukg.model.valueobject.KgNodeStatus;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgTextbook 实体测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgTextbookTest {

    private static final String TEST_URI = "http://edukg.org/knowledge/3.1/textbook/一年级上册";
    private static final String TEST_LABEL = "一年级上册";
    private static final String TEST_GRADE = "一年级";
    private static final String TEST_STAGE = "primary";
    private static final String TEST_EDITION = "人教版";
    private static final String TEST_SUBJECT = "math";

    @Test
    @Order(1)
    @DisplayName("create() 应正确赋值并默认 status=active")
    void create_shouldSetAllFieldsAndDefaultStatus() {
        KgTextbook textbook = KgTextbook.create(TEST_URI, TEST_LABEL, TEST_GRADE, TEST_STAGE, TEST_EDITION, TEST_SUBJECT);

        assertEquals(TEST_URI, textbook.getUri());
        assertEquals(TEST_LABEL, textbook.getLabel());
        assertEquals(TEST_GRADE, textbook.getGrade());
        assertEquals(TEST_STAGE, textbook.getStage());
        assertEquals(TEST_SUBJECT, textbook.getSubject());
        assertEquals("active", textbook.getStatus());
        assertEquals(0L, textbook.getCreatedBy());
        assertEquals(0L, textbook.getModifiedBy());
        assertFalse(textbook.getDeleted());
    }

    @Test
    @Order(2)
    @DisplayName("markDeleted() 应将 status 设为 merged 并设置 mergedToUri")
    void markDeleted_shouldSetStatusToMergedAndSetMergedToUri() {
        KgTextbook textbook = KgTextbook.create(TEST_URI, TEST_LABEL, TEST_GRADE, TEST_STAGE, TEST_EDITION, TEST_SUBJECT);
        String mergedToUri = "http://edukg.org/knowledge/3.2/textbook/一年级上册";

        textbook.markDeleted(mergedToUri);

        assertEquals(KgNodeStatus.MERGED.getValue(), textbook.getStatus());
        assertEquals(mergedToUri, textbook.getMergedToUri());
    }

    @Test
    @Order(3)
    @DisplayName("isMerged() 应在 status=merged 时返回 true")
    void isMerged_shouldReturnTrueWhenMerged() {
        KgTextbook textbook = KgTextbook.create(TEST_URI, TEST_LABEL, TEST_GRADE, TEST_STAGE, TEST_EDITION, TEST_SUBJECT);
        textbook.markDeleted("http://edukg.org/knowledge/3.2/textbook/一年级上册");

        assertTrue(textbook.isMerged());
    }

    @Test
    @Order(4)
    @DisplayName("isMerged() 应在 status=active 时返回 false")
    void isMerged_shouldReturnFalseWhenActive() {
        KgTextbook textbook = KgTextbook.create(TEST_URI, TEST_LABEL, TEST_GRADE, TEST_STAGE, TEST_EDITION, TEST_SUBJECT);

        assertFalse(textbook.isMerged());
    }
}
