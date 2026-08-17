package com.ai.edu.domain.learning.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * QuestionTypeAlias 变体题型别名实体领域单测（test.md ALI 别名数据层）。
 */
class QuestionTypeAliasTest {

    @Test
    @DisplayName("create() 记录变体别名与 canonical 题型")
    void create_shouldBindAliasToQuestionType() {
        QuestionTypeAlias alias = QuestionTypeAlias.create("鸡兔同笼", 5L);
        assertEquals("鸡兔同笼", alias.getAliasLabel());
        assertEquals(5L, alias.getQuestionTypeId());
        assertNotNull(alias.getCreatedAt());
    }

    @Test
    @DisplayName("restore() 从持久化状态恢复全字段")
    void restore_shouldHydrateAllFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 17, 3, 30);
        QuestionTypeAlias alias = QuestionTypeAlias.restore(9L, "鸡兔同笼", 5L, createdAt);
        assertEquals(9L, alias.getId());
        assertEquals("鸡兔同笼", alias.getAliasLabel());
        assertEquals(5L, alias.getQuestionTypeId());
        assertEquals(createdAt, alias.getCreatedAt());
    }

    @Test
    @DisplayName("setId 供仓储回填主键")
    void setId_shouldHydrateId() {
        QuestionTypeAlias alias = QuestionTypeAlias.create("鸡兔同笼", 5L);
        alias.setId(9L);
        assertEquals(9L, alias.getId());
    }
}
