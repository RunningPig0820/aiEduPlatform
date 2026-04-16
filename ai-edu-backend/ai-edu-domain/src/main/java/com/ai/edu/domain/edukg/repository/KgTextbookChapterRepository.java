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

    List<KgTextbookChapter> findAllActive();
}
