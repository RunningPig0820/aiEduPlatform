package com.ai.edu.infrastructure.persistence.neo4j;

import com.ai.edu.common.dto.kg.HealthCheckResult;
import com.ai.edu.common.dto.kg.ReconciliationResult;
import com.ai.edu.common.dto.kg.UriValidationResult;
import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.service.KgSyncRecordService;
import com.ai.edu.infrastructure.persistence.edukg.mapper.*;
import com.ai.edu.infrastructure.persistence.edukg.po.KgSyncRecordPo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Neo4j 同步记录服务
 *
 * 负责：同步记录管理 + URI 校验 + 同步对账 + Neo4j 健康检查
 */
@Slf4j
@Service
public class Neo4jSyncRecordService implements KgSyncRecordService {

    @Resource
    private Driver neo4jDriver;

    @Resource
    private KgSyncRecordMapper kgSyncRecordMapper;

    @Resource
    private KgTextbookMapper kgTextbookMapper;

    @Resource
    private KgChapterMapper kgChapterMapper;

    @Resource
    private KgSectionMapper kgSectionMapper;

    @Resource
    private KgKnowledgePointMapper kgKnowledgePointMapper;

    @Resource
    private KgTextbookChapterMapper kgTextbookChapterMapper;

    @Resource
    private KgChapterSectionMapper kgChapterSectionMapper;

    @Resource
    private KgSectionKPMapper kgSectionKPMapper;

    // ==================== 同步记录管理 ====================

    public KgSyncRecord createSyncRecord(String syncType, String scope, Long createdBy) {
        KgSyncRecord record = KgSyncRecord.create(syncType, scope, createdBy);
        KgSyncRecordPo po = KgSyncRecordPo.from(record);
        kgSyncRecordMapper.insert(po);
        record.setId(po.getId());
        return record;
    }

    public void completeSyncRecord(Long recordId, int insertedCount, int updatedCount,
                                   int statusChangedCount, String reconciliationStatus,
                                   String reconciliationDetails) {
        KgSyncRecordPo po = kgSyncRecordMapper.selectById(recordId);
        if (po != null) {
            KgSyncRecord entity = po.toEntity();
            entity.completeSuccess(insertedCount, updatedCount, statusChangedCount,
                    reconciliationStatus, reconciliationDetails);
            kgSyncRecordMapper.updateById(KgSyncRecordPo.from(entity));
        }
    }

    public void failSyncRecord(Long recordId, String errorMessage) {
        KgSyncRecordPo po = kgSyncRecordMapper.selectById(recordId);
        if (po != null) {
            KgSyncRecord entity = po.toEntity();
            entity.completeFailure(errorMessage);
            kgSyncRecordMapper.updateById(KgSyncRecordPo.from(entity));
        }
    }

    public List<KgSyncRecord> getSyncRecords(int limit) {
        return KgSyncRecordPo.toEntityList(kgSyncRecordMapper.selectRecent(limit));
    }

    public KgSyncRecord getLatestSyncRecord() {
        List<KgSyncRecordPo> records = kgSyncRecordMapper.selectRecent(1);
        return records.isEmpty() ? null : records.get(0).toEntity();
    }

    // ==================== URI 校验 ====================

    public UriValidationResult validateAllUris(
            List<KgTextbook> textbooks,
            List<KgChapter> chapters,
            List<KgSection> sections,
            List<KgKnowledgePoint> kps) {
        List<String> allErrors = new ArrayList<>();
        boolean allValid = true;

        UriValidationResult tbResult = validateUris(textbooks.stream().map(KgTextbook::getUri).toList(), "Textbook");
        if (!tbResult.valid) { allValid = false; allErrors.addAll(tbResult.errors); }

        UriValidationResult chResult = validateUris(chapters.stream().map(KgChapter::getUri).toList(), "Chapter");
        if (!chResult.valid) { allValid = false; allErrors.addAll(chResult.errors); }

        UriValidationResult secResult = validateUris(sections.stream().map(KgSection::getUri).toList(), "Section");
        if (!secResult.valid) { allValid = false; allErrors.addAll(secResult.errors); }

        UriValidationResult kpResult = validateUris(kps.stream().map(KgKnowledgePoint::getUri).toList(), "KnowledgePoint");
        if (!kpResult.valid) { allValid = false; allErrors.addAll(kpResult.errors); }

        return new UriValidationResult(allValid, allErrors);
    }

    public UriValidationResult validateUris(List<String> uris, String nodeType) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String uri : uris) {
            if (uri == null || uri.isBlank()) {
                errors.add(String.format("[%s] URI is null or blank", nodeType));
                continue;
            }
            if (!uri.contains(":") || uri.length() < 10) {
                errors.add(String.format("[%s] Invalid URI format: %s", nodeType, uri));
                continue;
            }
            if (seen.contains(uri)) {
                errors.add(String.format("[%s] Duplicate URI: %s", nodeType, uri));
            }
            seen.add(uri);
        }

        return new UriValidationResult(errors.isEmpty(), errors);
    }

    // ==================== 同步对账 ====================

    public ReconciliationResult reconcile(
            Set<String> neo4jTextbookUris, Set<String> neo4jChapterUris,
            Set<String> neo4jSectionUris, Set<String> neo4jKpUris,
            List<KgTextbookChapter> neo4jTbChRelations, List<KgChapterSection> neo4jChSecRelations, List<KgSectionKP> neo4jSecKpRelations) {

        List<String> differences = new ArrayList<>();

        // 节点对账
        int mysqlTbCount = kgTextbookMapper.selectAllActive().size();
        if (mysqlTbCount != neo4jTextbookUris.size()) {
            differences.add(String.format("Textbook count mismatch: MySQL=%d, Neo4j=%d", mysqlTbCount, neo4jTextbookUris.size()));
        }

        int mysqlChCount = kgChapterMapper.selectByStatus("active").size();
        if (mysqlChCount != neo4jChapterUris.size()) {
            differences.add(String.format("Chapter count mismatch: MySQL=%d, Neo4j=%d", mysqlChCount, neo4jChapterUris.size()));
        }

        int mysqlSecCount = kgSectionMapper.selectByStatus("active").size();
        if (mysqlSecCount != neo4jSectionUris.size()) {
            differences.add(String.format("Section count mismatch: MySQL=%d, Neo4j=%d", mysqlSecCount, neo4jSectionUris.size()));
        }

        int mysqlKpCount = kgKnowledgePointMapper.selectByStatus("active").size();
        if (mysqlKpCount != neo4jKpUris.size()) {
            differences.add(String.format("KP count mismatch: MySQL=%d, Neo4j=%d", mysqlKpCount, neo4jKpUris.size()));
        }

        // 关联对账
        int mysqlTbChCount = kgTextbookChapterMapper.selectAllActiveRelations().size();
        if (mysqlTbChCount != neo4jTbChRelations.size()) {
            differences.add(String.format("Textbook-Chapter relation count: MySQL=%d, Neo4j=%d", mysqlTbChCount, neo4jTbChRelations.size()));
        }

        int mysqlChSecCount = kgChapterSectionMapper.selectAllActiveRelations().size();
        if (mysqlChSecCount != neo4jChSecRelations.size()) {
            differences.add(String.format("Chapter-Section relation count: MySQL=%d, Neo4j=%d", mysqlChSecCount, neo4jChSecRelations.size()));
        }

        int mysqlSecKpCount = kgSectionKPMapper.selectAllActiveRelations().size();
        if (mysqlSecKpCount != neo4jSecKpRelations.size()) {
            differences.add(String.format("Section-KP relation count: MySQL=%d, Neo4j=%d", mysqlSecKpCount, neo4jSecKpRelations.size()));
        }

        boolean matched = differences.isEmpty();
        String details = matched ? "All counts matched" : String.join("; ", differences);
        log.info("Reconciliation: {} - {}", matched ? "PASS" : "FAIL", details);

        return new ReconciliationResult(matched,
                mysqlTbCount, neo4jTextbookUris.size(),
                mysqlChCount, neo4jChapterUris.size(),
                mysqlSecCount, neo4jSectionUris.size(),
                mysqlKpCount, neo4jKpUris.size(),
                mysqlTbChCount, neo4jTbChRelations.size(),
                mysqlChSecCount, neo4jChSecRelations.size(),
                mysqlSecKpCount, neo4jSecKpRelations.size(),
                differences);
    }

    // ==================== Neo4j 健康检查 ====================

    public HealthCheckResult checkNeo4jHealth() {
        long startTime = System.currentTimeMillis();
        try (var session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                tx.run("RETURN 1").single();
                return null;
            });
            long responseTime = System.currentTimeMillis() - startTime;
            return new HealthCheckResult(true, responseTime, "Neo4j is healthy");
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return new HealthCheckResult(false, responseTime, "Neo4j connection failed: " + e.getMessage());
        }
    }
}
