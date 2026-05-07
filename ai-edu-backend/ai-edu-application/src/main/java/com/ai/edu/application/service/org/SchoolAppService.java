package com.ai.edu.application.service.org;

import com.ai.edu.application.dto.org.command.CreateSchoolCommand;
import com.ai.edu.application.dto.org.SchoolDTO;
import com.ai.edu.application.dto.org.command.UpdateSchoolCommand;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.organization.model.aggregate.SchoolOrganizationAggregate;
import com.ai.edu.domain.organization.model.entity.School;
import com.ai.edu.domain.organization.model.valueobject.SchoolInstitutionalType;
import com.ai.edu.domain.organization.model.valueobject.SchoolStage;
import com.ai.edu.domain.organization.repository.SchoolRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学校应用服务
 * 处理学校组织的创建、更新、查询等用例
 */
@Slf4j
@Service
public class SchoolAppService {

    @Resource
    private SchoolRepository schoolRepository;

    /**
     * 创建学校
     *
     * @param command 创建请求
     * @return 学校DTO
     */
    @Transactional
    public SchoolDTO createSchool(CreateSchoolCommand command) {
        log.info("创建学校: name={}, type={}, stages={}", command.getName(), command.getType(), command.getStages());

        // 1. 检查名称是否已存在
        if (schoolRepository.existsByName(command.getName())) {
            throw new BusinessException(ErrorCode.SCHOOL_NAME_EXISTS, "学校名称已存在");
        }

        // 2. 转换值对象
        SchoolInstitutionalType institutionalType = SchoolInstitutionalType.of(command.getType());
        List<SchoolStage> stages = command.getStages().stream()
                .map(SchoolStage::of)
                .collect(Collectors.toList());

        // 3. 创建聚合根
        SchoolOrganizationAggregate aggregate = SchoolOrganizationAggregate.create(
                command.getName(),
                command.getIconUrl(),
                institutionalType,
                stages
        );

        // 4. 保存（使用现有School实体作为持久化载体）
        School school = School.create(command.getName(), null, command.getType());
        School savedSchool = schoolRepository.save(school);

        // 设置聚合根ID
        aggregate.setId(SchoolId.of(savedSchool.getId()));

        log.info("学校创建成功: id={}, name={}", savedSchool.getId(), savedSchool.getName());

        return toDTO(savedSchool, command.getIconUrl(), command.getType(), command.getStages());
    }

    /**
     * 更新学校
     *
     * @param id      学校ID
     * @param command 更新请求
     * @return 学校DTO
     */
    @Transactional
    public SchoolDTO updateSchool(Long id, UpdateSchoolCommand command) {
        log.info("更新学校: id={}, name={}", id, command.getName());

        // 1. 查找学校
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "学校不存在"));

        // 2. 检查名称冲突（如果修改了名称）
        if (!school.getName().equals(command.getName()) && schoolRepository.existsByName(command.getName())) {
            throw new BusinessException(ErrorCode.SCHOOL_NAME_EXISTS, "学校名称已存在");
        }

        // 3. 更新学校信息
        school = updateSchoolFields(school, command);
        schoolRepository.save(school);

        log.info("学校更新成功: id={}", id);

        return toDTO(school, command.getIconUrl(), command.getType(), command.getStages());
    }

    /**
     * 获取学校详情
     *
     * @param id 学校ID
     * @return 学校DTO
     */
    public SchoolDTO getSchoolById(Long id) {
        log.info("获取学校详情: id={}", id);

        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "学校不存在"));

        return toDTO(school);
    }

    /**
     * 获取学校列表
     *
     * @return 学校列表
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
     *
     * @param type 学校类型
     * @return 学校列表
     */
    public List<SchoolDTO> listSchoolsByType(String type) {
        log.info("按类型获取学校列表: type={}", type);

        List<School> schools = schoolRepository.findBySchoolType(type);
        return schools.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 删除学校
     *
     * @param id 学校ID
     */
    @Transactional
    public void deleteSchool(Long id) {
        log.info("删除学校: id={}", id);
        schoolRepository.deleteById(id);
    }

    // ==================== 私有方法 ====================

    /**
     * 更新学校字段
     */
    private School updateSchoolFields(School school, UpdateSchoolCommand command) {
        // 使用反射或直接设置字段（School实体目前字段受限）
        // 这里简化处理：创建新的School实体
        School updatedSchool = School.create(command.getName(), school.getCode(), command.getType());
        // 需要School实体支持更多字段更新方法
        return updatedSchool;
    }

    /**
     * 转换为DTO
     */
    private SchoolDTO toDTO(School school) {
        return toDTO(school, null, school.getSchoolType(), null);
    }

    /**
     * 转换为DTO（带完整信息）
     */
    private SchoolDTO toDTO(School school, String iconUrl, String type, List<String> stages) {
        return SchoolDTO.builder()
                .id(school.getId())
                .name(school.getName())
                .iconUrl(iconUrl)
                .type(type != null ? type : school.getSchoolType())
                .stages(stages)
                .status("ACTIVE")
                .build();
    }
}