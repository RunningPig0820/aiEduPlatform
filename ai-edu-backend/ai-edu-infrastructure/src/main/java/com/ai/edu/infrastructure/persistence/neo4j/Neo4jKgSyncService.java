package com.ai.edu.infrastructure.persistence.neo4j;

import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.service.KgSyncDomainService;
import com.ai.edu.infrastructure.persistence.edukg.mapper.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Neo4j 知识图谱同步服务（领域服务实现）
 *
 * 负责从 Neo4j 查询知识图谱数据并同步到 MySQL：
 * - 任务 3.2: Neo4j 同步服务
 * - 任务 3.3: Neo4j 查询节点并映射为 MySQL Entity（按 URI）
 * - 任务 3.4: Neo4j 查询层级关系（CONTAINS/IN_UNIT）并映射为关联 Entity
 * - 任务 3.5: UPSERT 批量写入逻辑（ON DUPLICATE KEY UPDATE，按 URI）
 */
@Slf4j
@Service
public class Neo4jKgSyncService implements KgSyncDomainService {

    @Resource
    private Driver neo4jDriver;

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

    @Resource
    private KgSyncRecordMapper kgSyncRecordMapper;

    // ==================== 任务 3.3: Neo4j 查询节点并映射为 MySQL Entity ====================

    /**
     * 从 Neo4j 查询指定类型的节点并映射为 MySQL Entity 列表
     */
    @Override
    public List<KgTextbook> syncTextbookNodes() {
        return queryNeo4jNodes("Textbook", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            String grade = getStringProperty(record, "grade");
            String phase = getStringProperty(record, "phase");
            String subject = getStringProperty(record, "subject");
            return KgTextbook.create(uri, label, grade, phase, subject);
        });
    }

    @Override
    public List<KgChapter> syncChapterNodes() {
        return queryNeo4jNodes("Chapter", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgChapter.create(uri, label);
        });
    }

    @Override
    public List<KgSection> syncSectionNodes() {
        return queryNeo4jNodes("Section", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgSection.create(uri, label);
        });
    }

    @Override
    public List<KgKnowledgePoint> syncKnowledgePointNodes() {
        return queryNeo4jNodes("KnowledgePoint", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgKnowledgePoint.create(uri, label);
        });
    }

    /**
     * 通用 Neo4j 节点查询与映射方法
     * 按 URI 查询 Neo4j 中的活跃节点，并映射为对应的 Entity
     */
    private <T> List<T> queryNeo4jNodes(String label, Function<Record, T> mapper) {
        String query = String.format(
                "MATCH (n:%s) WHERE n.status IS NULL OR n.status <> 'deleted' RETURN n",
                label
        );
        List<T> results = new ArrayList<>();
        try (var session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                var result = tx.run(query);
                while (result.hasNext()) {
                    Record record = result.next();
                    try {
                        results.add(mapper.apply(record));
                    } catch (Exception e) {
                        log.warn("Failed to map Neo4j node of type {} with URI '{}': {}",
                                label, getUriSafe(record), e.getMessage());
                    }
                }
                return null;
            });
        }
        log.info("Queried {} nodes of type '{}' from Neo4j", results.size(), label);
        return results;
    }

    // ==================== 任务 3.4: Neo4j 查询层级关系并映射为关联 Entity ====================

    /**
     * 查询教材-章节关系（Textbook CONTAINS Chapter）
     */
    @Override
    public List<KgTextbookChapter> syncTextbookChapterRelations() {
        String query = """
                MATCH (t:Textbook)-[r:CONTAINS]->(c:Chapter)
                RETURN t.uri AS textbookUri, c.uri AS chapterUri, r.order_index AS orderIndex
                ORDER BY t.uri, r.order_index
                """;
        return queryNeo4jRelations(query, record -> {
            String textbookUri = record.get("textbookUri").asString();
            String chapterUri = record.get("chapterUri").asString();
            int orderIndex = record.get("orderIndex").asInt(0);
            return KgTextbookChapter.create(textbookUri, chapterUri, orderIndex);
        });
    }

    /**
     * 查询章节-小节关系（Chapter CONTAINS Section）
     */
    @Override
    public List<KgChapterSection> syncChapterSectionRelations() {
        String query = """
                MATCH (c:Chapter)-[r:CONTAINS]->(s:Section)
                RETURN c.uri AS chapterUri, s.uri AS sectionUri, r.order_index AS orderIndex
                ORDER BY c.uri, r.order_index
                """;
        return queryNeo4jRelations(query, record -> {
            String chapterUri = record.get("chapterUri").asString();
            String sectionUri = record.get("sectionUri").asString();
            int orderIndex = record.get("orderIndex").asInt(0);
            return KgChapterSection.create(chapterUri, sectionUri, orderIndex);
        });
    }

    /**
     * 查询小节-知识点关系（Section IN_UNIT KnowledgePoint 或反向）
     */
    @Override
    public List<KgSectionKP> syncSectionKPRelations() {
        String query = """
                MATCH (s:Section)-[r:HAS_KNOWLEDGE_POINT]->(kp:KnowledgePoint)
                RETURN s.uri AS sectionUri, kp.uri AS kpUri, r.order_index AS orderIndex
                ORDER BY s.uri, r.order_index
                """;
        return queryNeo4jRelations(query, record -> {
            String sectionUri = record.get("sectionUri").asString();
            String kpUri = record.get("kpUri").asString();
            int orderIndex = record.get("orderIndex").asInt(0);
            return KgSectionKP.create(sectionUri, kpUri, orderIndex);
        });
    }

    /**
     * 通用 Neo4j 关系查询与映射方法
     */
    private <T> List<T> queryNeo4jRelations(String cypherQuery, Function<Record, T> mapper) {
        List<T> results = new ArrayList<>();
        try (var session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                var result = tx.run(cypherQuery);
                while (result.hasNext()) {
                    Record record = result.next();
                    try {
                        results.add(mapper.apply(record));
                    } catch (Exception e) {
                        log.warn("Failed to map Neo4j relation: {}", e.getMessage());
                    }
                }
                return null;
            });
        }
        log.info("Queried {} relations from Neo4j", results.size());
        return results;
    }

    // ==================== 任务 3.5: UPSERT 批量写入逻辑 ====================

    /**
     * UPSERT 教材节点：按 URI 判断 INSERT 或 UPDATE
     * 使用 INSERT ... ON DUPLICATE KEY UPDATE 实现
     */
    @Override
    public int upsertTextbooks(List<KgTextbook> textbooks) {
        if (textbooks == null || textbooks.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (KgTextbook tb : textbooks) {
            KgTextbook existing = kgTextbookMapper.selectByUri(tb.getUri());
            if (existing == null) {
                kgTextbookMapper.insert(tb);
                count++;
                log.debug("Inserted textbook: {}", tb.getUri());
            } else {
                // 用 Neo4j 新数据更新 MySQL 旧数据
                existing.updateFrom(tb);
                kgTextbookMapper.updateById(existing);
                log.debug("Updated textbook: {}", tb.getUri());
            }
        }
        return count;
    }

    /**
     * UPSERT 章节节点（批量）
     */
    @Override
    public int upsertChapters(List<KgChapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return 0;
        }
        // 查询已存在的 URI 集合
        List<String> uris = chapters.stream().map(KgChapter::getUri).toList();
        List<KgChapter> existing = kgChapterMapper.selectByUris(uris);
        Set<String> existingUris = existing.stream().map(KgChapter::getUri).collect(Collectors.toSet());

        int count = 0;
        for (KgChapter ch : chapters) {
            if (!existingUris.contains(ch.getUri())) {
                kgChapterMapper.insert(ch);
                count++;
            }
            // 已存在的节点不需要重复更新（Neo4j 数据为准）
        }
        return count;
    }

    /**
     * UPSERT 小节节点（批量）
     */
    @Override
    public int upsertSections(List<KgSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return 0;
        }
        List<String> uris = sections.stream().map(KgSection::getUri).toList();
        List<KgSection> existing = kgSectionMapper.selectByUris(uris);
        Set<String> existingUris = existing.stream().map(KgSection::getUri).collect(Collectors.toSet());

        int count = 0;
        for (KgSection sec : sections) {
            if (!existingUris.contains(sec.getUri())) {
                kgSectionMapper.insert(sec);
                count++;
            }
        }
        return count;
    }

    /**
     * UPSERT 知识点节点（批量）
     */
    @Override
    public int upsertKnowledgePoints(List<KgKnowledgePoint> knowledgePoints) {
        if (knowledgePoints == null || knowledgePoints.isEmpty()) {
            return 0;
        }
        List<String> uris = knowledgePoints.stream().map(KgKnowledgePoint::getUri).toList();
        List<KgKnowledgePoint> existing = kgKnowledgePointMapper.selectByUris(uris);
        Set<String> existingUris = existing.stream().map(KgKnowledgePoint::getUri).collect(Collectors.toSet());

        int count = 0;
        for (KgKnowledgePoint kp : knowledgePoints) {
            if (!existingUris.contains(kp.getUri())) {
                kgKnowledgePointMapper.insert(kp);
                count++;
            }
        }
        return count;
    }

    // ==================== 任务 3.6: 关联表 UPSERT 同步逻辑 ====================

    /**
     * 教材-章节关联 UPSERT 同步
     * 按复合键 (textbookUri, chapterUri) 对比：
     * - MySQL 中不存在 → INSERT
     * - 两者都有但 orderIndex 不同 → UPDATE order_index
     * - MySQL 有但 Neo4j 无 → 软 DELETE
     */
    @Override
    public int rebuildTextbookChapterRelations(List<KgTextbookChapter> neo4jRelations) {
        if (neo4jRelations == null || neo4jRelations.isEmpty()) {
            int deleted = kgTextbookChapterMapper.batchDeleteAll(0L);
            log.info("No textbook-chapter relations from Neo4j, soft-deleted {} records", deleted);
            return 0;
        }

        int totalOps = 0;
        // 按 textbookUri 分组
        Map<String, List<KgTextbookChapter>> neo4jGrouped = neo4jRelations.stream()
                .collect(Collectors.groupingBy(KgTextbookChapter::getTextbookUri));

        // 获取所有 Neo4j 中的 textbookUri 集合
        Set<String> neo4jTextbookUris = neo4jGrouped.keySet();

        // 处理每个 textbookUri 组
        for (Map.Entry<String, List<KgTextbookChapter>> entry : neo4jGrouped.entrySet()) {
            String textbookUri = entry.getKey();
            List<KgTextbookChapter> neo4jGroup = entry.getValue();
            List<KgTextbookChapter> mysqlGroup = kgTextbookChapterMapper.selectByTextbookUri(textbookUri);

            // 构建 MySQL 记录的复合键 Set
            Set<String> mysqlKeys = mysqlGroup.stream()
                    .map(KgTextbookChapter::getChapterUri)
                    .collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream()
                    .map(KgTextbookChapter::getChapterUri)
                    .collect(Collectors.toSet());

            Map<String, KgTextbookChapter> mysqlByKey = mysqlGroup.stream()
                    .collect(Collectors.toMap(KgTextbookChapter::getChapterUri, r -> r));

            // 新增：Neo4j 有但 MySQL 无
            for (KgTextbookChapter neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getChapterUri())) {
                    kgTextbookChapterMapper.insert(neo4jRel);
                    totalOps++;
                    log.debug("Inserted textbook-chapter: {} -> {}", textbookUri, neo4jRel.getChapterUri());
                } else {
                    // 更新 orderIndex（如有变化）
                    KgTextbookChapter mysqlRel = mysqlByKey.get(neo4jRel.getChapterUri());
                    if (!mysqlRel.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgTextbookChapterMapper.updateOrderIndex(textbookUri, neo4jRel.getChapterUri(), neo4jRel.getOrderIndex(), 0L);
                        totalOps++;
                    }
                }
            }

            // 软删除：MySQL 有但 Neo4j 无
            for (KgTextbookChapter mysqlRel : mysqlGroup) {
                if (!neo4jKeys.contains(mysqlRel.getChapterUri())) {
                    kgTextbookChapterMapper.softDeleteRelation(textbookUri, mysqlRel.getChapterUri(), 0L);
                    totalOps++;
                    log.debug("Soft-deleted textbook-chapter: {} -> {}", textbookUri, mysqlRel.getChapterUri());
                }
            }
        }

        // MySQL 有但 Neo4j 完全没有的 textbookUri → 整个父端删除
        List<KgTextbookChapter> allExisting = kgTextbookChapterMapper.selectAllActiveRelations();
        for (KgTextbookChapter rel : allExisting) {
            if (!neo4jTextbookUris.contains(rel.getTextbookUri())) {
                kgTextbookChapterMapper.softDeleteByTextbookUri(rel.getTextbookUri(), 0L);
                totalOps++;
            }
        }

        log.info("Textbook-chapter UPSERT: {} operations", totalOps);
        return totalOps;
    }

    /**
     * 章节-小节关联 UPSERT 同步
     */
    @Override
    public int rebuildChapterSectionRelations(List<KgChapterSection> neo4jRelations) {
        if (neo4jRelations == null || neo4jRelations.isEmpty()) {
            int deleted = kgChapterSectionMapper.batchDeleteAll(0L);
            log.info("No chapter-section relations from Neo4j, soft-deleted {} records", deleted);
            return 0;
        }

        int totalOps = 0;
        Map<String, List<KgChapterSection>> neo4jGrouped = neo4jRelations.stream()
                .collect(Collectors.groupingBy(KgChapterSection::getChapterUri));
        Set<String> neo4jChapterUris = neo4jGrouped.keySet();

        for (Map.Entry<String, List<KgChapterSection>> entry : neo4jGrouped.entrySet()) {
            String chapterUri = entry.getKey();
            List<KgChapterSection> neo4jGroup = entry.getValue();
            List<KgChapterSection> mysqlGroup = kgChapterSectionMapper.selectByChapterUri(chapterUri);

            Set<String> mysqlKeys = mysqlGroup.stream().map(KgChapterSection::getSectionUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream().map(KgChapterSection::getSectionUri).collect(Collectors.toSet());
            Map<String, KgChapterSection> mysqlByKey = mysqlGroup.stream()
                    .collect(Collectors.toMap(KgChapterSection::getSectionUri, r -> r));

            for (KgChapterSection neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getSectionUri())) {
                    kgChapterSectionMapper.insert(neo4jRel);
                    totalOps++;
                } else {
                    KgChapterSection mysqlRel = mysqlByKey.get(neo4jRel.getSectionUri());
                    if (!mysqlRel.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgChapterSectionMapper.updateOrderIndex(chapterUri, neo4jRel.getSectionUri(), neo4jRel.getOrderIndex(), 0L);
                        totalOps++;
                    }
                }
            }

            for (KgChapterSection mysqlRel : mysqlGroup) {
                if (!neo4jKeys.contains(mysqlRel.getSectionUri())) {
                    kgChapterSectionMapper.softDeleteRelation(chapterUri, mysqlRel.getSectionUri(), 0L);
                    totalOps++;
                }
            }
        }

        // 删除 Neo4j 中不存在的 chapterUri
        List<KgChapterSection> allExisting = kgChapterSectionMapper.selectAllActiveRelations();
        for (KgChapterSection rel : allExisting) {
            if (!neo4jChapterUris.contains(rel.getChapterUri())) {
                kgChapterSectionMapper.softDeleteByChapterUri(rel.getChapterUri(), 0L);
                totalOps++;
            }
        }

        log.info("Chapter-section UPSERT: {} operations", totalOps);
        return totalOps;
    }

    /**
     * 小节-知识点关联 UPSERT 同步
     */
    @Override
    public int rebuildSectionKPRelations(List<KgSectionKP> neo4jRelations) {
        if (neo4jRelations == null || neo4jRelations.isEmpty()) {
            int deleted = kgSectionKPMapper.batchDeleteAll(0L);
            log.info("No section-kp relations from Neo4j, soft-deleted {} records", deleted);
            return 0;
        }

        int totalOps = 0;
        Map<String, List<KgSectionKP>> neo4jGrouped = neo4jRelations.stream()
                .collect(Collectors.groupingBy(KgSectionKP::getSectionUri));
        Set<String> neo4jSectionUris = neo4jGrouped.keySet();

        for (Map.Entry<String, List<KgSectionKP>> entry : neo4jGrouped.entrySet()) {
            String sectionUri = entry.getKey();
            List<KgSectionKP> neo4jGroup = entry.getValue();
            List<KgSectionKP> mysqlGroup = kgSectionKPMapper.selectBySectionUri(sectionUri);

            Set<String> mysqlKeys = mysqlGroup.stream().map(KgSectionKP::getKpUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream().map(KgSectionKP::getKpUri).collect(Collectors.toSet());
            Map<String, KgSectionKP> mysqlByKey = mysqlGroup.stream()
                    .collect(Collectors.toMap(KgSectionKP::getKpUri, r -> r));

            for (KgSectionKP neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getKpUri())) {
                    kgSectionKPMapper.insert(neo4jRel);
                    totalOps++;
                } else {
                    KgSectionKP mysqlRel = mysqlByKey.get(neo4jRel.getKpUri());
                    if (!mysqlRel.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgSectionKPMapper.updateOrderIndex(sectionUri, neo4jRel.getKpUri(), neo4jRel.getOrderIndex(), 0L);
                        totalOps++;
                    }
                }
            }

            for (KgSectionKP mysqlRel : mysqlGroup) {
                if (!neo4jKeys.contains(mysqlRel.getKpUri())) {
                    kgSectionKPMapper.softDeleteRelation(sectionUri, mysqlRel.getKpUri(), 0L);
                    totalOps++;
                }
            }
        }

        // 删除 Neo4j 中不存在的 sectionUri
        List<KgSectionKP> allExisting = kgSectionKPMapper.selectAllActiveRelations();
        for (KgSectionKP rel : allExisting) {
            if (!neo4jSectionUris.contains(rel.getSectionUri())) {
                kgSectionKPMapper.softDeleteBySectionUri(rel.getSectionUri(), 0L);
                totalOps++;
            }
        }

        log.info("Section-KP UPSERT: {} operations", totalOps);
        return totalOps;
    }

    // ==================== 任务 3.7: 状态变更逻辑 ====================

    /**
     * MySQL 中有但 Neo4j 中无的节点标记为 deleted
     * 支持所有 4 类节点类型
     */
    @Override
    public int markDeletedNodes(String neo4jNodeType, Set<String> neo4jUris) {
        int count = 0;
        switch (neo4jNodeType) {
            case "Textbook":
                List<KgTextbook> allTextbooks = kgTextbookMapper.selectAllActive();
                for (KgTextbook tb : allTextbooks) {
                    if (!neo4jUris.contains(tb.getUri())) {
                        kgTextbookMapper.updateStatus(tb.getUri(), "deleted", 0L);
                        count++;
                    }
                }
                break;
            case "Chapter":
                List<KgChapter> allChapters = kgChapterMapper.selectByStatus("active");
                for (KgChapter ch : allChapters) {
                    if (!neo4jUris.contains(ch.getUri())) {
                        kgChapterMapper.updateStatus(ch.getUri(), "deleted", 0L);
                        count++;
                    }
                }
                break;
            case "Section":
                List<KgSection> allSections = kgSectionMapper.selectByStatus("active");
                for (KgSection sec : allSections) {
                    if (!neo4jUris.contains(sec.getUri())) {
                        kgSectionMapper.updateStatus(sec.getUri(), "deleted", 0L);
                        count++;
                    }
                }
                break;
            case "KnowledgePoint":
                List<KgKnowledgePoint> allKps = kgKnowledgePointMapper.selectByStatus("active");
                for (KgKnowledgePoint kp : allKps) {
                    if (!neo4jUris.contains(kp.getUri())) {
                        kgKnowledgePointMapper.updateStatus(kp.getUri(), "deleted", 0L);
                        count++;
                    }
                }
                break;
        }
        if (count > 0) {
            log.info("Marked {} {} nodes as deleted (not found in Neo4j)", count, neo4jNodeType);
        }
        return count;
    }

    // ==================== 任务 3.8: 同步记录写入与更新 ====================

    /**
     * 创建同步记录
     */
    @Override
    public KgSyncRecord createSyncRecord(String syncType, String scope, Long createdBy) {
        KgSyncRecord record = KgSyncRecord.create(syncType, scope, createdBy);
        kgSyncRecordMapper.insert(record);
        return record;
    }

    /**
     * 完成同步记录（成功）
     */
    @Override
    public void completeSyncRecord(Long recordId, int insertedCount, int updatedCount,
                                   int statusChangedCount, String reconciliationStatus,
                                   String reconciliationDetails) {
        KgSyncRecord record = kgSyncRecordMapper.selectById(recordId);
        if (record != null) {
            record.completeSuccess(insertedCount, updatedCount, statusChangedCount,
                    reconciliationStatus, reconciliationDetails);
            kgSyncRecordMapper.updateById(record);
        }
    }

    /**
     * 完成同步记录（失败）
     */
    @Override
    public void failSyncRecord(Long recordId, String errorMessage) {
        KgSyncRecord record = kgSyncRecordMapper.selectById(recordId);
        if (record != null) {
            record.completeFailure(errorMessage);
            kgSyncRecordMapper.updateById(record);
        }
    }

    /**
     * 查询同步记录列表
     */
    @Override
    public List<KgSyncRecord> getSyncRecords(int limit) {
        return kgSyncRecordMapper.selectRecent(limit);
    }

    /**
     * 查询最新同步记录
     */
    @Override
    public KgSyncRecord getLatestSyncRecord() {
        List<KgSyncRecord> records = kgSyncRecordMapper.selectRecent(1);
        return records.isEmpty() ? null : records.get(0);
    }

    // ==================== 任务 3.9: URI 校验逻辑 ====================

    /**
     * 校验 URI 列表：非空、格式、重复检查
     *
     * @param uris URI 列表
     * @param nodeType 节点类型（用于错误信息）
     * @return 校验结果
     */
    public UriValidationResult validateUris(List<String> uris, String nodeType) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String uri : uris) {
            // 非空检查
            if (uri == null || uri.isBlank()) {
                errors.add(String.format("[%s] URI is null or blank", nodeType));
                continue;
            }

            // 格式检查（必须包含 neo4j:// 或为合法 URI 格式）
            if (!uri.contains(":") || uri.length() < 10) {
                errors.add(String.format("[%s] Invalid URI format: %s", nodeType, uri));
                continue;
            }

            // 重复检查
            if (seen.contains(uri)) {
                errors.add(String.format("[%s] Duplicate URI: %s", nodeType, uri));
            }
            seen.add(uri);
        }

        return new UriValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 批量校验所有节点的 URI
     */
    @Override
    public UriValidationResult validateAllUris(List<KgTextbook> textbooks, List<KgChapter> chapters,
                                               List<KgSection> sections, List<KgKnowledgePoint> kps) {
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

    // ==================== 任务 3.10: 同步对账校验 ====================

    /**
     * 同步对账校验：对比 MySQL vs Neo4j 节点数和关联数
     */
    @Override
    public ReconciliationResult reconcile(Set<String> neo4jTextbookUris, Set<String> neo4jChapterUris,
                                          Set<String> neo4jSectionUris, Set<String> neo4jKpUris,
                                          List<KgTextbookChapter> neo4jTbChRelations,
                                          List<KgChapterSection> neo4jChSecRelations,
                                          List<KgSectionKP> neo4jSecKpRelations) {
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

    // ==================== 任务 3.13: Neo4j 健康检查 ====================

    /**
     * Neo4j 健康检查：连接测试 + 响应时间
     */
    @Override
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

    // ==================== 辅助方法 ====================

    private String getUri(Record record) {
        Node node = record.get("n").asNode();
        return node.get("uri").asString();
    }

    private String getUriSafe(Record record) {
        try {
            return getUri(record);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getLabel(Record record) {
        Node node = record.get("n").asNode();
        return node.get("label").asString("");
    }

    private String getStringProperty(Record record, String property) {
        Node node = record.get("n").asNode();
        if (node.containsKey(property)) {
            return node.get(property).asString("");
        }
        return "";
    }
}
