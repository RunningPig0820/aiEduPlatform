package com.ai.edu.application.service.org;

import cn.hutool.json.JSONUtil;
import com.ai.edu.application.dto.org.AdminClassNodeDTO;
import com.ai.edu.application.dto.org.GradeOptionDTO;
import com.ai.edu.application.dto.org.StageConfigDTO;
import com.ai.edu.application.dto.org.command.CreateAdminClassNodeCommand;
import com.ai.edu.application.dto.org.command.UpdateAdminClassNodeCommand;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.organization.model.entity.Department;
import com.ai.edu.domain.organization.model.entity.DepartmentEdu;
import com.ai.edu.domain.organization.model.valueobject.DepartmentId;
import com.ai.edu.domain.organization.model.valueobject.enums.DepartmentTypeEnum;
import com.ai.edu.domain.organization.model.valueobject.enums.DeptEduTypeEnum;
import com.ai.edu.domain.organization.model.valueobject.GradeLevel;
import com.ai.edu.domain.organization.model.valueobject.enums.SchoolStageEnum;
import com.ai.edu.domain.organization.model.valueobject.enums.StageYearCodeEnum;
import com.ai.edu.domain.organization.repository.DepartmentEduRepository;
import com.ai.edu.domain.organization.repository.DepartmentRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 行政班应用服务
 * 管理行政班节点（学段/年级/班级）的创建、更新、查询、删除
 */
@Slf4j
@Service
public class AdminClassAppService {

    @Resource
    private DepartmentRepository departmentRepository;

    @Resource
    private DepartmentEduRepository departmentEduRepository;

    /**
     * 创建行政班节点
     */
    @DS("org")
    @Transactional
    public AdminClassNodeDTO createNode(CreateAdminClassNodeCommand command) {
        log.info("创建行政班节点: {}", JSONUtil.toJsonStr(command));

        DeptEduTypeEnum deptType = DeptEduTypeEnum.of(command.getDeptType());

        // 年级和班级节点需要 gradeCode + enrollmentYear
        if (deptType.requiresGradeInfo()) {
            if (command.getGradeCode() == null || command.getGradeCode().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_PARAMS, "年级/班级节点需填写 gradeCode");
            }
            if (command.getEnrollmentYear() == null || command.getEnrollmentYear().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_PARAMS, "年级/班级节点需填写 enrollmentYear");
            }
        }

        // 创建 Department
        Department department;
        if (command.getParentId() == null) {
            department = Department.createAdminClassRoot(
                    SchoolId.of(command.getSchoolId()),
                    command.getName(),
                    command.getSortOrder(),
                    command.getDescription()
            );
        } else {
            Department parent = departmentRepository.findById(DepartmentId.of(command.getParentId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "父节点不存在"));
            department = Department.createAdminClassChild(
                    SchoolId.of(command.getSchoolId()),
                    command.getName(),
                    parent,
                    command.getSortOrder(),
                    command.getDescription()
            );
        }
        Department savedDept = departmentRepository.saveOrUpdate(department);

        // 创建 DepartmentEdu
        DepartmentEdu edu = switch (deptType) {
            case STAGE -> DepartmentEdu.createStage(
                    savedDept.getIdValue(), SchoolId.of(command.getSchoolId()),
                    command.getStageCode(), command.getStageYearCode());
            case GRADE -> DepartmentEdu.createGrade(
                    savedDept.getIdValue(), SchoolId.of(command.getSchoolId()),
                    command.getStageCode(), command.getStageYearCode(),
                    command.getGradeCode(), command.getEnrollmentYear());
            case CLASS -> DepartmentEdu.createClass(
                    savedDept.getIdValue(), SchoolId.of(command.getSchoolId()),
                    command.getStageCode(), command.getStageYearCode(),
                    command.getGradeCode(), command.getEnrollmentYear());
            default -> throw new BusinessException(ErrorCode.INVALID_PARAMS, "无效的节点类型");
        };
        departmentEduRepository.save(edu);

        log.info("行政班节点创建成功: deptId={}, name={}", savedDept.getIdValue(), savedDept.getName());
        return toDTO(savedDept, edu);
    }

    /**
     * 更新行政班节点
     */
    @DS("org")
    @Transactional
    public AdminClassNodeDTO updateNode(Long deptId, UpdateAdminClassNodeCommand command) {
        log.info("更新行政班节点: deptId={}", deptId);

        Department department = departmentRepository.findById(DepartmentId.of(deptId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "节点不存在"));

        // 更新 Department
        department.update(
                command.getName() != null ? command.getName() : department.getName(),
                command.getSortOrder(),
                command.getDescription() != null ? command.getDescription() : department.getDescription()
        );
        departmentRepository.saveOrUpdate(department);

        // 更新 DepartmentEdu
        DepartmentEdu edu = departmentEduRepository.findByDeptId(deptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "扩展属性不存在"));
        edu.update(command.getStageCode(), command.getStageYearCode(),
                command.getGradeCode(), command.getEnrollmentYear());
        departmentEduRepository.save(edu);

        log.info("行政班节点更新成功: deptId={}", deptId);
        return toDTO(department, edu);
    }

    /**
     * 获取节点详情
     */
    public AdminClassNodeDTO getNodeDetail(Long deptId) {
        Department department = departmentRepository.findById(DepartmentId.of(deptId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "节点不存在"));
        DepartmentEdu edu = departmentEduRepository.findByDeptId(deptId).orElse(null);
        return toDTO(department, edu);
    }

    /**
     * 获取学校行政班树
     */
    public List<AdminClassNodeDTO> getNodeTree(Long schoolId) {
        log.info("获取行政班树: schoolId={}", schoolId);

        List<Department> departments = departmentRepository.findBySchoolIdAndType(
                SchoolId.of(schoolId), DepartmentTypeEnum.ADMIN_CLASS);

        // 加载所有扩展属性
        Map<Long, DepartmentEdu> eduMap = new HashMap<>();
        List<DepartmentEdu> eduList = departmentEduRepository.findBySchoolId(SchoolId.of(schoolId));
        for (DepartmentEdu edu : eduList) {
            eduMap.put(edu.getDeptId(), edu);
        }

        return buildTree(departments, eduMap);
    }

    /**
     * 获取学段配置列表（固定选项）
     * 每个学段包含其年制和可用的年级范围
     */
    public List<StageConfigDTO> getStageConfigs() {
        log.info("获取学段配置列表");

        return List.of(
                buildStageConfig(SchoolStageEnum.PRIMARY, StageYearCodeEnum.PRIMARY_FIVE),
                buildStageConfig(SchoolStageEnum.PRIMARY, StageYearCodeEnum.PRIMARY_SIX),
                buildStageConfig(SchoolStageEnum.JUNIOR_HIGH, StageYearCodeEnum.JUNIOR_THREE),
                buildStageConfig(SchoolStageEnum.JUNIOR_HIGH, StageYearCodeEnum.JUNIOR_FOUR),
                buildStageConfig(SchoolStageEnum.SENIOR_HIGH, StageYearCodeEnum.SENIOR_THREE)
        );
    }

    /**
     * 根据学段和年制获取可用年级列表
     */
    public List<GradeOptionDTO> getGradeOptions(String stageCode, String stageYearCode) {
        log.info("获取年级选项: stageCode={}, stageYearCode={}", stageCode, stageYearCode);

        StageYearCodeEnum yearCode = StageYearCodeEnum.of(stageYearCode);

        List<GradeOptionDTO> grades = new ArrayList<>();
        for (Integer gradeLevel : yearCode.getGradeCodes()) {
            GradeLevel gl = GradeLevel.of(gradeLevel);
            grades.add(GradeOptionDTO.builder()
                    .gradeCode(gradeLevel)
                    .gradeName(gl.getDisplayName())
                    .build());
        }
        return grades;
    }

    /**
     * 删除行政班节点
     */
    @DS("org")
    @Transactional
    public void deleteNode(Long deptId) {
        log.info("删除行政班节点: deptId={}", deptId);

        Department department = departmentRepository.findById(DepartmentId.of(deptId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "节点不存在"));

        // 检查是否有子节点
        if (departmentRepository.hasChildren(DepartmentId.of(deptId))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在子节点，需先删除子节点");
        }

        // 删除扩展属性
        departmentEduRepository.deleteByDeptId(deptId);

        // 删除部门
        departmentRepository.deleteById(DepartmentId.of(deptId));

        log.info("行政班节点删除成功: deptId={}", deptId);
    }

    private StageConfigDTO buildStageConfig(SchoolStageEnum stage, StageYearCodeEnum yearCode) {
        List<GradeOptionDTO> grades = new ArrayList<>();
        for (Integer gradeLevel : yearCode.getGradeCodes()) {
            GradeLevel gl = GradeLevel.of(gradeLevel);
            grades.add(GradeOptionDTO.builder()
                    .gradeCode(gradeLevel)
                    .gradeName(gl.getDisplayName())
                    .build());
        }

        return StageConfigDTO.builder()
                .stageCode(stage.getValue())
                .stageName(stage.getDescription())
                .stageYearCode(yearCode.getValue())
                .stageYearName(yearCode.getDescription())
                .yearCount(yearCode.getYearCount())
                .startGrade(yearCode.getStartGrade())
                .endGrade(yearCode.getEndGrade())
                .grades(grades)
                .build();
    }

    // ==================== 私有方法 ====================

    private AdminClassNodeDTO toDTO(Department department, DepartmentEdu edu) {
        AdminClassNodeDTO.AdminClassNodeDTOBuilder builder = AdminClassNodeDTO.builder()
                .deptId(department.getIdValue())
                .schoolId(department.getSchoolIdValue())
                .name(department.getName())
                .parentId(department.getParentId())
                .departmentPath(department.getDepartmentPath())
                .departmentType(department.getDepartmentTypeValue())
                .sortOrder(department.getSortOrder())
                .description(department.getDescription())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt());

        if (edu != null) {
            builder.deptType(edu.getDeptTypeValue())
                    .stageCode(edu.getStageCode())
                    .stageYearCode(edu.getStageYearCode())
                    .gradeCode(edu.getGradeCode())
                    .enrollmentYear(edu.getEnrollmentYear());
        }

        return builder.build();
    }

    private List<AdminClassNodeDTO> buildTree(List<Department> departments, Map<Long, DepartmentEdu> eduMap) {
        // 按 parentId 分组
        Map<Long, List<Department>> parentMap = new HashMap<>();
        for (Department dept : departments) {
            Long parentId = dept.getParentId() != null ? dept.getParentId() : 0L;
            parentMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(dept);
        }

        // 构建根节点树
        List<Department> roots = parentMap.getOrDefault(0L, new ArrayList<>());
        List<AdminClassNodeDTO> tree = new ArrayList<>();
        for (Department root : roots) {
            tree.add(buildTreeNode(root, parentMap, eduMap));
        }
        return tree;
    }

    private AdminClassNodeDTO buildTreeNode(Department department, Map<Long, List<Department>> parentMap,
                                             Map<Long, DepartmentEdu> eduMap) {
        DepartmentEdu edu = eduMap.get(department.getIdValue());
        AdminClassNodeDTO dto = toDTO(department, edu);

        List<Department> children = parentMap.getOrDefault(department.getIdValue(), new ArrayList<>());
        if (!children.isEmpty()) {
            List<AdminClassNodeDTO> childDTOs = new ArrayList<>();
            for (Department child : children) {
                childDTOs.add(buildTreeNode(child, parentMap, eduMap));
            }
            dto.setChildren(childDTOs);
        }

        return dto;
    }
}
