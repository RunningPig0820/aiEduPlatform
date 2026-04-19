package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.*;

import java.util.List;

/**
 * Neo4j 知识图谱节点读取仓储
 *
 * 职责：从 Neo4j 查询节点并映射为领域实体（只读）
 */
public interface Neo4jNodeRepository {

    List<KgTextbook> findAllTextbooks();

    List<KgChapter> findAllChapters();

    List<KgSection> findAllSections();

    List<KgKnowledgePoint> findAllKnowledgePoints();
}
