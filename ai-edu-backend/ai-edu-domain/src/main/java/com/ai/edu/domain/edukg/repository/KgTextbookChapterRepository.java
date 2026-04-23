package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;

import java.util.List;

/**
 * 教材-章节关联仓储接口
 */
public interface KgTextbookChapterRepository {

    KgTextbookChapter save(KgTextbookChapter relation);

    void saveBatch(List<KgTextbookChapter> relations);

    void deleteByTextbookUri(String textbookUri);

    void deleteByChapterUri(String chapterUri);

    List<KgTextbookChapter> findByTextbookUri(String textbookUri);

    List<KgTextbookChapter> findByChapterUri(String chapterUri);

    /**
     * 按教材 URI 列表批量查询关联
     */
    List<KgTextbookChapter> findByTextbookUris(List<String> textbookUris);

    /**
     * 删除单条关联记录
     */
    void deleteRelation(String textbookUri, String chapterUri);

    /**
     * 更新排序索引
     */
    void updateOrderIndex(String textbookUri, String chapterUri, int orderIndex);
}
