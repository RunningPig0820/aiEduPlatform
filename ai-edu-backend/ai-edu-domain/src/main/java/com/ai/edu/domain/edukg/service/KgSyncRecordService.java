package com.ai.edu.domain.edukg.service;

import com.ai.edu.common.dto.kg.HealthCheckResult;
import com.ai.edu.common.dto.kg.ReconciliationResult;
import com.ai.edu.common.dto.kg.UriValidationResult;
import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;

import java.util.List;
import java.util.Set;

/**
 * 知识图谱同步记录/校验服务接口
 */
public interface KgSyncRecordService {

    KgSyncRecord createSyncRecord(String syncType, String scope, Long createdBy);

    void completeSyncRecord(Long recordId, int insertedCount, int updatedCount,
                            int statusChangedCount, String reconciliationStatus,
                            String reconciliationDetails);

    void failSyncRecord(Long recordId, String errorMessage);

    KgSyncRecord getLatestSyncRecord();

    List<KgSyncRecord> getSyncRecords(int limit);

    UriValidationResult validateAllUris(List<KgTextbook> textbooks, List<KgChapter> chapters,
                                        List<KgSection> sections, List<KgKnowledgePoint> kps);

    ReconciliationResult reconcile(Set<String> neo4jTextbookUris, Set<String> neo4jChapterUris,
                                    Set<String> neo4jSectionUris, Set<String> neo4jKpUris,
                                    List<KgTextbookChapter> neo4jTbChRelations,
                                    List<KgChapterSection> neo4jChSecRelations,
                                    List<KgSectionKP> neo4jSecKpRelations);

    HealthCheckResult checkNeo4jHealth();
}
