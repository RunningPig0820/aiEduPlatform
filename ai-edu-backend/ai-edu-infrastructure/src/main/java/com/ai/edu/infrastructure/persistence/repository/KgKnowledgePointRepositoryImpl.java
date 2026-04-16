package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgKnowledgePointMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识点仓储实现
 */
@Repository
public class KgKnowledgePointRepositoryImpl implements KgKnowledgePointRepository {

    @Resource
    private KgKnowledgePointMapper kgKnowledgePointMapper;

    @Override
    public KgKnowledgePoint save(KgKnowledgePoint knowledgePoint) {
        if (knowledgePoint.getId() == null) {
            kgKnowledgePointMapper.insert(knowledgePoint);
        } else {
            kgKnowledgePointMapper.updateById(knowledgePoint);
        }
        return knowledgePoint;
    }

    @Override
    public Optional<KgKnowledgePoint> findById(Long id) {
        return Optional.ofNullable(kgKnowledgePointMapper.selectById(id));
    }

    @Override
    public Optional<KgKnowledgePoint> findByUri(String uri) {
        return Optional.ofNullable(kgKnowledgePointMapper.selectByUri(uri));
    }

    @Override
    public List<KgKnowledgePoint> findByUris(List<String> uris) {
        return kgKnowledgePointMapper.selectByUris(uris);
    }

    @Override
    public List<KgKnowledgePoint> findByStatus(String status) {
        return kgKnowledgePointMapper.selectByStatus(status);
    }

    @Override
    public void updateStatus(String uri, String status) {
        kgKnowledgePointMapper.updateStatus(uri, status, 0L);
    }
}
