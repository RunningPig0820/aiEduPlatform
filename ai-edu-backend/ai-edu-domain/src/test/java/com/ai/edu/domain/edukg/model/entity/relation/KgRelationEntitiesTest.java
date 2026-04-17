package com.ai.edu.domain.edukg.model.entity.relation;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 关联实体测试（KgTextbookChapter, KgChapterSection, KgSectionKP）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgRelationEntitiesTest {

    @Test
    @Order(1)
    @DisplayName("KgTextbookChapter.create() 应正确赋值且 orderIndex 可为 0")
    void textbookChapterCreate_shouldSetFields() {
        String textbookUri = "http://edukg.org/knowledge/3.1/textbook/一年级上册";
        String chapterUri = "http://edukg.org/knowledge/3.1/chapter/准备课";

        KgTextbookChapter relation = KgTextbookChapter.create(textbookUri, chapterUri, 1);

        assertEquals(textbookUri, relation.getTextbookUri());
        assertEquals(chapterUri, relation.getChapterUri());
        assertEquals(1, relation.getOrderIndex());
        assertEquals(0L, relation.getCreatedBy());
        assertEquals(0L, relation.getModifiedBy());
        assertFalse(relation.getDeleted());
    }

    @Test
    @Order(2)
    @DisplayName("KgChapterSection.create() 应正确赋值")
    void chapterSectionCreate_shouldSetFields() {
        String chapterUri = "http://edukg.org/knowledge/3.1/chapter/准备课";
        String sectionUri = "http://edukg.org/knowledge/3.1/section/10以内数的认识";

        KgChapterSection relation = KgChapterSection.create(chapterUri, sectionUri, 2);

        assertEquals(chapterUri, relation.getChapterUri());
        assertEquals(sectionUri, relation.getSectionUri());
        assertEquals(2, relation.getOrderIndex());
        assertEquals(0L, relation.getCreatedBy());
        assertFalse(relation.getDeleted());
    }

    @Test
    @Order(3)
    @DisplayName("KgSectionKP.create() 应正确赋值")
    void sectionKpCreate_shouldSetFields() {
        String sectionUri = "http://edukg.org/knowledge/3.1/section/10以内数的认识";
        String kpUri = "http://edukg.org/knowledge/3.1/kp/加法";

        KgSectionKP relation = KgSectionKP.create(sectionUri, kpUri, 0);

        assertEquals(sectionUri, relation.getSectionUri());
        assertEquals(kpUri, relation.getKpUri());
        assertEquals(0, relation.getOrderIndex());
        assertEquals(0L, relation.getCreatedBy());
        assertFalse(relation.getDeleted());
    }
}
