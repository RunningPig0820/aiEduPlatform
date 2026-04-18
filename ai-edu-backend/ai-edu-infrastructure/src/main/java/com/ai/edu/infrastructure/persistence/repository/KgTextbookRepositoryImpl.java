package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgTextbook;
import com.ai.edu.domain.edukg.repository.KgTextbookRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgTextbookMapper;
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
        if (textbook.getId() == null) {
            kgTextbookMapper.insert(textbook);
        } else {
            kgTextbookMapper.updateById(textbook);
        }
        return textbook;
    }

    @Override
    public Optional<KgTextbook> findById(Long id) {
        return Optional.ofNullable(kgTextbookMapper.selectById(id));
    }

    @Override
    public Optional<KgTextbook> findByUri(String uri) {
        return Optional.ofNullable(kgTextbookMapper.selectByUri(uri));
    }

    @Override
    public List<KgTextbook> findBySubject(String subject) {
        return kgTextbookMapper.selectBySubject(subject);
    }

    @Override
    public List<KgTextbook> findBySubjectAndStage(String subject, String stage) {
        return kgTextbookMapper.selectBySubjectAndStage(subject, stage);
    }

    @Override
    public List<KgTextbook> findAllActive() {
        return kgTextbookMapper.selectAllActive();
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
