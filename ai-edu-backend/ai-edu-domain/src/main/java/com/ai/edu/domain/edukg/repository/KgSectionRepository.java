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

    void updateStatus(String uri, String status);
}
