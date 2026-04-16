package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgChapter;
import com.ai.edu.domain.edukg.repository.KgChapterRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgChapterMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 章节仓储实现
 */
@Repository
public class KgChapterRepositoryImpl implements KgChapterRepository {

    @Resource
    private KgChapterMapper kgChapterMapper;

    @Override
    public KgChapter save(KgChapter chapter) {
        if (chapter.getId() == null) {
            kgChapterMapper.insert(chapter);
        } else {
            kgChapterMapper.updateById(chapter);
        }
        return chapter;
    }

    @Override
    public Optional<KgChapter> findById(Long id) {
        return Optional.ofNullable(kgChapterMapper.selectById(id));
    }

    @Override
    public Optional<KgChapter> findByUri(String uri) {
        return Optional.ofNullable(kgChapterMapper.selectByUri(uri));
    }

    @Override
    public List<KgChapter> findByUris(List<String> uris) {
        return kgChapterMapper.selectByUris(uris);
    }

    @Override
    public void updateStatus(String uri, String status) {
        kgChapterMapper.updateStatus(uri, status, 0L);
    }
}
