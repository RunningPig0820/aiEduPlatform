package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgTextbook;
import com.ai.edu.domain.edukg.repository.KgTextbookRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgTextbookMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgTextbookPo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教材仓储实现
 */
@Slf4j
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
    public List<String> findDistinctGradesByEditionSubject(String edition, String subject) {
        return kgTextbookMapper.selectDistinctGradesByEditionSubject(edition, subject);
    }

    @Override
    public List<KgTextbook> findByEditionSubject(String edition, String subject) {
        return KgTextbookPo.toEntityList(kgTextbookMapper.selectByEditionSubject(edition, subject));
    }

    @Override
    public int upsert(List<KgTextbook> textbooks) {
        if (textbooks == null || textbooks.isEmpty()) {
            log.warn("upsert: textbooks list is null or empty, returning 0");
            return 0;
        }
        log.info("upsert: processing {} textbooks from Neo4j", textbooks.size());

        int count = 0;
        for (KgTextbook tb : textbooks) {
            KgTextbookPo existingPo = kgTextbookMapper.selectByUri(tb.getUri());
            if (existingPo == null) {
                log.info("upsert: INSERT new textbook uri={}, edition={}, stage={}",
                        tb.getUri(), tb.getEdition(), tb.getStage());
                KgTextbookPo po = KgTextbookPo.from(tb);
                kgTextbookMapper.insert(po);
                tb.setId(po.getId());
                count++;
            } else {
                KgTextbook existingEntity = existingPo.toEntity();
                log.info("upsert: UPDATE existing textbook uri={}, old edition={}, new edition={}, old stage={}, new stage={}",
                        tb.getUri(), existingEntity.getEdition(), tb.getEdition(),
                        existingEntity.getStage(), tb.getStage());
                existingEntity.updateFrom(tb);
                existingPo = KgTextbookPo.from(existingEntity);
                kgTextbookMapper.updateById(existingPo);
            }
        }
        log.info("upsert: completed, inserted {} new records", count);
        return count;
    }

    @Override
    public void updateStatus(String uri, String status) {
        kgTextbookMapper.updateStatus(uri, status, 0L);
    }

    @Override
    public List<KgTextbook> findAllActiveByEditionSubjectGrade(String edition, String subject, String grade) {
        return KgTextbookPo.toEntityList(
                kgTextbookMapper.selectAllActiveByEditionSubjectGrade(edition, subject, grade));
    }

    @Override
    public List<KgTextbook> findAllActiveByGrade(String grade) {
        return KgTextbookPo.toEntityList(kgTextbookMapper.selectAllActiveByGrade(grade));
    }
}
