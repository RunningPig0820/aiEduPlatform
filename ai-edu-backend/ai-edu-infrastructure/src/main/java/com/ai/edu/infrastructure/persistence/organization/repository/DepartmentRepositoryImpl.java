package com.ai.edu.infrastructure.persistence.organization.repository;

import com.ai.edu.domain.organization.model.entity.Department;
import com.ai.edu.domain.organization.model.valueobject.DepartmentId;
import com.ai.edu.domain.organization.model.valueobject.DepartmentQueryParam;
import com.ai.edu.domain.organization.model.valueobject.enums.DepartmentTypeEnum;
import com.ai.edu.domain.shared.valueobject.PageResult;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.domain.organization.repository.DepartmentRepository;
import com.ai.edu.infrastructure.persistence.organization.mapper.DepartmentMapper;
import com.ai.edu.infrastructure.persistence.organization.po.DepartmentPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 部门仓储实现
 * 实现 Entity 和 PO 的转换
 */
@Slf4j
@Repository
@DS("org")
public class DepartmentRepositoryImpl implements DepartmentRepository {

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public Department saveOrUpdate(Department department) {
        DepartmentPO po = toPO(department);

        if (department.getId() == null) {
            // 新插入：先插入获取ID，再更新departmentPath
            departmentMapper.insert(po);
            department.setId(DepartmentId.of(po.getId()));

            // 更新 departmentPath（包含自己的ID）
            department.updateDepartmentPathAfterSave(po.getId());
            po.setDepartmentPath(department.getDepartmentPath());
            departmentMapper.updateById(po);
        } else {
            departmentMapper.updateById(po);
        }
        return department;
    }

    @Override
    public Optional<Department> findById(DepartmentId id) {
        DepartmentPO po = departmentMapper.selectById(id.getValue());
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Optional<Department> findBySchoolIdAndName(SchoolId schoolId, String name) {
        DepartmentPO po = departmentMapper.selectBySchoolIdAndName(schoolId.getValue(), name);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<Department> findBySchoolId(SchoolId schoolId) {
        List<DepartmentPO> poList = departmentMapper.selectBySchoolId(schoolId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<Department> findBySchoolIdAndType(SchoolId schoolId, DepartmentTypeEnum departmentType) {
        List<DepartmentPO> poList = departmentMapper.selectBySchoolIdAndType(
                schoolId.getValue(), departmentType.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<Department> findRootDepartments(SchoolId schoolId) {
        List<DepartmentPO> poList = departmentMapper.selectRootDepartments(schoolId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<Department> findChildren(DepartmentId parentId) {
        List<DepartmentPO> poList = departmentMapper.selectChildren(parentId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<Department> findAllDescendants(DepartmentId departmentId) {
        Department department = findById(departmentId).orElse(null);
        if (department == null) {
            return List.of();
        }

        String path = department.getDepartmentPath();
        // department_path 已经包含自己的ID，子孙部门路径以 "path_" 开头
        String pathPattern = path + "_%";
        List<DepartmentPO> poList = departmentMapper.selectDescendants(pathPattern);
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public PageResult<Department> queryPage(DepartmentQueryParam param) {
        LambdaQueryWrapper<DepartmentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepartmentPO::getDeleted, false);

        // 学校ID查询
        if (param.getSchoolId() != null) {
            wrapper.eq(DepartmentPO::getSchoolId, param.getSchoolId());
        }

        // ID 精确查询
        if (param.getId() != null) {
            wrapper.eq(DepartmentPO::getId, param.getId());
        }

        // 名称模糊查询
        if (param.getName() != null && !param.getName().isBlank()) {
            wrapper.like(DepartmentPO::getName, param.getName());
        }

        // 上级部门查询
        if (param.getParentId() != null) {
            wrapper.eq(DepartmentPO::getParentId, param.getParentId());
        }

        // 只查询根部门
        if (param.getRootOnly() != null && param.getRootOnly()) {
            wrapper.isNull(DepartmentPO::getParentId);
        }

        // 按排序序号和创建时间排序
        wrapper.orderByAsc(DepartmentPO::getSortOrder);
        wrapper.orderByDesc(DepartmentPO::getCreatedAt);

        // 分页查询
        Page<DepartmentPO> page = new Page<>(param.getPageNum(), param.getPageSize());
        Page<DepartmentPO> result = departmentMapper.selectPage(page, wrapper);

        List<Department> list = result.getRecords().stream()
                .map(this::toEntity)
                .toList();

        return PageResult.<Department>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    @Override
    public boolean existsBySchoolIdAndName(SchoolId schoolId, String name) {
        return departmentMapper.existsBySchoolIdAndName(schoolId.getValue(), name);
    }

    @Override
    public boolean hasChildren(DepartmentId id) {
        return departmentMapper.hasChildren(id.getValue());
    }

    @Override
    public void deleteById(DepartmentId id) {
        DepartmentPO po = departmentMapper.selectById(id.getValue());
        if (po != null) {
            po.setDeleted(true);
            departmentMapper.updateById(po);
        }
    }

    @Override
    public void restoreById(DepartmentId id) {
        DepartmentPO po = departmentMapper.selectById(id.getValue());
        if (po != null) {
            po.setDeleted(false);
            departmentMapper.updateById(po);
        }
    }

    @Override
    public void updateDescendantsPath(DepartmentId departmentId, String newParentPath) {
        Department department = findById(departmentId).orElse(null);
        if (department == null || department.getDepartmentPath() == null) {
            return;
        }

        String oldPath = department.getDepartmentPath();
        String newPath = newParentPath != null ? newParentPath : "";

        // 更新子孙部门路径：将 oldPath 替换为 newPath
        String oldPathPattern = oldPath + "_%";
        departmentMapper.updateDescendantsPath(oldPath, newPath, oldPathPattern);
    }

    // ==================== 转换方法 ====================

    private Department toEntity(DepartmentPO po) {
        DepartmentTypeEnum departmentType = po.getDepartmentType() != null
                ? DepartmentTypeEnum.of(po.getDepartmentType())
                : DepartmentTypeEnum.ORG;
        return Department.fromPO(
                po.getId(),
                SchoolId.of(po.getSchoolId()),
                po.getName(),
                po.getParentId(),
                po.getDepartmentPath(),
                departmentType,
                po.getSortOrder(),
                po.getDescription(),
                po.getCreatedBy(),
                po.getModifiedBy(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getDeleted() != null && po.getDeleted()
        );
    }

    private DepartmentPO toPO(Department department) {
        DepartmentPO po = new DepartmentPO();

        if (department.getId() != null) {
            po.setId(department.getIdValue());
        }

        po.setSchoolId(department.getSchoolIdValue());
        po.setName(department.getName());
        po.setParentId(department.getParentId());
        po.setDepartmentPath(department.getDepartmentPath());
        po.setDepartmentType(department.getDepartmentTypeValue());
        po.setSortOrder(department.getSortOrder());
        po.setDescription(department.getDescription());
        po.setCreatedBy(department.getCreatedBy());
        po.setModifiedBy(department.getModifiedBy());
        po.setDeleted(department.isDeleted());

        return po;
    }
}