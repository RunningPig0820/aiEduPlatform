package com.ai.edu.domain.edukg.service;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;

import java.util.List;

/**
 * 知识图谱关系同步服务接口
 */
public interface KgRelationSyncService {

    List<KgTextbookChapter> syncTextbookChapterRelations();

    List<KgChapterSection> syncChapterSectionRelations();

    List<KgSectionKP> syncSectionKPRelations();

    int rebuildTextbookChapterRelations(List<KgTextbookChapter> relations);

    int rebuildChapterSectionRelations(List<KgChapterSection> relations);

    int rebuildSectionKPRelations(List<KgSectionKP> relations);
}
