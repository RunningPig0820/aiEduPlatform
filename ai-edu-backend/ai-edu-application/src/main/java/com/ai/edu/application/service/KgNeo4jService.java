package com.ai.edu.application.service;

import com.ai.edu.application.assembler.KgConvert;
import com.ai.edu.application.dto.kg.BatchRelationsDTO;
import com.ai.edu.application.dto.kg.HealthDTO;
import com.ai.edu.domain.edukg.service.KgRelationQueryDomainService;
import com.ai.edu.domain.edukg.service.KgSyncDomainService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
