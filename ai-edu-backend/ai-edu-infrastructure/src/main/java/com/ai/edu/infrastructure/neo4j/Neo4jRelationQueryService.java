package com.ai.edu.infrastructure.neo4j;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.service.KgRelationQueryDomainService;
import com.ai.edu.infrastructure.cache.Neo4jRelationCacheService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
}
