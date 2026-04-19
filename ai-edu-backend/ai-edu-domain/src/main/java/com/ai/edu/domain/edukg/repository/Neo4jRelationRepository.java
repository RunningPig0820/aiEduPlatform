package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;

import java.util.List;

/**
 * Neo4j 知识图谱关系读取仓储
 *
 * 职责：从 Neo4j 查询关系并映射为领域实体（只读）
 */
public interface Neo4jRelationRepository {

    List<KgTextbookChapter> findTextbookChapterRelations();

    List<KgChapterSection> findChapterSectionRelations();

    List<KgSectionKP> findSectionKPRelations();
}
