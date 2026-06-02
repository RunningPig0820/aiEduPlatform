package com.ai.edu.application.service.org;

import cn.hutool.json.JSONUtil;
import com.ai.edu.application.dto.org.PageResult;
import com.ai.edu.application.dto.org.command.CreateSchoolCommand;
import com.ai.edu.application.dto.org.SchoolDTO;
import com.ai.edu.application.dto.org.command.SchoolQueryCommand;
import com.ai.edu.application.dto.org.command.UpdateSchoolCommand;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.organization.model.entity.School;
import com.ai.edu.domain.organization.model.valueobject.SchoolInstitutionalType;
import com.ai.edu.domain.organization.repository.SchoolRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.infrastructure.persistence.organization.mapper.SchoolMapper;
import com.ai.edu.infrastructure.persistence.organization.po.SchoolPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学校应用服务
 * 处理学校组织的创建、更新、查询等用例
 *
 * 多数据源事务处理：
 * @DS("org") - 在方法执行前切换到 org 数据源
 * @Transactional - 在已切换的数据源上开启事务
 */
@Slf4j
@Service
public class SchoolAppService {

    @Resource
    private SchoolRepository schoolRepository;

    @Resource
    private SchoolMapper schoolMapper;

    /**
     * 创建学校
     */
    @DS("org")
    @Transactional
    public SchoolDTO createSchool(CreateSchoolCommand command) {
        log.info("创建学校: name={}, type={}, stages={}", command.getName(), command.getType(), command.getStages());

        // 1. 检查名称是否已存在
        if (schoolRepository.existsByName(command.getName())) {
            throw new BusinessException(ErrorCode.SCHOOL_NAME_EXISTS, "学校名称已存在");
        }

        // 2. 转换值对象
        SchoolInstitutionalType institutionalType = SchoolInstitutionalType.of(command.getType());

        // 3. 创建学校实体并设置属性
        School school = School.create(command.getName(),  institutionalType);

        // 设置 iconUrl
        if (command.getIconUrl() != null && !command.getIconUrl().isBlank()) {
            school.setIconUrl(command.getIconUrl());
        }

        // 设置 stages（转换为 JSON 字符串）
        if (command.getStages() != null && !command.getStages().isEmpty()) {
            school.setStages(JSONUtil.toJsonStr(command.getStages()));
        }

        // 4. 保存
        School savedSchool = schoolRepository.save(school);

        log.info("学校创建成功: id={}, name={}", savedSchool.getIdValue(), savedSchool.getName());
        return toDTO(savedSchool);
    }

    /**
     * 更新学校
     */
    @DS("org")
    @Transactional
    public SchoolDTO updateSchool(Long id, UpdateSchoolCommand command) {
        log.info("更新学校: id={}, name={}, stages={}", id, command.getName(), command.getStages());

        // 1. 查找学校
        School school = schoolRepository.findById(SchoolId.of(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "学校不存在"));

        // 2. 检查名称冲突（如果修改了名称）
        if (!school.getName().equals(command.getName()) && schoolRepository.existsByName(command.getName())) {
            throw new BusinessException(ErrorCode.SCHOOL_NAME_EXISTS, "学校名称已存在");
        }

        // 3. 更新学校信息
        SchoolInstitutionalType institutionalType = SchoolInstitutionalType.of(command.getType());
        school = updateSchoolFields(school, command, institutionalType);
        schoolRepository.save(school);

        log.info("学校更新成功: id={}", id);

        return toDTO(school);
    }

    /**
     * 获取学校详情
     */
    public SchoolDTO getSchoolById(Long id) {
        log.info("获取学校详情: id={}", id);

        School school = schoolRepository.findById(SchoolId.of(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "学校不存在"));

        return toDTO(school);
    }

    /**
     * 分页查询学校列表
     */
    @DS("org")
    public PageResult<SchoolDTO> querySchools(SchoolQueryCommand query) {
        log.info("分页查询学校: id={}, name={}, type={}, pageNum={}, pageSize={}",
                query.getId(), query.getName(), query.getType(), query.getPageNum(), query.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<SchoolPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchoolPO::getDeleted, false);  // 未删除

        // ID 精确查询
        if (query.getId() != null) {
            wrapper.eq(SchoolPO::getId, query.getId());
        }

        // 名称模糊查询
        if (query.getName() != null && !query.getName().isBlank()) {
            wrapper.like(SchoolPO::getName, query.getName());
        }

        // 类型查询
        if (query.getType() != null && !query.getType().isBlank()) {
            wrapper.eq(SchoolPO::getSchoolType, query.getType());
        }

        // 按创建时间倒序
        wrapper.orderByDesc(SchoolPO::getCreatedAt);

        // 分页查询
        Page<SchoolPO> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SchoolPO> result = schoolMapper.selectPage(page, wrapper);

        // 转换结果
        List<SchoolDTO> list = result.getRecords().stream()
                .map(this::poToDTO)
                .collect(Collectors.toList());

        return PageResult.<SchoolDTO>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .pages((int) result.getPages())
                .build();
    }

    /**
     * 获取学校列表
     */
    public List<SchoolDTO> listSchools() {
        log.info("获取学校列表");

        List<School> schools = schoolRepository.findAll();
        return schools.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按类型获取学校列表
     */
    public List<SchoolDTO> listSchoolsByType(String type) {
        log.info("按类型获取学校列表: type={}", type);

        SchoolInstitutionalType institutionalType = SchoolInstitutionalType.of(type);
        List<School> schools = schoolRepository.findByInstitutionalType(institutionalType);
        return schools.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 删除学校
     */
    @DS("org")
    @Transactional
    public void deleteSchool(Long id) {
        log.info("删除学校: id={}", id);
        schoolRepository.deleteById(SchoolId.of(id));
    }

    // ==================== 私有方法 ====================

    /**
     * 更新学校字段
     */
    private School updateSchoolFields(School school, UpdateSchoolCommand command, SchoolInstitutionalType institutionalType) {
        // 创建新的School实体保留原有ID
        School updatedSchool = School.create(command.getName(), institutionalType);
        if (school.getId() != null) {
            updatedSchool.setId(school.getId());
        }

        // 设置 iconUrl
        if (command.getIconUrl() != null && !command.getIconUrl().isBlank()) {
            updatedSchool.setIconUrl(command.getIconUrl());
        } else if (school.getIconUrl() != null) {
            updatedSchool.setIconUrl(school.getIconUrl());
        }

        // 设置 stages
        if (command.getStages() != null && !command.getStages().isEmpty()) {
            updatedSchool.setStages(JSONUtil.toJsonStr(command.getStages()));
        } else if (school.getStages() != null) {
            updatedSchool.setStages(school.getStages());
        }

        // 保留原有地址和描述
        if (school.getProvince() != null || school.getCity() != null) {
            updatedSchool.updateAddress(school.getProvince(), school.getCity(), school.getAddress());
        }
        if (school.getDescription() != null) {
            updatedSchool.updateDescription(school.getDescription());
        }

        return updatedSchool;
    }

    /**
     * 转换为DTO
     */
    private SchoolDTO toDTO(School school) {
        // 解析 stages JSON 字符串为列表
        List<String> stages = null;
        if (school.getStages() != null && !school.getStages().isEmpty()) {
            stages = JSONUtil.toList(school.getStages(), String.class);
        }

        return SchoolDTO.builder()
                .id(school.getIdValue())
                .name(school.getName())
                .iconUrl(school.getIconUrl())
                .type(school.getSchoolTypeValue())
                .stages(stages)
                .status(school.getStatus() != null ? school.getStatus() : "ACTIVE")
                .build();
    }

    /**
     * PO 转换为DTO（包含创建时间）
     */
    private SchoolDTO poToDTO(SchoolPO po) {
        // 解析 stages JSON 字符串为列表
        List<String> stages = null;
        if (po.getStages() != null && !po.getStages().isEmpty()) {
            stages = JSONUtil.toList(po.getStages(), String.class);
        }

        return SchoolDTO.builder()
                .id(po.getId())
                .name(po.getName())
                .iconUrl(po.getIconUrl())
                .type(po.getSchoolType())
                .stages(stages)
                .status(po.getStatus() != null ? po.getStatus() : "ACTIVE")
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}