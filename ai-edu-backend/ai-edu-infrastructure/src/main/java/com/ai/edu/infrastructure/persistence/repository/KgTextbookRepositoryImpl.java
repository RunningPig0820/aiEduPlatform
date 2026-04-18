package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgTextbook;
import com.ai.edu.domain.edukg.repository.KgTextbookRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgTextbookMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgTextbookPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 教材仓储实现
 */
@Repository
public class KgTextbookRepositoryImpl implements KgTextbookRepository {

    @Resource
    private KgTextbookMapper kgTextbookMapper;

    @Override
    public KgTextbook save(KgTextbook textbook) {
        KgTextbookPo po = KgTextbookPo.from(textbook);
        if (po.getId() == null) {
            kgTextbookMapper.insert(po);
            textbook.setId(po.getId());
        } else {
            kgTextbookMapper.updateById(po);
        }
        return textbook;
    }

    @Override
    public Optional<KgTextbook> findById(Long id) {
        KgTextbookPo po = kgTextbookMapper.selectById(id);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public Optional<KgTextbook> findByUri(String uri) {
        KgTextbookPo po = kgTextbookMapper.selectByUri(uri);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public List<KgTextbook> findBySubject(String subject) {
        return KgTextbookPo.toEntityList(kgTextbookMapper.selectBySubject(subject));
    }

    @Override
    public List<KgTextbook> findBySubjectAndStage(String subject, String stage) {
        return KgTextbookPo.toEntityList(kgTextbookMapper.selectBySubjectAndStage(subject, stage));
    }

    @Override
    public List<KgTextbook> findAllActive() {
        return KgTextbookPo.toEntityList(kgTextbookMapper.selectAllActive());
    }

    @Override
    public List<String> findDistinctGrades() {
        return kgTextbookMapper.selectDistinctGrades();
    }

    @Override
    public List<String> findDistinctGradesBySubject(String subject) {
        return kgTextbookMapper.selectDistinctGradesBySubject(subject);
    }

    @Override
    public void updateStatus(String uri, String status) {
        kgTextbookMapper.updateStatus(uri, status, 0L);
    }
}
