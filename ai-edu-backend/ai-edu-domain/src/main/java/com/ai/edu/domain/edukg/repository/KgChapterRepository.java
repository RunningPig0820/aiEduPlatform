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

    /**
     * 批量 UPSERT：按 URI 判断 insert（章节只插入新增，不更新已有）
     * @return 插入的数量
     */
    int upsert(List<KgChapter> chapters);

    void updateStatus(String uri, String status);
}
