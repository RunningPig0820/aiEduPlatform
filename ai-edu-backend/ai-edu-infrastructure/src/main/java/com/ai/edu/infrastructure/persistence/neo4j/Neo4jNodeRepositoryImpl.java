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
import java.util.HashMap;
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
        // edition 和 subject 必须传入，否则返回空列表
        if (edition == null || edition.isBlank() || subject == null || subject.isBlank()) {
            log.warn("findTextbooks: edition and subject are required, returning empty list");
            return List.of();
        }

        // 动态构建 Cypher 查询
        StringBuilder cypher = new StringBuilder("MATCH (t:Textbook) WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        addCondition(cypher, params, "t.edition = $edition", edition);
        addCondition(cypher, params, "t.subject = $subject", subject);
        addCondition(cypher, params, "t.stage = $stage", stage);
        addCondition(cypher, params, "t.grade = $grade", grade);

        cypher.append(" RETURN t");

        log.info("findTextbooks: Cypher={}, params={}", cypher, params);

        return queryNeo4jNodes(cypher.toString(), params, record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            String g = getStringProperty(record, "grade");
            String s = getStringProperty(record, "stage");
            String e = getStringProperty(record, "edition");
            String sub = getStringProperty(record, "subject");
            log.info("Neo4j Textbook mapped: uri={}, edition={}, stage={}, grade={}, subject={}", uri, e, s, g, sub);
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
        // Neo4j 中关系方向是 (TextbookKP)-[:IN_UNIT]->(Section)
        String query = """
                MATCH (kp:TextbookKP)-[:IN_UNIT]->(:Section)<-[:CONTAINS]-(:Chapter)<-[:CONTAINS]-(t:Textbook)
                WHERE t.uri IN $uris
                RETURN kp
                """;
        return queryNeo4jNodes(query, parameters("uris", textbookUris), record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgKnowledgePoint.create(uri, label);
        });
    }

    @Override
    public List<String> findDistinctGrades(String edition, String subject) {
        // edition 和 subject 必须传入，否则返回空列表
        if (edition == null || edition.isBlank() || subject == null || subject.isBlank()) {
            log.warn("findDistinctGrades: edition and subject are required, returning empty list");
            return List.of();
        }

        List<String> grades = new ArrayList<>();
        StringBuilder cypher = new StringBuilder("MATCH (t:Textbook) WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        addCondition(cypher, params, "t.edition = $edition", edition);
        addCondition(cypher, params, "t.subject = $subject", subject);

        cypher.append(" RETURN DISTINCT t.grade AS grade ORDER BY grade");

        try (var session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                var result = tx.run(cypher.toString(), params);
                while (result.hasNext()) {
                    Record record = result.next();
                    if (!record.get("grade").isNull()) {
                        grades.add(record.get("grade").asString(""));
                    }
                }
                return null;
            });
        }
        log.info("Queried {} distinct grades from Neo4j for edition={}, subject={}",
                grades.size(), edition, subject);
        return grades;
    }

    private <T> List<T> queryNeo4jNodes(String cypherQuery, Map<String, Object> params, Function<Record, T> mapper) {
        List<T> results = new ArrayList<>();
        log.info("Executing Neo4j query: {}, params: {}", cypherQuery, params);
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
        log.info("Neo4j query returned {} nodes", results.size());
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

    /**
     * 通用工具方法：参数非空时，才拼接 Cypher 条件 + 放入参数
     */
    private void addCondition(StringBuilder cypher, Map<String, Object> params, String condition, String value) {
        if (value == null || value.isBlank()) return;

        cypher.append(" AND ").append(condition);
        // 从条件中提取参数名，如 "t.edition = $edition" → "edition"
        String paramKey = condition.split(" = ")[1].replace("$", "");
        params.put(paramKey, value);
    }
}
