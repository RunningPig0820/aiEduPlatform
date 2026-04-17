package com.ai.edu.application.service;

import com.ai.edu.application.dto.kg.SyncRecordDTO;
import com.ai.edu.application.dto.kg.SyncRequest;
import com.ai.edu.application.dto.kg.SyncResult;
import com.ai.edu.application.dto.kg.SyncStatusDTO;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.service.KgSyncDomainService;
import com.ai.edu.domain.edukg.service.KgSyncDomainService.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KgSyncAppService 单元测试
 *
 * 测试目标：Mock 领域服务，验证同步流程编排、状态管理
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class KgSyncAppServiceTest {

    @Mock
    private KgSyncDomainService kgSyncDomainService;

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

    private ReconciliationResult createReconcileResult(boolean matched) {
        return new ReconciliationResult(matched,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of());
    }

    private void mockFullSyncChain() {
        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:tb1", "教材1", "七年级", "junior", "math")
        );
        List<KgChapter> chapters = List.of(KgChapter.create("uri:ch1", "第一章"));
        List<KgSection> sections = List.of(KgSection.create("uri:sec1", "第一节"));
        List<KgKnowledgePoint> kps = List.of(
                KgKnowledgePoint.create("uri:kp1", "知识点1")
        );

        when(kgSyncDomainService.syncTextbookNodes()).thenReturn(textbooks);
        when(kgSyncDomainService.syncChapterNodes()).thenReturn(chapters);
        when(kgSyncDomainService.syncSectionNodes()).thenReturn(sections);
        when(kgSyncDomainService.syncKnowledgePointNodes()).thenReturn(kps);
        when(kgSyncDomainService.syncTextbookChapterRelations()).thenReturn(List.of());
        when(kgSyncDomainService.syncChapterSectionRelations()).thenReturn(List.of());
        when(kgSyncDomainService.syncSectionKPRelations()).thenReturn(List.of());
        when(kgSyncDomainService.upsertTextbooks(anyList())).thenReturn(1);
        when(kgSyncDomainService.upsertChapters(anyList())).thenReturn(1);
        when(kgSyncDomainService.upsertSections(anyList())).thenReturn(1);
        when(kgSyncDomainService.upsertKnowledgePoints(anyList())).thenReturn(1);
        when(kgSyncDomainService.rebuildTextbookChapterRelations(anyList())).thenReturn(0);
        when(kgSyncDomainService.rebuildChapterSectionRelations(anyList())).thenReturn(0);
        when(kgSyncDomainService.rebuildSectionKPRelations(anyList())).thenReturn(0);
        when(kgSyncDomainService.validateAllUris(anyList(), anyList(), anyList(), anyList()))
                .thenReturn(new UriValidationResult(true, List.of()));
        when(kgSyncDomainService.markDeletedNodes(anyString(), anySet())).thenReturn(0);
        when(kgSyncDomainService.reconcile(anySet(), anySet(), anySet(), anySet(), anyList(), anyList(), anyList()))
                .thenReturn(createReconcileResult(true));
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
        SyncRequest request = SyncRequest.builder().subject("math").build();
        KgSyncRecord syncRecord = createSyncRecord(1L);
        when(kgSyncDomainService.createSyncRecord(anyString(), anyString(), anyLong())).thenReturn(syncRecord);
        mockFullSyncChain();

        SyncResult result = kgSyncAppService.syncFull(request);

        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals(1L, result.getSyncId());
        assertEquals("matched", result.getReconciliationStatus());

        verify(kgSyncDomainService, atLeast(1)).syncTextbookNodes();
        verify(kgSyncDomainService, atLeast(1)).syncChapterNodes();
        verify(kgSyncDomainService, atLeast(1)).syncSectionNodes();
        verify(kgSyncDomainService, atLeast(1)).syncKnowledgePointNodes();
        verify(kgSyncDomainService).upsertTextbooks(anyList());
        verify(kgSyncDomainService).upsertChapters(anyList());
        verify(kgSyncDomainService).upsertSections(anyList());
        verify(kgSyncDomainService).upsertKnowledgePoints(anyList());
        verify(kgSyncDomainService).rebuildTextbookChapterRelations(anyList());
        verify(kgSyncDomainService).rebuildChapterSectionRelations(anyList());
        verify(kgSyncDomainService).rebuildSectionKPRelations(anyList());
        verify(kgSyncDomainService, atLeast(1)).markDeletedNodes(anyString(), anySet());
        verify(kgSyncDomainService, atLeast(1)).reconcile(anySet(), anySet(), anySet(), anySet(), anyList(), anyList(), anyList());
        verify(kgSyncDomainService).completeSyncRecord(anyLong(), anyInt(), anyInt(), anyInt(), anyString(), anyString());
    }

    // ==================== 6.7.2 syncFull 定向同步 ====================

    @Test
    @Order(2)
    @DisplayName("syncFull 定向同步 — 按 textbookUri 过滤")
    void syncFull_targetedByUri_shouldFilterByTextbookUri() {
        String targetUri = "uri:tb1";
        SyncRequest request = SyncRequest.builder().subject("math").textbookUri(targetUri).build();

        List<KgTextbook> allTextbooks = List.of(
                KgTextbook.create(targetUri, "教材1", "七年级", "junior", "math"),
                KgTextbook.create("uri:tb2", "教材2", "八年级", "junior", "math")
        );
        when(kgSyncDomainService.syncTextbookNodes()).thenReturn(allTextbooks);
        when(kgSyncDomainService.syncChapterNodes()).thenReturn(List.of());
        when(kgSyncDomainService.syncSectionNodes()).thenReturn(List.of());
        when(kgSyncDomainService.syncKnowledgePointNodes()).thenReturn(List.of());
        when(kgSyncDomainService.syncTextbookChapterRelations()).thenReturn(List.of());
        when(kgSyncDomainService.syncChapterSectionRelations()).thenReturn(List.of());
        when(kgSyncDomainService.syncSectionKPRelations()).thenReturn(List.of());
        when(kgSyncDomainService.upsertTextbooks(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(kgSyncDomainService.upsertChapters(anyList())).thenReturn(0);
        when(kgSyncDomainService.upsertSections(anyList())).thenReturn(0);
        when(kgSyncDomainService.upsertKnowledgePoints(anyList())).thenReturn(0);
        when(kgSyncDomainService.rebuildTextbookChapterRelations(anyList())).thenReturn(0);
        when(kgSyncDomainService.rebuildChapterSectionRelations(anyList())).thenReturn(0);
        when(kgSyncDomainService.rebuildSectionKPRelations(anyList())).thenReturn(0);
        when(kgSyncDomainService.validateAllUris(anyList(), anyList(), anyList(), anyList()))
                .thenReturn(new UriValidationResult(true, List.of()));
        when(kgSyncDomainService.markDeletedNodes(anyString(), anySet())).thenReturn(0);
        when(kgSyncDomainService.reconcile(anySet(), anySet(), anySet(), anySet(), anyList(), anyList(), anyList()))
                .thenReturn(createReconcileResult(true));

        KgSyncRecord syncRecord = createSyncRecord(2L);
        when(kgSyncDomainService.createSyncRecord(anyString(), anyString(), anyLong())).thenReturn(syncRecord);

        SyncResult result = kgSyncAppService.syncFull(request);

        // 应只 upsert 1 个教材（被过滤后的）
        verify(kgSyncDomainService).upsertTextbooks(argThat(tbs ->
                tbs.size() == 1 && ((KgTextbook) tbs.get(0)).getUri().equals(targetUri)
        ));
        assertEquals("success", result.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("syncFull 定向同步 — 按 subject 过滤")
    void syncFull_targetedBySubject_shouldFilterBySubject() {
        SyncRequest request = SyncRequest.builder().subject("english").build();

        List<KgTextbook> textbooks = List.of(
                KgTextbook.create("uri:math", "数学教材", "七年级", "junior", "math"),
                KgTextbook.create("uri:eng", "英语教材", "七年级", "junior", "english")
        );
        when(kgSyncDomainService.syncTextbookNodes()).thenReturn(textbooks);
        when(kgSyncDomainService.syncChapterNodes()).thenReturn(List.of());
        when(kgSyncDomainService.syncSectionNodes()).thenReturn(List.of());
        when(kgSyncDomainService.syncKnowledgePointNodes()).thenReturn(List.of());
        when(kgSyncDomainService.syncTextbookChapterRelations()).thenReturn(List.of());
        when(kgSyncDomainService.syncChapterSectionRelations()).thenReturn(List.of());
        when(kgSyncDomainService.syncSectionKPRelations()).thenReturn(List.of());
        when(kgSyncDomainService.upsertTextbooks(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(kgSyncDomainService.upsertChapters(anyList())).thenReturn(0);
        when(kgSyncDomainService.upsertSections(anyList())).thenReturn(0);
        when(kgSyncDomainService.upsertKnowledgePoints(anyList())).thenReturn(0);
        when(kgSyncDomainService.rebuildTextbookChapterRelations(anyList())).thenReturn(0);
        when(kgSyncDomainService.rebuildChapterSectionRelations(anyList())).thenReturn(0);
        when(kgSyncDomainService.rebuildSectionKPRelations(anyList())).thenReturn(0);
        when(kgSyncDomainService.validateAllUris(anyList(), anyList(), anyList(), anyList()))
                .thenReturn(new UriValidationResult(true, List.of()));
        when(kgSyncDomainService.markDeletedNodes(anyString(), anySet())).thenReturn(0);
        when(kgSyncDomainService.reconcile(anySet(), anySet(), anySet(), anySet(), anyList(), anyList(), anyList()))
                .thenReturn(createReconcileResult(true));

        KgSyncRecord syncRecord = createSyncRecord(3L);
        when(kgSyncDomainService.createSyncRecord(anyString(), anyString(), anyLong())).thenReturn(syncRecord);

        SyncResult result = kgSyncAppService.syncFull(request);

        verify(kgSyncDomainService).upsertTextbooks(argThat(tbs ->
                tbs.size() == 1 && ((KgTextbook) tbs.get(0)).getSubject().equals("english")
        ));
        assertEquals("success", result.getStatus());
    }

    // ==================== 6.7.3 syncFull 同步锁 ====================

    @Test
    @Order(4)
    @DisplayName("syncFull 手动触发同步锁 — 连续调用应抛异常")
    void syncFull_concurrentCalls_shouldThrowOnSecondCall() {
        try {
            var field = KgSyncAppService.class.getDeclaredField("syncing");
            field.setAccessible(true);
            field.setBoolean(kgSyncAppService, true);
        } catch (Exception e) {
            fail("Failed to set syncing field: " + e.getMessage());
        }

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncFull(SyncRequest.builder().subject("math").build())
        );
        assertEquals("70006", ex.getCode());
        assertTrue(ex.getMessage().contains("已有同步任务正在执行"));
    }

    // ==================== 6.7.4 syncFull 异常回滚 ====================

    @Test
    @Order(5)
    @DisplayName("syncFull 异常回滚 — DomainService 抛异常应传播 BusinessException")
    void syncFull_domainServiceThrows_shouldThrowBusinessException() {
        when(kgSyncDomainService.syncTextbookNodes())
                .thenThrow(new RuntimeException("Neo4j connection timeout"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncFull(SyncRequest.builder().subject("math").build())
        );
        assertTrue(ex.getMessage().contains("Neo4j connection timeout"));

        // 验证 syncing 被重置
        try {
            var field = KgSyncAppService.class.getDeclaredField("syncing");
            field.setAccessible(true);
            assertFalse((boolean) field.get(kgSyncAppService), "syncing 应在 finally 中重置为 false");
        } catch (Exception e) {
            fail("Failed to check syncing field: " + e.getMessage());
        }
    }

    // ==================== 6.7.5 getSyncStatus ====================

    @Test
    @Order(6)
    @DisplayName("getSyncStatus 从未同步过应返回 never_synced")
    void getSyncStatus_neverSynced_shouldReturnNeverSynced() {
        when(kgSyncDomainService.getLatestSyncRecord()).thenReturn(null);

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

        when(kgSyncDomainService.getLatestSyncRecord()).thenReturn(record);

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

        when(kgSyncDomainService.getSyncRecords(5)).thenReturn(List.of(record1, record2));

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
        when(kgSyncDomainService.getSyncRecords(10)).thenReturn(List.of());

        List<SyncRecordDTO> result = kgSyncAppService.getSyncRecords(1, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== syncFull request=null 默认值 ====================

    @Test
    @Order(10)
    @DisplayName("syncFull request 为 null 应使用默认值 subject=math")
    void syncFull_nullRequest_shouldUseDefaults() {
        mockFullSyncChain();
        KgSyncRecord syncRecord = createSyncRecord(10L);
        when(kgSyncDomainService.createSyncRecord(anyString(), anyString(), anyLong())).thenReturn(syncRecord);

        SyncResult result = kgSyncAppService.syncFull(null);

        assertNotNull(result);
        assertEquals("success", result.getStatus());
    }

    // ==================== syncFull 参数校验 ====================

    @Test
    @Order(11)
    @DisplayName("syncFull subject 为空字符串应抛 KG_SYNC_PARAM_ERROR")
    void syncFull_blankSubject_shouldThrowParamError() {
        SyncRequest request = SyncRequest.builder().subject("  ").build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgSyncAppService.syncFull(request)
        );
        assertEquals("70007", ex.getCode());
        assertTrue(ex.getMessage().contains("学科不能为空"));
    }
}
