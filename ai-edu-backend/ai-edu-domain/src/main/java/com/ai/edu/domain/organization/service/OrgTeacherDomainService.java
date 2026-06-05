package com.ai.edu.domain.organization.service;

import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.organization.gateway.UserServiceGateway;
import com.ai.edu.domain.organization.model.entity.Department;
import com.ai.edu.domain.organization.model.entity.OrgTeacher;
import com.ai.edu.domain.organization.model.valueobject.DepartmentId;
import com.ai.edu.domain.organization.model.valueobject.OrgTeacherId;
import com.ai.edu.domain.organization.repository.DepartmentRepository;
import com.ai.edu.domain.organization.repository.OrgTeacherRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.domain.user.model.entity.User;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 教职工领域服务
 * 处理组织域的教职工相关业务逻辑
 *
 * 跨域调用通过防腐层（Gateway）隔离：
 * - UserServiceGateway：隔离对用户域的调用
 * - 组织域不直接依赖用户域的 Domain Service
 *
 * 注意：Domain Service 不标注 @DS（领域层保持纯粹）
 * 数据源切换在防腐层实现（Infrastructure 层）处理
 */
@Slf4j
@Service
public class OrgTeacherDomainService {

    @Resource
    private DepartmentRepository departmentRepository;

    @Resource
    private OrgTeacherRepository orgTeacherRepository;

    @Resource
    private UserServiceGateway userServiceGateway;  // 防腐层：隔离用户域调用

    // ==================== 跨域调用（通过防腐层） ====================

    /**
     * 查询或创建用户（通过防腐层调用用户域）
     */
    public Long getOrCreateUser(String name, String phone) {
        return userServiceGateway.findOrCreateUser(name, phone);
    }

    /**
     * 批量查询用户（通过防腐层调用用户域）
     */
    public List<User> findUsersByIds(List<Long> userIds) {
        return userServiceGateway.findUsersByIds(userIds);
    }

    // ==================== 组织域业务逻辑 ====================

    /**
     * 验证部门是否存在且属于该学校
     * Repository 有 @DS("org")，会自动切换数据源
     */
    public Department validateDepartment(Long schoolId, Long departmentId) {
        log.info("验证部门: schoolId={}, departmentId={}", schoolId, departmentId);

        Department department = departmentRepository.findById(DepartmentId.of(departmentId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不存在"));

        if (!department.getSchoolIdValue().equals(schoolId)) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "部门不属于该学校");
        }

        return department;
    }

    /**
     * 创建教职工关联关系
     * Repository 有 @DS("org")，会自动切换数据源
     */
    @Transactional
    public OrgTeacher createTeacherRelation(Long schoolId, Long userId, Long departmentId, Long currentUserId) {
        log.info("创建教职工关联关系: schoolId={}, userId={}, departmentId={}", schoolId, userId, departmentId);

        // 检查该用户是否已在该学校有教职工记录
        Optional<OrgTeacher> existingTeacher = orgTeacherRepository.findBySchoolIdAndUserId(SchoolId.of(schoolId), userId);
        if (existingTeacher.isPresent()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该用户已在本学校有教职工记录");
        }

        // 创建教职工关联关系
        OrgTeacher orgTeacher = OrgTeacher.create(
                SchoolId.of(schoolId),
                userId,
                departmentId,
                currentUserId
        );

        // 保存
        OrgTeacher savedTeacher = orgTeacherRepository.save(orgTeacher);

        log.info("教职工关联关系创建成功: id={}", savedTeacher.getIdValue());
        return savedTeacher;
    }
}