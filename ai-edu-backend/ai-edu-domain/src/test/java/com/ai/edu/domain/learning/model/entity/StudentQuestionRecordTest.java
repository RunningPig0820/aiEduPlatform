package com.ai.edu.domain.learning.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * StudentQuestionRecord 题目记录（掌握度事实源）领域单测（test.md QST 相关 / 题目采集事实源）。
 */
class StudentQuestionRecordTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 18, 10, 0);

    @Test
    @DisplayName("create() 默认 source=ai / score=0 / canonical 可空（题型未识别）")
    void create_shouldDefaultSourceAi() {
        StudentQuestionRecord record = StudentQuestionRecord.create(
                1001L, "笼子里有鸡和兔共 35 个头，94 只脚",
                "鸡兔同笼问题", BigDecimal.valueOf(0.5), 1, 0, 888L);

        assertEquals(1001L, record.getStudentId());
        assertEquals("笼子里有鸡和兔共 35 个头，94 只脚", record.getContent());
        assertEquals("ai", record.getSource());
        assertEquals("鸡兔同笼问题", record.getTopicLabel());
        assertEquals(BigDecimal.valueOf(0.50).setScale(2), record.getScore());
        assertEquals(1, record.getHintCount());
        assertEquals(0, record.getAnswerRequestCount());
        assertEquals(888L, record.getSessionId());
        assertNull(record.getCanonicalLabel(), "canonical 未聚集前为空（PENDING）");
        assertEquals(LocalDateTime.now().toLocalDate(), record.getCreatedAt().toLocalDate());
    }

    @Test
    @DisplayName("create() 支持 bank 来源与显式 createdAt")
    void create_shouldSupportBankSource() {
        StudentQuestionRecord record = StudentQuestionRecord.create(
                "bank", 1001L, "题目内容", "鸡兔同笼", "鸡兔同笼",
                BigDecimal.ONE, 0, 0, null, NOW);

        assertEquals("bank", record.getSource());
        assertEquals("鸡兔同笼", record.getCanonicalLabel());
        assertEquals(NOW, record.getCreatedAt());
        assertNull(record.getSessionId(), "无会话链接为 null（显示题目原文）");
    }

    @Test
    @DisplayName("restore() 完整恢复持久化字段（含 id/createdAt/canonical）")
    void restore_shouldRehydrateAllFields() {
        StudentQuestionRecord record = StudentQuestionRecord.restore(
                5001L, 1001L, "题目内容", "ai", "鸡兔同笼问题", "鸡兔同笼",
                BigDecimal.valueOf(0.70), 1, 0, 888L, NOW);

        assertEquals(5001L, record.getId());
        assertEquals("鸡兔同笼", record.getCanonicalLabel());
        assertEquals(BigDecimal.valueOf(0.70).setScale(2), record.getScore());
        assertEquals(NOW, record.getCreatedAt());
    }

    @Test
    @DisplayName("setId() 回填持久化主键")
    void setId_shouldFillId() {
        StudentQuestionRecord record = StudentQuestionRecord.create(
                1001L, "题目内容", "鸡兔同笼", BigDecimal.ONE, 0, 0, null);
        record.setId(5001L);
        assertEquals(5001L, record.getId());
    }
}