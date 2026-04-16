package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgSection;
import com.ai.edu.domain.edukg.repository.KgSectionRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgSectionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 小节仓储实现
 */
@Repository
public class KgSectionRepositoryImpl implements KgSectionRepository {

    @Resource
    private KgSectionMapper kgSectionMapper;

    @Override
    public KgSection save(KgSection section) {
        if (section.getId() == null) {
            kgSectionMapper.insert(section);
        } else {
            kgSectionMapper.updateById(section);
        }
        return section;
    }

    @Override
    public Optional<KgSection> findById(Long id) {
        return Optional.ofNullable(kgSectionMapper.selectById(id));
    }

    @Override
    public Optional<KgSection> findByUri(String uri) {
        return Optional.ofNullable(kgSectionMapper.selectByUri(uri));
    }

    @Override
    public List<KgSection> findByUris(List<String> uris) {
        return kgSectionMapper.selectByUris(uris);
    }

    @Override
    public void updateStatus(String uri, String status) {
        kgSectionMapper.updateStatus(uri, status, 0L);
    }
}
