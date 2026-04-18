package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.edukg.model.entity.KgSyncRecord;
import com.ai.edu.domain.edukg.repository.KgSyncRecordRepository;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgSyncRecordMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgSyncRecordPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 同步记录仓储实现
 */
@Repository
public class KgSyncRecordRepositoryImpl implements KgSyncRecordRepository {

    @Resource
    private KgSyncRecordMapper kgSyncRecordMapper;

    @Override
    public KgSyncRecord save(KgSyncRecord record) {
        KgSyncRecordPo po = KgSyncRecordPo.from(record);
        if (po.getId() == null) {
            kgSyncRecordMapper.insert(po);
            record.setId(po.getId());
        } else {
            kgSyncRecordMapper.updateById(po);
        }
        return record;
    }

    @Override
    public List<KgSyncRecord> findRecent(int limit) {
        return KgSyncRecordPo.toEntityList(kgSyncRecordMapper.selectRecent(limit));
    }

    @Override
    public Optional<KgSyncRecord> findById(Long id) {
        KgSyncRecordPo po = kgSyncRecordMapper.selectById(id);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }
}
