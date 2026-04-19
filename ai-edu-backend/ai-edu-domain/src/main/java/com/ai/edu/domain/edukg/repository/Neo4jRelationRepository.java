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

    /**
     * 按教材 URI 列表查询教材-章节关联
     */
    List<KgTextbookChapter> findTextbookChapterRelations(List<String> textbookUris);

    /**
     * 按章节 URI 列表查询章节-小节关联
     */
    List<KgChapterSection> findChapterSectionRelations(List<String> chapterUris);

    /**
     * 按小节 URI 列表查询小节-知识点关联
     */
    List<KgSectionKP> findSectionKPRelations(List<String> sectionUris);
}
