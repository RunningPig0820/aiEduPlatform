package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.KgSection;

import java.util.List;
import java.util.Optional;

/**
 * 小节仓储接口
 */
public interface KgSectionRepository {

    KgSection save(KgSection section);

    Optional<KgSection> findById(Long id);

    Optional<KgSection> findByUri(String uri);

    List<KgSection> findByUris(List<String> uris);

    /**
     * 批量 UPSERT：按 URI 判断 insert（小节只插入新增，不更新已有）
     * @return 插入的数量
     */
    int upsert(List<KgSection> sections);

    void updateStatus(String uri, String status);
}
