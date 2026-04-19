package com.ai.edu.application.service.kg;

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
import com.ai.edu.domain.edukg.repository.KgChapterSectionRepository;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.edukg.repository.Neo4jNodeRepository;
import com.ai.edu.domain.edukg.repository.Neo4jRelationRepository;
import com.ai.edu.domain.edukg.repository.KgSectionKPRepository;
import com.ai.edu.domain.edukg.repository.KgSectionRepository;
import com.ai.edu.domain.edukg.repository.KgSyncRecordRepository;
import com.ai.edu.domain.edukg.repository.KgTextbookChapterRepository;
import com.ai.edu.domain.edukg.repository.KgTextbookRepository;
import com.ai.edu.common.util.UriValidator;
import com.ai.edu.domain.shared.service.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private Neo4jNodeRepository neo4jNodeRepository;

    @Resource
    private Neo4jRelationRepository neo4jRelationRepository;

    @Resource
    private KgSyncRecordRepository kgSyncRecordRepository;

    @Resource
    private KgTextbookRepository kgTextbookRepository;

    @Resource
    private KgChapterRepository kgChapterRepository;

    @Resource
    private KgSectionRepository kgSectionRepository;

    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    @Resource
    private KgTextbookChapterRepository kgTextbookChapterRepository;

    @Resource
    private KgChapterSectionRepository kgChapterSectionRepository;

    @Resource
    private KgSectionKPRepository kgSectionKPRepository;

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
            KgSyncRecord newRecord = KgSyncRecord.create("full", scope, 0L);
            syncRecord = kgSyncRecordRepository.save(newRecord);

            // 执行同步
            SyncExecutionResult syncResult = executeSync(request);

            // 状态变更：标记 MySQL 中有但 Neo4j 中无的节点为 deleted
            int statusChangedCount = markDeletedNodes(request);

            // 对账校验
            ReconciliationResult reconciliation = reconcile(request);

            // 完成同步记录
            Optional<KgSyncRecord> recordOpt = kgSyncRecordRepository.findById(syncRecord.getId());
            if (recordOpt.isPresent()) {
                KgSyncRecord completed = recordOpt.get();
                completed.completeSuccess(
                        syncResult.insertedCount,
                        syncResult.updatedCount,
                        statusChangedCount,
                        reconciliation.matched ? "matched" : "mismatched",
                        reconciliation.differences.isEmpty() ? "All counts matched"
                                : String.join("; ", reconciliation.differences)
                );
                kgSyncRecordRepository.save(completed);
            }

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
                Optional<KgSyncRecord> failRecordOpt = kgSyncRecordRepository.findById(syncRecord.getId());
                failRecordOpt.ifPresent(record -> {
                    record.completeFailure(e.getMessage());
                    kgSyncRecordRepository.save(record);
                });
            }
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Sync failed after {}ms: {}", duration, e.getMessage(), e);
            if (syncRecord != null) {
                Optional<KgSyncRecord> failRecordOpt = kgSyncRecordRepository.findById(syncRecord.getId());
                failRecordOpt.ifPresent(record -> {
                    record.completeFailure(e.getMessage());
                    kgSyncRecordRepository.save(record);
                });
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
            KgSyncRecord newRecord = KgSyncRecord.create("textbook", scope, 0L);
            syncRecord = kgSyncRecordRepository.save(newRecord);

            // 从 Neo4j 读取教材节点（按 edition/subject 过滤）
            List<KgTextbook> textbooks = neo4jNodeRepository.findTextbooks(
                    request.getEdition(), request.getSubject(), null, null);

            // 写入 MySQL（按 uri 去重：insert or update）
            int insertedCount = kgTextbookRepository.upsert(textbooks);

            // 完成同步记录
            Optional<KgSyncRecord> recordOpt = kgSyncRecordRepository.findById(syncRecord.getId());
            if (recordOpt.isPresent()) {
                KgSyncRecord completed = recordOpt.get();
                completed.completeSuccess(insertedCount, 0, 0, "skipped",
                        "Textbook-only sync, reconciliation skipped");
                kgSyncRecordRepository.save(completed);
            }

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
                Optional<KgSyncRecord> failRecordOpt = kgSyncRecordRepository.findById(syncRecord.getId());
                failRecordOpt.ifPresent(record -> {
                    record.completeFailure(e.getMessage());
                    kgSyncRecordRepository.save(record);
                });
            }
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Textbook sync failed after {}ms: {}", duration, e.getMessage(), e);
            if (syncRecord != null) {
                Optional<KgSyncRecord> failRecordOpt = kgSyncRecordRepository.findById(syncRecord.getId());
                failRecordOpt.ifPresent(record -> {
                    record.completeFailure(e.getMessage());
                    kgSyncRecordRepository.save(record);
                });
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
        List<KgSyncRecord> records = kgSyncRecordRepository.findRecent(1);
        KgSyncRecord latest = records.isEmpty() ? null : records.get(0);
        return KgConvert.toSyncStatusDTO(latest);
    }

    /**
     * 查询同步历史记录
     */
    public List<SyncRecordDTO> getSyncRecords(int page, int size) {
        List<KgSyncRecord> records = kgSyncRecordRepository.findRecent(size);
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

        // 1. 同步教材节点（edition/subject 必传）
        List<KgTextbook> textbooks = neo4jNodeRepository.findTextbooks(
                request.getEdition(), request.getSubject(), request.getStage(), request.getGrade());

        Set<String> textbookUris = textbooks.stream()
                .map(KgTextbook::getUri)
                .collect(Collectors.toSet());
        totalInserted += kgTextbookRepository.upsert(textbooks);

        // 2. 同步章节节点（按教材过滤）
        List<KgChapter> chapters = neo4jNodeRepository.findChaptersByTextbookUris(new ArrayList<>(textbookUris));
        int chapterInserts = kgChapterRepository.upsert(chapters);
        totalInserted += chapterInserts;
        Set<String> chapterUris = chapters.stream()
                .map(KgChapter::getUri)
                .collect(Collectors.toSet());

        // 3. 同步小节节点（按教材过滤）
        List<KgSection> sections = neo4jNodeRepository.findSectionsByTextbookUris(new ArrayList<>(textbookUris));
        int sectionInserts = kgSectionRepository.upsert(sections);
        totalInserted += sectionInserts;
        Set<String> sectionUris = sections.stream()
                .map(KgSection::getUri)
                .collect(Collectors.toSet());

        // 4. 同步知识点节点（按教材过滤）
        List<KgKnowledgePoint> kps = neo4jNodeRepository.findKnowledgePointsByTextbookUris(new ArrayList<>(textbookUris));
        int kpInserts = kgKnowledgePointRepository.upsert(kps);
        totalInserted += kpInserts;
        Set<String> kpUris = kps.stream()
                .map(KgKnowledgePoint::getUri)
                .collect(Collectors.toSet());

        // 5. URI 校验
        UriValidationResult uriValidation = UriValidator.validateAllUris(
                Map.of("Textbook", textbookUris.stream().toList()),
                Map.of("Chapter", chapterUris.stream().toList()),
                Map.of("Section", sectionUris.stream().toList()),
                Map.of("KnowledgePoint", kpUris.stream().toList())
        );
        if (!uriValidation.valid) {
            log.warn("URI validation warnings: {}", uriValidation.errors);
        }

        // 6. 同步层级关联（按教材过滤）
        List<KgTextbookChapter> tbChRelations = neo4jRelationRepository.findTextbookChapterRelations(new ArrayList<>(textbookUris));
        totalInserted += rebuildTextbookChapterRelations(tbChRelations);

        List<KgChapterSection> chSecRelations = neo4jRelationRepository.findChapterSectionRelations(new ArrayList<>(chapterUris));
        totalInserted += rebuildChapterSectionRelations(chSecRelations);

        List<KgSectionKP> secKpRelations = neo4jRelationRepository.findSectionKPRelations(new ArrayList<>(sectionUris));
        totalInserted += rebuildSectionKPRelations(secKpRelations);

        return new SyncExecutionResult(totalInserted, totalUpdated);
    }

    private int markDeletedNodes(SyncRequest request) {
        int totalChanged = 0;

        // Textbook
        List<KgTextbook> neo4jTextbooks = neo4jNodeRepository.findTextbooks(
                request.getEdition(), request.getSubject(), request.getStage(), request.getGrade());
        Set<String> neo4jTextbookUris = neo4jTextbooks.stream().map(KgTextbook::getUri).collect(Collectors.toSet());
        for (KgTextbook tb : kgTextbookRepository.findAllActive()) {
            if (!neo4jTextbookUris.contains(tb.getUri())) {
                kgTextbookRepository.updateStatus(tb.getUri(), "deleted");
                totalChanged++;
            }
        }

        // Chapter
        Set<String> neo4jChapterUris = neo4jTextbooks.stream()
                .map(KgTextbook::getUri)
                .collect(Collectors.toSet());
        List<KgChapter> neo4jChapters = neo4jNodeRepository.findChaptersByTextbookUris(new ArrayList<>(neo4jChapterUris));
        Set<String> neo4jChapterUrisSet = neo4jChapters.stream().map(KgChapter::getUri).collect(Collectors.toSet());
        for (KgChapter ch : kgChapterRepository.findAllActive()) {
            if (!neo4jChapterUrisSet.contains(ch.getUri())) {
                kgChapterRepository.updateStatus(ch.getUri(), "deleted");
                totalChanged++;
            }
        }

        // Section
        List<KgSection> neo4jSections = neo4jNodeRepository.findSectionsByTextbookUris(new ArrayList<>(neo4jChapterUris));
        Set<String> neo4jSectionUris = neo4jSections.stream().map(KgSection::getUri).collect(Collectors.toSet());
        for (KgSection sec : kgSectionRepository.findAllActive()) {
            if (!neo4jSectionUris.contains(sec.getUri())) {
                kgSectionRepository.updateStatus(sec.getUri(), "deleted");
                totalChanged++;
            }
        }

        // KnowledgePoint
        List<KgKnowledgePoint> neo4jKps = neo4jNodeRepository.findKnowledgePointsByTextbookUris(new ArrayList<>(neo4jChapterUris));
        Set<String> neo4jKpUris = neo4jKps.stream().map(KgKnowledgePoint::getUri).collect(Collectors.toSet());
        for (KgKnowledgePoint kp : kgKnowledgePointRepository.findAllActive()) {
            if (!neo4jKpUris.contains(kp.getUri())) {
                kgKnowledgePointRepository.updateStatus(kp.getUri(), "deleted");
                totalChanged++;
            }
        }

        return totalChanged;
    }

    private ReconciliationResult reconcile(SyncRequest request) {
        List<KgTextbook> neo4jTextbooks = neo4jNodeRepository.findTextbooks(
                request.getEdition(), request.getSubject(), request.getStage(), request.getGrade());
        Set<String> neo4jTextbookUris = neo4jTextbooks.stream().map(KgTextbook::getUri).collect(Collectors.toSet());

        List<KgChapter> neo4jChapters = neo4jNodeRepository.findChaptersByTextbookUris(new ArrayList<>(neo4jTextbookUris));
        Set<String> neo4jChapterUris = neo4jChapters.stream().map(KgChapter::getUri).collect(Collectors.toSet());

        List<KgSection> neo4jSections = neo4jNodeRepository.findSectionsByTextbookUris(new ArrayList<>(neo4jTextbookUris));
        Set<String> neo4jSectionUris = neo4jSections.stream().map(KgSection::getUri).collect(Collectors.toSet());

        List<KgKnowledgePoint> neo4jKps = neo4jNodeRepository.findKnowledgePointsByTextbookUris(new ArrayList<>(neo4jTextbookUris));
        Set<String> neo4jKpUris = neo4jKps.stream().map(KgKnowledgePoint::getUri).collect(Collectors.toSet());

        List<KgTextbookChapter> tbChRelations = neo4jRelationRepository.findTextbookChapterRelations(new ArrayList<>(neo4jTextbookUris));
        List<KgChapterSection> chSecRelations = neo4jRelationRepository.findChapterSectionRelations(new ArrayList<>(neo4jChapterUris));
        List<KgSectionKP> secKpRelations = neo4jRelationRepository.findSectionKPRelations(new ArrayList<>(neo4jSectionUris));

        List<String> differences = new ArrayList<>();

        int mysqlTbCount = kgTextbookRepository.findAllActive().size();
        if (mysqlTbCount != neo4jTextbookUris.size()) {
            differences.add(String.format("Textbook count mismatch: MySQL=%d, Neo4j=%d", mysqlTbCount, neo4jTextbookUris.size()));
        }

        int mysqlChCount = kgChapterRepository.countActive();
        if (mysqlChCount != neo4jChapterUris.size()) {
            differences.add(String.format("Chapter count mismatch: MySQL=%d, Neo4j=%d", mysqlChCount, neo4jChapterUris.size()));
        }

        int mysqlSecCount = kgSectionRepository.countActive();
        if (mysqlSecCount != neo4jSectionUris.size()) {
            differences.add(String.format("Section count mismatch: MySQL=%d, Neo4j=%d", mysqlSecCount, neo4jSectionUris.size()));
        }

        int mysqlKpCount = kgKnowledgePointRepository.countActive();
        if (mysqlKpCount != neo4jKpUris.size()) {
            differences.add(String.format("KP count mismatch: MySQL=%d, Neo4j=%d", mysqlKpCount, neo4jKpUris.size()));
        }

        int mysqlTbChCount = kgTextbookChapterRepository.findAllActive().size();
        if (mysqlTbChCount != tbChRelations.size()) {
            differences.add(String.format("Textbook-Chapter relation count: MySQL=%d, Neo4j=%d", mysqlTbChCount, tbChRelations.size()));
        }

        int mysqlChSecCount = kgChapterSectionRepository.findAllActive().size();
        if (mysqlChSecCount != chSecRelations.size()) {
            differences.add(String.format("Chapter-Section relation count: MySQL=%d, Neo4j=%d", mysqlChSecCount, chSecRelations.size()));
        }

        int mysqlSecKpCount = kgSectionKPRepository.findAllActive().size();
        if (mysqlSecKpCount != secKpRelations.size()) {
            differences.add(String.format("Section-KP relation count: MySQL=%d, Neo4j=%d", mysqlSecKpCount, secKpRelations.size()));
        }

        boolean matched = differences.isEmpty();
        String details = matched ? "All counts matched" : String.join("; ", differences);
        log.info("Reconciliation: {} - {}", matched ? "PASS" : "FAIL", details);

        return new ReconciliationResult(matched,
                mysqlTbCount, neo4jTextbookUris.size(),
                mysqlChCount, neo4jChapterUris.size(),
                mysqlSecCount, neo4jSectionUris.size(),
                mysqlKpCount, neo4jKpUris.size(),
                mysqlTbChCount, tbChRelations.size(),
                mysqlChSecCount, chSecRelations.size(),
                mysqlSecKpCount, secKpRelations.size(),
                differences);
    }

    // ==================== 关联表 rebuild 私有方法 ====================

    /**
     * 教材-章节关联 UPSERT：按父端分组对比 Neo4j 与 MySQL
     */
    private int rebuildTextbookChapterRelations(List<KgTextbookChapter> neo4jRelations) {
        if (neo4jRelations == null || neo4jRelations.isEmpty()) {
            kgTextbookChapterRepository.deleteByTextbookUri("__ALL__");
            return 0;
        }

        int totalOps = 0;
        Map<String, List<KgTextbookChapter>> neo4jGrouped = neo4jRelations.stream()
                .collect(Collectors.groupingBy(KgTextbookChapter::getTextbookUri));
        Set<String> neo4jParentUris = neo4jGrouped.keySet();

        for (Map.Entry<String, List<KgTextbookChapter>> entry : neo4jGrouped.entrySet()) {
            String textbookUri = entry.getKey();
            List<KgTextbookChapter> neo4jGroup = entry.getValue();
            List<KgTextbookChapter> mysqlGroup = kgTextbookChapterRepository.findByTextbookUri(textbookUri);

            Set<String> mysqlKeys = mysqlGroup.stream().map(KgTextbookChapter::getChapterUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream().map(KgTextbookChapter::getChapterUri).collect(Collectors.toSet());
            Map<String, KgTextbookChapter> mysqlByChapterUri = mysqlGroup.stream()
                    .collect(Collectors.toMap(KgTextbookChapter::getChapterUri, r -> r));

            for (KgTextbookChapter neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getChapterUri())) {
                    kgTextbookChapterRepository.save(neo4jRel);
                    totalOps++;
                } else {
                    KgTextbookChapter mysqlRel = mysqlByChapterUri.get(neo4jRel.getChapterUri());
                    if (!mysqlRel.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgTextbookChapterRepository.updateOrderIndex(textbookUri, neo4jRel.getChapterUri(), neo4jRel.getOrderIndex());
                        totalOps++;
                    }
                }
            }

            for (KgTextbookChapter mysqlRel : mysqlGroup) {
                if (!neo4jKeys.contains(mysqlRel.getChapterUri())) {
                    kgTextbookChapterRepository.deleteRelation(textbookUri, mysqlRel.getChapterUri());
                    totalOps++;
                }
            }
        }

        // 清理 Neo4j 中已不存在的父端下的所有关联
        for (KgTextbookChapter mysqlRel : kgTextbookChapterRepository.findAllActive()) {
            if (!neo4jParentUris.contains(mysqlRel.getTextbookUri())) {
                kgTextbookChapterRepository.deleteByTextbookUri(mysqlRel.getTextbookUri());
                totalOps++;
            }
        }

        log.info("Textbook-chapter UPSERT: {} operations", totalOps);
        return totalOps;
    }

    /**
     * 章节-小节关联 UPSERT
     */
    private int rebuildChapterSectionRelations(List<KgChapterSection> neo4jRelations) {
        if (neo4jRelations == null || neo4jRelations.isEmpty()) {
            kgChapterSectionRepository.deleteByChapterUri("__ALL__");
            return 0;
        }

        int totalOps = 0;
        Map<String, List<KgChapterSection>> neo4jGrouped = neo4jRelations.stream()
                .collect(Collectors.groupingBy(KgChapterSection::getChapterUri));
        Set<String> neo4jParentUris = neo4jGrouped.keySet();

        for (Map.Entry<String, List<KgChapterSection>> entry : neo4jGrouped.entrySet()) {
            String chapterUri = entry.getKey();
            List<KgChapterSection> neo4jGroup = entry.getValue();
            List<KgChapterSection> mysqlGroup = kgChapterSectionRepository.findByChapterUri(chapterUri);

            Set<String> mysqlKeys = mysqlGroup.stream().map(KgChapterSection::getSectionUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream().map(KgChapterSection::getSectionUri).collect(Collectors.toSet());
            Map<String, KgChapterSection> mysqlBySectionUri = mysqlGroup.stream()
                    .collect(Collectors.toMap(KgChapterSection::getSectionUri, r -> r));

            for (KgChapterSection neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getSectionUri())) {
                    kgChapterSectionRepository.save(neo4jRel);
                    totalOps++;
                } else {
                    KgChapterSection mysqlRel = mysqlBySectionUri.get(neo4jRel.getSectionUri());
                    if (!mysqlRel.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgChapterSectionRepository.updateOrderIndex(chapterUri, neo4jRel.getSectionUri(), neo4jRel.getOrderIndex());
                        totalOps++;
                    }
                }
            }

            for (KgChapterSection mysqlRel : mysqlGroup) {
                if (!neo4jKeys.contains(mysqlRel.getSectionUri())) {
                    kgChapterSectionRepository.deleteRelation(chapterUri, mysqlRel.getSectionUri());
                    totalOps++;
                }
            }
        }

        for (KgChapterSection mysqlRel : kgChapterSectionRepository.findAllActive()) {
            if (!neo4jParentUris.contains(mysqlRel.getChapterUri())) {
                kgChapterSectionRepository.deleteByChapterUri(mysqlRel.getChapterUri());
                totalOps++;
            }
        }

        log.info("Chapter-section UPSERT: {} operations", totalOps);
        return totalOps;
    }

    /**
     * 小节-知识点关联 UPSERT
     */
    private int rebuildSectionKPRelations(List<KgSectionKP> neo4jRelations) {
        if (neo4jRelations == null || neo4jRelations.isEmpty()) {
            kgSectionKPRepository.deleteBySectionUri("__ALL__");
            return 0;
        }

        int totalOps = 0;
        Map<String, List<KgSectionKP>> neo4jGrouped = neo4jRelations.stream()
                .collect(Collectors.groupingBy(KgSectionKP::getSectionUri));
        Set<String> neo4jParentUris = neo4jGrouped.keySet();

        for (Map.Entry<String, List<KgSectionKP>> entry : neo4jGrouped.entrySet()) {
            String sectionUri = entry.getKey();
            List<KgSectionKP> neo4jGroup = entry.getValue();
            List<KgSectionKP> mysqlGroup = kgSectionKPRepository.findBySectionUri(sectionUri);

            Set<String> mysqlKeys = mysqlGroup.stream().map(KgSectionKP::getKpUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream().map(KgSectionKP::getKpUri).collect(Collectors.toSet());
            Map<String, KgSectionKP> mysqlByKpUri = mysqlGroup.stream()
                    .collect(Collectors.toMap(KgSectionKP::getKpUri, r -> r));

            for (KgSectionKP neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getKpUri())) {
                    kgSectionKPRepository.save(neo4jRel);
                    totalOps++;
                } else {
                    KgSectionKP mysqlRel = mysqlByKpUri.get(neo4jRel.getKpUri());
                    if (!mysqlRel.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgSectionKPRepository.updateOrderIndex(sectionUri, neo4jRel.getKpUri(), neo4jRel.getOrderIndex());
                        totalOps++;
                    }
                }
            }

            for (KgSectionKP mysqlRel : mysqlGroup) {
                if (!neo4jKeys.contains(mysqlRel.getKpUri())) {
                    kgSectionKPRepository.deleteRelation(sectionUri, mysqlRel.getKpUri());
                    totalOps++;
                }
            }
        }

        for (KgSectionKP mysqlRel : kgSectionKPRepository.findAllActive()) {
            if (!neo4jParentUris.contains(mysqlRel.getSectionUri())) {
                kgSectionKPRepository.deleteBySectionUri(mysqlRel.getSectionUri());
                totalOps++;
            }
        }

        log.info("Section-KP UPSERT: {} operations", totalOps);
        return totalOps;
    }

    /**
     * 同步执行结果
     */
    private record SyncExecutionResult(int insertedCount, int updatedCount) {
    }
}
