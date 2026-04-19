package com.ai.edu.infrastructure.persistence.neo4j;

import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.repository.Neo4jNodeRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Neo4j 节点读取仓储实现
 *
 * 负责：从 Neo4j 查询节点并映射为领域实体
 */
@Slf4j
@Repository
public class Neo4jNodeRepositoryImpl implements Neo4jNodeRepository {

    @Resource
    private Driver neo4jDriver;

    @Override
    public List<KgTextbook> findTextbooks(String edition, String subject, String stage, String grade) {
        String query = """
                MATCH (t:Textbook)
                WHERE t.edition = $edition AND t.subject = $subject
                AND ($stage IS NULL OR t.stage = $stage)
                AND ($grade IS NULL OR t.grade = $grade)
                RETURN t
                """;
        return queryNeo4jNodes(query, parameters("edition", edition, "subject", subject,
                "stage", stage, "grade", grade), record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            String g = getStringProperty(record, "grade");
            String s = getStringProperty(record, "stage");
            String e = getStringProperty(record, "edition");
            String sub = getStringProperty(record, "subject");
            return KgTextbook.create(uri, label, g, s, e, sub);
        });
    }

    @Override
    public List<KgChapter> findChaptersByTextbookUris(List<String> textbookUris) {
        String query = """
                MATCH (t:Textbook)-[:CONTAINS]->(c:Chapter)
                WHERE t.uri IN $uris
                RETURN c
                """;
        return queryNeo4jNodes(query, parameters("uris", textbookUris), record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgChapter.create(uri, label);
        });
    }

    @Override
    public List<KgSection> findSectionsByTextbookUris(List<String> textbookUris) {
        String query = """
                MATCH (t:Textbook)-[:CONTAINS]->(:Chapter)-[:CONTAINS]->(s:Section)
                WHERE t.uri IN $uris
                RETURN s
                """;
        return queryNeo4jNodes(query, parameters("uris", textbookUris), record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgSection.create(uri, label);
        });
    }

    @Override
    public List<KgKnowledgePoint> findKnowledgePointsByTextbookUris(List<String> textbookUris) {
        String query = """
                MATCH (t:Textbook)-[:CONTAINS]->(:Chapter)-[:CONTAINS]->(:Section)-[:HAS_KNOWLEDGE_POINT]->(kp:KnowledgePoint)
                WHERE t.uri IN $uris
                RETURN kp
                """;
        return queryNeo4jNodes(query, parameters("uris", textbookUris), record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgKnowledgePoint.create(uri, label);
        });
    }

    private <T> List<T> queryNeo4jNodes(String cypherQuery, Map<String, Object> params, Function<Record, T> mapper) {
        List<T> results = new ArrayList<>();
        try (var session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                var result = tx.run(cypherQuery, params);
                while (result.hasNext()) {
                    Record record = result.next();
                    try {
                        results.add(mapper.apply(record));
                    } catch (Exception e) {
                        log.warn("Failed to map Neo4j node: {}", e.getMessage());
                    }
                }
                return null;
            });
        }
        log.info("Queried {} nodes from Neo4j", results.size());
        return results;
    }

    private static Map<String, Object> parameters(Object... keyValues) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            params.put((String) keyValues[i], keyValues[i + 1]);
        }
        return params;
    }

    // ==================== 辅助方法 ====================

    private String getUri(Record record) {
        Node node = record.get(0).asNode();
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
        Node node = record.get(0).asNode();
        return node.get("label").asString("");
    }

    private String getStringProperty(Record record, String property) {
        Node node = record.get(0).asNode();
        if (node.containsKey(property)) {
            return node.get(property).asString("");
        }
        return "";
    }
}
