package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.DerivedKpObsMapper;
import com.ai.edu.infrastructure.persistence.learning.po.DerivedKpObsPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 个体派生观测仓储实现（PO ↔ 实体桥接，UPSERT 幂等去重）。
 *
 * <p>kp_uri 非空走 UNIQUE 去重（mapper.upsert 命中则 occurrence_count +1）；
 * kp_uri 为空（PENDING）不参与 UNIQUE，先查同生同题型 PENDING 再决定插/累加。
 */
@Repository
public class DerivedKpObsRepositoryImpl implements DerivedKpObsRepository {

    @Resource
    private DerivedKpObsMapper derivedKpObsMapper;

    @Override
    public DerivedKpObs upsert(DerivedKpObs obs) {
        DerivedKpObsPo po = DerivedKpObsPo.from(obs);
        if (obs.getKpUri() == null || obs.getKpUri().isBlank()) {
            DerivedKpObsPo existing = derivedKpObsMapper.selectPending(obs.getStudentId(), obs.getTopicLabel());
            if (existing != null) {
                derivedKpObsMapper.incrementOccurrence(existing.getId());
                DerivedKpObs result = existing.toEntity();
                result.incrementOccurrence();
                return result;
            }
            derivedKpObsMapper.insert(po);
        } else {
            derivedKpObsMapper.upsert(po);
        }
        if (po.getId() != null) {
            obs.setId(po.getId());
        }
        return obs;
    }

    @Override
    public List<DerivedKpObs> findByStudentId(Long studentId) {
        return DerivedKpObsPo.toEntityList(derivedKpObsMapper.selectByStudentId(studentId));
    }

    @Override
    public List<DerivedKpObs> findByTopicLabel(String topicLabel) {
        return DerivedKpObsPo.toEntityList(derivedKpObsMapper.selectByTopicLabel(topicLabel));
    }

    @Override
    public List<DerivedKpObs> findResolved() {
        return DerivedKpObsPo.toEntityList(derivedKpObsMapper.selectResolved());
    }
}
