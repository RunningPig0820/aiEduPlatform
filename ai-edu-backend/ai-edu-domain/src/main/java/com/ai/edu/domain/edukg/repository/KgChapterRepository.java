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

    /**
     * 查询所有活跃的章节
     */
    List<KgChapter> findAllActive();

    /**
     * 查询与指定教材 URI 列表关联的活跃章节（用于按 grade 范围隔离）
     */
    List<KgChapter> findAllActiveByTextbookUris(List<String> textbookUris);

    /**
     * 统计活跃章节数量（用于对账）
     */
    int countActive();
}
