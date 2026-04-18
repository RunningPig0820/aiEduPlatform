package com.ai.edu.application.service;

import com.ai.edu.application.assembler.KgConvert;
import com.ai.edu.application.dto.kg.SyncRecordDTO;
import com.ai.edu.application.dto.kg.SyncRequest;
import com.ai.edu.application.dto.kg.SyncResult;
import com.ai.edu.application.dto.kg.SyncStatusDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.dto.kg.ReconciliationResult;
import com.ai.edu.common.dto.kg.UriValidationResult;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.repository.KgChapterRepository;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.edukg.repository.KgSectionRepository;
import com.ai.edu.domain.edukg.repository.KgTextbookRepository;
import com.ai.edu.domain.edukg.service.KgNodeSyncService;
import com.ai.edu.domain.edukg.service.KgRelationSyncService;
import com.ai.edu.domain.edukg.service.KgSyncRecordService;
import com.ai.edu.domain.shared.service.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 知识图谱同步应用服务
 * 职责：编排同步流程，处理同步锁、同步记录、参数校验
 */
@Slf4j
@Service
public class KgSyncAppService {

    @Resource
    @Qualifier("neo4jNodeSyncService")
    private KgNodeSyncService nodeSync;

    @Resource
    @Qualifier("neo4jRelationSyncService")
    private KgRelationSyncService relationSync;

    @Resource
    @Qualifier("neo4jSyncRecordService")
    private KgSyncRecordService recordService;

    @Resource
    private KgTextbookRepository kgTextbookRepository;

    @Resource
    private KgChapterRepository kgChapterRepository;

    @Resource
    private KgSectionRepository kgSectionRepository;

    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    @Resource
    private RedisService redisService;

    private static final String KG_SYNC_LOCK_KEY = "ai-edu:kg:sync:lock";
    private static final long KG_SYNC_LOCK_TIMEOUT_SECONDS = 600;

    /**
     * 全量/定向同步
     *
     * @param request 同步请求（必传参数：subject + edition）
     * @return 同步结果
     */
    @Transactional("kg")
    public SyncResult syncFull(SyncRequest request) {
        // 同步锁检查（Redis 分布式锁）
        String lockValue = UUID.randomUUID().toString();
        if (!redisService.tryLock(KG_SYNC_LOCK_KEY, lockValue, KG_SYNC_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new BusinessException(ErrorCode.KG_SYNC_IN_PROGRESS, "已有同步任务正在执行，请稍后重试");
        }

        // 参数校验
        validateSyncRequest(request);

        long startTime = System.currentTimeMillis();
        log.info("Starting KG sync: subject={}, edition={}",
                request.getSubject(), request.getEdition());

        KgSyncRecord syncRecord = null;
        try {
            // 构建同步范围
            String scope = buildScope(request);

            // 创建同步记录
            syncRecord = recordService.createSyncRecord("full", scope, 0L);

            // 执行同步
            SyncExecutionResult syncResult = executeSync(request);

            // 状态变更：标记 MySQL 中有但 Neo4j 中无的节点为 deleted
            int statusChangedCount = markDeletedNodes();

            // 对账校验
            ReconciliationResult reconciliation = reconcile();

            // 完成同步记录
            recordService.completeSyncRecord(
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
                recordService.failSyncRecord(syncRecord.getId(), e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Sync failed after {}ms: {}", duration, e.getMessage(), e);
            if (syncRecord != null) {
                recordService.failSyncRecord(syncRecord.getId(), e.getMessage());
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "同步失败: " + e.getMessage());
        } finally {
            redisService.unlock(KG_SYNC_LOCK_KEY, lockValue);
        }
    }

    /**
     * 单独同步教材节点（不同步章节/小节/知识点）
     *
     * @param request 同步过滤条件（必传：subject + edition）
     * @return 同步结果
     */
    public SyncResult syncTextbooksOnly(SyncRequest request) {
        // 同步锁检查（Redis 分布式锁）
        String lockValue = UUID.randomUUID().toString();
        if (!redisService.tryLock(KG_SYNC_LOCK_KEY, lockValue, KG_SYNC_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new BusinessException(ErrorCode.KG_SYNC_IN_PROGRESS, "已有同步任务正在执行，请稍后重试");
        }

        // 参数校验
        validateSyncRequest(request);

        long startTime = System.currentTimeMillis();
        log.info("Starting textbook-only sync: subject={}, edition={}",
                request.getSubject(), request.getEdition());

        KgSyncRecord syncRecord = null;
        try {
            // 创建同步记录
            String scope = buildScope(request);
            syncRecord = recordService.createSyncRecord("textbook", scope, 0L);

            // 从 Neo4j 读取教材节点
            List<KgTextbook> textbooks = nodeSync.syncTextbookNodes();

            // 按 edition 过滤
            textbooks = textbooks.stream()
                    .filter(tb -> request.getEdition().equals(tb.getEdition()))
                    .toList();

            // 写入 MySQL（按 uri 去重：insert or update）
            int insertedCount = kgTextbookRepository.upsert(textbooks);

            // 完成同步记录
            recordService.completeSyncRecord(
                    syncRecord.getId(),
                    insertedCount,
                    0,
                    0,
                    "skipped",
                    "Textbook-only sync, reconciliation skipped"
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("Textbook sync completed in {}ms: inserted/updated={}", duration, insertedCount);

            return KgConvert.toSyncResult(
                    syncRecord.getId(),
                    "success",
                    insertedCount,
                    0,
                    0,
                    "skipped",
                    duration
            );
        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Textbook sync failed after {}ms: {}", duration, e.getMessage(), e);
            if (syncRecord != null) {
                recordService.failSyncRecord(syncRecord.getId(), e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Textbook sync failed after {}ms: {}", duration, e.getMessage(), e);
            if (syncRecord != null) {
                recordService.failSyncRecord(syncRecord.getId(), e.getMessage());
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "教材同步失败: " + e.getMessage());
        } finally {
            redisService.unlock(KG_SYNC_LOCK_KEY, lockValue);
        }
    }

    /**
     * 查询同步状态
     */
    public SyncStatusDTO getSyncStatus() {
        KgSyncRecord latest = recordService.getLatestSyncRecord();
        return KgConvert.toSyncStatusDTO(latest);
    }

    /**
     * 查询同步历史记录
     */
    public List<SyncRecordDTO> getSyncRecords(int page, int size) {
        List<KgSyncRecord> records = recordService.getSyncRecords(size);
        return KgConvert.toSyncRecordDTOs(records);
    }

    // ==================== 私有方法 ====================

    private void validateSyncRequest(SyncRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.KG_SYNC_PARAM_ERROR, "同步参数不能为空");
        }
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new BusinessException(ErrorCode.KG_SYNC_PARAM_ERROR, "学科不能为空");
        }
        if (request.getEdition() == null || request.getEdition().isBlank()) {
            throw new BusinessException(ErrorCode.KG_SYNC_PARAM_ERROR, "版本不能为空");
        }
    }

    private String buildScope(SyncRequest request) {
        return String.format(
                "{\"subject\":\"%s\",\"stage\":\"%s\",\"grade\":\"%s\",\"edition\":\"%s\"}",
                request.getSubject() != null ? request.getSubject() : "",
                request.getStage() != null ? request.getStage() : "",
                request.getGrade() != null ? request.getGrade() : "",
                request.getEdition() != null ? request.getEdition() : ""
        );
    }

    private SyncExecutionResult executeSync(SyncRequest request) {
        int totalInserted = 0;
        int totalUpdated = 0;

        // 1. 同步教材节点
        List<KgTextbook> textbooks = nodeSync.syncTextbookNodes();
        // 按 edition / subject / grade 过滤
        if (request.getEdition() != null && !request.getEdition().isBlank()) {
            textbooks = textbooks.stream()
                    .filter(tb -> request.getEdition().equals(tb.getEdition()))
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
        totalInserted += kgTextbookRepository.upsert(textbooks);

        // 2. 同步章节节点
        List<KgChapter> chapters = nodeSync.syncChapterNodes();
        int chapterInserts = kgChapterRepository.upsert(chapters);
        // TODO: domain service upsert methods currently return total count (insert+update)
        // For now, treat all as inserted. Future: return separate insert/update counts.
        totalInserted += chapterInserts;
        Set<String> chapterUris = chapters.stream()
                .map(KgChapter::getUri)
                .collect(Collectors.toSet());

        // 3. 同步小节节点
        List<KgSection> sections = nodeSync.syncSectionNodes();
        int sectionInserts = kgSectionRepository.upsert(sections);
        totalInserted += sectionInserts;
        Set<String> sectionUris = sections.stream()
                .map(KgSection::getUri)
                .collect(Collectors.toSet());

        // 4. 同步知识点节点
        List<KgKnowledgePoint> kps = nodeSync.syncKnowledgePointNodes();
        int kpInserts = kgKnowledgePointRepository.upsert(kps);
        totalInserted += kpInserts;
        Set<String> kpUris = kps.stream()
                .map(KgKnowledgePoint::getUri)
                .collect(Collectors.toSet());

        // 5. URI 校验
        UriValidationResult uriValidation = recordService.validateAllUris(textbooks, chapters, sections, kps);
        if (!uriValidation.valid) {
            log.warn("URI validation warnings: {}", uriValidation.errors);
        }

        // 6. 同步层级关联
        List<KgTextbookChapter> tbChRelations = relationSync.syncTextbookChapterRelations();
        totalInserted += relationSync.rebuildTextbookChapterRelations(tbChRelations);

        List<KgChapterSection> chSecRelations = relationSync.syncChapterSectionRelations();
        totalInserted += relationSync.rebuildChapterSectionRelations(chSecRelations);

        List<KgSectionKP> secKpRelations = relationSync.syncSectionKPRelations();
        totalInserted += relationSync.rebuildSectionKPRelations(secKpRelations);

        return new SyncExecutionResult(totalInserted, totalUpdated);
    }

    private int markDeletedNodes() {
        int totalChanged = 0;

        Set<String> neo4jTextbookUris = nodeSync.syncTextbookNodes().stream()
                .map(KgTextbook::getUri)
                .collect(Collectors.toSet());
        totalChanged += nodeSync.markDeletedNodes("Textbook", neo4jTextbookUris);

        Set<String> neo4jChapterUris = nodeSync.syncChapterNodes().stream()
                .map(KgChapter::getUri)
                .collect(Collectors.toSet());
        totalChanged += nodeSync.markDeletedNodes("Chapter", neo4jChapterUris);

        Set<String> neo4jSectionUris = nodeSync.syncSectionNodes().stream()
                .map(KgSection::getUri)
                .collect(Collectors.toSet());
        totalChanged += nodeSync.markDeletedNodes("Section", neo4jSectionUris);

        Set<String> neo4jKpUris = nodeSync.syncKnowledgePointNodes().stream()
                .map(KgKnowledgePoint::getUri)
                .collect(Collectors.toSet());
        totalChanged += nodeSync.markDeletedNodes("KnowledgePoint", neo4jKpUris);

        return totalChanged;
    }

    private ReconciliationResult reconcile() {
        Set<String> neo4jTextbookUris = nodeSync.syncTextbookNodes().stream()
                .map(KgTextbook::getUri)
                .collect(Collectors.toSet());
        Set<String> neo4jChapterUris = nodeSync.syncChapterNodes().stream()
                .map(KgChapter::getUri)
                .collect(Collectors.toSet());
        Set<String> neo4jSectionUris = nodeSync.syncSectionNodes().stream()
                .map(KgSection::getUri)
                .collect(Collectors.toSet());
        Set<String> neo4jKpUris = nodeSync.syncKnowledgePointNodes().stream()
                .map(KgKnowledgePoint::getUri)
                .collect(Collectors.toSet());

        List<KgTextbookChapter> tbChRelations = relationSync.syncTextbookChapterRelations();
        List<KgChapterSection> chSecRelations = relationSync.syncChapterSectionRelations();
        List<KgSectionKP> secKpRelations = relationSync.syncSectionKPRelations();

        return recordService.reconcile(
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
