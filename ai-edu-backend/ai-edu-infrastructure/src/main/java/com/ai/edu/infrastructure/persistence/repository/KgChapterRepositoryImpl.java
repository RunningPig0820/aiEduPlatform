package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgChapter;
import com.ai.edu.domain.edukg.repository.KgChapterRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgChapterMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgChapterPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 章节仓储实现
 */
@Repository
public class KgChapterRepositoryImpl implements KgChapterRepository {

    @Resource
    private KgChapterMapper kgChapterMapper;

    @Override
    public KgChapter save(KgChapter chapter) {
        KgChapterPo po = KgChapterPo.from(chapter);
        if (po.getId() == null) {
            kgChapterMapper.insert(po);
            chapter.setId(po.getId());
        } else {
            kgChapterMapper.updateById(po);
        }
        return chapter;
    }

    @Override
    public Optional<KgChapter> findById(Long id) {
        KgChapterPo po = kgChapterMapper.selectById(id);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public Optional<KgChapter> findByUri(String uri) {
        KgChapterPo po = kgChapterMapper.selectByUri(uri);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public List<KgChapter> findByUris(List<String> uris) {
        return KgChapterPo.toEntityList(kgChapterMapper.selectByUris(uris));
    }

    @Override
    public int upsert(List<KgChapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return 0;
        }
        List<String> uris = chapters.stream().map(KgChapter::getUri).toList();
        List<KgChapterPo> existingPos = kgChapterMapper.selectByUris(uris);
        Set<String> existingUris = existingPos.stream().map(KgChapterPo::getUri).collect(Collectors.toSet());

        int count = 0;
        for (KgChapter ch : chapters) {
            if (!existingUris.contains(ch.getUri())) {
                kgChapterMapper.insert(KgChapterPo.from(ch));
                count++;
            }
        }
        return count;
    }

    @Override
    public void updateStatus(String uri, String status) {
        kgChapterMapper.updateStatus(uri, status, 0L);
    }
}
