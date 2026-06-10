package com.ai.edu.infrastructure.persistence.organization.repository;

import com.ai.edu.domain.organization.model.entity.School;
import com.ai.edu.domain.organization.model.valueobject.SchoolInstitutionalType;
import com.ai.edu.domain.organization.model.valueobject.SchoolQueryParam;
import com.ai.edu.domain.organization.model.valueobject.enums.SchoolStatusEnum;
import com.ai.edu.domain.organization.repository.SchoolRepository;
import com.ai.edu.domain.shared.valueobject.PageResult;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.infrastructure.persistence.organization.mapper.SchoolMapper;
import com.ai.edu.infrastructure.persistence.organization.po.SchoolPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public PageResult<School> queryPage(SchoolQueryParam param) {
        // 构建查询条件
        LambdaQueryWrapper<SchoolPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchoolPO::getDeleted, false);  // 未删除

        // ID 精确查询
        if (param.getId() != null) {
            wrapper.eq(SchoolPO::getId, param.getId());
        }

        // 名称模糊查询
        if (param.getName() != null && !param.getName().isBlank()) {
            wrapper.like(SchoolPO::getName, param.getName());
        }

        // 类型查询
        if (param.getType() != null && !param.getType().isBlank()) {
            wrapper.eq(SchoolPO::getSchoolType, param.getType());
        }

        // 按创建时间倒序
        wrapper.orderByDesc(SchoolPO::getCreatedAt);

        // 分页查询
        Page<SchoolPO> page = new Page<>(param.getPageNum(), param.getPageSize());
        Page<SchoolPO> result = schoolMapper.selectPage(page, wrapper);

        // 转换结果
        List<School> list = result.getRecords().stream()
                .map(this::toEntity)
                .toList();

        return PageResult.<School>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
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

        SchoolStatusEnum status = SchoolStatusEnum.ACTIVE;
        if (po.getStatus() != null && !po.getStatus().isEmpty()) {
            status = SchoolStatusEnum.of(po.getStatus());
        }

        School school = School.create(
            po.getName(),
            institutionalType,
            po.getIconUrl(),
            po.getStages(),
            po.getProvince(),
            po.getCity(),
            po.getDistrict(),
            po.getAddress(),
            po.getDescription()
        );

        if (po.getId() != null) {
            school.setId(SchoolId.of(po.getId()));
        }

        school.setStatus(status);
        school.setCreatedAt(po.getCreatedAt());
        school.setUpdatedAt(po.getUpdatedAt());

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
        po.setProvince(school.getProvince());
        po.setCity(school.getCity());
        po.setDistrict(school.getDistrict());
        po.setAddress(school.getAddress());
        po.setSchoolType(school.getSchoolTypeValue());
        po.setIconUrl(school.getIconUrl());
        po.setStages(school.getStages());
        po.setStatus(school.getStatusValue());
        po.setDescription(school.getDescription());
        po.setDeleted(school.isDeleted());

        return po;
    }
}