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
import java.util.List;
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

    // ==================== Neo4j 节点查询 ====================

    @Override
    public List<KgTextbook> findAllTextbooks() {
        return queryNeo4jNodes("Textbook", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            String grade = getStringProperty(record, "grade");
            String stage = getStringProperty(record, "stage");
            String edition = getStringProperty(record, "edition");
            String subject = getStringProperty(record, "subject");
            return KgTextbook.create(uri, label, grade, stage, edition, subject);
        });
    }

    @Override
    public List<KgChapter> findAllChapters() {
        return queryNeo4jNodes("Chapter", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgChapter.create(uri, label);
        });
    }

    @Override
    public List<KgSection> findAllSections() {
        return queryNeo4jNodes("Section", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgSection.create(uri, label);
        });
    }

    @Override
    public List<KgKnowledgePoint> findAllKnowledgePoints() {
        return queryNeo4jNodes("KnowledgePoint", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgKnowledgePoint.create(uri, label);
        });
    }

    private <T> List<T> queryNeo4jNodes(String label, Function<Record, T> mapper) {
        String query = String.format("MATCH (n:%s) RETURN n", label);
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
