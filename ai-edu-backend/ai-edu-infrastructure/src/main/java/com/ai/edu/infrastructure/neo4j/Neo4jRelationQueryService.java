package com.ai.edu.infrastructure.neo4j;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.model.result.GraphQueryResult;
import com.ai.edu.domain.edukg.model.result.RelatedConcept;
import com.ai.edu.domain.edukg.model.result.TextbookHierarchy;
import com.ai.edu.domain.edukg.service.KgRelationQueryDomainService;
import com.ai.edu.infrastructure.cache.Neo4jRelationCacheService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Neo4j 图谱关系查询服务
 * 含 Redis 缓存 + 降级机制
 *
 * 查询链路：
 * 1. 尝试从 Redis 缓存获取
 * 2. 缓存未命中 → 查询 Neo4j
 * 3. Neo4j 查询失败 → 降级到 MySQL 查询
 */
@Slf4j
@Service
public class Neo4jRelationQueryService implements KgRelationQueryDomainService {

    @Resource
    private Driver neo4jDriver;

    @Resource
    private Neo4jRelationCacheService cacheService;

    @Resource
    private com.ai.edu.infrastructure.persistence.repository.KgTextbookChapterRepositoryImpl textbookChapterRepo;

    @Resource
    private com.ai.edu.infrastructure.persistence.repository.KgChapterSectionRepositoryImpl chapterSectionRepo;

    @Resource
    private com.ai.edu.infrastructure.persistence.repository.KgSectionKPRepositoryImpl sectionKPRepo;

    /**
     * 查询教材-章节关联（带缓存 + 降级）
     */
    @Override
    public List<KgTextbookChapter> getTextbookChapterRelations(String textbookUri) {
        // 1. 尝试缓存
        List<KgTextbookChapter> cached = cacheService.getTextbookChapterRelations(textbookUri);
        if (cached != null) {
            log.debug("Cache hit for textbook-chapter: {}", textbookUri);
            return cached;
        }

        // 2. 查询 Neo4j
        try {
            List<KgTextbookChapter> relations = queryTextbookChapterFromNeo4j(textbookUri);
            cacheService.setTextbookChapterRelations(textbookUri, relations);
            return relations;
        } catch (Exception e) {
            log.warn("Neo4j query failed, falling back to MySQL: {}", e.getMessage());
            // 3. 降级到 MySQL
            return textbookChapterRepo.findByTextbookUri(textbookUri);
        }
    }

    /**
     * 查询章节-小节关联（带缓存 + 降级）
     */
    @Override
    public List<KgChapterSection> getChapterSectionRelations(String chapterUri) {
        List<KgChapterSection> cached = cacheService.getChapterSectionRelations(chapterUri);
        if (cached != null) {
            log.debug("Cache hit for chapter-section: {}", chapterUri);
            return cached;
        }

        try {
            List<KgChapterSection> relations = queryChapterSectionFromNeo4j(chapterUri);
            cacheService.setChapterSectionRelations(chapterUri, relations);
            return relations;
        } catch (Exception e) {
            log.warn("Neo4j query failed, falling back to MySQL: {}", e.getMessage());
            return chapterSectionRepo.findByChapterUri(chapterUri);
        }
    }

    /**
     * 查询小节-知识点关联（带缓存 + 降级）
     */
    @Override
    public List<KgSectionKP> getSectionKPRelations(String sectionUri) {
        List<KgSectionKP> cached = cacheService.getSectionKPRelations(sectionUri);
        if (cached != null) {
            log.debug("Cache hit for section-kp: {}", sectionUri);
            return cached;
        }

        try {
            List<KgSectionKP> relations = querySectionKPFromNeo4j(sectionUri);
            cacheService.setSectionKPRelations(sectionUri, relations);
            return relations;
        } catch (Exception e) {
            log.warn("Neo4j query failed, falling back to MySQL: {}", e.getMessage());
            return sectionKPRepo.findBySectionUri(sectionUri);
        }
    }

    // ==================== Neo4j 查询方法 ====================

    private List<KgTextbookChapter> queryTextbookChapterFromNeo4j(String textbookUri) {
        String query = """
                MATCH (t:Textbook {uri: $textbookUri})-[r:CONTAINS]->(c:Chapter)
                RETURN t.uri AS textbookUri, c.uri AS chapterUri, r.order_index AS orderIndex
                ORDER BY r.order_index
                """;
        return queryNeo4jRelations(query, Map.entry("textbookUri", textbookUri),
                record -> {
                    String tbUri = record.get("textbookUri").asString();
                    String chUri = record.get("chapterUri").asString();
                    int order = record.get("orderIndex").asInt(0);
                    return KgTextbookChapter.create(tbUri, chUri, order);
                });
    }

    private List<KgChapterSection> queryChapterSectionFromNeo4j(String chapterUri) {
        String query = """
                MATCH (c:Chapter {uri: $chapterUri})-[r:CONTAINS]->(s:Section)
                RETURN c.uri AS chapterUri, s.uri AS sectionUri, r.order_index AS orderIndex
                ORDER BY r.order_index
                """;
        return queryNeo4jRelations(query, Map.entry("chapterUri", chapterUri),
                record -> {
                    String chUri = record.get("chapterUri").asString();
                    String secUri = record.get("sectionUri").asString();
                    int order = record.get("orderIndex").asInt(0);
                    return KgChapterSection.create(chUri, secUri, order);
                });
    }

    private List<KgSectionKP> querySectionKPFromNeo4j(String sectionUri) {
        String query = """
                MATCH (s:Section {uri: $sectionUri})-[r:HAS_KNOWLEDGE_POINT]->(kp:KnowledgePoint)
                RETURN s.uri AS sectionUri, kp.uri AS kpUri, r.order_index AS orderIndex
                ORDER BY r.order_index
                """;
        return queryNeo4jRelations(query, Map.entry("sectionUri", sectionUri),
                record -> {
                    String secUri = record.get("sectionUri").asString();
                    String kpUri = record.get("kpUri").asString();
                    int order = record.get("orderIndex").asInt(0);
                    return KgSectionKP.create(secUri, kpUri, order);
                });
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> queryNeo4jRelations(String cypherQuery,
                                            java.util.Map.Entry<String, String> param,
                                            java.util.function.Function<org.neo4j.driver.Record, T> mapper) {
        List<T> results = new java.util.ArrayList<>();
        try (var session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                var result = tx.run(cypherQuery,
                        org.neo4j.driver.Values.parameters(param.getKey(), param.getValue()));
                while (result.hasNext()) {
                    results.add(mapper.apply(result.next()));
                }
                return null;
            });
        }
        return results;
    }

    /**
     * 查询知识点的图谱数据（层级路径 + 关联概念）
     */
    @Override
    public GraphQueryResult queryGraphForKnowledgePoint(String kpUri) {
        String query = """
                MATCH (kp:KnowledgePoint {uri: $kpUri})
                OPTIONAL MATCH (s:Section)-[:HAS_KNOWLEDGE_POINT]->(kp)
                OPTIONAL MATCH (c:Chapter)-[:CONTAINS]->(s)
                OPTIONAL MATCH (t:Textbook)-[:CONTAINS]->(c)
                OPTIONAL MATCH (kp)-[:MATCHES_KG]->(concept:Concept)
                RETURN t.uri AS textbookUri, t.label AS textbookLabel,
                       c.uri AS chapterUri, c.label AS chapterLabel,
                       s.uri AS sectionUri, s.label AS sectionLabel,
                       concept.uri AS conceptUri, concept.label AS conceptLabel,
                       kp.uri AS kpUri, kp.label AS kpLabel,
                       kp.difficulty AS kpDifficulty, kp.cognitive_level AS kpCognitiveLevel
                """;

        Set<TextbookHierarchy> hierarchies = new LinkedHashSet<>();
        Set<RelatedConcept> concepts = new LinkedHashSet<>();
        String kpLabel = null, kpDifficulty = null, kpCognitiveLevel = null;

        try (var session = neo4jDriver.session()) {
            var result = session.readTransaction(tx -> {
                var r = tx.run(query, org.neo4j.driver.Values.parameters("kpUri", kpUri));
                return r.list();
            });

            for (var record : result) {
                kpLabel = record.get("kpLabel").isNull() ? null : record.get("kpLabel").asString();
                kpDifficulty = record.get("kpDifficulty").isNull() ? null : record.get("kpDifficulty").asString();
                kpCognitiveLevel = record.get("kpCognitiveLevel").isNull() ? null : record.get("kpCognitiveLevel").asString();

                String tbUri = record.get("textbookUri").isNull() ? null : record.get("textbookUri").asString();
                String tbLabel = record.get("textbookLabel").isNull() ? null : record.get("textbookLabel").asString();
                String chUri = record.get("chapterUri").isNull() ? null : record.get("chapterUri").asString();
                String chLabel = record.get("chapterLabel").isNull() ? null : record.get("chapterLabel").asString();
                String secUri = record.get("sectionUri").isNull() ? null : record.get("sectionUri").asString();
                String secLabel = record.get("sectionLabel").isNull() ? null : record.get("sectionLabel").asString();
                String cUri = record.get("conceptUri").isNull() ? null : record.get("conceptUri").asString();
                String cLabel = record.get("conceptLabel").isNull() ? null : record.get("conceptLabel").asString();

                // 收集层级路径
                if (secUri != null) {
                    hierarchies.add(new TextbookHierarchy(
                            tbUri, tbLabel, chUri, chLabel, secUri, secLabel));
                }

                // 收集关联概念
                if (cUri != null) {
                    concepts.add(new RelatedConcept(cUri, cLabel));
                }
            }
        } catch (Exception e) {
            log.warn("Neo4j query failed for graph query of {}: {}", kpUri, e.getMessage());
        }

        return new GraphQueryResult(kpUri, kpLabel, kpDifficulty, kpCognitiveLevel, null,
                new ArrayList<>(hierarchies), new ArrayList<>(concepts));
    }
}
