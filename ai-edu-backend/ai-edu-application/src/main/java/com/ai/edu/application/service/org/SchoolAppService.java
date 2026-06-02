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
import com.ai.edu.domain.organization.model.valueobject.SchoolQueryParam;
import com.ai.edu.domain.organization.repository.SchoolRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.baomidou.dynamic.datasource.annotation.DS;
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

        // 3. 转换 stages 为 JSON 字符串
        String stagesJson = null;
        if (command.getStages() != null && !command.getStages().isEmpty()) {
            stagesJson = JSONUtil.toJsonStr(command.getStages());
        }

        // 4. 创建学校实体（所有参数封装在实体中）
        School school = School.create(
                command.getName(),
                institutionalType,
                command.getIconUrl(),
                stagesJson,
                command.getProvince(),
                command.getCity(),
                command.getDistrict(),
                command.getAddress(),
                command.getDescription()
        );

        // 5. 保存
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

        // 3. 转换值对象
        SchoolInstitutionalType institutionalType = SchoolInstitutionalType.of(command.getType());

        // 4. 转换 stages 为 JSON 字符串
        String stagesJson = null;
        if (command.getStages() != null && !command.getStages().isEmpty()) {
            stagesJson = JSONUtil.toJsonStr(command.getStages());
        } else if (school.getStages() != null) {
            stagesJson = school.getStages();
        }

        // 5. 更新学校信息（所有参数封装在实体中）
        school.update(
                command.getName(),
                institutionalType,
                command.getIconUrl() != null ? command.getIconUrl() : school.getIconUrl(),
                stagesJson,
                command.getProvince(),
                command.getCity(),
                command.getDistrict(),
                command.getAddress(),
                command.getDescription() != null ? command.getDescription() : school.getDescription()
        );

        // 6. 保存
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
    public PageResult<SchoolDTO> querySchools(SchoolQueryCommand query) {
        log.info("分页查询学校: id={}, name={}, type={}, pageNum={}, pageSize={}",
                query.getId(), query.getName(), query.getType(), query.getPageNum(), query.getPageSize());

        // 构建查询参数
        SchoolQueryParam param = SchoolQueryParam.builder()
                .id(query.getId())
                .name(query.getName())
                .type(query.getType())
                .pageNum(query.getPageNum())
                .pageSize(query.getPageSize())
                .build();

        // 通过 Repository 查询
        com.ai.edu.domain.shared.valueobject.PageResult<School> result = schoolRepository.queryPage(param);

        // 转换结果
        List<SchoolDTO> list = result.getList().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.<SchoolDTO>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum(result.getPageNum())
                .pageSize(result.getPageSize())
                .pages(result.getPages())
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
                .province(school.getProvince())
                .city(school.getCity())
                .district(school.getDistrict())
                .address(school.getAddress())
                .description(school.getDescription())
                .status(school.getStatusValue())
                .statusValue(school.getStatusDescription())
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }
}