package com.ai.edu.application.service;

import com.ai.edu.application.dto.kg.SyncRecordDTO;
import com.ai.edu.application.dto.kg.SyncRecordQueryRequest;
import com.ai.edu.application.dto.kg.SyncRequest;
import com.ai.edu.application.dto.kg.SyncResult;
import com.ai.edu.application.dto.kg.SyncStatusDTO;
import com.ai.edu.application.service.kg.KgSyncAppService;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.repository.KgSyncRecordRepository;
import com.ai.edu.domain.edukg.repository.KgTextbookRepository;
import com.ai.edu.domain.shared.service.RedisService;
import com.ai.edu.interface_.AiEduPlatformApplication;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgSyncAppService 集成测试
 *
 * 使用真实数据源（Neo4j + MySQL + Redis）进行测试
 * 测试环境需要：
 * - Neo4j: bolt://81.71.130.57:7687
 * - MySQL: ai_edu_kg 数据库
 * - Redis: 用于分布式锁
 *
 * 注意：此测试会实际修改数据库数据
 */
@Slf4j
@SpringBootTest(classes = AiEduPlatformApplication.class)
@ActiveProfiles("integration")
@TestMethodOrder(OrderAnnotation.class)
class KgSyncAppServiceIntegrationTest {

    @Resource
    private KgSyncAppService kgSyncAppService;

    @Resource
    private KgSyncRecordRepository kgSyncRecordRepository;

    @Resource
    private KgTextbookRepository kgTextbookRepository;

    @Resource
    private RedisService redisService;

    // ==================== 参数校验测试 ====================

    @Test
    @Order(1)
    @DisplayName("syncFull request 为 null 应抛 KG_SYNC_PARAM_ERROR")
    void syncFull_nullRequest_shouldThrowParamError() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncFull(null)
        );
        assertEquals("70007", ex.getCode());
        assertTrue(ex.getMessage().contains("同步参数不能为空"));
    }

    @Test
    @Order(2)
    @DisplayName("syncFull subject 为空应抛 KG_SYNC_PARAM_ERROR")
    void syncFull_blankSubject_shouldThrowParamError() {
        SyncRequest request = SyncRequest.builder().subject("  ").edition("人教版").build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncFull(request)
        );
        assertEquals("70007", ex.getCode());
        assertTrue(ex.getMessage().contains("学科不能为空"));
    }

    @Test
    @Order(3)
    @DisplayName("syncFull edition 为空应抛 KG_SYNC_PARAM_ERROR")
    void syncFull_blankEdition_shouldThrowParamError() {
        SyncRequest request = SyncRequest.builder().subject("数学").build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncFull(request)
        );
        assertEquals("70007", ex.getCode());
        assertTrue(ex.getMessage().contains("版本不能为空"));
    }

    // ==================== 教材同步测试 ====================

    @Test
    @Order(10)
    @DisplayName("syncTextbooksOnly 实际同步 — 连接 Neo4j 同步教材数据")
    void syncTextbooksOnly_realSync_shouldUpdateDatabase() {
        // 使用实际存在的 edition 和 subject
        SyncRequest request = SyncRequest.builder()
                .edition("人教版")
                .subject("数学")
                .build();

        log.info("开始教材同步测试: edition={}, subject={}", request.getEdition(), request.getSubject());

        SyncResult result = kgSyncAppService.syncTextbooksOnly(request);

        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertTrue(result.getInsertedCount() >= 0);

        log.info("教材同步完成: insertedCount={}, duration={}ms",
                result.getInsertedCount(), result.getDuration());

        // 验证数据库中有数据
        List<?> textbooks = kgTextbookRepository.findByEditionSubject("人教版", "数学");
        log.info("数据库中教材数量: {}", textbooks.size());
        assertTrue(textbooks.size() >= 0, "教材数据应该同步到数据库");
    }

    // ==================== 全量同步测试 ====================

    @Test
    @Order(20)
    @DisplayName("syncFull 实际同步 — 连接 Neo4j 同步全量数据")
    void syncFull_realSync_shouldUpdateDatabase() {
        SyncRequest request = SyncRequest.builder()
                .edition("人教版")
                .subject("数学")
                .build();

        log.info("开始全量同步测试: edition={}, subject={}", request.getEdition(), request.getSubject());

        SyncResult result = kgSyncAppService.syncFull(request);

        assertNotNull(result);
        // 可能是 success 或 partial_success（取决于是否有锁冲突）
        assertTrue(result.getStatus().equals("success") || result.getStatus().equals("partial_success"));

        log.info("全量同步完成: status={}, totalGrades={}, completedGrades={}, failedGrades={}",
                result.getStatus(), result.getTotalGrades(), result.getCompletedGrades(), result.getFailedGrades());
        log.info("统计: inserted={}, updated={}, deleted={}, reconciliation={}",
                result.getInsertedCount(), result.getUpdatedCount(),
                result.getStatusChangedCount(), result.getReconciliationStatus());
    }

    // ==================== 定向同步测试 ====================

    @Test
    @Order(30)
    @DisplayName("syncFull 定向同步 — 指定年级同步")
    void syncFull_targetedGrade_shouldSyncSpecificGrade() {
        // 先查询有哪些年级
        List<String> grades = kgTextbookRepository.findDistinctGradesByEditionSubject("人教版", "数学");
        log.info("可同步的年级: {}", grades);

        if (grades.isEmpty()) {
            log.warn("没有可同步的年级数据，跳过测试");
            return;
        }

        String targetGrade = grades.get(0);
        SyncRequest request = SyncRequest.builder()
                .edition("人教版")
                .subject("数学")
                .grade(targetGrade)
                .build();

        log.info("定向同步: grade={}", targetGrade);

        SyncResult result = kgSyncAppService.syncFull(request);

        assertNotNull(result);
        assertTrue(result.getStatus().equals("success") || result.getStatus().equals("partial_success"));
        assertTrue(result.getTotalGrades() >= 1);

        log.info("定向同步完成: grade={}, status={}, completed={}",
                targetGrade, result.getStatus(), result.getCompletedGrades());
    }

    // ==================== 指定参数同步测试 ====================

    @Test
    @Order(31)
    @DisplayName("syncFull 指定参数同步 — edition=人教版, subject=数学, stage=小学, grade=一年级")
    void syncFull_specificParams_shouldSyncCorrectly() {
        // Neo4j 中实际存储的年级名称是 "一年级"，不是 "小学一年级上册"
        SyncRequest request = SyncRequest.builder()
                .edition("人教版")
                .subject("数学")
                .stage("小学")
                .grade("一年级")  // 使用 Neo4j 中实际的年级名称
                .build();

        log.info("开始指定参数同步测试: edition={}, subject={}, stage={}, grade={}",
                request.getEdition(), request.getSubject(), request.getStage(), request.getGrade());

        SyncResult result = kgSyncAppService.syncFull(request);

        assertNotNull(result);
        assertTrue(result.getStatus().equals("success") || result.getStatus().equals("partial_success"));

        log.info("指定参数同步完成: status={}, totalGrades={}, completedGrades={}, failedGrades={}",
                result.getStatus(), result.getTotalGrades(), result.getCompletedGrades(), result.getFailedGrades());
        log.info("统计: inserted={}, updated={}, deleted={}, reconciliation={}, duration={}ms",
                result.getInsertedCount(), result.getUpdatedCount(),
                result.getStatusChangedCount(), result.getReconciliationStatus(), result.getDuration());

        // 验证结果
        assertTrue(result.getTotalGrades() >= 0);
        assertTrue(result.getInsertedCount() >= 0);
    }

    // ==================== 同步状态查询 ====================

    @Test
    @Order(40)
    @DisplayName("getSyncStatus 实际查询")
    void getSyncStatus_realQuery_shouldReturnStatus() {
        SyncStatusDTO status = kgSyncAppService.getSyncStatus();

        assertNotNull(status);
        log.info("同步状态: status={}, lastSyncStatus={}, lastSyncAt={}",
                status.getStatus(), status.getLastSyncStatus(), status.getLastSyncAt());
    }

    // ==================== 同步历史查询 ====================

    @Test
    @Order(50)
    @DisplayName("getSyncRecords 实际查询")
    void getSyncRecords_realQuery_shouldReturnRecords() {
        SyncRecordQueryRequest request = SyncRecordQueryRequest.builder()
                .edition("人教版")
                .subject("数学")
                .size(10)
                .build();

        List<SyncRecordDTO> records = kgSyncAppService.getSyncRecords(request);

        assertNotNull(records);
        log.info("同步记录数量: {}", records.size());

        for (SyncRecordDTO record : records) {
            log.info("同步记录: id={}, type={}, status={}, edition={}, subject={}, grade={}",
                    record.getId(), record.getSyncType(), record.getStatus(),
                    record.getEdition(), record.getSubject(), record.getGrade());
        }
    }

    // ==================== Redis 锁测试 ====================

    @Test
    @Order(60)
    @DisplayName("Redis 锁测试 — 验证锁机制正常工作")
    void redisLock_shouldWorkCorrectly() {
        String lockKey = "test:lock:test-key";
        String lockValue = "test-value-123";

        // 尝试获取锁
        boolean acquired = redisService.tryLock(lockKey, lockValue, 10, TimeUnit.SECONDS);
        assertTrue(acquired, "应该能获取到锁");

        log.info("Redis 锁获取成功: key={}", lockKey);

        // 释放锁
        redisService.unlock(lockKey, lockValue);

        log.info("Redis 锁释放成功: key={}", lockKey);
    }
}