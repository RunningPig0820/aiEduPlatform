package com.ai.edu.application.service;

import com.ai.edu.application.assembler.KgConvert;
import com.ai.edu.application.dto.kg.BatchRelationsDTO;
import com.ai.edu.application.dto.kg.HealthDTO;
import com.ai.edu.application.dto.kg.KgGraphDTO;
import com.ai.edu.domain.edukg.model.entity.KgChapter;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.model.entity.KgSection;
import com.ai.edu.domain.edukg.model.result.GraphQueryResult;
import com.ai.edu.domain.edukg.model.result.RelatedConcept;
import com.ai.edu.domain.edukg.model.result.TextbookHierarchy;
import com.ai.edu.domain.edukg.repository.KgChapterRepository;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.edukg.repository.KgSectionRepository;
import com.ai.edu.domain.edukg.service.KgRelationQueryDomainService;
import com.ai.edu.domain.edukg.service.KgSyncDomainService;
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
    private KgSyncDomainService kgSyncDomainService;

    @Resource
    private KgRelationQueryDomainService kgRelationQueryDomainService;

    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    @Resource
    private KgSectionRepository kgSectionRepository;

    @Resource
    private KgChapterRepository kgChapterRepository;

    /**
     * Neo4j 健康检查
     */
    public HealthDTO getNeo4jHealth() {
        KgSyncDomainService.HealthCheckResult health = kgSyncDomainService.checkNeo4jHealth();
        return KgConvert.toHealthDTO(health.healthy, health.responseTimeMs, health.message);
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
                kgRelationQueryDomainService.getTextbookChapterRelations(uri).forEach(rel ->
                        related.add(rel.getChapterUri()));

                // 查询章节-小节关联
                kgRelationQueryDomainService.getChapterSectionRelations(uri).forEach(rel ->
                        related.add(rel.getSectionUri()));

                // 查询小节-知识点关联
                kgRelationQueryDomainService.getSectionKPRelations(uri).forEach(rel ->
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
     */
    public KgGraphDTO getKnowledgePointGraph(String kpUri) {
        // 查询知识点本身
        Optional<KgKnowledgePoint> kpOpt = kgKnowledgePointRepository.findByUri(kpUri);
        if (kpOpt.isEmpty()) {
            return KgGraphDTO.builder().nodes(List.of()).edges(List.of()).build();
        }
        KgKnowledgePoint kp = kpOpt.get();

        // 从领域服务获取图谱数据
        GraphQueryResult graphData = kgRelationQueryDomainService.queryGraphForKnowledgePoint(kpUri);

        List<KgGraphDTO.GraphNode> nodes = new ArrayList<>();
        List<KgGraphDTO.GraphEdge> edges = new ArrayList<>();
        Map<String, Boolean> seenNodes = new LinkedHashMap<>();

        // 添加知识点自身节点
        Map<String, Object> kpData = new LinkedHashMap<>();
        kpData.put("uri", kp.getUri());
        kpData.put("name", kp.getLabel());
        kpData.put("difficulty", kp.getDifficulty());
        kpData.put("cognitiveLevel", kp.getCognitiveLevel());
        kpData.put("importance", kp.getImportance());

        nodes.add(KgGraphDTO.GraphNode.builder()
                .id(kp.getUri())
                .type("kp")
                .label(kp.getLabel())
                .data(kpData)
                .build());
        seenNodes.put(kp.getUri(), true);

        int edgeIndex = 0;

        // 处理教材层级路径
        for (TextbookHierarchy hierarchy : graphData.hierarchies()) {
            // 添加 Section 节点（textbook_kp 类型）
            if (hierarchy.sectionUri() != null && !seenNodes.containsKey("tkp:" + hierarchy.sectionUri())) {
                Map<String, Object> secData = buildTextbookKpData(
                        hierarchy.sectionUri(), hierarchy.sectionLabel(), "", "", "", hierarchy.sectionLabel());

                nodes.add(KgGraphDTO.GraphNode.builder()
                        .id(hierarchy.sectionUri())
                        .type("textbook_kp")
                        .label(hierarchy.sectionLabel())
                        .data(secData)
                        .build());
                seenNodes.put("tkp:" + hierarchy.sectionUri(), true);

                // 边：Section -> KP
                edges.add(KgGraphDTO.GraphEdge.builder()
                        .id("edge-" + (edgeIndex++))
                        .source(hierarchy.sectionUri())
                        .target(kpUri)
                        .label("关联")
                        .build());
            }

            // 添加 Chapter 节点
            if (hierarchy.chapterUri() != null && !seenNodes.containsKey("tkp:" + hierarchy.chapterUri())) {
                Map<String, Object> chData = buildTextbookKpData(
                        hierarchy.chapterUri(), hierarchy.chapterLabel(), "", "", hierarchy.chapterLabel(), "");

                nodes.add(KgGraphDTO.GraphNode.builder()
                        .id(hierarchy.chapterUri())
                        .type("textbook_kp")
                        .label(hierarchy.chapterLabel())
                        .data(chData)
                        .build());
                seenNodes.put("tkp:" + hierarchy.chapterUri(), true);
            }

            // 边：Chapter -> Section
            if (hierarchy.chapterUri() != null && hierarchy.sectionUri() != null) {
                edges.add(KgGraphDTO.GraphEdge.builder()
                        .id("edge-" + (edgeIndex++))
                        .source(hierarchy.chapterUri())
                        .target(hierarchy.sectionUri())
                        .label("关联")
                        .build());
            }

            // 添加 Textbook 节点
            if (hierarchy.textbookUri() != null && !seenNodes.containsKey("tkp:" + hierarchy.textbookUri())) {
                Map<String, Object> tbData = buildTextbookKpData(
                        hierarchy.textbookUri(), hierarchy.textbookLabel(), "", "", "", "");

                nodes.add(KgGraphDTO.GraphNode.builder()
                        .id(hierarchy.textbookUri())
                        .type("textbook_kp")
                        .label(hierarchy.textbookLabel())
                        .data(tbData)
                        .build());
                seenNodes.put("tkp:" + hierarchy.textbookUri(), true);
            }

            // 边：Textbook -> Chapter
            if (hierarchy.textbookUri() != null && hierarchy.chapterUri() != null) {
                edges.add(KgGraphDTO.GraphEdge.builder()
                        .id("edge-" + (edgeIndex++))
                        .source(hierarchy.textbookUri())
                        .target(hierarchy.chapterUri())
                        .label("关联")
                        .build());
            }
        }

        // 处理关联概念
        for (RelatedConcept concept : graphData.relatedConcepts()) {
            if (!seenNodes.containsKey(concept.conceptUri())) {
                Map<String, Object> cData = new LinkedHashMap<>();
                cData.put("uri", concept.conceptUri());
                cData.put("name", concept.conceptLabel());

                nodes.add(KgGraphDTO.GraphNode.builder()
                        .id(concept.conceptUri())
                        .type("kp")
                        .label(concept.conceptLabel())
                        .data(cData)
                        .build());
                seenNodes.put(concept.conceptUri(), true);

                // 边：KP -> Concept
                edges.add(KgGraphDTO.GraphEdge.builder()
                        .id("edge-" + (edgeIndex++))
                        .source(kpUri)
                        .target(concept.conceptUri())
                        .label("关联")
                        .build());
            }
        }

        return KgGraphDTO.builder().nodes(nodes).edges(edges).build();
    }

    private Map<String, Object> buildTextbookKpData(String uri, String name,
                                                     String subject, String grade, String unit, String lesson) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uri", uri);
        data.put("name", name != null ? name : uri);
        data.put("subject", subject);
        data.put("grade", grade);
        data.put("unit", unit);
        data.put("lesson", lesson);
        return data;
    }
}
