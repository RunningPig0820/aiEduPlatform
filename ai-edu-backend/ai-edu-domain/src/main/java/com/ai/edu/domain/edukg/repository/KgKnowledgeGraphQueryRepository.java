package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.model.result.GraphQueryResult;

import java.util.List;

/**
 * 知识图谱查询仓储接口
 * 职责：定义按 URI 查询 Neo4j 图谱关联关系的能力
 */
public interface KgKnowledgeGraphQueryRepository {

    /**
     * 查询教材-章节关联
     */
    List<KgTextbookChapter> getTextbookChapterRelations(String textbookUri);

    /**
     * 查询章节-小节关联
     */
    List<KgChapterSection> getChapterSectionRelations(String chapterUri);

    /**
     * 查询小节-知识点关联
     */
    List<KgSectionKP> getSectionKPRelations(String sectionUri);

    /**
     * 查询知识点的图谱数据（含层级路径和关联概念）
     */
    GraphQueryResult queryGraphForKnowledgePoint(String kpUri);
}
