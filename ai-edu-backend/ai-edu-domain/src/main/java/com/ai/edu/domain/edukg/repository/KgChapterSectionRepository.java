package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;

import java.util.List;

/**
 * 章节-小节关联仓储接口
 */
public interface KgChapterSectionRepository {

    KgChapterSection save(KgChapterSection relation);

    void saveBatch(List<KgChapterSection> relations);

    void deleteByChapterUri(String chapterUri);

    void deleteBySectionUri(String sectionUri);

    List<KgChapterSection> findByChapterUri(String chapterUri);

    List<KgChapterSection> findBySectionUri(String sectionUri);

    List<KgChapterSection> findAllActive();

    /**
     * 删除单条关联记录
     */
    void deleteRelation(String chapterUri, String sectionUri);

    /**
     * 更新排序索引
     */
    void updateOrderIndex(String chapterUri, String sectionUri, int orderIndex);
}
