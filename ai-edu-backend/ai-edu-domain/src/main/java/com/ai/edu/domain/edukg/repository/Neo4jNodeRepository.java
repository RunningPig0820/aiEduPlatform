package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.*;

import java.util.List;

/**
 * Neo4j 知识图谱节点读取仓储
 *
 * 职责：从 Neo4j 查询节点并映射为领域实体（只读）
 */
public interface Neo4jNodeRepository {

    /**
     * 按教材/学科查询（edition 和 subject 必传）
     */
    List<KgTextbook> findTextbooks(String edition, String subject, String stage, String grade);

    /**
     * 按所属教材 URI 列表查询章节
     */
    List<KgChapter> findChaptersByTextbookUris(List<String> textbookUris);

    /**
     * 按所属教材 URI 列表查询小节（通过 chapter 关联）
     */
    List<KgSection> findSectionsByTextbookUris(List<String> textbookUris);

    /**
     * 按所属教材 URI 列表查询知识点（通过 section 关联）
     */
    List<KgKnowledgePoint> findKnowledgePointsByTextbookUris(List<String> textbookUris);
}
