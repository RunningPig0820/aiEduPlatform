package com.ai.edu.infrastructure.persistence.organization.repository;

import com.ai.edu.domain.organization.model.entity.SchoolUserAssociation;
import com.ai.edu.domain.organization.model.valueobject.SchoolUserAssociationId;
import com.ai.edu.domain.organization.model.valueobject.SchoolUserRole;
import com.ai.edu.domain.organization.repository.SchoolUserRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.domain.shared.valueobject.UserId;
import com.ai.edu.infrastructure.persistence.organization.mapper.SchoolUserMapper;
import com.ai.edu.infrastructure.persistence.organization.po.SchoolUserPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学校用户关联仓储实现
 */
@Repository
public class SchoolUserRepositoryImpl implements SchoolUserRepository {

    @Resource
    private SchoolUserMapper schoolUserMapper;

    @Override
    public SchoolUserAssociation save(SchoolUserAssociation association) {
        SchoolUserPO po = toPO(association);

        if (association.getId() == null) {
            schoolUserMapper.insert(po);
            association.setId(SchoolUserAssociationId.of(po.getId()));
        } else {
            schoolUserMapper.updateById(po);
        }
        return association;
    }

    @Override
    public Optional<SchoolUserAssociation> findById(Long id) {
        SchoolUserPO po = schoolUserMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<SchoolUserAssociation> findByUserId(UserId userId) {
        List<SchoolUserPO> poList = schoolUserMapper.selectByUserId(userId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<SchoolUserAssociation> findBySchoolId(SchoolId schoolId) {
        List<SchoolUserPO> poList = schoolUserMapper.selectBySchoolId(schoolId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<SchoolUserAssociation> findBySchoolIdAndUserId(SchoolId schoolId, UserId userId) {
        SchoolUserPO po = schoolUserMapper.selectBySchoolIdAndUserId(schoolId.getValue(), userId.getValue());
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public boolean existsBySchoolIdAndUserId(SchoolId schoolId, UserId userId) {
        return schoolUserMapper.existsBySchoolIdAndUserId(schoolId.getValue(), userId.getValue());
    }

    @Override
    public void deleteById(Long id) {
        SchoolUserPO po = schoolUserMapper.selectById(id);
        if (po != null) {
            po.setDeleted(true);
            schoolUserMapper.updateById(po);
        }
    }

    @Override
    public void deleteBySchoolIdAndUserId(SchoolId schoolId, UserId userId) {
        schoolUserMapper.deleteBySchoolIdAndUserId(schoolId.getValue(), userId.getValue());
    }

    @Override
    public void deleteBySchoolId(SchoolId schoolId) {
        schoolUserMapper.deleteBySchoolId(schoolId.getValue());
    }

    @Override
    public long countBySchoolId(SchoolId schoolId) {
        return schoolUserMapper.countBySchoolId(schoolId.getValue());
    }

    @Override
    public long countByUserId(UserId userId) {
        return schoolUserMapper.countByUserId(userId.getValue());
    }

    /**
     * 转换PO到领域实体
     */
    private SchoolUserAssociation toEntity(SchoolUserPO po) {
        SchoolUserAssociation association = SchoolUserAssociation.create(
                SchoolId.of(po.getSchoolId()),
                UserId.of(po.getUserId()),
                SchoolUserRole.of(po.getRoleType())
        );
        association.setId(SchoolUserAssociationId.of(po.getId()));
        return association;
    }

    /**
     * 转换领域实体到PO
     */
    private SchoolUserPO toPO(SchoolUserAssociation association) {
        SchoolUserPO po = new SchoolUserPO();
        if (association.getId() != null) {
            po.setId(association.getId().getValue());
        }
        po.setSchoolId(association.getSchoolId().getValue());
        po.setUserId(association.getUserId().getValue());
        po.setRoleType(association.getRole().getValue());
        po.setCreatedAt(association.getCreatedAt());
        po.setDeleted(false);
        return po;
    }
}