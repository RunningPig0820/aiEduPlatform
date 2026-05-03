package com.ai.edu.application.service.kg;

import com.ai.edu.application.assembler.KgConvert;
import com.ai.edu.application.dto.kg.BatchRelationsDTO;
import com.ai.edu.application.dto.kg.HealthDTO;
import com.ai.edu.application.dto.kg.KgGraphDTO;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.model.result.ExpandRelationResult;
import com.ai.edu.domain.edukg.model.result.GraphQueryResult;
import com.ai.edu.domain.edukg.model.result.RelatedConcept;
import com.ai.edu.domain.edukg.model.result.TextbookHierarchy;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.edukg.repository.KgKnowledgeGraphQueryRepository;
import com.ai.edu.domain.shared.service.Neo4jHealthChecker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Neo4j 查询应用服务
 * 职责：图谱关系查询、健康检查、批量关联（含 Redis 缓存 + 降级）
 */
@Slf4j
@Service
public class KgNeo4jService {

    @Resource
    private Neo4jHealthChecker neo4jHealthChecker;

    @Resource
    private KgKnowledgeGraphQueryRepository kgKnowledgeGraphQueryRepository;

    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;


    /**
     * Neo4j 健康检查
     */
    public HealthDTO getNeo4jHealth() {
        long startTime = System.currentTimeMillis();
        boolean healthy = neo4jHealthChecker.isConnected();
        long responseTime = System.currentTimeMillis() - startTime;
        String message = healthy ? "Neo4j is healthy" : "Neo4j connection failed";
        return KgConvert.toHealthDTO(healthy, responseTime, message);
    }

    /**
     * 批量获取概念关联
     */
    public BatchRelationsDTO batchGetConceptRelations(List<String> uris) {
        Map<String, List<String>> uriToRelated = new HashMap<>();

        for (String uri : uris) {
            try {
                List<String> related = new ArrayList<>();

                // 查询教材-章节关联
                kgKnowledgeGraphQueryRepository.getTextbookChapterRelations(uri).forEach(rel ->
                        related.add(rel.getChapterUri()));

                // 查询章节-小节关联
                kgKnowledgeGraphQueryRepository.getChapterSectionRelations(uri).forEach(rel ->
                        related.add(rel.getSectionUri()));

                // 查询小节-知识点关联
                kgKnowledgeGraphQueryRepository.getSectionKPRelations(uri).forEach(rel ->
                        related.add(rel.getKpUri()));

                uriToRelated.put(uri, related);
            } catch (Exception e) {
                log.warn("Failed to query relations for URI {}: {}", uri, e.getMessage());
                uriToRelated.put(uri, List.of());
            }
        }

        return KgConvert.toBatchRelationsDTO(uriToRelated);
    }

    /**
     * 获取知识点的图谱数据（用于前端图谱可视化）
     *
     * 以 Neo4j 为主要数据源，MySQL 作为元数据补充。
     * 图结构（层级链 + 关联概念）完全来自 Neo4j。
     */
    public KgGraphDTO getKnowledgePointGraph(String kpUri) {
        // 1. 从 Neo4j 获取图数据
        GraphQueryResult graphData = kgKnowledgeGraphQueryRepository.queryGraphForKnowledgePoint(kpUri);

        // 2. 尝试 MySQL 补充元数据（difficulty / importance / cognitiveLevel 可能更丰富）
        Optional<KgKnowledgePoint> kpOpt = kgKnowledgePointRepository.findByUri(kpUri);

        // 3. 确定 KP 的元数据：优先 MySQL，回退 Neo4j
        String kpLabel;
        String difficulty;
        String cognitiveLevel;
        String importance;

        if (kpOpt.isPresent()) {
            KgKnowledgePoint kp = kpOpt.get();
            kpLabel = kp.getLabel();
            difficulty = kp.getDifficulty();
            cognitiveLevel = kp.getCognitiveLevel();
            importance = kp.getImportance();
        } else if (graphData.kpLabel() != null) {
            kpLabel = graphData.kpLabel();
            difficulty = graphData.kpDifficulty();
            cognitiveLevel = graphData.kpCognitiveLevel();
            importance = graphData.kpImportance();
        } else {
            return KgGraphDTO.builder().nodes(List.of()).edges(List.of()).build();
        }

        // 回退缺失字段
        if (kpLabel == null) kpLabel = graphData.kpLabel();
        if (difficulty == null) difficulty = graphData.kpDifficulty();
        if (cognitiveLevel == null) cognitiveLevel = graphData.kpCognitiveLevel();
        if (importance == null) importance = graphData.kpImportance();

        List<KgGraphDTO.GraphNode> nodes = new ArrayList<>();
        List<KgGraphDTO.GraphEdge> edges = new ArrayList<>();
        Map<String, Boolean> seenNodes = new LinkedHashMap<>();

        // 添加知识点自身节点
        Map<String, Object> kpData = new LinkedHashMap<>();
        kpData.put("uri", kpUri);
        kpData.put("name", kpLabel);
        kpData.put("difficulty", difficulty);
        kpData.put("cognitiveLevel", cognitiveLevel);
        kpData.put("importance", importance);

        nodes.add(KgGraphDTO.GraphNode.builder()
                .id(kpUri)
                .type("textbook_kp")
                .label(kpLabel)
                .data(kpData)
                .build());
        seenNodes.put(kpUri, true);

        // 处理教材层级路径：Textbook → Chapter → Section → KP
        // Neo4j 方向：(KP)-[:IN_UNIT]->(Section), (Chapter)-[:CONTAINS]->(Section), (Textbook)-[:CONTAINS]->(Chapter)
        for (TextbookHierarchy hierarchy : graphData.hierarchies()) {
            // 边：KP → Section  (Neo4j: (KP)-[IN_UNIT]->(Section))
            if (hierarchy.sectionUri() != null) {
                if (!seenNodes.containsKey(hierarchy.sectionUri())) {
                    nodes.add(KgGraphDTO.GraphNode.builder()
                            .id(hierarchy.sectionUri())
                            .type("section")
                            .label(hierarchy.sectionLabel())
                            .data(Map.of("uri", hierarchy.sectionUri(), "name", hierarchy.sectionLabel()))
                            .build());
                    seenNodes.put(hierarchy.sectionUri(), true);
                }
                edges.add(KgGraphDTO.GraphEdge.builder()
                        .id(kpUri + "→IN_UNIT→" + hierarchy.sectionUri())
                        .source(kpUri)
                        .target(hierarchy.sectionUri())
                        .label("IN_UNIT")
                        .build());
            }

            // 边：Chapter → Section  (Neo4j: (Chapter)-[CONTAINS]->(Section))
            if (hierarchy.chapterUri() != null) {
                if (!seenNodes.containsKey(hierarchy.chapterUri())) {
                    nodes.add(KgGraphDTO.GraphNode.builder()
                            .id(hierarchy.chapterUri())
                            .type("chapter")
                            .label(hierarchy.chapterLabel())
                            .data(Map.of("uri", hierarchy.chapterUri(), "name", hierarchy.chapterLabel()))
                            .build());
                    seenNodes.put(hierarchy.chapterUri(), true);
                }
                if (hierarchy.sectionUri() != null) {
                    edges.add(KgGraphDTO.GraphEdge.builder()
                            .id(hierarchy.chapterUri() + "→CONTAINS→" + hierarchy.sectionUri())
                            .source(hierarchy.chapterUri())
                            .target(hierarchy.sectionUri())
                            .label("CONTAINS")
                            .build());
                }
            }

            // 边：Textbook → Chapter  (Neo4j: (Textbook)-[CONTAINS]->(Chapter))
            if (hierarchy.textbookUri() != null) {
                if (!seenNodes.containsKey(hierarchy.textbookUri())) {
                    nodes.add(KgGraphDTO.GraphNode.builder()
                            .id(hierarchy.textbookUri())
                            .type("textbook")
                            .label(hierarchy.textbookLabel())
                            .data(Map.of("uri", hierarchy.textbookUri(), "name", hierarchy.textbookLabel()))
                            .build());
                    seenNodes.put(hierarchy.textbookUri(), true);
                }
                if (hierarchy.chapterUri() != null) {
                    edges.add(KgGraphDTO.GraphEdge.builder()
                            .id(hierarchy.textbookUri() + "→CONTAINS→" + hierarchy.chapterUri())
                            .source(hierarchy.textbookUri())
                            .target(hierarchy.chapterUri())
                            .label("CONTAINS")
                            .build());
                }
            }
        }

        // 处理关联概念：KP → Concept  (Neo4j: (KP)-[MATCHES_KG]->(Concept))
        for (RelatedConcept concept : graphData.relatedConcepts()) {
            if (!seenNodes.containsKey(concept.conceptUri())) {
                nodes.add(KgGraphDTO.GraphNode.builder()
                        .id(concept.conceptUri())
                        .type("concept")
                        .label(concept.conceptLabel())
                        .data(Map.of("uri", concept.conceptUri(), "name", concept.conceptLabel()))
                        .build());
                seenNodes.put(concept.conceptUri(), true);
            }
            edges.add(KgGraphDTO.GraphEdge.builder()
                    .id(kpUri + "→MATCHES_KG→" + concept.conceptUri())
                    .source(kpUri)
                    .target(concept.conceptUri())
                    .label("MATCHES_KG")
                    .build());
        }

        return KgGraphDTO.builder().nodes(nodes).edges(edges).build();
    }

    /** 结构关系：教材知识点的层级归属 */
    private static final List<String> STRUCTURE_RELATIONS = List.of("IN_UNIT", "CONTAINS");

    /** 知识关系：概念/知识点之间的语义关联 */
    private static final List<String> KNOWLEDGE_RELATIONS = List.of(
            "MATCHES_KG", "RELATED_TO", "BELONGS_TO", "PART_OF",
            "HAS_TYPE", "SUB_CLASS_OF", "DEVELOPS", "HAS_DIMENSION",
            "ASSOCIATED_WITH", "PEER_RELATION");

    private static final int DEFAULT_LIMIT = 20;

    /**
     * 展开结构关系邻居（IN_UNIT、CONTAINS）
     */
    public KgGraphDTO expandNodeStructure(String nodeUri, int limit) {
        int queryLimit = resolveLimit(limit) + 1; // 多查一条判断 hasMore
        List<ExpandRelationResult> relations = kgKnowledgeGraphQueryRepository.expandNode(nodeUri, STRUCTURE_RELATIONS, queryLimit);
        return buildExpandGraph(nodeUri, relations, limit);
    }

    /**
     * 展开知识关系邻居（MATCHES_KG、RELATED_TO 等）
     */
    public KgGraphDTO expandNodeKnowledge(String nodeUri, int limit) {
        int queryLimit = resolveLimit(limit) + 1;
        List<ExpandRelationResult> relations = kgKnowledgeGraphQueryRepository.expandNode(nodeUri, KNOWLEDGE_RELATIONS, queryLimit);
        return buildExpandGraph(nodeUri, relations, limit);
    }

    private int resolveLimit(int limit) {
        return limit > 0 ? limit : DEFAULT_LIMIT;
    }

    /**
     * 将展开结果构建为 KgGraphDTO
     */
    private KgGraphDTO buildExpandGraph(String nodeUri, List<ExpandRelationResult> relations, int requestedLimit) {
        if (relations.isEmpty()) {
            return KgGraphDTO.builder().nodes(List.of()).edges(List.of()).hasMore(false).build();
        }

        boolean hasMore = relations.size() > requestedLimit;
        if (hasMore) {
            relations = relations.subList(0, requestedLimit);
        }

        List<KgGraphDTO.GraphNode> nodes = new ArrayList<>();
        List<KgGraphDTO.GraphEdge> edges = new ArrayList<>();
        Map<String, Boolean> seenNodes = new LinkedHashMap<>();

        // 源节点
        ExpandRelationResult first = relations.getFirst();
        String sourceLabel = first.sourceLabel() != null ? first.sourceLabel() : nodeUri;
        nodes.add(KgConvert.toGraphNode(nodeUri, first.sourceLabels(), sourceLabel));
        seenNodes.put(nodeUri, true);

        for (ExpandRelationResult rel : relations) {
            String targetUri = rel.targetUri();
            if (targetUri == null) continue;

            if (!seenNodes.containsKey(targetUri)) {
                nodes.add(KgConvert.toGraphNode(targetUri, rel.targetLabels(), rel.targetLabel()));
                seenNodes.put(targetUri, true);
            }

            String edgeSource = rel.isOutgoing() ? nodeUri : targetUri;
            String edgeTarget = rel.isOutgoing() ? targetUri : nodeUri;
            edges.add(KgGraphDTO.GraphEdge.builder()
                    .id(edgeSource + "→" + rel.relType() + "→" + edgeTarget)
                    .source(edgeSource).target(edgeTarget).label(rel.relType())
                    .build());
        }

        return KgGraphDTO.builder().nodes(nodes).edges(edges).hasMore(hasMore).build();
    }

}
