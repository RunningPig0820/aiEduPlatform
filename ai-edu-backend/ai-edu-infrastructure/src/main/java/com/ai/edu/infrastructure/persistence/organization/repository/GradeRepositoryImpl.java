package com.ai.edu.infrastructure.persistence.organization.repository;

import com.ai.edu.domain.organization.model.entity.Grade;
import com.ai.edu.domain.organization.model.valueobject.GradeLevel;
import com.ai.edu.domain.organization.repository.GradeRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.infrastructure.persistence.organization.mapper.GradeMapper;
import com.ai.edu.infrastructure.persistence.organization.po.GradePO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 年级仓储实现
 * 实现 Entity 和 PO 的转换
 */
@Repository
public class GradeRepositoryImpl implements GradeRepository {

    @Resource
    private GradeMapper gradeMapper;

    @Override
    public Grade save(Grade grade) {
        GradePO po = toPO(grade);

        if (grade.getId() == null) {
            gradeMapper.insert(po);
            grade.setId(po.getId());
        } else {
            gradeMapper.updateById(po);
        }
        return grade;
    }

    @Override
    public Optional<Grade> findById(Long id) {
        GradePO po = gradeMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Optional<Grade> findByCode(String code) {
        GradePO po = gradeMapper.selectByCode(code).orElse(null);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<Grade> findBySchoolId(SchoolId schoolId) {
        List<GradePO> poList = gradeMapper.selectBySchoolId(schoolId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<Grade> findByGradeLevel(GradeLevel gradeLevel) {
        List<GradePO> poList = gradeMapper.selectByGradeLevel(gradeLevel.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<Grade> findAllActive() {
        List<GradePO> poList = gradeMapper.selectAllActive();
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return gradeMapper.existsByCode(code);
    }

    @Override
    public void deleteById(Long id) {
        GradePO po = gradeMapper.selectById(id);
        if (po != null) {
            po.setDeleted(true);
            gradeMapper.updateById(po);
        }
    }

    @Override
    public void restoreById(Long id) {
        GradePO po = gradeMapper.selectById(id);
        if (po != null) {
            po.setDeleted(false);
            gradeMapper.updateById(po);
        }
    }

    // ==================== 转换方法 ====================

    /**
     * PO 转 Entity
     */
    private Grade toEntity(GradePO po) {
        GradeLevel gradeLevel = null;
        if (po.getGradeLevel() != null) {
            gradeLevel = GradeLevel.of(po.getGradeLevel());
        }

        Grade grade = Grade.create(po.getName(), gradeLevel);

        grade.setId(po.getId());

        if (po.getSchoolId() != null) {
            // 需要通过 SchoolId 设置，但 Grade 实体需要支持这个方法
            // Grade.assignSchool 方法不存在，这里暂时不设置
        }

        if (po.getCode() != null) {
            grade.setCode(po.getCode());
        }

        if (po.getDescription() != null) {
            grade.updateDescription(po.getDescription());
        }

        return grade;
    }

    /**
     * Entity 转 PO
     */
    private GradePO toPO(Grade grade) {
        GradePO po = new GradePO();

        po.setId(grade.getId());
        po.setSchoolId(grade.getSchoolIdValue());
        po.setName(grade.getName());
        po.setCode(grade.getCode());
        po.setGradeLevel(grade.getGradeLevelValue());
        po.setDescription(grade.getDescription());
        po.setDeleted(grade.isDeleted());

        return po;
    }
}