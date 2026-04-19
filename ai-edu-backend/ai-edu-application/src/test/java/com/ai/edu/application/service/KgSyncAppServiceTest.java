package com.ai.edu.application.service;

import com.ai.edu.application.dto.kg.SyncRecordDTO;
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
import com.ai.edu.domain.edukg.repository.KgChapterRepository;
import com.ai.edu.domain.edukg.repository.KgChapterSectionRepository;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.edukg.repository.KgSectionKPRepository;
import com.ai.edu.domain.edukg.repository.KgSectionRepository;
import com.ai.edu.domain.edukg.repository.KgSyncRecordRepository;
import com.ai.edu.domain.edukg.repository.KgTextbookChapterRepository;
import com.ai.edu.domain.edukg.repository.KgTextbookRepository;
import com.ai.edu.domain.edukg.service.KgRelationSyncService;
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
    private KgRelationSyncService relationSync;

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

        when(neo4jNodeRepository.findAllTextbooks()).thenReturn(textbooks);
        when(neo4jNodeRepository.findAllChapters()).thenReturn(chapters);
        when(neo4jNodeRepository.findAllSections()).thenReturn(sections);
        when(neo4jNodeRepository.findAllKnowledgePoints()).thenReturn(kps);
        when(relationSync.syncTextbookChapterRelations()).thenReturn(List.of());
        when(relationSync.syncChapterSectionRelations()).thenReturn(List.of());
        when(relationSync.syncSectionKPRelations()).thenReturn(List.of());
        when(kgTextbookRepository.upsert(anyList())).thenReturn(1);
        when(kgChapterRepository.upsert(anyList())).thenReturn(1);
        when(kgSectionRepository.upsert(anyList())).thenReturn(1);
        when(kgKnowledgePointRepository.upsert(anyList())).thenReturn(1);
        when(relationSync.rebuildTextbookChapterRelations(anyList())).thenReturn(0);
        when(relationSync.rebuildChapterSectionRelations(anyList())).thenReturn(0);
        when(relationSync.rebuildSectionKPRelations(anyList())).thenReturn(0);
        // Repository mocks for markDeletedNodes + reconcile
        when(kgTextbookRepository.findAllActive()).thenReturn(textbooks);
        when(kgChapterRepository.findAllActive()).thenReturn(List.of());
        when(kgChapterRepository.countActive()).thenReturn(1);
        when(kgSectionRepository.findAllActive()).thenReturn(List.of());
        when(kgSectionRepository.countActive()).thenReturn(1);
        when(kgKnowledgePointRepository.findAllActive()).thenReturn(List.of());
        when(kgKnowledgePointRepository.countActive()).thenReturn(1);
        when(kgTextbookChapterRepository.findAllActive()).thenReturn(List.of());
        when(kgChapterSectionRepository.findAllActive()).thenReturn(List.of());
        when(kgSectionKPRepository.findAllActive()).thenReturn(List.of());
    }

    private KgSyncRecord createSyncRecord(Long id) {
        KgSyncRecord record = KgSyncRecord.create("full", "math", 0L);
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
        assertEquals(1L, result.getSyncId());
        assertEquals("matched", result.getReconciliationStatus());

        verify(neo4jNodeRepository, atLeast(1)).findAllTextbooks();
        verify(neo4jNodeRepository, atLeast(1)).findAllChapters();
        verify(neo4jNodeRepository, atLeast(1)).findAllSections();
        verify(neo4jNodeRepository, atLeast(1)).findAllKnowledgePoints();
        verify(kgTextbookRepository).upsert(anyList());
        verify(kgChapterRepository).upsert(anyList());
        verify(kgSectionRepository).upsert(anyList());
        verify(kgKnowledgePointRepository).upsert(anyList());
        verify(relationSync).rebuildTextbookChapterRelations(anyList());
        verify(relationSync).rebuildChapterSectionRelations(anyList());
        verify(relationSync).rebuildSectionKPRelations(anyList());
        verify(kgSyncRecordRepository, atLeast(2)).save(any(KgSyncRecord.class));
    }

    // ==================== 6.7.2 syncFull 定向同步 ====================

    @Test
    @Order(2)
    @DisplayName("syncFull 定向同步 — 按 edition 过滤")
    void syncFull_targetedByEdition_shouldFilterByEdition() {
        String targetEdition = "人教版";
        SyncRequest request = SyncRequest.builder().subject("math").edition(targetEdition).build();

        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        List<KgTextbook> allTextbooks = List.of(
                KgTextbook.create("uri:tb1", "教材1", "七年级", "junior", targetEdition, "math"),
                KgTextbook.create("uri:tb2", "教材2", "八年级", "junior", "北师大版", "math")
        );
        when(neo4jNodeRepository.findAllTextbooks()).thenReturn(allTextbooks);
        when(neo4jNodeRepository.findAllChapters()).thenReturn(List.of());
        when(neo4jNodeRepository.findAllSections()).thenReturn(List.of());
        when(neo4jNodeRepository.findAllKnowledgePoints()).thenReturn(List.of());
        when(relationSync.syncTextbookChapterRelations()).thenReturn(List.of());
        when(relationSync.syncChapterSectionRelations()).thenReturn(List.of());
        when(relationSync.syncSectionKPRelations()).thenReturn(List.of());
        when(kgTextbookRepository.upsert(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(kgChapterRepository.upsert(anyList())).thenReturn(0);
        when(kgSectionRepository.upsert(anyList())).thenReturn(0);
        when(kgKnowledgePointRepository.upsert(anyList())).thenReturn(0);
        when(relationSync.rebuildTextbookChapterRelations(anyList())).thenReturn(0);
        when(relationSync.rebuildChapterSectionRelations(anyList())).thenReturn(0);
        when(relationSync.rebuildSectionKPRelations(anyList())).thenReturn(0);
        // Repository mocks for markDeletedNodes + reconcile
        when(kgTextbookRepository.findAllActive()).thenReturn(List.of());
        when(kgChapterRepository.findAllActive()).thenReturn(List.of());
        when(kgSectionRepository.findAllActive()).thenReturn(List.of());
        when(kgKnowledgePointRepository.findAllActive()).thenReturn(List.of());

        KgSyncRecord syncRecord = createSyncRecord(2L);
        when(kgSyncRecordRepository.save(any(KgSyncRecord.class))).thenReturn(syncRecord);
        when(kgSyncRecordRepository.findById(2L)).thenReturn(Optional.of(syncRecord));

        SyncResult result = kgSyncAppService.syncFull(request);

        // 应只 upsert 1 个教材（被过滤后的）
        verify(kgTextbookRepository).upsert(argThat(tbs ->
                tbs.size() == 1 && ((KgTextbook) tbs.get(0)).getEdition().equals(targetEdition)
        ));
        assertEquals("success", result.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("syncFull 定向同步 — 按 subject 过滤")
    void syncFull_targetedBySubject_shouldFilterBySubject() {
        SyncRequest request = SyncRequest.builder().subject("english").edition("人教版").build();

        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:math", "数学教材", "七年级", "junior", "人教版", "math"),
                KgTextbook.create("uri:eng", "英语教材", "七年级", "junior", "人教版", "english")
        );
        when(neo4jNodeRepository.findAllTextbooks()).thenReturn(textbooks);
        when(neo4jNodeRepository.findAllChapters()).thenReturn(List.of());
        when(neo4jNodeRepository.findAllSections()).thenReturn(List.of());
        when(neo4jNodeRepository.findAllKnowledgePoints()).thenReturn(List.of());
        when(relationSync.syncTextbookChapterRelations()).thenReturn(List.of());
        when(relationSync.syncChapterSectionRelations()).thenReturn(List.of());
        when(relationSync.syncSectionKPRelations()).thenReturn(List.of());
        when(kgTextbookRepository.upsert(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(kgChapterRepository.upsert(anyList())).thenReturn(0);
        when(kgSectionRepository.upsert(anyList())).thenReturn(0);
        when(kgKnowledgePointRepository.upsert(anyList())).thenReturn(0);
        when(relationSync.rebuildTextbookChapterRelations(anyList())).thenReturn(0);
        when(relationSync.rebuildChapterSectionRelations(anyList())).thenReturn(0);
        when(relationSync.rebuildSectionKPRelations(anyList())).thenReturn(0);
        // Repository mocks for markDeletedNodes + reconcile
        when(kgTextbookRepository.findAllActive()).thenReturn(List.of());
        when(kgChapterRepository.findAllActive()).thenReturn(List.of());
        when(kgSectionRepository.findAllActive()).thenReturn(List.of());
        when(kgKnowledgePointRepository.findAllActive()).thenReturn(List.of());

        KgSyncRecord syncRecord = createSyncRecord(3L);
        when(kgSyncRecordRepository.save(any(KgSyncRecord.class))).thenReturn(syncRecord);
        when(kgSyncRecordRepository.findById(3L)).thenReturn(Optional.of(syncRecord));

        SyncResult result = kgSyncAppService.syncFull(request);

        verify(kgTextbookRepository).upsert(argThat(tbs ->
                tbs.size() == 1 && ((KgTextbook) tbs.get(0)).getSubject().equals("english")
        ));
        assertEquals("success", result.getStatus());
    }

    // ==================== 6.7.3 syncFull 同步锁 ====================

    @Test
    @Order(4)
    @DisplayName("syncFull 手动触发同步锁 — 连续调用应抛异常")
    void syncFull_concurrentCalls_shouldThrowOnSecondCall() {
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncFull(SyncRequest.builder().subject("math").edition("人教版").build())
        );
        assertEquals("70006", ex.getCode());
        assertTrue(ex.getMessage().contains("已有同步任务正在执行"));
    }

    // ==================== 6.7.4 syncFull 异常回滚 ====================

    @Test
    @Order(5)
    @DisplayName("syncFull 异常回滚 — NodeSync 抛异常应传播 BusinessException")
    void syncFull_nodeSyncThrows_shouldThrowBusinessException() {
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(neo4jNodeRepository.findAllTextbooks())
                .thenThrow(new RuntimeException("Neo4j connection timeout"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncFull(SyncRequest.builder().subject("math").edition("人教版").build())
        );
        assertTrue(ex.getMessage().contains("Neo4j connection timeout"));

        // 验证锁被释放
        verify(redisService).unlock(anyString(), anyString());
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
        KgSyncRecord record = KgSyncRecord.create("full", "math", 0L);
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
        KgSyncRecord record1 = KgSyncRecord.create("full", "math", 0L);
        record1.completeSuccess(10, 5, 0, "matched", "ok");
        setSyncRecordId(record1, 1L);

        KgSyncRecord record2 = KgSyncRecord.create("full", "english", 0L);
        record2.completeSuccess(8, 2, 1, "matched", "ok");
        setSyncRecordId(record2, 2L);

        when(kgSyncRecordRepository.findRecent(5)).thenReturn(List.of(record1, record2));

        List<SyncRecordDTO> result = kgSyncAppService.getSyncRecords(1, 5);

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
        when(kgSyncRecordRepository.findRecent(10)).thenReturn(List.of());

        List<SyncRecordDTO> result = kgSyncAppService.getSyncRecords(1, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== syncFull request=null 参数校验 ====================

    @Test
    @Order(10)
    @DisplayName("syncFull request 为 null 应抛 KG_SYNC_PARAM_ERROR")
    void syncFull_nullRequest_shouldThrowParamError() {
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

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
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
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
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
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
        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:tb1", "教材1", "七年级", "junior", "人教版", "math"),
                KgTextbook.create("uri:tb2", "教材2", "八年级", "junior", "北师大版", "math")
        );
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(neo4jNodeRepository.findAllTextbooks()).thenReturn(textbooks);
        when(kgTextbookRepository.upsert(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());

        KgSyncRecord syncRecord = createSyncRecord(20L);
        when(kgSyncRecordRepository.save(any(KgSyncRecord.class))).thenReturn(syncRecord);
        when(kgSyncRecordRepository.findById(20L)).thenReturn(Optional.of(syncRecord));

        SyncRequest request = SyncRequest.builder().subject("math").edition("人教版").build();
        SyncResult result = kgSyncAppService.syncTextbooksOnly(request);

        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals(20L, result.getSyncId());
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
        when(neo4jNodeRepository.findAllTextbooks())
                .thenThrow(new RuntimeException("Neo4j connection timeout"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncTextbooksOnly(SyncRequest.builder().subject("math").edition("人教版").build())
        );
        assertTrue(ex.getMessage().contains("Neo4j connection timeout"));

        verify(kgSyncRecordRepository, times(2)).save(any(KgSyncRecord.class));

        // 验证锁被释放
        verify(redisService).unlock(anyString(), anyString());
    }
}
