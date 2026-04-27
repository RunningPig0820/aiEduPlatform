package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgChapter;
import com.ai.edu.domain.edukg.repository.KgChapterRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgChapterMapper;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgTextbookChapterMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgChapterPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 章节仓储实现
 *
 * Repository 可调用多个 Mapper 来组装领域数据，符合 DDD 规范：
 * - Mapper 只做单表数据访问
 * - Repository 负责领域数据的组装
 */
@Repository
public class KgChapterRepositoryImpl implements KgChapterRepository {

    @Resource
    private KgChapterMapper kgChapterMapper;

    @Resource
    private KgTextbookChapterMapper kgTextbookChapterMapper;

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

    @Override
    public int countActive() {
        return kgChapterMapper.selectByStatus("active").size();
    }

    /**
     * 查询与指定教材 URI 列表关联的活跃章节
     *
     * 使用两次查询模式（避免 JOIN 在大数据量时不可控）：
     * 1. 从关联表 textbook_chapter 查询 chapter_uri 列表
     * 2. 用 chapter_uri 列表查章节表
     */
    @Override
    public List<KgChapter> findAllActiveByTextbookUris(List<String> textbookUris) {
        if (textbookUris == null || textbookUris.isEmpty()) {
            return List.of();
        }
        // 第一次查询：从关联表获取 chapter_uris
        List<String> chapterUris = kgTextbookChapterMapper.selectChapterUrisByTextbookUris(textbookUris);
        if (chapterUris.isEmpty()) {
            return List.of();
        }
        // 第二次查询：用 uris 查章节实体
        return KgChapterPo.toEntityList(kgChapterMapper.selectByUris(chapterUris));
    }
}
