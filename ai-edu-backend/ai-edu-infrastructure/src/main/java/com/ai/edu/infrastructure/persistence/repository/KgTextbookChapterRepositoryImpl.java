package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.repository.KgTextbookChapterRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgTextbookChapterMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgTextbookChapterPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 教材-章节关联仓储实现
 */
@Repository
public class KgTextbookChapterRepositoryImpl implements KgTextbookChapterRepository {

    @Resource
    private KgTextbookChapterMapper kgTextbookChapterMapper;

    @Override
    public KgTextbookChapter save(KgTextbookChapter relation) {
        KgTextbookChapterPo po = KgTextbookChapterPo.from(relation);
        if (po.getId() == null) {
            kgTextbookChapterMapper.insert(po);
            relation.setId(po.getId());
        } else {
            kgTextbookChapterMapper.updateById(po);
        }
        return relation;
    }

    @Override
    public void saveBatch(List<KgTextbookChapter> relations) {
        if (relations != null && !relations.isEmpty()) {
            kgTextbookChapterMapper.batchInsert(KgTextbookChapterPo.fromList(relations));
        }
    }

    @Override
    public void deleteByTextbookUri(String textbookUri) {
        kgTextbookChapterMapper.deleteByTextbookUri(textbookUri, 0L);
    }

    @Override
    public void deleteByChapterUri(String chapterUri) {
        kgTextbookChapterMapper.deleteByChapterUri(chapterUri, 0L);
    }

    @Override
    public List<KgTextbookChapter> findByTextbookUri(String textbookUri) {
        return KgTextbookChapterPo.toEntityList(kgTextbookChapterMapper.selectByTextbookUri(textbookUri));
    }

    @Override
    public List<KgTextbookChapter> findByChapterUri(String chapterUri) {
        return KgTextbookChapterPo.toEntityList(kgTextbookChapterMapper.selectByChapterUri(chapterUri));
    }

    @Override
    public List<KgTextbookChapter> findAllActive() {
        return KgTextbookChapterPo.toEntityList(kgTextbookChapterMapper.selectAllActiveRelations());
    }
}
