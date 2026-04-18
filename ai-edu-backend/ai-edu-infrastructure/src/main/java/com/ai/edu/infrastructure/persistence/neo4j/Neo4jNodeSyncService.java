package com.ai.edu.infrastructure.persistence.neo4j;

import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.service.KgNodeSyncService;
import com.ai.edu.infrastructure.persistence.edukg.mapper.*;
import com.ai.edu.infrastructure.persistence.edukg.po.*;
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
 * Neo4j 节点同步服务
 *
 * 负责：Neo4j 节点查询 + MySQL 节点 UPSERT
 */
@Slf4j
@Service
public class Neo4jNodeSyncService implements KgNodeSyncService {

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

    // ==================== Neo4j 节点查询 ====================

    public List<KgTextbook> syncTextbookNodes() {
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

    public List<KgChapter> syncChapterNodes() {
        return queryNeo4jNodes("Chapter", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgChapter.create(uri, label);
        });
    }

    public List<KgSection> syncSectionNodes() {
        return queryNeo4jNodes("Section", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgSection.create(uri, label);
        });
    }

    public List<KgKnowledgePoint> syncKnowledgePointNodes() {
        return queryNeo4jNodes("KnowledgePoint", record -> {
            String uri = getUri(record);
            String label = getLabel(record);
            return KgKnowledgePoint.create(uri, label);
        });
    }

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

    // ==================== 标记删除 ====================

    public int markDeletedNodes(String neo4jNodeType, Set<String> neo4jUris) {
        int count = 0;
        switch (neo4jNodeType) {
            case "Textbook":
                List<KgTextbookPo> allTextbookPos = kgTextbookMapper.selectAllActive();
                for (KgTextbookPo po : allTextbookPos) {
                    if (!neo4jUris.contains(po.getUri())) {
                        kgTextbookMapper.updateStatus(po.getUri(), "deleted", 0L);
                        count++;
                    }
                }
                break;
            case "Chapter":
                List<KgChapterPo> allChapterPos = kgChapterMapper.selectByStatus("active");
                for (KgChapterPo po : allChapterPos) {
                    if (!neo4jUris.contains(po.getUri())) {
                        kgChapterMapper.updateStatus(po.getUri(), "deleted", 0L);
                        count++;
                    }
                }
                break;
            case "Section":
                List<KgSectionPo> allSectionPos = kgSectionMapper.selectByStatus("active");
                for (KgSectionPo po : allSectionPos) {
                    if (!neo4jUris.contains(po.getUri())) {
                        kgSectionMapper.updateStatus(po.getUri(), "deleted", 0L);
                        count++;
                    }
                }
                break;
            case "KnowledgePoint":
                List<KgKnowledgePointPo> allKpPos = kgKnowledgePointMapper.selectByStatus("active");
                for (KgKnowledgePointPo po : allKpPos) {
                    if (!neo4jUris.contains(po.getUri())) {
                        kgKnowledgePointMapper.updateStatus(po.getUri(), "deleted", 0L);
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
