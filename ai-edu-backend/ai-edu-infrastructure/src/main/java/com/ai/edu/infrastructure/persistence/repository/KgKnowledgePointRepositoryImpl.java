package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgKnowledgePointMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgKnowledgePointPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识点仓储实现
 */
@Repository
public class KgKnowledgePointRepositoryImpl implements KgKnowledgePointRepository {

    @Resource
    private KgKnowledgePointMapper kgKnowledgePointMapper;

    @Override
    public KgKnowledgePoint save(KgKnowledgePoint knowledgePoint) {
        KgKnowledgePointPo po = KgKnowledgePointPo.from(knowledgePoint);
        if (po.getId() == null) {
            kgKnowledgePointMapper.insert(po);
            knowledgePoint.setId(po.getId());
        } else {
            kgKnowledgePointMapper.updateById(po);
        }
        return knowledgePoint;
    }

    @Override
    public Optional<KgKnowledgePoint> findById(Long id) {
        KgKnowledgePointPo po = kgKnowledgePointMapper.selectById(id);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public Optional<KgKnowledgePoint> findByUri(String uri) {
        KgKnowledgePointPo po = kgKnowledgePointMapper.selectByUri(uri);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public List<KgKnowledgePoint> findByUris(List<String> uris) {
        return KgKnowledgePointPo.toEntityList(kgKnowledgePointMapper.selectByUris(uris));
    }

    @Override
    public int upsert(List<KgKnowledgePoint> knowledgePoints) {
        if (knowledgePoints == null || knowledgePoints.isEmpty()) {
            return 0;
        }
        List<String> uris = knowledgePoints.stream().map(KgKnowledgePoint::getUri).toList();
        List<KgKnowledgePointPo> existingPos = kgKnowledgePointMapper.selectByUris(uris);
        Set<String> existingUris = existingPos.stream().map(KgKnowledgePointPo::getUri).collect(Collectors.toSet());

        int count = 0;
        for (KgKnowledgePoint kp : knowledgePoints) {
            if (!existingUris.contains(kp.getUri())) {
                kgKnowledgePointMapper.insert(KgKnowledgePointPo.from(kp));
                count++;
            }
        }
        return count;
    }

    @Override
    public List<KgKnowledgePoint> findByStatus(String status) {
        return KgKnowledgePointPo.toEntityList(kgKnowledgePointMapper.selectByStatus(status));
    }

    @Override
    public void updateStatus(String uri, String status) {
        kgKnowledgePointMapper.updateStatus(uri, status, 0L);
    }

    @Override
    public int countActive() {
        return kgKnowledgePointMapper.selectByStatus("active").size();
    }
}
