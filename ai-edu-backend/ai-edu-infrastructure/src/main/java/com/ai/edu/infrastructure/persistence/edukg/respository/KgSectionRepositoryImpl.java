package com.ai.edu.infrastructure.persistence.edukg.respository;

import com.ai.edu.domain.edukg.model.entity.KgSection;
import com.ai.edu.domain.edukg.repository.KgSectionRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgChapterSectionMapper;
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
 *
 * Repository 可调用多个 Mapper 来组装领域数据，符合 DDD 规范：
 * - Mapper 只做单表数据访问
 * - Repository 负责领域数据的组装
 */
@Repository
public class KgSectionRepositoryImpl implements KgSectionRepository {

    @Resource
    private KgSectionMapper kgSectionMapper;

    @Resource
    private KgChapterSectionMapper kgChapterSectionMapper;

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

    @Override
    public int countActive() {
        return kgSectionMapper.selectByStatus("active").size();
    }

    /**
     * 查询与指定章节 URI 列表关联的活跃小节
     *
     * 使用两次查询模式（避免 JOIN 在大数据量时不可控）：
     * 1. 从关联表 chapter_section 查询 section_uri 列表
     * 2. 用 section_uri 列表查小节表
     */
    @Override
    public List<KgSection> findAllActiveByChapterUris(List<String> chapterUris) {
        if (chapterUris == null || chapterUris.isEmpty()) {
            return List.of();
        }
        // 第一次查询：从关联表获取 section_uris
        List<String> sectionUris = kgChapterSectionMapper.selectSectionUrisByChapterUris(chapterUris);
        if (sectionUris.isEmpty()) {
            return List.of();
        }
        // 第二次查询：用 uris 查小节实体
        return KgSectionPo.toEntityList(kgSectionMapper.selectByUris(sectionUris));
    }
}
