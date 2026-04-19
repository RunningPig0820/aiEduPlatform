package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;

import java.util.List;

/**
 * 小节-知识点关联仓储接口
 */
public interface KgSectionKPRepository {

    KgSectionKP save(KgSectionKP relation);

    void saveBatch(List<KgSectionKP> relations);

    void deleteBySectionUri(String sectionUri);

    void deleteByKpUri(String kpUri);

    List<KgSectionKP> findBySectionUri(String sectionUri);

    List<KgSectionKP> findByKpUri(String kpUri);

    List<KgSectionKP> findAllActive();

    /**
     * 删除单条关联记录
     */
    void deleteRelation(String sectionUri, String kpUri);

    /**
     * 更新排序索引
     */
    void updateOrderIndex(String sectionUri, String kpUri, int orderIndex);
}
