package com.ai.edu.domain.edukg.model.entity;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgChapter 实体测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgChapterTest {

    private static final String TEST_URI = "http://edukg.org/knowledge/3.1/chapter/准备课";
    private static final String TEST_LABEL = "准备课";
    private static final String TEST_TOPIC = "数与代数";

    @Test
    @Order(1)
    @DisplayName("create() 应正确赋值并默认 status=active")
    void create_shouldSetAllFieldsAndDefaultStatus() {
        KgChapter chapter = KgChapter.create(TEST_URI, TEST_LABEL);

        assertEquals(TEST_URI, chapter.getUri());
        assertEquals(TEST_LABEL, chapter.getLabel());
        assertEquals("active", chapter.getStatus());
        assertNull(chapter.getTopic());
        assertNull(chapter.getMergedToUri());
        assertEquals(0L, chapter.getCreatedBy());
        assertEquals(0L, chapter.getModifiedBy());
        assertFalse(chapter.getDeleted());
    }

    @Test
    @Order(2)
    @DisplayName("updateTopic() 应设置 topic 字段")
    void updateTopic_shouldSetTopic() {
        KgChapter chapter = KgChapter.create(TEST_URI, TEST_LABEL);

        chapter.updateTopic(TEST_TOPIC);

        assertEquals(TEST_TOPIC, chapter.getTopic());
    }
}
