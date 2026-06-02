package com.ai.edu.infrastructure.persistence.organization.repository;

import com.ai.edu.domain.organization.model.entity.School;
import com.ai.edu.domain.organization.model.valueobject.SchoolInstitutionalType;
import com.ai.edu.domain.organization.repository.SchoolRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.infrastructure.persistence.organization.mapper.SchoolMapper;
import com.ai.edu.infrastructure.persistence.organization.po.SchoolPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学校仓储实现
 * 实现 Entity 和 PO 的转换
 */
@Slf4j
@Repository
@DS("org")
public class SchoolRepositoryImpl implements SchoolRepository {

    @Resource
    private SchoolMapper schoolMapper;

    @Override
    public School save(School school) {
        // 调试：打印当前数据源
        String currentDs = DynamicDataSourceContextHolder.peek();
        log.info("当前数据源: {}", currentDs);

        SchoolPO po = toPO(school);
        log.info("准备插入 SchoolPO: name={}, schoolType={}, status={}, iconUrl={}",
                po.getName(), po.getSchoolType(), po.getStatus(), po.getIconUrl());

        if (school.getId() == null) {
            schoolMapper.insert(po);
            school.setId(SchoolId.of(po.getId()));
        } else {
            schoolMapper.updateById(po);
        }
        return school;
    }

    @Override
    public Optional<School> findById(SchoolId id) {
        SchoolPO po = schoolMapper.selectById(id.getValue());
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Optional<School> findByCode(String code) {
        SchoolPO po = schoolMapper.selectByCode(code);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Optional<School> findByName(String name) {
        SchoolPO po = schoolMapper.selectByName(name);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<School> findAll() {
        List<SchoolPO> poList = schoolMapper.selectAll();
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<School> findByProvinceAndCity(String province, String city) {
        List<SchoolPO> poList = schoolMapper.selectByProvinceAndCity(province, city);
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<School> findByInstitutionalType(SchoolInstitutionalType institutionalType) {
        List<SchoolPO> poList = schoolMapper.selectByInstitutionalType(institutionalType.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<School> findAllActive() {
        List<SchoolPO> poList = schoolMapper.selectAllActive();
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return schoolMapper.existsByCode(code);
    }

    @Override
    public boolean existsByName(String name) {
        return schoolMapper.existsByName(name);
    }

    @Override
    public void deleteById(SchoolId id) {
        SchoolPO po = schoolMapper.selectById(id.getValue());
        if (po != null) {
            po.setDeleted(true);
            schoolMapper.updateById(po);
        }
    }

    @Override
    public void restoreById(SchoolId id) {
        SchoolPO po = schoolMapper.selectById(id.getValue());
        if (po != null) {
            po.setDeleted(false);
            schoolMapper.updateById(po);
        }
    }

    // ==================== 转换方法 ====================

    /**
     * PO 转 Entity
     */
    private School toEntity(SchoolPO po) {
        SchoolInstitutionalType institutionalType = null;
        if (po.getSchoolType() != null && !po.getSchoolType().isEmpty()) {
            institutionalType = SchoolInstitutionalType.of(po.getSchoolType());
        }

        School school = School.create(
            po.getName(),
            po.getCode(),
            institutionalType
        );

        if (po.getId() != null) {
            school.setId(SchoolId.of(po.getId()));
        }

        if (po.getProvince() != null || po.getCity() != null || po.getDistrict() != null || po.getAddress() != null) {
            school.updateAddress(po.getProvince(), po.getCity(), po.getDistrict(), po.getAddress());
        }

        if (po.getDescription() != null) {
            school.updateDescription(po.getDescription());
        }

        if (po.getIconUrl() != null) {
            school.setIconUrl(po.getIconUrl());
        }

        if (po.getStages() != null) {
            school.setStages(po.getStages());
        }

        if (po.getStatus() != null) {
            school.setStatus(po.getStatus());
        }

        return school;
    }

    /**
     * Entity 转 PO
     */
    private SchoolPO toPO(School school) {
        SchoolPO po = new SchoolPO();

        if (school.getId() != null) {
            po.setId(school.getId().getValue());
        }

        po.setName(school.getName());
        po.setCode(school.getCode());
        po.setProvince(school.getProvince());
        po.setCity(school.getCity());
        po.setDistrict(school.getDistrict());
        po.setAddress(school.getAddress());
        po.setSchoolType(school.getSchoolTypeValue());
        po.setIconUrl(school.getIconUrl());
        po.setStages(school.getStages());
        po.setStatus(school.getStatus() != null ? school.getStatus() : "ACTIVE");
        po.setDescription(school.getDescription());
        po.setDeleted(school.isDeleted());

        return po;
    }
}