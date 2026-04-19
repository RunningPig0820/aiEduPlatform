package com.ai.edu.infrastructure.persistence.neo4j;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.repository.Neo4jRelationRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Neo4j 关系读取仓储实现
 *
 * 负责：从 Neo4j 查询关系并映射为领域实体
 */
@Slf4j
@Repository
public class Neo4jRelationRepositoryImpl implements Neo4jRelationRepository {

    @Resource
    private Driver neo4jDriver;

    @Override
    public List<KgTextbookChapter> findTextbookChapterRelations() {
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

    @Override
    public List<KgChapterSection> findChapterSectionRelations() {
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

    @Override
    public List<KgSectionKP> findSectionKPRelations() {
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
}
