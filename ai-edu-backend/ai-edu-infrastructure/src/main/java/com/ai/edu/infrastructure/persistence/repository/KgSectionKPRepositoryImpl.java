package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.repository.KgSectionKPRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgSectionKPMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgSectionKPPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 小节-知识点关联仓储实现
 */
@Repository
public class KgSectionKPRepositoryImpl implements KgSectionKPRepository {

    @Resource
    private KgSectionKPMapper kgSectionKPMapper;

    @Override
    public KgSectionKP save(KgSectionKP relation) {
        KgSectionKPPo po = KgSectionKPPo.from(relation);
        if (po.getId() == null) {
            kgSectionKPMapper.insert(po);
            relation.setId(po.getId());
        } else {
            kgSectionKPMapper.updateById(po);
        }
        return relation;
    }

    @Override
    public void saveBatch(List<KgSectionKP> relations) {
        if (relations != null && !relations.isEmpty()) {
            kgSectionKPMapper.batchInsert(KgSectionKPPo.fromList(relations));
        }
    }

    @Override
    public void deleteBySectionUri(String sectionUri) {
        kgSectionKPMapper.deleteBySectionUri(sectionUri, 0L);
    }

    @Override
    public void deleteByKpUri(String kpUri) {
        kgSectionKPMapper.deleteByKpUri(kpUri, 0L);
    }

    @Override
    public List<KgSectionKP> findBySectionUri(String sectionUri) {
        return KgSectionKPPo.toEntityList(kgSectionKPMapper.selectBySectionUri(sectionUri));
    }

    @Override
    public List<KgSectionKP> findByKpUri(String kpUri) {
        return KgSectionKPPo.toEntityList(kgSectionKPMapper.selectByKpUri(kpUri));
    }

    @Override
    public List<KgSectionKP> findAllActive() {
        return KgSectionKPPo.toEntityList(kgSectionKPMapper.selectAllActiveRelations());
    }
}
