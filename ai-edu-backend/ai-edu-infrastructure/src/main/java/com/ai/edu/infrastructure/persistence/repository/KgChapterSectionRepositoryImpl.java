package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.repository.KgChapterSectionRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgChapterSectionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 章节-小节关联仓储实现
 */
@Repository
public class KgChapterSectionRepositoryImpl implements KgChapterSectionRepository {

    @Resource
    private KgChapterSectionMapper kgChapterSectionMapper;

    @Override
    public KgChapterSection save(KgChapterSection relation) {
        if (relation.getId() == null) {
            kgChapterSectionMapper.insert(relation);
        } else {
            kgChapterSectionMapper.updateById(relation);
        }
        return relation;
    }

    @Override
    public void saveBatch(List<KgChapterSection> relations) {
        if (relations != null && !relations.isEmpty()) {
            kgChapterSectionMapper.batchInsert(relations);
        }
    }

    @Override
    public void deleteByChapterUri(String chapterUri) {
        kgChapterSectionMapper.deleteByChapterUri(chapterUri, 0L);
    }

    @Override
    public void deleteBySectionUri(String sectionUri) {
        kgChapterSectionMapper.deleteBySectionUri(sectionUri, 0L);
    }

    @Override
    public List<KgChapterSection> findByChapterUri(String chapterUri) {
        return kgChapterSectionMapper.selectByChapterUri(chapterUri);
    }

    @Override
    public List<KgChapterSection> findBySectionUri(String sectionUri) {
        return kgChapterSectionMapper.selectBySectionUri(sectionUri);
    }

    @Override
    public List<KgChapterSection> findAllActive() {
        return kgChapterSectionMapper.selectAllActiveRelations();
    }
}
