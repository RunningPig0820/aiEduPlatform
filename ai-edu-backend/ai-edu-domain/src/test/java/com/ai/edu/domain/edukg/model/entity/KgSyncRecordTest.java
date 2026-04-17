package com.ai.edu.domain.edukg.model.entity;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgSyncRecord 实体测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgSyncRecordTest {

    @Test
    @Order(1)
    @DisplayName("create() 应正确赋值并默认 status=running")
    void create_shouldSetFieldsAndDefaultStatus() {
        KgSyncRecord record = KgSyncRecord.create("full", "grade=一年级", 100L);

        assertEquals("full", record.getSyncType());
        assertEquals("grade=一年级", record.getScope());
        assertEquals("running", record.getStatus());
        assertEquals(100L, record.getCreatedBy());
        assertEquals((Integer) 0, record.getInsertedCount());
        assertEquals((Integer) 0, record.getUpdatedCount());
        assertEquals((Integer) 0, record.getStatusChangedCount());
        assertNull(record.getReconciliationStatus());
        assertNull(record.getErrorMessage());
        assertNull(record.getDetails());
        assertNotNull(record.getStartedAt());
        assertNull(record.getFinishedAt());
        assertFalse(record.getDeleted());
    }

    @Test
    @Order(2)
    @DisplayName("completeSuccess() 应更新状态和计数")
    void completeSuccess_shouldUpdateStatusAndCounts() {
        KgSyncRecord record = KgSyncRecord.create("full", null, 100L);

        record.completeSuccess(10, 5, 2, "matched", "All counts matched");

        assertEquals("success", record.getStatus());
        assertEquals(10, record.getInsertedCount());
        assertEquals(5, record.getUpdatedCount());
        assertEquals(2, record.getStatusChangedCount());
        assertEquals("matched", record.getReconciliationStatus());
        assertEquals("All counts matched", record.getReconciliationDetails());
        assertNotNull(record.getFinishedAt());
        assertNull(record.getErrorMessage());
    }

    @Test
    @Order(3)
    @DisplayName("completeFailure() 应更新 status=failed 并设置错误信息")
    void completeFailure_shouldUpdateStatusAndSetErrorMessage() {
        KgSyncRecord record = KgSyncRecord.create("full", null, 100L);

        record.completeFailure("Neo4j connection timeout");

        assertEquals("failed", record.getStatus());
        assertEquals("Neo4j connection timeout", record.getErrorMessage());
        assertNotNull(record.getFinishedAt());
    }

    @Test
    @Order(4)
    @DisplayName("completeFailure() 不应修改 insertedCount 等计数字段")
    void completeFailure_shouldNotModifyCounts() {
        KgSyncRecord record = KgSyncRecord.create("full", null, 100L);
        record.completeSuccess(10, 5, 2, "matched", "OK");

        record.completeFailure("error after success");

        assertEquals("failed", record.getStatus());
        assertEquals("error after success", record.getErrorMessage());
        assertEquals(10, record.getInsertedCount());
        assertEquals(5, record.getUpdatedCount());
    }
}
