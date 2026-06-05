package com.ai.edu.domain.organization.service;

import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.organization.model.entity.Department;
import com.ai.edu.domain.organization.model.entity.OrgTeacher;
import com.ai.edu.domain.organization.model.valueobject.DepartmentId;
import com.ai.edu.domain.organization.repository.DepartmentRepository;
import com.ai.edu.domain.organization.repository.OrgTeacherRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 教职工领域服务
 *
 * 只处理组织域内的业务逻辑，保持纯净：
 * - 验证部门
 * - 创建教职工关联关系
 *
 * 跨域调用（用户域）由 AppService 通过 Gateway 处理
 */
@Slf4j
@Service
public class OrgTeacherDomainService {

    @Resource
    private DepartmentRepository departmentRepository;

    @Resource
    private OrgTeacherRepository orgTeacherRepository;

    /**
     * 验证部门是否存在且属于该学校
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