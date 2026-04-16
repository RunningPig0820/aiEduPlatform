package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;

import java.util.List;
import java.util.Optional;

/**
 * 知识点仓储接口
 */
public interface KgKnowledgePointRepository {

    KgKnowledgePoint save(KgKnowledgePoint knowledgePoint);

    Optional<KgKnowledgePoint> findById(Long id);

    Optional<KgKnowledgePoint> findByUri(String uri);

    List<KgKnowledgePoint> findByUris(List<String> uris);

    List<KgKnowledgePoint> findByStatus(String status);

    void updateStatus(String uri, String status);
}
