package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgSection;
import com.ai.edu.domain.edukg.repository.KgSectionRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgSectionMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgSectionPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 小节仓储实现
 */
@Repository
public class KgSectionRepositoryImpl implements KgSectionRepository {

    @Resource
    private KgSectionMapper kgSectionMapper;

    @Override
    public KgSection save(KgSection section) {
        KgSectionPo po = KgSectionPo.from(section);
        if (po.getId() == null) {
            kgSectionMapper.insert(po);
            section.setId(po.getId());
        } else {
            kgSectionMapper.updateById(po);
        }
        return section;
    }

    @Override
    public Optional<KgSection> findById(Long id) {
        KgSectionPo po = kgSectionMapper.selectById(id);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public Optional<KgSection> findByUri(String uri) {
        KgSectionPo po = kgSectionMapper.selectByUri(uri);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public List<KgSection> findByUris(List<String> uris) {
        return KgSectionPo.toEntityList(kgSectionMapper.selectByUris(uris));
    }

    @Override
    public int upsert(List<KgSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return 0;
        }
        List<String> uris = sections.stream().map(KgSection::getUri).toList();
        List<KgSectionPo> existingPos = kgSectionMapper.selectByUris(uris);
        Set<String> existingUris = existingPos.stream().map(KgSectionPo::getUri).collect(Collectors.toSet());

        int count = 0;
        for (KgSection sec : sections) {
            if (!existingUris.contains(sec.getUri())) {
                kgSectionMapper.insert(KgSectionPo.from(sec));
                count++;
            }
        }
        return count;
    }

    @Override
    public void updateStatus(String uri, String status) {
        kgSectionMapper.updateStatus(uri, status, 0L);
    }
}
