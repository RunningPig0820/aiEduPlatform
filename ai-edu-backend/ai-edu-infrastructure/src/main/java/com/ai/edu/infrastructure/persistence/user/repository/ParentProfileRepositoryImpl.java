package com.ai.edu.infrastructure.persistence.user.repository;

import com.ai.edu.domain.user.model.entity.ParentProfile;
import com.ai.edu.domain.user.repository.ParentProfileRepository;
import com.ai.edu.infrastructure.persistence.user.mapper.ParentProfileMapper;
import com.ai.edu.infrastructure.persistence.user.po.ParentProfilePO;
import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 家长信息仓储实现
 */
@Slf4j
@Repository
@DS("user")
public class ParentProfileRepositoryImpl implements ParentProfileRepository {

    @Resource
    private ParentProfileMapper parentProfileMapper;

    @Override
    public ParentProfile save(ParentProfile profile) {
        ParentProfilePO po = toPO(profile);
        if (profile.getId() == null) {
            parentProfileMapper.insert(po);
            profile.setId(po.getId());
        } else {
            parentProfileMapper.updateById(po);
        }
        return profile;
    }

    @Override
    public List<ParentProfile> saveAll(List<ParentProfile> profiles) {
        List<ParentProfile> savedList = new ArrayList<>();
        for (ParentProfile profile : profiles) {
            savedList.add(save(profile));
        }
        return savedList;
    }

    @Override
    public List<ParentProfile> findByStudentUserId(Long studentUserId) {
        List<ParentProfilePO> poList = parentProfileMapper.selectByStudentUserId(studentUserId);
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<ParentProfile> findByParentUserId(Long parentUserId) {
        List<ParentProfilePO> poList = parentProfileMapper.selectByParentUserId(parentUserId);
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteByStudentUserId(Long studentUserId) {
        parentProfileMapper.deleteByStudentUserId(studentUserId);
    }

    // ==================== 转换方法 ====================

    private ParentProfile toEntity(ParentProfilePO po) {
        return ParentProfile.fromPO(
                po.getId(),
                po.getStudentUserId(),
                po.getParentUserId(),
                po.getRelationship(),
                po.getIsPrimary(),
                po.getCreatedBy(),
                po.getModifiedBy(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getDeleted() != null && po.getDeleted()
        );
    }

    private ParentProfilePO toPO(ParentProfile entity) {
        ParentProfilePO po = new ParentProfilePO();
        if (entity.getId() != null) {
            po.setId(entity.getId());
        }
        po.setStudentUserId(entity.getStudentUserId());
        po.setParentUserId(entity.getParentUserId());
        po.setRelationship(entity.getRelationship());
        po.setIsPrimary(entity.getIsPrimary());
        po.setCreatedBy(entity.getCreatedBy());
        po.setModifiedBy(entity.getModifiedBy());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.isDeleted());
        return po;
    }
}
