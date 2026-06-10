package com.ai.edu.infrastructure.persistence.organization.repository;

import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.model.valueobject.enums.ClassStatusEnum;
import com.ai.edu.domain.organization.model.valueobject.GradeLevel;
import com.ai.edu.domain.organization.model.valueobject.SchoolYear;
import com.ai.edu.domain.organization.model.valueobject.enums.ClassTypeEnum;
import com.ai.edu.domain.organization.repository.ClassRepository;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.infrastructure.persistence.organization.mapper.ClassMapper;
import com.ai.edu.infrastructure.persistence.organization.po.ClassPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 班级仓储实现
 * 实现 Entity 和 PO 的转换
 */
@Repository
public class ClassRepositoryImpl implements ClassRepository {

    @Resource
    private ClassMapper classMapper;

    @Override
    public Class save(Class classEntity) {
        ClassPO po = toPO(classEntity);

        if (classEntity.getId() == null) {
            classMapper.insert(po);
            classEntity.setId(ClassId.of(po.getId()));
        } else {
            classMapper.updateById(po);
        }
        return classEntity;
    }

    @Override
    public Optional<Class> findById(ClassId id) {
        ClassPO po = classMapper.selectById(id.getValue());
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Optional<Class> findByCode(String code) {
        ClassPO po = classMapper.selectByCode(code).orElse(null);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<Class> findBySchoolId(SchoolId schoolId) {
        List<ClassPO> poList = classMapper.selectBySchoolId(schoolId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<Class> findByGrade(GradeLevel grade) {
        List<ClassPO> poList = classMapper.selectByGrade(grade.toString());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<Class> findBySchoolYear(SchoolYear schoolYear) {
        List<ClassPO> poList = classMapper.selectBySchoolYear(schoolYear.toString());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<Class> findByStatus(ClassStatusEnum status) {
        List<ClassPO> poList = classMapper.selectByStatus(status.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<Class> findActiveBySchoolId(SchoolId schoolId) {
        List<ClassPO> poList = classMapper.selectActiveBySchoolId(schoolId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<Class> findActiveById(ClassId id) {
        ClassPO po = classMapper.selectActiveById(id.getValue()).orElse(null);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public boolean existsByNameAndSchoolYear(String name, SchoolYear schoolYear) {
        return classMapper.existsByNameAndSchoolYear(name, schoolYear.toString());
    }

    @Override
    public int countBySchoolIdAndStatus(SchoolId schoolId, ClassStatusEnum status) {
        return classMapper.countBySchoolIdAndStatus(schoolId.getValue(), status.getValue());
    }

    @Override
    public void deleteById(ClassId id) {
        ClassPO po = classMapper.selectById(id.getValue());
        if (po != null) {
            po.setDeleted(true);
            classMapper.updateById(po);
        }
    }

    @Override
    public void restoreById(ClassId id) {
        ClassPO po = classMapper.selectById(id.getValue());
        if (po != null) {
            po.setDeleted(false);
            classMapper.updateById(po);
        }
    }

    // ==================== 转换方法 ====================

    /**
     * PO 转 Entity
     */
    private Class toEntity(ClassPO po) {
        GradeLevel grade = null;
        if (po.getGrade() != null && !po.getGrade().isEmpty()) {
            try {
                grade = GradeLevel.of(Integer.parseInt(po.getGrade()));
            } catch (NumberFormatException ignored) {
                // 如果 grade 字段不是数字，保持 null
            }
        }

        SchoolYear schoolYear = null;
        if (po.getSchoolYear() != null && !po.getSchoolYear().isEmpty()) {
            schoolYear = SchoolYear.of(po.getSchoolYear());
        }

        Class cls = Class.create(
            po.getName(),
            grade,
            schoolYear
        );

        if (po.getId() != null) {
            cls.setId(ClassId.of(po.getId()));
        }

        if (po.getSchoolId() != null) {
            cls.assignSchool(SchoolId.of(po.getSchoolId()));
        }

        if (po.getCode() != null) {
            cls.setCode(po.getCode());
        }

        if (po.getStatus() != null) {
            cls.setStatus(ClassStatusEnum.of(po.getStatus()));
        }

        if (po.getClassType() != null) {
            cls.setClassType(ClassTypeEnum.of(po.getClassType()));
        }

        return cls;
    }

    /**
     * Entity 转 PO
     */
    private ClassPO toPO(Class classEntity) {
        ClassPO po = new ClassPO();

        if (classEntity.getId() != null) {
            po.setId(classEntity.getId().getValue());
        }

        po.setSchoolId(classEntity.getSchoolIdValue());
        po.setName(classEntity.getName());
        po.setCode(classEntity.getCode());
        po.setGrade(classEntity.getGradeValue());
        po.setSchoolYear(classEntity.getSchoolYearValue());
        po.setStatus(classEntity.getStatusValue() != null ? classEntity.getStatusValue() : "ACTIVE");
        po.setClassType(classEntity.getClassTypeValue());
        po.setDescription(classEntity.getDescription());
        po.setDeleted(classEntity.isDeleted());

        return po;
    }
}