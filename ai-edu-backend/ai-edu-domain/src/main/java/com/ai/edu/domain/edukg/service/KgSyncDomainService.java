package com.ai.edu.domain.edukg.service;

import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;

import java.util.List;
import java.util.Set;

/**
 * 知识图谱同步领域服务接口
 * 职责：定义从 Neo4j 同步知识图谱到 MySQL 的能力
 */
public interface KgSyncDomainService {

    // ==================== 节点同步 ====================

    List<KgTextbook> syncTextbookNodes();

    List<KgChapter> syncChapterNodes();

    List<KgSection> syncSectionNodes();

    List<KgKnowledgePoint> syncKnowledgePointNodes();

    // ==================== 关系同步 ====================

    List<KgTextbookChapter> syncTextbookChapterRelations();

    List<KgChapterSection> syncChapterSectionRelations();

    List<KgSectionKP> syncSectionKPRelations();

    // ==================== UPSERT ====================

    int upsertTextbooks(List<KgTextbook> textbooks);

    int upsertChapters(List<KgChapter> chapters);

    int upsertSections(List<KgSection> sections);

    int upsertKnowledgePoints(List<KgKnowledgePoint> knowledgePoints);

    // ==================== 关联表重建 ====================

    int rebuildTextbookChapterRelations(List<KgTextbookChapter> relations);

    int rebuildChapterSectionRelations(List<KgChapterSection> relations);

    int rebuildSectionKPRelations(List<KgSectionKP> relations);

    // ==================== 状态变更 ====================

    int markDeletedNodes(String neo4jNodeType, Set<String> neo4jUris);

    // ==================== 同步记录 ====================

    KgSyncRecord createSyncRecord(String syncType, String scope, Long createdBy);

    void completeSyncRecord(Long recordId, int insertedCount, int updatedCount,
                            int statusChangedCount, String reconciliationStatus,
                            String reconciliationDetails);

    void failSyncRecord(Long recordId, String errorMessage);

    KgSyncRecord getLatestSyncRecord();

    List<KgSyncRecord> getSyncRecords(int limit);

    // ==================== URI 校验 ====================

    UriValidationResult validateAllUris(List<KgTextbook> textbooks, List<KgChapter> chapters,
                                        List<KgSection> sections, List<KgKnowledgePoint> kps);

    // ==================== 对账校验 ====================

    ReconciliationResult reconcile(Set<String> neo4jTextbookUris, Set<String> neo4jChapterUris,
                                    Set<String> neo4jSectionUris, Set<String> neo4jKpUris,
                                    List<KgTextbookChapter> neo4jTbChRelations,
                                    List<KgChapterSection> neo4jChSecRelations,
                                    List<KgSectionKP> neo4jSecKpRelations);

    // ==================== 健康检查 ====================

    HealthCheckResult checkNeo4jHealth();

    // ==================== 内部类 ====================

    class UriValidationResult {
        public final boolean valid;
        public final List<String> errors;

        public UriValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
    }

    class ReconciliationResult {
        public final boolean matched;
        public final int mysqlTextbookCount;
        public final int neo4jTextbookCount;
        public final int mysqlChapterCount;
        public final int neo4jChapterCount;
        public final int mysqlSectionCount;
        public final int neo4jSectionCount;
        public final int mysqlKpCount;
        public final int neo4jKpCount;
        public final int mysqlTextbookChapterCount;
        public final int neo4jTextbookChapterCount;
        public final int mysqlChapterSectionCount;
        public final int neo4jChapterSectionCount;
        public final int mysqlSectionKpCount;
        public final int neo4jSectionKpCount;
        public final List<String> differences;

        public ReconciliationResult(boolean matched,
                                    int mysqlTb, int neo4jTb, int mysqlCh, int neo4jCh,
                                    int mysqlSec, int neo4jSec, int mysqlKp, int neo4jKp,
                                    int mysqlTbCh, int neo4jTbCh, int mysqlChSec, int neo4jChSec,
                                    int mysqlSecKp, int neo4jSecKp, List<String> differences) {
            this.matched = matched;
            this.mysqlTextbookCount = mysqlTb;
            this.neo4jTextbookCount = neo4jTb;
            this.mysqlChapterCount = mysqlCh;
            this.neo4jChapterCount = neo4jCh;
            this.mysqlSectionCount = mysqlSec;
            this.neo4jSectionCount = neo4jSec;
            this.mysqlKpCount = mysqlKp;
            this.neo4jKpCount = neo4jKp;
            this.mysqlTextbookChapterCount = mysqlTbCh;
            this.neo4jTextbookChapterCount = neo4jTbCh;
            this.mysqlChapterSectionCount = mysqlChSec;
            this.neo4jChapterSectionCount = neo4jChSec;
            this.mysqlSectionKpCount = mysqlSecKp;
            this.neo4jSectionKpCount = neo4jSecKp;
            this.differences = differences;
        }
    }

    class HealthCheckResult {
        public final boolean healthy;
        public final long responseTimeMs;
        public final String message;

        public HealthCheckResult(boolean healthy, long responseTimeMs, String message) {
            this.healthy = healthy;
            this.responseTimeMs = responseTimeMs;
            this.message = message;
        }
    }
}
