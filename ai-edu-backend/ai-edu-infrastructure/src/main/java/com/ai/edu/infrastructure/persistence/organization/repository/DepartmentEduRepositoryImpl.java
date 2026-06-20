package com.ai.edu.infrastructure.persistence.organization.repository;

import com.ai.edu.domain.organization.model.entity.DepartmentEdu;
import com.ai.edu.domain.organization.model.valueobject.DepartmentEduId;
import com.ai.edu.domain.organization.model.valueobject.enums.DeptEduTypeEnum;
import com.ai.edu.domain.organization.repository.DepartmentEduRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.infrastructure.persistence.organization.mapper.DepartmentEduMapper;
import com.ai.edu.infrastructure.persistence.organization.po.DepartmentEduPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 教育部门扩展属性仓储实现
 */
@Slf4j
@Repository
@DS("org")
public class DepartmentEduRepositoryImpl implements DepartmentEduRepository {

    @Resource
    private DepartmentEduMapper departmentEduMapper;

    @Override
    public DepartmentEdu save(DepartmentEdu departmentEdu) {
        DepartmentEduPO po = toPO(departmentEdu);

        if (departmentEdu.getId() == null) {
            departmentEduMapper.insert(po);
            departmentEdu.setId(DepartmentEduId.of(po.getId()));
        } else {
            departmentEduMapper.updateById(po);
        }
        return departmentEdu;
    }

    @Override
    public Optional<DepartmentEdu> findById(DepartmentEduId id) {
        DepartmentEduPO po = departmentEduMapper.selectById(id.getValue());
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Optional<DepartmentEdu> findByDeptId(Long deptId) {
        DepartmentEduPO po = departmentEduMapper.selectByDeptId(deptId);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<DepartmentEdu> findBySchoolId(SchoolId schoolId) {
        List<DepartmentEduPO> poList = departmentEduMapper.selectBySchoolId(schoolId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteByDeptId(Long deptId) {
        // 先查主键 ID，再用 MyBatis-Plus 内置逻辑删除
        DepartmentEduPO po = departmentEduMapper.selectByDeptId(deptId);
        if (po != null) {
            departmentEduMapper.deleteById(po.getId());
        }
    }

    // ==================== 转换方法 ====================

    private DepartmentEdu toEntity(DepartmentEduPO po) {
        return DepartmentEdu.fromPO(
                po.getId(),
                po.getDeptId(),
                SchoolId.of(po.getSchoolId()),
                DeptEduTypeEnum.of(po.getDeptType()),
                po.getStageCode(),
                po.getStageYearCode(),
                po.getGradeCode(),
                po.getEnrollmentYear(),
                po.getCreatedBy(),
                po.getModifiedBy(),
                po.getCreatedOn(),
                po.getModifiedOn(),
                po.getDeleted() != null && po.getDeleted()
        );
    }

    private DepartmentEduPO toPO(DepartmentEdu departmentEdu) {
        DepartmentEduPO po = new DepartmentEduPO();

        if (departmentEdu.getId() != null) {
            po.setId(departmentEdu.getIdValue());
        }

        po.setDeptId(departmentEdu.getDeptId());
        po.setSchoolId(departmentEdu.getSchoolIdValue());
        po.setDeptType(departmentEdu.getDeptTypeValue());
        po.setStageCode(departmentEdu.getStageCode());
        po.setStageYearCode(departmentEdu.getStageYearCode());
        po.setGradeCode(departmentEdu.getGradeCode());
        po.setEnrollmentYear(departmentEdu.getEnrollmentYear());
        po.setCreatedBy(departmentEdu.getCreatedBy());
        po.setModifiedBy(departmentEdu.getModifiedBy());
        po.setDeleted(departmentEdu.isDeleted());

        return po;
    }
}
