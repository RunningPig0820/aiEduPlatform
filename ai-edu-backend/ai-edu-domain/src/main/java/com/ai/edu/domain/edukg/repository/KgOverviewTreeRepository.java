package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.valueobject.KgTreeNode;

import java.util.List;

/**
 * 教材树浏览仓储（知识地图点击式下钻——每次单层查询，最多 2 表 JOIN，索引命中）。
 *
 * <p>替代原「学段→知识点」7 表 JOIN 分页（慢 SQL）：学段→课本→章节→小节→知识点逐层下钻，
 * 点击才查下一层，每层数据量小无需分页。数据源 kg 镜像只读。
 */
public interface KgOverviewTreeRepository {

    /** 按学段（中文 label：小学/初中/高中）查年级（单表 DISTINCT grade）。 */
    List<KgTreeNode> findGradesByStage(String stage);

    /** 按学段 + 年级查课本（单表，WHERE stage+grade）。 */
    List<KgTreeNode> findTextbooksByStage(String stage, String grade);

    /** 查课本下章节（textbook_chapter ⋈ chapter，2 表，ORDER BY order_index）。 */
    List<KgTreeNode> findChaptersByTextbookUri(String textbookUri);

    /** 查章节下小节（chapter_section ⋈ section，2 表，ORDER BY order_index）。 */
    List<KgTreeNode> findSectionsByChapterUri(String chapterUri);

    /** 查小节下知识点（section_kp ⋈ knowledge_point，2 表，ORDER BY order_index）。 */
    List<KgTreeNode> findKnowledgePointsBySectionUri(String sectionUri);
}
