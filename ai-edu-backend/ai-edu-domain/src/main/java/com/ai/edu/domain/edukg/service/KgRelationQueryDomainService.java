package com.ai.edu.domain.edukg.service;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;

import java.util.List;

/**
 * 图谱关系查询领域服务接口
 * 职责：定义按 URI 查询关联关系的能力（支持缓存 + 降级）
 */
public interface KgRelationQueryDomainService {

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
}
