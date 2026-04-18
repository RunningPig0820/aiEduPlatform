package com.ai.edu.infrastructure.persistence.neo4j;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.service.KgRelationSyncService;
import com.ai.edu.infrastructure.persistence.edukg.mapper.*;
import com.ai.edu.infrastructure.persistence.edukg.po.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Neo4j 关系同步服务
 *
 * 负责：Neo4j 关系查询 + MySQL 关联表 UPSERT（按父端分组对比）
 */
@Slf4j
@Service
public class Neo4jRelationSyncService implements KgRelationSyncService {

    @Resource
    private Driver neo4jDriver;

    @Resource
    private KgTextbookChapterMapper kgTextbookChapterMapper;

    @Resource
    private KgChapterSectionMapper kgChapterSectionMapper;

    @Resource
    private KgSectionKPMapper kgSectionKPMapper;

    // ==================== Neo4j 关系查询 ====================

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

    // ==================== MySQL 关联表 UPSERT ====================

    /**
     * 教材-章节关联 UPSERT：按复合键 (textbookUri, chapterUri) 对比
     */
    public int rebuildTextbookChapterRelations(List<KgTextbookChapter> neo4jRelations) {
        if (neo4jRelations == null || neo4jRelations.isEmpty()) {
            int deleted = kgTextbookChapterMapper.batchDeleteAll(0L);
            log.info("No textbook-chapter relations from Neo4j, soft-deleted {} records", deleted);
            return 0;
        }

        int totalOps = 0;
        Map<String, List<KgTextbookChapter>> neo4jGrouped = neo4jRelations.stream()
                .collect(Collectors.groupingBy(KgTextbookChapter::getTextbookUri));
        Set<String> neo4jParentUris = neo4jGrouped.keySet();

        for (Map.Entry<String, List<KgTextbookChapter>> entry : neo4jGrouped.entrySet()) {
            String textbookUri = entry.getKey();
            List<KgTextbookChapter> neo4jGroup = entry.getValue();
            List<KgTextbookChapterPo> mysqlGroupPos = kgTextbookChapterMapper.selectByTextbookUri(textbookUri);

            Set<String> mysqlKeys = mysqlGroupPos.stream().map(KgTextbookChapterPo::getChapterUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream().map(KgTextbookChapter::getChapterUri).collect(Collectors.toSet());
            Map<String, KgTextbookChapterPo> mysqlPoByKey = mysqlGroupPos.stream()
                    .collect(Collectors.toMap(KgTextbookChapterPo::getChapterUri, r -> r));

            for (KgTextbookChapter neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getChapterUri())) {
                    kgTextbookChapterMapper.insert(KgTextbookChapterPo.from(neo4jRel));
                    totalOps++;
                } else {
                    KgTextbookChapterPo mysqlPo = mysqlPoByKey.get(neo4jRel.getChapterUri());
                    if (!mysqlPo.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgTextbookChapterMapper.updateOrderIndex(textbookUri, neo4jRel.getChapterUri(), neo4jRel.getOrderIndex(), 0L);
                        totalOps++;
                    }
                }
            }

            for (KgTextbookChapterPo mysqlPo : mysqlGroupPos) {
                if (!neo4jKeys.contains(mysqlPo.getChapterUri())) {
                    kgTextbookChapterMapper.softDeleteRelation(textbookUri, mysqlPo.getChapterUri(), 0L);
                    totalOps++;
                }
            }
        }

        List<KgTextbookChapterPo> allExistingPos = kgTextbookChapterMapper.selectAllActiveRelations();
        for (KgTextbookChapterPo po : allExistingPos) {
            if (!neo4jParentUris.contains(po.getTextbookUri())) {
                kgTextbookChapterMapper.softDeleteByTextbookUri(po.getTextbookUri(), 0L);
                totalOps++;
            }
        }

        log.info("Textbook-chapter UPSERT: {} operations", totalOps);
        return totalOps;
    }

    /**
     * 章节-小节关联 UPSERT
     */
    public int rebuildChapterSectionRelations(List<KgChapterSection> neo4jRelations) {
        if (neo4jRelations == null || neo4jRelations.isEmpty()) {
            int deleted = kgChapterSectionMapper.batchDeleteAll(0L);
            log.info("No chapter-section relations from Neo4j, soft-deleted {} records", deleted);
            return 0;
        }

        int totalOps = 0;
        Map<String, List<KgChapterSection>> neo4jGrouped = neo4jRelations.stream()
                .collect(Collectors.groupingBy(KgChapterSection::getChapterUri));
        Set<String> neo4jParentUris = neo4jGrouped.keySet();

        for (Map.Entry<String, List<KgChapterSection>> entry : neo4jGrouped.entrySet()) {
            String chapterUri = entry.getKey();
            List<KgChapterSection> neo4jGroup = entry.getValue();
            List<KgChapterSectionPo> mysqlGroupPos = kgChapterSectionMapper.selectByChapterUri(chapterUri);

            Set<String> mysqlKeys = mysqlGroupPos.stream().map(KgChapterSectionPo::getSectionUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream().map(KgChapterSection::getSectionUri).collect(Collectors.toSet());
            Map<String, KgChapterSectionPo> mysqlPoByKey = mysqlGroupPos.stream()
                    .collect(Collectors.toMap(KgChapterSectionPo::getSectionUri, r -> r));

            for (KgChapterSection neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getSectionUri())) {
                    kgChapterSectionMapper.insert(KgChapterSectionPo.from(neo4jRel));
                    totalOps++;
                } else {
                    KgChapterSectionPo mysqlPo = mysqlPoByKey.get(neo4jRel.getSectionUri());
                    if (!mysqlPo.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgChapterSectionMapper.updateOrderIndex(chapterUri, neo4jRel.getSectionUri(), neo4jRel.getOrderIndex(), 0L);
                        totalOps++;
                    }
                }
            }

            for (KgChapterSectionPo mysqlPo : mysqlGroupPos) {
                if (!neo4jKeys.contains(mysqlPo.getSectionUri())) {
                    kgChapterSectionMapper.softDeleteRelation(chapterUri, mysqlPo.getSectionUri(), 0L);
                    totalOps++;
                }
            }
        }

        List<KgChapterSectionPo> allExistingPos = kgChapterSectionMapper.selectAllActiveRelations();
        for (KgChapterSectionPo po : allExistingPos) {
            if (!neo4jParentUris.contains(po.getChapterUri())) {
                kgChapterSectionMapper.softDeleteByChapterUri(po.getChapterUri(), 0L);
                totalOps++;
            }
        }

        log.info("Chapter-section UPSERT: {} operations", totalOps);
        return totalOps;
    }

    /**
     * 小节-知识点关联 UPSERT
     */
    public int rebuildSectionKPRelations(List<KgSectionKP> neo4jRelations) {
        if (neo4jRelations == null || neo4jRelations.isEmpty()) {
            int deleted = kgSectionKPMapper.batchDeleteAll(0L);
            log.info("No section-kp relations from Neo4j, soft-deleted {} records", deleted);
            return 0;
        }

        int totalOps = 0;
        Map<String, List<KgSectionKP>> neo4jGrouped = neo4jRelations.stream()
                .collect(Collectors.groupingBy(KgSectionKP::getSectionUri));
        Set<String> neo4jParentUris = neo4jGrouped.keySet();

        for (Map.Entry<String, List<KgSectionKP>> entry : neo4jGrouped.entrySet()) {
            String sectionUri = entry.getKey();
            List<KgSectionKP> neo4jGroup = entry.getValue();
            List<KgSectionKPPo> mysqlGroupPos = kgSectionKPMapper.selectBySectionUri(sectionUri);

            Set<String> mysqlKeys = mysqlGroupPos.stream().map(KgSectionKPPo::getKpUri).collect(Collectors.toSet());
            Set<String> neo4jKeys = neo4jGroup.stream().map(KgSectionKP::getKpUri).collect(Collectors.toSet());
            Map<String, KgSectionKPPo> mysqlPoByKey = mysqlGroupPos.stream()
                    .collect(Collectors.toMap(KgSectionKPPo::getKpUri, r -> r));

            for (KgSectionKP neo4jRel : neo4jGroup) {
                if (!mysqlKeys.contains(neo4jRel.getKpUri())) {
                    kgSectionKPMapper.insert(KgSectionKPPo.from(neo4jRel));
                    totalOps++;
                } else {
                    KgSectionKPPo mysqlPo = mysqlPoByKey.get(neo4jRel.getKpUri());
                    if (!mysqlPo.getOrderIndex().equals(neo4jRel.getOrderIndex())) {
                        kgSectionKPMapper.updateOrderIndex(sectionUri, neo4jRel.getKpUri(), neo4jRel.getOrderIndex(), 0L);
                        totalOps++;
                    }
                }
            }

            for (KgSectionKPPo mysqlPo : mysqlGroupPos) {
                if (!neo4jKeys.contains(mysqlPo.getKpUri())) {
                    kgSectionKPMapper.softDeleteRelation(sectionUri, mysqlPo.getKpUri(), 0L);
                    totalOps++;
                }
            }
        }

        List<KgSectionKPPo> allExistingPos = kgSectionKPMapper.selectAllActiveRelations();
        for (KgSectionKPPo po : allExistingPos) {
            if (!neo4jParentUris.contains(po.getSectionUri())) {
                kgSectionKPMapper.softDeleteBySectionUri(po.getSectionUri(), 0L);
                totalOps++;
            }
        }

        log.info("Section-KP UPSERT: {} operations", totalOps);
        return totalOps;
    }
}
