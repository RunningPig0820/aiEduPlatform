package com.ai.edu.application.service.org;

import com.ai.edu.application.dto.org.OrgTeacherDTO;
import com.ai.edu.application.dto.org.command.CreateOrgTeacherCommand;
import com.ai.edu.application.dto.org.command.OrgTeacherQueryParamDTO;
import com.ai.edu.application.dto.org.command.UpdateOrgTeacherCommand;
import com.ai.edu.application.dto.org.PageResult;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.organization.model.entity.OrgTeacher;
import com.ai.edu.domain.organization.model.valueobject.OrgTeacherId;
import com.ai.edu.domain.organization.model.valueobject.OrgTeacherQueryParam;
import com.ai.edu.domain.organization.repository.OrgTeacherRepository;
import com.ai.edu.domain.organization.repository.DepartmentRepository;
import com.ai.edu.domain.user.service.UserQueryService;
import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.organization.model.entity.Department;
import com.ai.edu.domain.organization.model.valueobject.DepartmentId;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 教职工应用服务
 * 处理教职工的添加、查询、修改、删除等用例
 * 通过聚合查询整合用户域基本信息，返回完整教职工信息
 */
@Slf4j
@Service
public class OrgTeacherAppService {

    @Resource
    private OrgTeacherRepository orgTeacherRepository;

    @Resource
    private DepartmentRepository departmentRepository;

    @Resource
    private UserQueryService userQueryService;

    /**
     * 创建教职工（关联关系）
     * 流程：查询用户 → 不存在则创建用户 → 创建组织关联关系
     */
    @DS("org")
    @Transactional
    public OrgTeacherDTO createOrgTeacher(Long schoolId, Long currentUserId, CreateOrgTeacherCommand command) {
        log.info("创建教职工: schoolId={}, phone={}, departmentId={}", schoolId, command.getPhone(), command.getDepartmentId());

        // 1. 验证部门是否存在且属于该学校
        Department department = departmentRepository.findById(DepartmentId.of(command.getDepartmentId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不存在"));

        if (!department.getSchoolIdValue().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不属于该学校");
        }

        // 2. 查询用户域是否存在该手机号的用户
        Optional<User> userOpt = userQueryService.findByPhone(command.getPhone());

        Long userId;
        if (userOpt.isPresent()) {
            // 用户已存在，直接使用
            userId = userOpt.get().getId();
            log.info("用户已存在: userId={}, phone={}", userId, command.getPhone());
        } else {
            // 用户不存在，创建新用户
            userId = userQueryService.createUser(command.getName(), command.getPhone());
            log.info("创建新用户: userId={}, name={}, phone={}", userId, command.getName(), command.getPhone());
        }

        // 3. 检查该用户是否已在该学校有教职工记录
        Optional<OrgTeacher> existingTeacher = orgTeacherRepository.findBySchoolIdAndUserId(SchoolId.of(schoolId), userId);
        if (existingTeacher.isPresent()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该用户已在本学校有教职工记录");
        }

        // 4. 创建教职工关联关系
        OrgTeacher orgTeacher = OrgTeacher.create(
                SchoolId.of(schoolId),
                userId,
                command.getDepartmentId(),
                currentUserId
        );

        // 5. 保存
        OrgTeacher savedTeacher = orgTeacherRepository.save(orgTeacher);

        log.info("教职工创建成功: id={}, userId={}, departmentId={}", savedTeacher.getIdValue(), userId, command.getDepartmentId());

        // 6. 聚合返回完整信息
        return buildDTO(savedTeacher, department.getName());
    }

    /**
     * 聚合查询教职工列表
     * 查询组织域关联关系 → 批量查询用户域基本信息 → 合并返回完整信息
     */
    public PageResult<OrgTeacherDTO> listOrgTeachers(Long schoolId, OrgTeacherQueryParamDTO queryDTO) {
        log.info("查询教职工列表: schoolId={}, departmentId={}, userId={}, pageNum={}, pageSize={}",
                schoolId, queryDTO.getDepartmentId(), queryDTO.getUserId(), queryDTO.getPageNum(), queryDTO.getPageSize());

        // 1. 构建查询参数
        OrgTeacherQueryParam param = OrgTeacherQueryParam.builder()
                .schoolId(schoolId)
                .departmentId(queryDTO.getDepartmentId())
                .userId(queryDTO.getUserId())
                .build();

        // 2. 分页查询组织域关联关系
        int pageNum = queryDTO.getPageNum() != null ? queryDTO.getPageNum() : 1;
        int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 10;

        IPage<OrgTeacher> page = orgTeacherRepository.queryPage(param, pageNum, pageSize);

        // 3. 批量查询用户域基本信息
        List<Long> userIds = page.getRecords().stream()
                .map(OrgTeacher::getUserId)
                .toList();

        List<User> users = userQueryService.findByIds(userIds);

        // 构建 userId -> User 映射
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 4. 批量查询部门名称
        List<Long> departmentIds = page.getRecords().stream()
                .map(OrgTeacher::getDepartmentId)
                .distinct()
                .toList();

        Map<Long, String> departmentNameMap = departmentIds.stream()
                .map(deptId -> departmentRepository.findById(DepartmentId.of(deptId)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Department::getIdValue, Department::getName));

        // 5. 合并返回完整信息
        List<OrgTeacherDTO> dtoList = page.getRecords().stream()
                .map(teacher -> {
                    User user = userMap.get(teacher.getUserId());
                    String departmentName = departmentNameMap.get(teacher.getDepartmentId());
                    return buildDTO(teacher, user, departmentName);
                })
                .toList();

        // 6. 构建分页结果
        return PageResult.<OrgTeacherDTO>builder()
                .list(dtoList)
                .total(page.getTotal())
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .pages((int) page.getPages())
                .build();
    }

    /**
     * 查询教职工详情（聚合查询）
     */
    public OrgTeacherDTO getOrgTeacher(Long schoolId, Long id) {
        log.info("查询教职工详情: schoolId={}, id={}", schoolId, id);

        // 1. 查询组织域关联关系
        OrgTeacher orgTeacher = orgTeacherRepository.findById(OrgTeacherId.of(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "教职工不存在"));

        // 2. 校验教职工属于该学校
        if (!orgTeacher.getSchoolIdValue().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "教职工不属于该学校");
        }

        // 3. 查询用户域基本信息
        List<User> users = userQueryService.findByIds(List.of(orgTeacher.getUserId()));
        User user = users.isEmpty() ? null : users.get(0);

        // 4. 查询部门名称
        String departmentName = departmentRepository.findById(DepartmentId.of(orgTeacher.getDepartmentId()))
                .map(Department::getName)
                .orElse(null);

        // 5. 聚合返回完整信息
        return buildDTO(orgTeacher, user, departmentName);
    }

    /**
     * 更新教职工所属部门
     * 组织域只支持修改所属部门，用户基本信息修改在用户中心处理
     */
    @DS("org")
    @Transactional
    public OrgTeacherDTO updateOrgTeacher(Long schoolId, Long id, Long currentUserId, UpdateOrgTeacherCommand command) {
        log.info("更新教职工所属部门: schoolId={}, id={}, newDepartmentId={}", schoolId, id, command.getDepartmentId());

        // 1. 查询教职工关联关系
        OrgTeacher orgTeacher = orgTeacherRepository.findById(OrgTeacherId.of(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "教职工不存在"));

        // 2. 校验教职工属于该学校
        if (!orgTeacher.getSchoolIdValue().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "教职工不属于该学校");
        }

        // 3. 验证新部门是否存在且属于该学校
        Department newDepartment = departmentRepository.findById(DepartmentId.of(command.getDepartmentId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不存在"));

        if (!newDepartment.getSchoolIdValue().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不属于该学校");
        }

        // 4. 更新所属部门
        orgTeacher.updateDepartment(command.getDepartmentId(), currentUserId);

        // 5. 保存
        orgTeacherRepository.save(orgTeacher);

        log.info("教职工部门更新成功: id={}, newDepartmentId={}", id, command.getDepartmentId());

        // 6. 聚合返回完整信息
        return getOrgTeacher(schoolId, id);
    }

    /**
     * 删除教职工关联关系
     * 只删除组织域的关联关系，用户域的用户数据保留不受影响
     */
    @DS("org")
    @Transactional
    public void deleteOrgTeacher(Long schoolId, Long id) {
        log.info("删除教职工关联关系: schoolId={}, id={}", schoolId, id);

        // 1. 查询教职工关联关系
        OrgTeacher orgTeacher = orgTeacherRepository.findById(OrgTeacherId.of(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "教职工不存在"));

        // 2. 校验教职工属于该学校
        if (!orgTeacher.getSchoolIdValue().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "教职工不属于该学校");
        }

        // 3. 删除关联关系（逻辑删除）
        orgTeacherRepository.deleteById(OrgTeacherId.of(id));

        log.info("教职工关联关系删除成功: id={}, userId保留={}", id, orgTeacher.getUserId());
    }

    // ==================== 私有方法 ====================

    /**
     * 构建 DTO（仅包含关联关系 + 部门名称）
     */
    private OrgTeacherDTO buildDTO(OrgTeacher teacher, String departmentName) {
        return OrgTeacherDTO.builder()
                .id(teacher.getIdValue())
                .userId(teacher.getUserId())
                .schoolId(teacher.getSchoolIdValue())
                .departmentId(teacher.getDepartmentId())
                .departmentName(departmentName)
                .createdBy(teacher.getCreatedBy())
                .modifiedBy(teacher.getModifiedBy())
                .createdAt(teacher.getCreatedAt())
                .updatedAt(teacher.getUpdatedAt())
                .build();
    }

    /**
     * 构建 DTO（包含完整信息：关联关系 + 用户基本信息 + 部门名称）
     */
    private OrgTeacherDTO buildDTO(OrgTeacher teacher, User user, String departmentName) {
        OrgTeacherDTO dto = buildDTO(teacher, departmentName);

        if (user != null) {
            dto.setName(user.getRealName());
            dto.setPhone(user.getPhone());
        }

        return dto;
    }
}