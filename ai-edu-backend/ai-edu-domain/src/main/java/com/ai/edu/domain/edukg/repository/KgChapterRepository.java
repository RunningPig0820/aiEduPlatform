package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.KgChapter;

import java.util.List;
import java.util.Optional;

/**
 * 章节仓储接口
 */
public interface KgChapterRepository {

    KgChapter save(KgChapter chapter);

    Optional<KgChapter> findById(Long id);

    Optional<KgChapter> findByUri(String uri);

    List<KgChapter> findByUris(List<String> uris);

    void updateStatus(String uri, String status);
}
