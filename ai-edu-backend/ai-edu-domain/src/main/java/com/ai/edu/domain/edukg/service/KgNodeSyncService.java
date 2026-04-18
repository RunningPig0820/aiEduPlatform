package com.ai.edu.domain.edukg.service;

import com.ai.edu.domain.edukg.model.entity.*;

import java.util.List;
import java.util.Set;

/**
 * 知识图谱节点同步服务接口
 */
public interface KgNodeSyncService {

    List<KgTextbook> syncTextbookNodes();

    List<KgChapter> syncChapterNodes();

    List<KgSection> syncSectionNodes();

    List<KgKnowledgePoint> syncKnowledgePointNodes();

    /**
     * 标记 MySQL 中有但 Neo4j 中无的节点为 deleted
     */
    int markDeletedNodes(String neo4jNodeType, Set<String> neo4jUris);
}
