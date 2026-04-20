package com.ai.edu.application.service.kg;

import com.ai.edu.application.assembler.KgConvert;
import com.ai.edu.application.dto.kg.SyncRecordDTO;
import com.ai.edu.application.dto.kg.SyncRecordQueryRequest;
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
import com.ai.edu.domain.edukg.repository.*;
import com.ai.edu.common.util.UriValidator;
import com.ai.edu.domain.shared.service.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
 *
 * 同步粒度：按 edition/subject/stage/grade 拆分为独立子任务，
 * 每个 grade 子任务拥有独立的锁、独立的同步记录、独立的对账。
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

    private static final long KG_SYNC_LOCK_TIMEOUT_SECONDS = 600;
    private static final long KG_SYNC_STALE_THRESHOLD_MINUTES = 10;

    /**
     * 全量/定向同步
     *
     * @param request 同步请求（必传参数：subject + edition）
     * @return 同步结果（聚合所有 grade 的统计）
     */
    public SyncResult syncFull(SyncRequest request) {
        // 参数校验
        validateSyncRequest(request);

        String edition = request.getEdition();
        String subject = request.getSubject();
        String stage = request.getStage();
        String grade = request.getGrade();

        log.info("Starting KG sync: subject={}, edition={}, stage={}, grade={}",
                subject, edition, stage, grade);

        long startTime = System.currentTimeMillis();

        // 从 Neo4j 查询该 edition+subject 下的所有唯一 grade
        List<String> allGrades = neo4jNodeRepository.findDistinctGrades(edition, subject);
        if (allGrades.isEmpty()) {
            log.warn("No grades found in Neo4j for edition={}, subject={}", edition, subject);
            return new SyncResult().buildEmptySyncResult(startTime);
        }

        // 如果指定了 grade 参数，过滤为仅该 grade
        List<String> targetGrades = (grade != null && !grade.isBlank())
                ? allGrades.stream().filter(g -> g.equals(grade)).toList()
                : allGrades;

        if (targetGrades.isEmpty()) {
            log.warn("No matching grade found for edition={}, subject={}, grade={}",
                    edition, subject, grade);
            return new SyncResult().buildEmptySyncResult(startTime);
        }

        log.info("Sync will process {} grade(s): {}", targetGrades.size(), targetGrades);

        int totalInserted = 0;
        int totalUpdated = 0;
        int totalDeleted = 0;
        int completedGrades = 0;
        int failedGrades = 0;
        boolean anyMismatch = false;

        for (String g : targetGrades) {
            GradeSyncResult gradeResult = syncOneGrade(edition, subject, stage, g);
            if (gradeResult.success) {
                completedGrades++;
                totalInserted += gradeResult.insertedCount;
                totalUpdated += gradeResult.updatedCount;
                totalDeleted += gradeResult.statusChangedCount;
                if (gradeResult.reconcilationMismatch) {
                    anyMismatch = true;
                }
            } else {
                failedGrades++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Overall sync completed in {}ms: grades={}/{}, inserted={}, updated={}, deleted={}",
                duration, completedGrades, targetGrades.size(), totalInserted, totalUpdated, totalDeleted);

        SyncResult.SyncResultBuilder builder = SyncResult.builder()
                .status(failedGrades == 0 ? "success" : "partial_success")
                .insertedCount(totalInserted)
                .updatedCount(totalUpdated)
                .statusChangedCount(totalDeleted)
                .reconciliationStatus(anyMismatch ? "mismatched" : "matched")
                .duration(duration)
                .completedGrades(completedGrades)
                .failedGrades(failedGrades)
                .totalGrades(targetGrades.size());

        return builder.build();
    }

    /**
     * 单独同步教材节点（不同步章节/小节/知识点）
     */
    public SyncResult syncTextbooksOnly(SyncRequest request) {
        String lockKey = buildTextbookSyncLockKey(request.getEdition(), request.getSubject());
        String lockValue = UUID.randomUUID().toString();
        if (!redisService.tryLock(lockKey, lockValue, KG_SYNC_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new BusinessException(ErrorCode.KG_SYNC_IN_PROGRESS, "已有同步任务正在执行，请稍后重试");
        }

        validateSyncRequest(request);

        long startTime = System.currentTimeMillis();
        log.info("Starting textbook-only sync: subject={}, edition={}",
                request.getSubject(), request.getEdition());

        KgSyncRecord syncRecord = null;
        try {
            KgSyncRecord newRecord = KgSyncRecord.create(
                    "textbook", request.getEdition(), request.getSubject(), null, null, 0L);
            syncRecord = kgSyncRecordRepository.save(newRecord);

            List<KgTextbook> textbooks = neo4jNodeRepository.findTextbooks(
                    request.getEdition(), request.getSubject(), null, null);

            int insertedCount = kgTextbookRepository.upsert(textbooks);

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
                    syncRecord.getId(), "success", insertedCount, 0, 0, "skipped", duration);
        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Textbook sync failed after {}ms: {}", duration, e.getMessage(), e);
            if (syncRecord != null) {
                kgSyncRecordRepository.findById(syncRecord.getId()).ifPresent(record -> {
                    record.completeFailure(e.getMessage());
                    kgSyncRecordRepository.save(record);
                });
            }
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Textbook sync failed after {}ms: {}", duration, e.getMessage(), e);
            if (syncRecord != null) {
                kgSyncRecordRepository.findById(syncRecord.getId()).ifPresent(record -> {
                    record.completeFailure(e.getMessage());
                    kgSyncRecordRepository.save(record);
                });
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "教材同步失败: " + e.getMessage());
        } finally {
            redisService.unlock(lockKey, lockValue);
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
     * 查询同步历史记录（支持按维度筛选）
     */
    public List<SyncRecordDTO> getSyncRecords(SyncRecordQueryRequest request) {
        int size = request.getSize() != null ? request.getSize() : 10;
        List<KgSyncRecord> records = kgSyncRecordRepository.findByScope(
                request.getEdition(),
                request.getSubject(),
                request.getStage(),
                request.getGrade(),
                size);
        return KgConvert.toSyncRecordDTOs(records);
    }

    // ==================== 私有方法 ====================

    /**
     * 同步单个年级（独立锁、独立记录、独立对账）
     */
    protected GradeSyncResult syncOneGrade(String edition, String subject, String stage, String grade) {
        String lockKey = buildSyncLockKey(edition, subject, stage, grade);
        String lockValue = UUID.randomUUID().toString();

        // 检测过期任务
        detectAndMarkStale(edition, subject, stage, grade);

        if (!redisService.tryLock(lockKey, lockValue, KG_SYNC_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            log.warn("Grade sync skipped (locked): edition={}, subject={}, grade={}", edition, subject, grade);
            return new GradeSyncResult(false, 0, 0, 0, false);
        }

        KgSyncRecord syncRecord = null;
        try {
            // 创建同步记录
            KgSyncRecord newRecord = KgSyncRecord.create("full", edition, subject, stage, grade, 0L);
            syncRecord = kgSyncRecordRepository.save(newRecord);

            // 构建该 grade 的同步请求
            SyncRequest gradeRequest = SyncRequest.builder()
                    .edition(edition)
                    .subject(subject)
                    .stage(stage)
                    .grade(grade)
                    .build();

            // 执行同步
            SyncExecutionResult syncResult = executeSync(gradeRequest);

            // 标记删除节点（仅该 grade 范围）
            int statusChangedCount = markDeletedNodes(gradeRequest);

            // 对账校验（仅该 grade 范围）
            ReconciliationResult reconciliation = reconcile(gradeRequest);

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

            log.info("Grade sync completed: grade={}, inserted={}, updated={}, deleted={}, reconciled={}",
                    grade, syncResult.insertedCount, syncResult.updatedCount, statusChangedCount,
                    reconciliation.matched ? "matched" : "mismatched");

            return new GradeSyncResult(true, syncResult.insertedCount, syncResult.updatedCount,
                    statusChangedCount, !reconciliation.matched);
        } catch (BusinessException e) {
            log.error("Grade sync failed: grade={}, error={}", grade, e.getMessage(), e);
            if (syncRecord != null) {
                kgSyncRecordRepository.findById(syncRecord.getId()).ifPresent(record -> {
                    record.completeFailure(e.getMessage());
                    kgSyncRecordRepository.save(record);
                });
            }
            return new GradeSyncResult(false, 0, 0, 0, false);
        } catch (Exception e) {
            log.error("Grade sync failed: grade={}, error={}", grade, e.getMessage(), e);
            if (syncRecord != null) {
                kgSyncRecordRepository.findById(syncRecord.getId()).ifPresent(record -> {
                    record.completeFailure(e.getMessage());
                    kgSyncRecordRepository.save(record);
                });
            }
            return new GradeSyncResult(false, 0, 0, 0, false);
        } finally {
            redisService.unlock(lockKey, lockValue);
        }
    }

    /**
     * 检测并标记过期的运行中任务（视为崩溃）
     */
    private void detectAndMarkStale(String edition, String subject, String stage, String grade) {
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(KG_SYNC_STALE_THRESHOLD_MINUTES);
        Optional<KgSyncRecord> runningOpt = kgSyncRecordRepository
                .findLatestRunningByScope(edition, subject, stage, grade);

        if (runningOpt.isPresent() && runningOpt.get().isStale(staleThreshold)) {
            KgSyncRecord staleRecord = runningOpt.get();
            log.warn("Detected stale sync task: id={}, startedAt={}, marking as failed",
                    staleRecord.getId(), staleRecord.getStartedAt());
            staleRecord.completeFailure("Task exceeded time limit (10 minutes), considered crashed");
            kgSyncRecordRepository.save(staleRecord);
        }
    }

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

    private String buildSyncLockKey(String edition, String subject, String stage, String grade) {
        return String.format("ai-edu:kg:sync:lock:%s:%s:%s:%s", edition, subject, stage, grade);
    }

    private String buildTextbookSyncLockKey(String edition, String subject) {
        return String.format("ai-edu:kg:sync:lock:textbooks:%s:%s", edition, subject);
    }

    private SyncExecutionResult executeSync(SyncRequest request) {
        int totalInserted = 0;
        int totalUpdated = 0;

        // 1. 同步教材节点
        List<KgTextbook> textbooks = neo4jNodeRepository.findTextbooks(
                request.getEdition(), request.getSubject(), request.getStage(), request.getGrade());

        Set<String> textbookUris = textbooks.stream()
                .map(KgTextbook::getUri)
                .collect(Collectors.toSet());
        totalInserted += kgTextbookRepository.upsert(textbooks);

        // 2. 同步章节节点
        List<KgChapter> chapters = neo4jNodeRepository.findChaptersByTextbookUris(new ArrayList<>(textbookUris));
        totalInserted += kgChapterRepository.upsert(chapters);
        Set<String> chapterUris = chapters.stream()
                .map(KgChapter::getUri)
                .collect(Collectors.toSet());

        // 3. 同步小节节点
        List<KgSection> sections = neo4jNodeRepository.findSectionsByTextbookUris(new ArrayList<>(textbookUris));
        totalInserted += kgSectionRepository.upsert(sections);
        Set<String> sectionUris = sections.stream()
                .map(KgSection::getUri)
                .collect(Collectors.toSet());

        // 4. 同步知识点节点
        List<KgKnowledgePoint> kps = neo4jNodeRepository.findKnowledgePointsByTextbookUris(new ArrayList<>(textbookUris));
        totalInserted += kgKnowledgePointRepository.upsert(kps);
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

        // 6. 同步层级关联
        List<KgTextbookChapter> tbChRelations = neo4jRelationRepository.findTextbookChapterRelations(new ArrayList<>(textbookUris));
        totalInserted += rebuildTextbookChapterRelations(tbChRelations);

        List<KgChapterSection> chSecRelations = neo4jRelationRepository.findChapterSectionRelations(new ArrayList<>(chapterUris));
        totalInserted += rebuildChapterSectionRelations(chSecRelations);

        List<KgSectionKP> secKpRelations = neo4jRelationRepository.findSectionKPRelations(new ArrayList<>(sectionUris));
        totalInserted += rebuildSectionKPRelations(secKpRelations);

        return new SyncExecutionResult(totalInserted, totalUpdated);
    }

    /**
     * 标记 MySQL 中有但 Neo4j 中无的节点为 deleted
     * 作用域：仅限当前 grade 范围内的节点
     */
    private int markDeletedNodes(SyncRequest request) {
        int totalChanged = 0;

        // Textbook — 仅查当前 grade 范围
        List<KgTextbook> neo4jTextbooks = neo4jNodeRepository.findTextbooks(
                request.getEdition(), request.getSubject(), request.getStage(), request.getGrade());
        Set<String> neo4jTextbookUris = neo4jTextbooks.stream()
                .map(KgTextbook::getUri).collect(Collectors.toSet());
        for (KgTextbook tb : kgTextbookRepository.findAllActiveByEditionSubjectGrade(
                request.getEdition(), request.getSubject(), request.getGrade())) {
            if (!neo4jTextbookUris.contains(tb.getUri())) {
                kgTextbookRepository.updateStatus(tb.getUri(), "deleted");
                totalChanged++;
            }
        }

        // Chapter — 通过 textbook_chapter 关联表，只查与当前 grade 教材关联的章节
        List<KgChapter> neo4jChapters = neo4jNodeRepository
                .findChaptersByTextbookUris(new ArrayList<>(neo4jTextbookUris));
        Set<String> neo4jChapterUrisSet = neo4jChapters.stream()
                .map(KgChapter::getUri).collect(Collectors.toSet());
        for (KgChapter ch : kgChapterRepository.findAllActiveByTextbookUris(new ArrayList<>(neo4jTextbookUris))) {
            if (!neo4jChapterUrisSet.contains(ch.getUri())) {
                kgChapterRepository.updateStatus(ch.getUri(), "deleted");
                totalChanged++;
            }
        }

        // Section — 通过 chapter_section 关联表，用 Neo4j chapter URIs 做 scope
        List<KgSection> neo4jSections = neo4jNodeRepository
                .findSectionsByTextbookUris(new ArrayList<>(neo4jTextbookUris));
        Set<String> neo4jSectionUris = neo4jSections.stream()
                .map(KgSection::getUri).collect(Collectors.toSet());
        for (KgSection sec : kgSectionRepository.findAllActiveByChapterUris(new ArrayList<>(neo4jChapterUrisSet))) {
            if (!neo4jSectionUris.contains(sec.getUri())) {
                kgSectionRepository.updateStatus(sec.getUri(), "deleted");
                totalChanged++;
            }
        }

        // KnowledgePoint — 通过 section_kp 关联表，用 Neo4j section URIs 做 scope
        List<KgKnowledgePoint> neo4jKps = neo4jNodeRepository
                .findKnowledgePointsByTextbookUris(new ArrayList<>(neo4jTextbookUris));
        Set<String> neo4jKpUris = neo4jKps.stream()
                .map(KgKnowledgePoint::getUri).collect(Collectors.toSet());
        for (KgKnowledgePoint kp : kgKnowledgePointRepository.findAllActiveBySectionUris(new ArrayList<>(neo4jSectionUris))) {
            if (!neo4jKpUris.contains(kp.getUri())) {
                kgKnowledgePointRepository.updateStatus(kp.getUri(), "deleted");
                totalChanged++;
            }
        }

        return totalChanged;
    }

    /**
     * 对账校验
     * 作用域：仅限当前 grade 范围
     */
    private ReconciliationResult reconcile(SyncRequest request) {
        List<KgTextbook> neo4jTextbooks = neo4jNodeRepository.findTextbooks(
                request.getEdition(), request.getSubject(), request.getStage(), request.getGrade());
        Set<String> neo4jTextbookUris = neo4jTextbooks.stream()
                .map(KgTextbook::getUri).collect(Collectors.toSet());

        List<KgChapter> neo4jChapters = neo4jNodeRepository
                .findChaptersByTextbookUris(new ArrayList<>(neo4jTextbookUris));
        Set<String> neo4jChapterUris = neo4jChapters.stream()
                .map(KgChapter::getUri).collect(Collectors.toSet());

        List<KgSection> neo4jSections = neo4jNodeRepository
                .findSectionsByTextbookUris(new ArrayList<>(neo4jTextbookUris));
        Set<String> neo4jSectionUris = neo4jSections.stream()
                .map(KgSection::getUri).collect(Collectors.toSet());

        List<KgKnowledgePoint> neo4jKps = neo4jNodeRepository
                .findKnowledgePointsByTextbookUris(new ArrayList<>(neo4jTextbookUris));
        Set<String> neo4jKpUris = neo4jKps.stream()
                .map(KgKnowledgePoint::getUri).collect(Collectors.toSet());

        List<KgTextbookChapter> tbChRelations = neo4jRelationRepository
                .findTextbookChapterRelations(new ArrayList<>(neo4jTextbookUris));
        List<KgChapterSection> chSecRelations = neo4jRelationRepository
                .findChapterSectionRelations(new ArrayList<>(neo4jChapterUris));
        List<KgSectionKP> secKpRelations = neo4jRelationRepository
                .findSectionKPRelations(new ArrayList<>(neo4jSectionUris));

        List<String> differences = new ArrayList<>();

        // Textbook 对账 — 仅对比当前 grade 范围
        int mysqlTbCount = kgTextbookRepository
                .findAllActiveByEditionSubjectGrade(request.getEdition(), request.getSubject(), request.getGrade()).size();
        if (mysqlTbCount != neo4jTextbookUris.size()) {
            differences.add(String.format("Textbook count mismatch: MySQL=%d, Neo4j=%d",
                    mysqlTbCount, neo4jTextbookUris.size()));
        }

        // Chapter 对账 — 仅对比与当前 grade 教材关联的章节
        int mysqlChCount = kgChapterRepository
                .findAllActiveByTextbookUris(new ArrayList<>(neo4jTextbookUris)).size();
        if (mysqlChCount != neo4jChapterUris.size()) {
            differences.add(String.format("Chapter count mismatch: MySQL=%d, Neo4j=%d",
                    mysqlChCount, neo4jChapterUris.size()));
        }

        // Section 对账 — 仅对比与当前 grade 章节关联的小节
        int mysqlSecCount = kgSectionRepository
                .findAllActiveByChapterUris(new ArrayList<>(neo4jChapterUris)).size();
        if (mysqlSecCount != neo4jSectionUris.size()) {
            differences.add(String.format("Section count mismatch: MySQL=%d, Neo4j=%d",
                    mysqlSecCount, neo4jSectionUris.size()));
        }

        // KP 对账 — 仅对比与当前 grade 小节关联的知识点
        int mysqlKpCount = kgKnowledgePointRepository
                .findAllActiveBySectionUris(new ArrayList<>(neo4jSectionUris)).size();
        if (mysqlKpCount != neo4jKpUris.size()) {
            differences.add(String.format("KP count mismatch: MySQL=%d, Neo4j=%d",
                    mysqlKpCount, neo4jKpUris.size()));
        }

        int mysqlTbChCount = kgTextbookChapterRepository.findAllActive().size();
        if (mysqlTbChCount != tbChRelations.size()) {
            differences.add(String.format("Textbook-Chapter relation count: MySQL=%d, Neo4j=%d",
                    mysqlTbChCount, tbChRelations.size()));
        }

        int mysqlChSecCount = kgChapterSectionRepository.findAllActive().size();
        if (mysqlChSecCount != chSecRelations.size()) {
            differences.add(String.format("Chapter-Section relation count: MySQL=%d, Neo4j=%d",
                    mysqlChSecCount, chSecRelations.size()));
        }

        int mysqlSecKpCount = kgSectionKPRepository.findAllActive().size();
        if (mysqlSecKpCount != secKpRelations.size()) {
            differences.add(String.format("Section-KP relation count: MySQL=%d, Neo4j=%d",
                    mysqlSecKpCount, secKpRelations.size()));
        }

        boolean matched = differences.isEmpty();
        String details = matched ? "All counts matched" : String.join("; ", differences);
        log.info("Reconciliation for grade={}: {} - {}", request.getGrade(),
                matched ? "PASS" : "FAIL", details);

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

            Set<String> mysqlKeys = mysqlGroup.stream()
                    .map(KgTextbookChapter::getChapterUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream()
                    .map(KgTextbookChapter::getChapterUri).collect(Collectors.toSet());
            Map<String, KgTextbookChapter> mysqlByChapterUri = mysqlGroup.stream()
                    .collect(Collectors.toMap(KgTextbookChapter::getChapterUri, r -> r));

            for (KgTextbookChapter neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getChapterUri())) {
                    kgTextbookChapterRepository.save(neo4jRel);
                    totalOps++;
                } else {
                    KgTextbookChapter mysqlRel = mysqlByChapterUri.get(neo4jRel.getChapterUri());
                    if (!mysqlRel.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgTextbookChapterRepository.updateOrderIndex(
                                textbookUri, neo4jRel.getChapterUri(), neo4jRel.getOrderIndex());
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

        for (KgTextbookChapter mysqlRel : kgTextbookChapterRepository.findAllActive()) {
            if (!neo4jParentUris.contains(mysqlRel.getTextbookUri())) {
                kgTextbookChapterRepository.deleteByTextbookUri(mysqlRel.getTextbookUri());
                totalOps++;
            }
        }

        log.info("Textbook-chapter UPSERT: {} operations", totalOps);
        return totalOps;
    }

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

            Set<String> mysqlKeys = mysqlGroup.stream()
                    .map(KgChapterSection::getSectionUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream()
                    .map(KgChapterSection::getSectionUri).collect(Collectors.toSet());
            Map<String, KgChapterSection> mysqlBySectionUri = mysqlGroup.stream()
                    .collect(Collectors.toMap(KgChapterSection::getSectionUri, r -> r));

            for (KgChapterSection neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getSectionUri())) {
                    kgChapterSectionRepository.save(neo4jRel);
                    totalOps++;
                } else {
                    KgChapterSection mysqlRel = mysqlBySectionUri.get(neo4jRel.getSectionUri());
                    if (!mysqlRel.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgChapterSectionRepository.updateOrderIndex(
                                chapterUri, neo4jRel.getSectionUri(), neo4jRel.getOrderIndex());
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

            Set<String> mysqlKeys = mysqlGroup.stream()
                    .map(KgSectionKP::getKpUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream()
                    .map(KgSectionKP::getKpUri).collect(Collectors.toSet());
            Map<String, KgSectionKP> mysqlByKpUri = mysqlGroup.stream()
                    .collect(Collectors.toMap(KgSectionKP::getKpUri, r -> r));

            for (KgSectionKP neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getKpUri())) {
                    kgSectionKPRepository.save(neo4jRel);
                    totalOps++;
                } else {
                    KgSectionKP mysqlRel = mysqlByKpUri.get(neo4jRel.getKpUri());
                    if (!mysqlRel.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgSectionKPRepository.updateOrderIndex(
                                sectionUri, neo4jRel.getKpUri(), neo4jRel.getOrderIndex());
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

    /**
     * 单年级同步结果
     */
    protected record GradeSyncResult(boolean success, int insertedCount, int updatedCount,
                                      int statusChangedCount, boolean reconcilationMismatch) {
    }
}
