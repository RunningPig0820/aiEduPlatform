package com.ai.edu.application.service;

import com.ai.edu.application.dto.kg.SyncRecordDTO;
import com.ai.edu.application.dto.kg.SyncRecordQueryRequest;
import com.ai.edu.application.dto.kg.SyncRequest;
import com.ai.edu.application.dto.kg.SyncResult;
import com.ai.edu.application.dto.kg.SyncStatusDTO;
import com.ai.edu.application.service.kg.KgSyncAppService;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.ai.edu.domain.edukg.repository.Neo4jNodeRepository;
import com.ai.edu.domain.edukg.repository.Neo4jRelationRepository;
import com.ai.edu.domain.edukg.repository.KgChapterRepository;
import com.ai.edu.domain.edukg.repository.KgChapterSectionRepository;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.edukg.repository.KgSectionKPRepository;
import com.ai.edu.domain.edukg.repository.KgSectionRepository;
import com.ai.edu.domain.edukg.repository.KgSyncRecordRepository;
import com.ai.edu.domain.edukg.repository.KgTextbookChapterRepository;
import com.ai.edu.domain.edukg.repository.KgTextbookRepository;
import com.ai.edu.domain.shared.service.RedisService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KgSyncAppService 单元测试
 *
 * 测试目标：Mock 三个基础设施服务，验证同步流程编排、状态管理
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class KgSyncAppServiceTest {

    @Mock
    private Neo4jNodeRepository neo4jNodeRepository;

    @Mock
    private Neo4jRelationRepository neo4jRelationRepository;

    @Mock
    private KgSyncRecordRepository kgSyncRecordRepository;

    @Mock
    private KgTextbookRepository kgTextbookRepository;

    @Mock
    private KgChapterRepository kgChapterRepository;

    @Mock
    private KgSectionRepository kgSectionRepository;

    @Mock
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    @Mock
    private KgTextbookChapterRepository kgTextbookChapterRepository;

    @Mock
    private KgChapterSectionRepository kgChapterSectionRepository;

    @Mock
    private KgSectionKPRepository kgSectionKPRepository;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private KgSyncAppService kgSyncAppService;

    // ==================== Helper ====================

    private void setSyncRecordId(KgSyncRecord record, long id) {
        try {
            var field = KgSyncRecord.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(record, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mockFullSyncChain() {
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:tb1", "教材1", "七年级", "junior", "人教版", "math")
        );
        List<KgChapter> chapters = List.of(KgChapter.create("uri:ch1", "第一章"));
        List<KgSection> sections = List.of(KgSection.create("uri:sec1", "第一节"));
        List<KgKnowledgePoint> kps = List.of(
                KgKnowledgePoint.create("uri:kp1", "知识点1")
        );

        when(neo4jNodeRepository.findDistinctGrades(any(), any())).thenReturn(List.of("七年级"));
        when(neo4jNodeRepository.findTextbooks(any(), any(), any(), any())).thenReturn(textbooks);
        when(neo4jNodeRepository.findChaptersByTextbookUris(anyList())).thenReturn(chapters);
        when(neo4jNodeRepository.findSectionsByTextbookUris(anyList())).thenReturn(sections);
        when(neo4jNodeRepository.findKnowledgePointsByTextbookUris(anyList())).thenReturn(kps);
        when(neo4jRelationRepository.findTextbookChapterRelations(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findChapterSectionRelations(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findSectionKPRelations(anyList())).thenReturn(List.of());
        when(kgTextbookRepository.upsert(anyList())).thenReturn(1);
        when(kgChapterRepository.upsert(anyList())).thenReturn(1);
        when(kgSectionRepository.upsert(anyList())).thenReturn(1);
        when(kgKnowledgePointRepository.upsert(anyList())).thenReturn(1);
        // Repository mocks for markDeletedNodes + reconcile (grade-scoped)
        when(kgTextbookRepository.findAllActiveByEditionSubjectGrade(any(), any(), any())).thenReturn(textbooks);
        when(kgChapterRepository.findAllActiveByTextbookUris(anyList())).thenReturn(chapters);
        when(kgSectionRepository.findAllActiveByChapterUris(anyList())).thenReturn(sections);
        when(kgKnowledgePointRepository.findAllActiveBySectionUris(anyList())).thenReturn(kps);
        // 关联表 scoped mock（替代原来的 findAllActive）
        when(kgTextbookChapterRepository.findByTextbookUris(anyList())).thenReturn(List.of());
        when(kgChapterSectionRepository.findByChapterUris(anyList())).thenReturn(List.of());
        when(kgSectionKPRepository.findBySectionUris(anyList())).thenReturn(List.of());
    }

    private KgSyncRecord createSyncRecord(Long id) {
        KgSyncRecord record = KgSyncRecord.create("full", "人教版", "math", null, null, 0L);
        if (id != null) setSyncRecordId(record, id);
        return record;
    }

    // ==================== 6.7.1 syncFull 全量同步 ====================

    @Test
    @Order(1)
    @DisplayName("syncFull 全量同步 — 应调用完整链路并返回成功结果")
    void syncFull_fullSync_shouldCallFullChainAndReturnSuccess() {
        SyncRequest request = SyncRequest.builder().subject("math").edition("人教版").build();
        KgSyncRecord syncRecord = createSyncRecord(1L);
        when(kgSyncRecordRepository.save(any(KgSyncRecord.class))).thenReturn(syncRecord);
        when(kgSyncRecordRepository.findById(1L)).thenReturn(Optional.of(syncRecord));
        mockFullSyncChain();

        SyncResult result = kgSyncAppService.syncFull(request);

        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals("matched", result.getReconciliationStatus());
        assertEquals(1, result.getTotalGrades());
        assertEquals(1, result.getCompletedGrades());
        assertEquals(0, result.getFailedGrades());

        verify(neo4jNodeRepository).findDistinctGrades(eq("人教版"), eq("math"));
        verify(neo4jNodeRepository, atLeast(1)).findTextbooks(any(), any(), any(), any());
        verify(neo4jNodeRepository, atLeast(1)).findChaptersByTextbookUris(anyList());
        verify(neo4jNodeRepository, atLeast(1)).findSectionsByTextbookUris(anyList());
        verify(neo4jNodeRepository, atLeast(1)).findKnowledgePointsByTextbookUris(anyList());
        verify(kgTextbookRepository).upsert(anyList());
        verify(kgChapterRepository).upsert(anyList());
        verify(kgSectionRepository).upsert(anyList());
        verify(kgKnowledgePointRepository).upsert(anyList());
        verify(kgSyncRecordRepository, atLeast(2)).save(any(KgSyncRecord.class));
    }

    // ==================== 6.7.2 syncFull 定向同步 ====================

    @Test
    @Order(2)
    @DisplayName("syncFull 定向同步 — 按 edition 过滤")
    void syncFull_targetedByEdition_shouldFilterByEdition() {
        String targetEdition = "人教版";
        SyncRequest request = SyncRequest.builder().subject("math").edition(targetEdition).build();

        List<String> grades = List.of("七年级", "八年级");
        when(neo4jNodeRepository.findDistinctGrades(any(), any())).thenReturn(grades);
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        List<KgTextbook> filteredTextbooks = List.of(
                KgTextbook.create("uri:tb1", "教材1", "七年级", "junior", targetEdition, "math")
        );
        when(neo4jNodeRepository.findTextbooks(any(), any(), any(), any())).thenReturn(filteredTextbooks);
        when(neo4jNodeRepository.findChaptersByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jNodeRepository.findSectionsByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jNodeRepository.findKnowledgePointsByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findTextbookChapterRelations(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findChapterSectionRelations(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findSectionKPRelations(anyList())).thenReturn(List.of());
        when(kgTextbookRepository.upsert(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(kgChapterRepository.upsert(anyList())).thenReturn(0);
        when(kgSectionRepository.upsert(anyList())).thenReturn(0);
        when(kgKnowledgePointRepository.upsert(anyList())).thenReturn(0);
        when(kgTextbookRepository.findAllActiveByEditionSubjectGrade(any(), any(), any())).thenReturn(List.of());
        when(kgChapterRepository.findAllActiveByTextbookUris(anyList())).thenReturn(List.of());
        when(kgSectionRepository.findAllActiveByChapterUris(anyList())).thenReturn(List.of());
        when(kgKnowledgePointRepository.findAllActiveBySectionUris(anyList())).thenReturn(List.of());
        when(kgTextbookChapterRepository.findByTextbookUris(anyList())).thenReturn(List.of());
        when(kgChapterSectionRepository.findByChapterUris(anyList())).thenReturn(List.of());
        when(kgSectionKPRepository.findBySectionUris(anyList())).thenReturn(List.of());
        when(kgSyncRecordRepository.findLatestRunningByScope(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        KgSyncRecord syncRecord = createSyncRecord(2L);
        when(kgSyncRecordRepository.save(any(KgSyncRecord.class))).thenReturn(syncRecord);
        when(kgSyncRecordRepository.findById(anyLong())).thenReturn(Optional.of(syncRecord));

        SyncResult result = kgSyncAppService.syncFull(request);

        assertEquals("success", result.getStatus());
        assertEquals(2, result.getTotalGrades());
        assertEquals(2, result.getCompletedGrades());
    }

    @Test
    @Order(3)
    @DisplayName("syncFull 定向同步 — 按 subject 过滤")
    void syncFull_targetedBySubject_shouldFilterBySubject() {
        SyncRequest request = SyncRequest.builder().subject("english").edition("人教版").build();

        when(neo4jNodeRepository.findDistinctGrades(any(), any())).thenReturn(List.of("七年级"));
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        List<KgTextbook> filteredTextbooks = List.of(
                KgTextbook.create("uri:eng", "英语教材", "七年级", "junior", "人教版", "english")
        );
        when(neo4jNodeRepository.findTextbooks(any(), any(), any(), any())).thenReturn(filteredTextbooks);
        when(neo4jNodeRepository.findChaptersByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jNodeRepository.findSectionsByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jNodeRepository.findKnowledgePointsByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findTextbookChapterRelations(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findChapterSectionRelations(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findSectionKPRelations(anyList())).thenReturn(List.of());
        when(kgTextbookRepository.upsert(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(kgChapterRepository.upsert(anyList())).thenReturn(0);
        when(kgSectionRepository.upsert(anyList())).thenReturn(0);
        when(kgKnowledgePointRepository.upsert(anyList())).thenReturn(0);
        when(kgTextbookRepository.findAllActiveByEditionSubjectGrade(any(), any(), any())).thenReturn(List.of());
        when(kgChapterRepository.findAllActiveByTextbookUris(anyList())).thenReturn(List.of());
        when(kgSectionRepository.findAllActiveByChapterUris(anyList())).thenReturn(List.of());
        when(kgKnowledgePointRepository.findAllActiveBySectionUris(anyList())).thenReturn(List.of());
        when(kgTextbookChapterRepository.findByTextbookUris(anyList())).thenReturn(List.of());
        when(kgChapterSectionRepository.findByChapterUris(anyList())).thenReturn(List.of());
        when(kgSectionKPRepository.findBySectionUris(anyList())).thenReturn(List.of());
        when(kgSyncRecordRepository.findLatestRunningByScope(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        KgSyncRecord syncRecord = createSyncRecord(3L);
        when(kgSyncRecordRepository.save(any(KgSyncRecord.class))).thenReturn(syncRecord);
        when(kgSyncRecordRepository.findById(anyLong())).thenReturn(Optional.of(syncRecord));

        SyncResult result = kgSyncAppService.syncFull(request);

        assertEquals("success", result.getStatus());
    }

    // ==================== 6.7.3 syncFull 同步锁 ====================

    @Test
    @Order(4)
    @DisplayName("syncFull 单个年级锁冲突 — 应跳过该年级并返回 partial_success")
    void syncFull_gradeLockConflict_shouldReturnPartialSuccess() {
        SyncRequest request = SyncRequest.builder().subject("math").edition("人教版").build();

        when(neo4jNodeRepository.findDistinctGrades(any(), any())).thenReturn(List.of("七年级"));
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);
        when(kgSyncRecordRepository.findLatestRunningByScope(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        SyncResult result = kgSyncAppService.syncFull(request);

        assertEquals("partial_success", result.getStatus());
        assertEquals(1, result.getTotalGrades());
        assertEquals(0, result.getCompletedGrades());
        assertEquals(1, result.getFailedGrades());
    }

    // ==================== 6.7.4 syncFull 异常回滚 ====================

    @Test
    @Order(5)
    @DisplayName("syncFull 异常回滚 — NodeSync 抛异常应记录 failed grade")
    void syncFull_nodeSyncThrows_shouldRecordFailedGrade() {
        when(neo4jNodeRepository.findDistinctGrades(any(), any())).thenReturn(List.of("七年级"));
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(neo4jNodeRepository.findTextbooks(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Neo4j connection timeout"));

        SyncResult result = kgSyncAppService.syncFull(
                SyncRequest.builder().subject("math").edition("人教版").build());

        assertEquals("partial_success", result.getStatus());
        assertEquals(1, result.getTotalGrades());
        assertEquals(0, result.getCompletedGrades());
        assertEquals(1, result.getFailedGrades());
    }

    // ==================== 6.7.5 getSyncStatus ====================

    @Test
    @Order(6)
    @DisplayName("getSyncStatus 从未同步过应返回 never_synced")
    void getSyncStatus_neverSynced_shouldReturnNeverSynced() {
        when(kgSyncRecordRepository.findRecent(1)).thenReturn(List.of());

        SyncStatusDTO result = kgSyncAppService.getSyncStatus();

        assertNotNull(result);
        assertEquals("never_synced", result.getStatus());
    }

    @Test
    @Order(7)
    @DisplayName("getSyncStatus 有同步记录应返回状态")
    void getSyncStatus_hasRecord_shouldReturnStatus() {
        KgSyncRecord record = KgSyncRecord.create("full", "人教版", "math", null, null, 0L);
        record.completeSuccess(10, 5, 3, "matched", "All counts matched");

        when(kgSyncRecordRepository.findRecent(1)).thenReturn(List.of(record));

        SyncStatusDTO result = kgSyncAppService.getSyncStatus();

        assertNotNull(result);
        assertEquals("idle", result.getStatus());
        assertEquals("success", result.getLastSyncStatus());
    }

    // ==================== 6.7.6 getSyncRecords ====================

    @Test
    @Order(8)
    @DisplayName("getSyncRecords 应返回分页记录")
    void getSyncRecords_shouldReturnRecords() {
        KgSyncRecord record1 = KgSyncRecord.create("full", "人教版", "math", null, null, 0L);
        record1.completeSuccess(10, 5, 0, "matched", "ok");
        setSyncRecordId(record1, 1L);

        KgSyncRecord record2 = KgSyncRecord.create("full", "人教版", "english", null, null, 0L);
        record2.completeSuccess(8, 2, 1, "matched", "ok");
        setSyncRecordId(record2, 2L);

        when(kgSyncRecordRepository.findByScope(null, null, null, null, 5)).thenReturn(List.of(record1, record2));

        SyncRecordQueryRequest request = SyncRecordQueryRequest.builder().size(5).build();
        List<SyncRecordDTO> result = kgSyncAppService.getSyncRecords(request);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("full", result.get(0).getSyncType());
        assertEquals("success", result.get(0).getStatus());
        assertEquals(10, result.get(0).getInsertedCount());
    }

    @Test
    @Order(9)
    @DisplayName("getSyncRecords 无记录应返回空列表")
    void getSyncRecords_noRecords_shouldReturnEmpty() {
        when(kgSyncRecordRepository.findByScope(null, null, null, null, 10)).thenReturn(List.of());

        SyncRecordQueryRequest request = SyncRecordQueryRequest.builder().size(10).build();
        List<SyncRecordDTO> result = kgSyncAppService.getSyncRecords(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== syncFull request=null 参数校验 ====================

    @Test
    @Order(10)
    @DisplayName("syncFull request 为 null 应抛 KG_SYNC_PARAM_ERROR")
    void syncFull_nullRequest_shouldThrowParamError() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncFull(null)
        );
        assertEquals("70007", ex.getCode());
        assertTrue(ex.getMessage().contains("同步参数不能为空"));
    }

    // ==================== syncFull 参数校验 ====================

    @Test
    @Order(11)
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
    @Order(11)
    @DisplayName("syncFull edition 为空应抛 KG_SYNC_PARAM_ERROR")
    void syncFull_blankEdition_shouldThrowParamError() {
        SyncRequest request = SyncRequest.builder().subject("math").build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncFull(request)
        );
        assertEquals("70007", ex.getCode());
        assertTrue(ex.getMessage().contains("版本不能为空"));
    }

    // ==================== 6.7.12 syncTextbooksOnly 正常流程 ====================

    @Test
    @Order(12)
    @DisplayName("syncTextbooksOnly 正常流程 — 按 edition 过滤并返回成功")
    void syncTextbooksOnly_withEdition_shouldFilterAndReturnSuccess() {
        List<KgTextbook> filteredTextbooks = List.of(
                KgTextbook.create("uri:tb1", "教材1", "七年级", "junior", "人教版", "math")
        );
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(neo4jNodeRepository.findTextbooks(any(), any(), any(), any())).thenReturn(filteredTextbooks);
        when(kgTextbookRepository.upsert(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());

        KgSyncRecord syncRecord = createSyncRecord(20L);
        when(kgSyncRecordRepository.save(any(KgSyncRecord.class))).thenReturn(syncRecord);
        when(kgSyncRecordRepository.findById(20L)).thenReturn(Optional.of(syncRecord));

        SyncRequest request = SyncRequest.builder().subject("math").edition("人教版").build();
        SyncResult result = kgSyncAppService.syncTextbooksOnly(request);

        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals(1, result.getInsertedCount());

        verify(kgTextbookRepository).upsert(argThat(tbs ->
                tbs.size() == 1 && ((KgTextbook) tbs.get(0)).getEdition().equals("人教版")
        ));
        verify(kgSyncRecordRepository, times(2)).save(any(KgSyncRecord.class));
    }

    // ==================== 6.7.13 syncTextbooksOnly 同步锁 ====================

    @Test
    @Order(13)
    @DisplayName("syncTextbooksOnly 同步锁 — 同步中再次调用应抛异常")
    void syncTextbooksOnly_concurrentCalls_shouldThrowOnSecondCall() {
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncTextbooksOnly(SyncRequest.builder().subject("math").edition("人教版").build())
        );
        assertEquals("70006", ex.getCode());
        assertTrue(ex.getMessage().contains("已有同步任务正在执行"));
    }

    // ==================== 6.7.14 syncTextbooksOnly 异常场景 ====================

    @Test
    @Order(14)
    @DisplayName("syncTextbooksOnly 异常场景 — NodeSync 抛异常应记录 failed")
    void syncTextbooksOnly_nodeSyncThrows_shouldRecordFailed() {
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        KgSyncRecord syncRecord = createSyncRecord(30L);
        when(kgSyncRecordRepository.save(any(KgSyncRecord.class))).thenReturn(syncRecord);
        when(kgSyncRecordRepository.findById(30L)).thenReturn(Optional.of(syncRecord));
        when(neo4jNodeRepository.findTextbooks(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Neo4j connection timeout"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncTextbooksOnly(SyncRequest.builder().subject("math").edition("人教版").build())
        );
        assertTrue(ex.getMessage().contains("Neo4j connection timeout"));

        verify(kgSyncRecordRepository, times(2)).save(any(KgSyncRecord.class));
        verify(redisService).unlock(anyString(), anyString());
    }

    // ==================== 新增：过期任务检测 ====================

    @Test
    @Order(20)
    @DisplayName("过期任务检测 — 存在 stale running 任务应标记 failed 并继续同步")
    void staleTask_shouldMarkFailedAndContinue() {
        when(neo4jNodeRepository.findDistinctGrades(any(), any())).thenReturn(List.of("七年级"));
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // 模拟一个过期的 running 记录
        KgSyncRecord staleRecord = KgSyncRecord.create("full", "人教版", "math", null, "七年级", 0L);
        setSyncRecordId(staleRecord, 99L);
        when(kgSyncRecordRepository.findLatestRunningByScope(any(), any(), any(), any()))
                .thenReturn(Optional.of(staleRecord));

        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:tb1", "教材1", "七年级", "junior", "人教版", "math")
        );
        when(neo4jNodeRepository.findTextbooks(any(), any(), any(), any())).thenReturn(textbooks);
        when(neo4jNodeRepository.findChaptersByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jNodeRepository.findSectionsByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jNodeRepository.findKnowledgePointsByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findTextbookChapterRelations(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findChapterSectionRelations(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findSectionKPRelations(anyList())).thenReturn(List.of());
        when(kgTextbookRepository.upsert(anyList())).thenReturn(1);
        when(kgChapterRepository.upsert(anyList())).thenReturn(0);
        when(kgSectionRepository.upsert(anyList())).thenReturn(0);
        when(kgKnowledgePointRepository.upsert(anyList())).thenReturn(0);
        when(kgTextbookRepository.findAllActiveByEditionSubjectGrade(any(), any(), any())).thenReturn(textbooks);
        when(kgChapterRepository.findAllActiveByTextbookUris(anyList())).thenReturn(List.of());
        when(kgSectionRepository.findAllActiveByChapterUris(anyList())).thenReturn(List.of());
        when(kgKnowledgePointRepository.findAllActiveBySectionUris(anyList())).thenReturn(List.of());
        when(kgTextbookChapterRepository.findByTextbookUris(anyList())).thenReturn(List.of());
        when(kgChapterSectionRepository.findByChapterUris(anyList())).thenReturn(List.of());
        when(kgSectionKPRepository.findBySectionUris(anyList())).thenReturn(List.of());

        KgSyncRecord syncRecord = createSyncRecord(100L);
        when(kgSyncRecordRepository.save(any(KgSyncRecord.class)))
                .thenReturn(staleRecord)
                .thenReturn(syncRecord);
        when(kgSyncRecordRepository.findById(anyLong())).thenReturn(Optional.of(syncRecord));

        SyncResult result = kgSyncAppService.syncFull(
                SyncRequest.builder().subject("math").edition("人教版").build());

        assertEquals("success", result.getStatus());

        // 验证 save 被调用了两次：一次标记过期任务，一次保存新记录
        verify(kgSyncRecordRepository, times(2)).save(any(KgSyncRecord.class));
    }

    // ==================== 新增：多年级部分失败 ====================

    @Test
    @Order(21)
    @DisplayName("多年级同步 — 部分成功部分失败应返回 partial_success")
    void multiGrade_partialFailure_shouldReturnPartialSuccess() {
        when(neo4jNodeRepository.findDistinctGrades(any(), any())).thenReturn(List.of("七年级", "八年级"));
        // 七年级能获取锁，八年级不能
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true)   // 七年级
                .thenReturn(false); // 八年级
        when(kgSyncRecordRepository.findLatestRunningByScope(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:tb1", "教材1", "七年级", "junior", "人教版", "math")
        );
        when(neo4jNodeRepository.findTextbooks(any(), any(), any(), any())).thenReturn(textbooks);
        when(neo4jNodeRepository.findChaptersByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jNodeRepository.findSectionsByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jNodeRepository.findKnowledgePointsByTextbookUris(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findTextbookChapterRelations(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findChapterSectionRelations(anyList())).thenReturn(List.of());
        when(neo4jRelationRepository.findSectionKPRelations(anyList())).thenReturn(List.of());
        when(kgTextbookRepository.upsert(anyList())).thenReturn(1);
        when(kgChapterRepository.upsert(anyList())).thenReturn(0);
        when(kgSectionRepository.upsert(anyList())).thenReturn(0);
        when(kgKnowledgePointRepository.upsert(anyList())).thenReturn(0);
        when(kgTextbookRepository.findAllActiveByEditionSubjectGrade(any(), any(), any())).thenReturn(textbooks);
        when(kgChapterRepository.findAllActiveByTextbookUris(anyList())).thenReturn(List.of());
        when(kgSectionRepository.findAllActiveByChapterUris(anyList())).thenReturn(List.of());
        when(kgKnowledgePointRepository.findAllActiveBySectionUris(anyList())).thenReturn(List.of());
        when(kgTextbookChapterRepository.findByTextbookUris(anyList())).thenReturn(List.of());
        when(kgChapterSectionRepository.findByChapterUris(anyList())).thenReturn(List.of());
        when(kgSectionKPRepository.findBySectionUris(anyList())).thenReturn(List.of());

        KgSyncRecord syncRecord = createSyncRecord(200L);
        when(kgSyncRecordRepository.save(any(KgSyncRecord.class))).thenReturn(syncRecord);
        when(kgSyncRecordRepository.findById(anyLong())).thenReturn(Optional.of(syncRecord));

        SyncResult result = kgSyncAppService.syncFull(
                SyncRequest.builder().subject("math").edition("人教版").build());

        assertEquals("partial_success", result.getStatus());
        assertEquals(2, result.getTotalGrades());
        assertEquals(1, result.getCompletedGrades());
        assertEquals(1, result.getFailedGrades());
    }

    // ==================== 新增：空年级列表 ====================

    @Test
    @Order(22)
    @DisplayName("空年级列表 — Neo4j 无数据应返回空结果")
    void emptyGrades_shouldReturnEmptyResult() {
        when(neo4jNodeRepository.findDistinctGrades(any(), any())).thenReturn(List.of());

        SyncResult result = kgSyncAppService.syncFull(
                SyncRequest.builder().subject("math").edition("人教版").build());

        assertEquals("success", result.getStatus());
        assertEquals(0, result.getTotalGrades());
    }
}
