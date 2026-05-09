package com.ai.edu.application.service.org;

import com.ai.edu.application.dto.org.command.AssociateUserWithSchoolCommand;
import com.ai.edu.application.dto.org.UserSchoolAssociationDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.organization.model.entity.SchoolUserAssociation;
import com.ai.edu.domain.organization.model.valueobject.SchoolUserRole;
import com.ai.edu.domain.organization.repository.SchoolRepository;
import com.ai.edu.domain.organization.repository.SchoolUserRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.domain.shared.valueobject.UserId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 组织应用服务
 * 处理用户与学校的关联、权限检查等用例
 */
@Slf4j
@Service
public class OrganizationAppService {

    @Resource
    private SchoolRepository schoolRepository;

    @Resource
    private SchoolUserRepository schoolUserRepository;

    /**
     * 关联用户与学校
     */
    @Transactional
    public UserSchoolAssociationDTO associateUserWithSchool(Long schoolId, AssociateUserWithSchoolCommand command) {
        log.info("关联用户与学校: schoolId={}, userId={}, role={}", schoolId, command.getUserId(), command.getRole());

        // 1. 检查学校是否存在
        if (!schoolRepository.findById(SchoolId.of(schoolId)).isPresent()) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "学校不存在");
        }

        // 2. 检查是否已存在关联
        SchoolId schId = SchoolId.of(schoolId);
        UserId usrId = UserId.of(command.getUserId());
        if (schoolUserRepository.existsBySchoolIdAndUserId(schId, usrId)) {
            throw new BusinessException(ErrorCode.SCHOOL_USER_ASSOCIATION_EXISTS, "用户已关联该学校");
        }

        // 3. 创建关联
        SchoolUserRole role = SchoolUserRole.of(command.getRole());
        SchoolUserAssociation association = SchoolUserAssociation.create(schId, usrId, role);
        SchoolUserAssociation savedAssociation = schoolUserRepository.save(association);

        log.info("用户学校关联成功: associationId={}", savedAssociation.getId().getValue());

        return toDTO(savedAssociation);
    }

    /**
     * 获取用户的学校列表
     */
    public List<UserSchoolAssociationDTO> getUserSchools(Long userId) {
        log.info("获取用户学校列表: userId={}", userId);

        UserId usrId = UserId.of(userId);
        List<SchoolUserAssociation> associations = schoolUserRepository.findByUserId(usrId);

        return associations.stream()
                .map(this::toDTOWithSchoolName)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否有学校访问权限
     */
    public boolean checkUserSchoolPermission(Long userId, Long schoolId) {
        log.info("检查用户学校权限: userId={}, schoolId={}", userId, schoolId);

        SchoolId schId = SchoolId.of(schoolId);
        UserId usrId = UserId.of(userId);

        boolean hasPermission = schoolUserRepository.existsBySchoolIdAndUserId(schId, usrId);

        log.info("权限检查结果: userId={}, schoolId={}, hasPermission={}", userId, schoolId, hasPermission);

        return hasPermission;
    }

    /**
     * 获取用户在某学校的角色
     */
    public String getUserRoleInSchool(Long userId, Long schoolId) {
        log.info("获取用户学校角色: userId={}, schoolId={}", userId, schoolId);

        SchoolId schId = SchoolId.of(schoolId);
        UserId usrId = UserId.of(userId);

        return schoolUserRepository.findBySchoolIdAndUserId(schId, usrId)
                .map(assoc -> assoc.getRole().getValue())
                .orElse(null);
    }

    /**
     * 移除用户与学校的关联
     */
    @Transactional
    public void removeUserFromSchool(Long userId, Long schoolId) {
        log.info("移除用户学校关联: userId={}, schoolId={}", userId, schoolId);

        SchoolId schId = SchoolId.of(schoolId);
        UserId usrId = UserId.of(userId);

        schoolUserRepository.deleteBySchoolIdAndUserId(schId, usrId);

        log.info("用户学校关联已移除: userId={}, schoolId={}", userId, schoolId);
    }

    // ==================== 私有方法 ====================

    /**
     * 转换为DTO
     */
    private UserSchoolAssociationDTO toDTO(SchoolUserAssociation association) {
        return UserSchoolAssociationDTO.builder()
                .id(association.getId().getValue())
                .schoolId(association.getSchoolId().getValue())
                .userId(association.getUserId().getValue())
                .role(association.getRole().getValue())
                .createdAt(association.getCreatedAt())
                .build();
    }

    /**
     * 转换为DTO（带学校名称）
     */
    private UserSchoolAssociationDTO toDTOWithSchoolName(SchoolUserAssociation association) {
        UserSchoolAssociationDTO dto = toDTO(association);

        // 查询学校名称
        schoolRepository.findById(association.getSchoolId())
                .ifPresent(school -> dto.setSchoolName(school.getName()));

        return dto;
    }
}