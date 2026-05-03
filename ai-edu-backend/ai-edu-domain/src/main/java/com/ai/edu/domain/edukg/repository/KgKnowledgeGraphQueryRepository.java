package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.model.result.ExpandRelationResult;
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

    /**
     * 展开任意节点的直接邻居，按关系类型过滤
     *
     * @param nodeUri       源节点 URI
     * @param relationTypes 允许的关系类型列表
     * @param limit         返回数量上限
     */
    List<ExpandRelationResult> expandNode(String nodeUri, List<String> relationTypes, int limit);
}
