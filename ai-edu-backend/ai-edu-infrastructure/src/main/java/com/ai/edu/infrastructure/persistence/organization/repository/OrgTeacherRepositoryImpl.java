package com.ai.edu.infrastructure.persistence.organization.repository;

import com.ai.edu.domain.organization.model.entity.OrgTeacher;
import com.ai.edu.domain.organization.model.valueobject.OrgTeacherId;
import com.ai.edu.domain.organization.model.valueobject.OrgTeacherQueryParam;
import com.ai.edu.domain.organization.repository.OrgTeacherRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.infrastructure.persistence.organization.mapper.OrgTeacherMapper;
import com.ai.edu.infrastructure.persistence.organization.po.OrgTeacherPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 教职工仓储实现
 * 实现 Entity 和 PO 的转换
 */
@Slf4j
@Repository
@DS("org")
public class OrgTeacherRepositoryImpl implements OrgTeacherRepository {

    @Resource
    private OrgTeacherMapper orgTeacherMapper;

    @Override
    public OrgTeacher save(OrgTeacher orgTeacher) {
        OrgTeacherPO po = toPO(orgTeacher);

        if (orgTeacher.getId() == null) {
            // 新插入
            orgTeacherMapper.insert(po);
            orgTeacher.setId(OrgTeacherId.of(po.getId()));
        } else {
            // 更新
            orgTeacherMapper.updateById(po);
        }
        return orgTeacher;
    }

    @Override
    public Optional<OrgTeacher> findById(OrgTeacherId id) {
        OrgTeacherPO po = orgTeacherMapper.selectById(id.getValue());
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<OrgTeacher> findBySchoolId(SchoolId schoolId) {
        List<OrgTeacherPO> poList = orgTeacherMapper.selectBySchoolId(schoolId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<OrgTeacher> findByDepartmentId(Long departmentId) {
        List<OrgTeacherPO> poList = orgTeacherMapper.selectByDepartmentId(departmentId);
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<OrgTeacher> findBySchoolIdAndUserId(SchoolId schoolId, Long userId) {
        OrgTeacherPO po = orgTeacherMapper.selectBySchoolIdAndUserId(schoolId.getValue(), userId);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Page<OrgTeacher> queryPage(OrgTeacherQueryParam param, int pageNum, int pageSize) {
        LambdaQueryWrapper<OrgTeacherPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgTeacherPO::getDeleted, false);

        // 学校ID查询
        if (param.hasSchoolId()) {
            wrapper.eq(OrgTeacherPO::getSchoolId, param.getSchoolId());
        }

        // 部门ID查询
        if (param.hasDepartmentId()) {
            wrapper.eq(OrgTeacherPO::getDepartmentId, param.getDepartmentId());
        }

        // 用户ID查询
        if (param.hasUserId()) {
            wrapper.eq(OrgTeacherPO::getUserId, param.getUserId());
        }

        // 按创建时间降序排序
        wrapper.orderByDesc(OrgTeacherPO::getCreatedAt);

        // 分页查询
        Page<OrgTeacherPO> page = new Page<>(pageNum, pageSize);
        Page<OrgTeacherPO> result = orgTeacherMapper.selectPage(page, wrapper);

        List<OrgTeacher> list = result.getRecords().stream()
                .map(this::toEntity)
                .toList();

        Page<OrgTeacher> entityPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        entityPage.setRecords(list);
        return entityPage;
    }

    @Override
    public void deleteById(OrgTeacherId id) {
        OrgTeacherPO po = orgTeacherMapper.selectById(id.getValue());
        if (po != null) {
            po.setDeleted(true);
            orgTeacherMapper.updateById(po);
        }
    }

    @Override
    public List<OrgTeacher> findByIds(List<OrgTeacherId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> idValues = ids.stream()
                .map(OrgTeacherId::getValue)
                .toList();

        LambdaQueryWrapper<OrgTeacherPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrgTeacherPO::getId, idValues);
        wrapper.eq(OrgTeacherPO::getDeleted, false);

        List<OrgTeacherPO> poList = orgTeacherMapper.selectList(wrapper);
        return poList.stream().map(this::toEntity).toList();
    }

    // ==================== 转换方法 ====================

    private OrgTeacher toEntity(OrgTeacherPO po) {
        return OrgTeacher.fromPO(
                po.getId(),
                SchoolId.of(po.getSchoolId()),
                po.getUserId(),
                po.getDepartmentId(),
                po.getCreatedBy(),
                po.getModifiedBy(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getDeleted() != null && po.getDeleted()
        );
    }

    private OrgTeacherPO toPO(OrgTeacher orgTeacher) {
        OrgTeacherPO po = new OrgTeacherPO();

        if (orgTeacher.getId() != null) {
            po.setId(orgTeacher.getIdValue());
        }

        po.setSchoolId(orgTeacher.getSchoolIdValue());
        po.setUserId(orgTeacher.getUserId());
        po.setDepartmentId(orgTeacher.getDepartmentId());
        po.setCreatedBy(orgTeacher.getCreatedBy());
        po.setModifiedBy(orgTeacher.getModifiedBy());
        po.setDeleted(orgTeacher.isDeleted());

        return po;
    }
}