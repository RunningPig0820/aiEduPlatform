package com.ai.edu.application.service;

import com.ai.edu.application.assembler.KgConvert;
import com.ai.edu.application.dto.kg.SyncRecordDTO;
import com.ai.edu.application.dto.kg.SyncRequest;
import com.ai.edu.application.dto.kg.SyncResult;
import com.ai.edu.application.dto.kg.SyncStatusDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.service.KgSyncDomainService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 知识图谱同步应用服务
 * 职责：编排同步流程，处理同步锁、同步记录、参数校验
 */
@Slf4j
@Service
public class KgSyncAppService {

    @Resource
    private KgSyncDomainService kgSyncDomainService;

    private final AtomicBoolean syncing = new AtomicBoolean(false);

    /**
     * 全量/定向同步
     *
     * @param request 同步请求（可选参数：subject/phase/grade/textbookUri）
     * @return 同步结果
     */
    @Transactional("kg")
    public SyncResult syncFull(SyncRequest request) {
        if (request == null) {
            request = SyncRequest.builder().subject("math").build();
        }

        // 同步锁检查（原子操作）
        if (!syncing.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.KG_SYNC_IN_PROGRESS, "已有同步任务正在执行，请稍后重试");
        }

        // 参数校验
        validateSyncRequest(request);

        long startTime = System.currentTimeMillis();
        log.info("Starting KG sync: subject={}, phase={}, grade={}, textbookUri={}",
                request.getSubject(), request.getPhase(), request.getGrade(), request.getTextbookUri());

        KgSyncRecord syncRecord = null;
        try {
            // 构建同步范围
            String scope = buildScope(request);

            // 创建同步记录
            syncRecord = kgSyncDomainService.createSyncRecord("full", scope, 0L);

            // 执行同步
            SyncExecutionResult syncResult = executeSync(request);

            // 状态变更：标记 MySQL 中有但 Neo4j 中无的节点为 deleted
            int statusChangedCount = markDeletedNodes();

            // 对账校验
            KgSyncDomainService.ReconciliationResult reconciliation = reconcile();

            // 完成同步记录
            kgSyncDomainService.completeSyncRecord(
                    syncRecord.getId(),
                    syncResult.insertedCount,
                    syncResult.updatedCount,
                    statusChangedCount,
                    reconciliation.matched ? "matched" : "mismatched",
                    reconciliation.differences.isEmpty() ? "All counts matched"
                            : String.join("; ", reconciliation.differences)
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("Sync completed in {}ms: inserted={}, updated={}, deleted={}, reconciled={}",
                    duration, syncResult.insertedCount, syncResult.updatedCount, statusChangedCount,
                    reconciliation.matched ? "matched" : "mismatched");

            return KgConvert.toSyncResult(
                    syncRecord.getId(),
                    "success",
                    syncResult.insertedCount,
                    syncResult.updatedCount,
                    statusChangedCount,
                    reconciliation.matched ? "matched" : "mismatched",
                    duration
            );
        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Sync failed after {}ms: {}", duration, e.getMessage(), e);
            if (syncRecord != null) {
                kgSyncDomainService.failSyncRecord(syncRecord.getId(), e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Sync failed after {}ms: {}", duration, e.getMessage(), e);
            if (syncRecord != null) {
                kgSyncDomainService.failSyncRecord(syncRecord.getId(), e.getMessage());
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "同步失败: " + e.getMessage());
        } finally {
            syncing.set(false);
        }
    }

    /**
     * 查询同步状态
     */
    public SyncStatusDTO getSyncStatus() {
        KgSyncRecord latest = kgSyncDomainService.getLatestSyncRecord();
        return KgConvert.toSyncStatusDTO(latest);
    }

    /**
     * 查询同步历史记录
     */
    public List<SyncRecordDTO> getSyncRecords(int page, int size) {
        List<KgSyncRecord> records = kgSyncDomainService.getSyncRecords(size);
        return KgConvert.toSyncRecordDTOs(records);
    }

    // ==================== 私有方法 ====================

    private void validateSyncRequest(SyncRequest request) {
        if (request.getSubject() != null && request.getSubject().isBlank()) {
            throw new BusinessException(ErrorCode.KG_SYNC_PARAM_ERROR, "学科不能为空字符串");
        }
    }

    private String buildScope(SyncRequest request) {
        return String.format(
                "{\"subject\":\"%s\",\"phase\":\"%s\",\"grade\":\"%s\",\"textbookUri\":\"%s\"}",
                request.getSubject() != null ? request.getSubject() : "math",
                request.getPhase() != null ? request.getPhase() : "",
                request.getGrade() != null ? request.getGrade() : "",
                request.getTextbookUri() != null ? request.getTextbookUri() : ""
        );
    }

    private SyncExecutionResult executeSync(SyncRequest request) {
        int totalInserted = 0;
        int totalUpdated = 0;

        // 1. 同步教材节点
        List<KgTextbook> textbooks = kgSyncDomainService.syncTextbookNodes();
        if (request.getTextbookUri() != null && !request.getTextbookUri().isBlank()) {
            textbooks = textbooks.stream()
                    .filter(tb -> request.getTextbookUri().equals(tb.getUri()))
                    .toList();
        }
        if (request.getSubject() != null) {
            textbooks = textbooks.stream()
                    .filter(tb -> request.getSubject().equals(tb.getSubject()))
                    .toList();
        }
        if (request.getGrade() != null) {
            textbooks = textbooks.stream()
                    .filter(tb -> request.getGrade().equals(tb.getGrade()))
                    .toList();
        }

        Set<String> textbookUris = textbooks.stream()
                .map(KgTextbook::getUri)
                .collect(Collectors.toSet());
        totalInserted += kgSyncDomainService.upsertTextbooks(textbooks);

        // 2. 同步章节节点
        List<KgChapter> chapters = kgSyncDomainService.syncChapterNodes();
        int chapterInserts = kgSyncDomainService.upsertChapters(chapters);
        // TODO: domain service upsert methods currently return total count (insert+update)
        // For now, treat all as inserted. Future: return separate insert/update counts.
        totalInserted += chapterInserts;
        Set<String> chapterUris = chapters.stream()
                .map(KgChapter::getUri)
                .collect(Collectors.toSet());

        // 3. 同步小节节点
        List<KgSection> sections = kgSyncDomainService.syncSectionNodes();
        int sectionInserts = kgSyncDomainService.upsertSections(sections);
        totalInserted += sectionInserts;
        Set<String> sectionUris = sections.stream()
                .map(KgSection::getUri)
                .collect(Collectors.toSet());

        // 4. 同步知识点节点
        List<KgKnowledgePoint> kps = kgSyncDomainService.syncKnowledgePointNodes();
        int kpInserts = kgSyncDomainService.upsertKnowledgePoints(kps);
        totalInserted += kpInserts;
        Set<String> kpUris = kps.stream()
                .map(KgKnowledgePoint::getUri)
                .collect(Collectors.toSet());

        // 5. URI 校验
        KgSyncDomainService.UriValidationResult uriValidation = kgSyncDomainService.validateAllUris(textbooks, chapters, sections, kps);
        if (!uriValidation.valid) {
            log.warn("URI validation warnings: {}", uriValidation.errors);
        }

        // 6. 同步层级关联
        List<KgTextbookChapter> tbChRelations = kgSyncDomainService.syncTextbookChapterRelations();
        totalInserted += kgSyncDomainService.rebuildTextbookChapterRelations(tbChRelations);

        List<KgChapterSection> chSecRelations = kgSyncDomainService.syncChapterSectionRelations();
        totalInserted += kgSyncDomainService.rebuildChapterSectionRelations(chSecRelations);

        List<KgSectionKP> secKpRelations = kgSyncDomainService.syncSectionKPRelations();
        totalInserted += kgSyncDomainService.rebuildSectionKPRelations(secKpRelations);

        return new SyncExecutionResult(totalInserted, totalUpdated);
    }

    private int markDeletedNodes() {
        int totalChanged = 0;

        Set<String> neo4jTextbookUris = kgSyncDomainService.syncTextbookNodes().stream()
                .map(KgTextbook::getUri)
                .collect(Collectors.toSet());
        totalChanged += kgSyncDomainService.markDeletedNodes("Textbook", neo4jTextbookUris);

        Set<String> neo4jChapterUris = kgSyncDomainService.syncChapterNodes().stream()
                .map(KgChapter::getUri)
                .collect(Collectors.toSet());
        totalChanged += kgSyncDomainService.markDeletedNodes("Chapter", neo4jChapterUris);

        Set<String> neo4jSectionUris = kgSyncDomainService.syncSectionNodes().stream()
                .map(KgSection::getUri)
                .collect(Collectors.toSet());
        totalChanged += kgSyncDomainService.markDeletedNodes("Section", neo4jSectionUris);

        Set<String> neo4jKpUris = kgSyncDomainService.syncKnowledgePointNodes().stream()
                .map(KgKnowledgePoint::getUri)
                .collect(Collectors.toSet());
        totalChanged += kgSyncDomainService.markDeletedNodes("KnowledgePoint", neo4jKpUris);

        return totalChanged;
    }

    private KgSyncDomainService.ReconciliationResult reconcile() {
        Set<String> neo4jTextbookUris = kgSyncDomainService.syncTextbookNodes().stream()
                .map(KgTextbook::getUri)
                .collect(Collectors.toSet());
        Set<String> neo4jChapterUris = kgSyncDomainService.syncChapterNodes().stream()
                .map(KgChapter::getUri)
                .collect(Collectors.toSet());
        Set<String> neo4jSectionUris = kgSyncDomainService.syncSectionNodes().stream()
                .map(KgSection::getUri)
                .collect(Collectors.toSet());
        Set<String> neo4jKpUris = kgSyncDomainService.syncKnowledgePointNodes().stream()
                .map(KgKnowledgePoint::getUri)
                .collect(Collectors.toSet());

        List<KgTextbookChapter> tbChRelations = kgSyncDomainService.syncTextbookChapterRelations();
        List<KgChapterSection> chSecRelations = kgSyncDomainService.syncChapterSectionRelations();
        List<KgSectionKP> secKpRelations = kgSyncDomainService.syncSectionKPRelations();

        return kgSyncDomainService.reconcile(
                neo4jTextbookUris, neo4jChapterUris, neo4jSectionUris, neo4jKpUris,
                tbChRelations, chSecRelations, secKpRelations
        );
    }

    /**
     * 同步执行结果
     */
    private record SyncExecutionResult(int insertedCount, int updatedCount) {
    }
}
