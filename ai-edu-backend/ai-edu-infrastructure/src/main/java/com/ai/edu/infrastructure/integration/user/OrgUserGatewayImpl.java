package com.ai.edu.infrastructure.integration.user;

import com.ai.edu.common.util.EncryptUtil;
import com.ai.edu.domain.organization.acl.ParentBinding;
import com.ai.edu.domain.organization.acl.ParentInfo;
import com.ai.edu.domain.organization.acl.StudentInfo;
import com.ai.edu.domain.organization.acl.TeacherInfo;
import com.ai.edu.domain.organization.gateway.OrgUserGateway;
import com.ai.edu.domain.user.model.entity.ParentProfile;
import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.repository.ParentProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 组织域用户网关实现（防腐层）
 *
 * 实现组织域定义的 OrgUserGateway 接口。
 * 核心职责：模型转换（用户域 User → 组织域 TeacherInfo）
 *
 * 设计原则：
 * - 查询逻辑复用：注入 UserDataProvider（基础设施层共享组件）
 * - 模型转换独立：专注做组织域专属的转换
 * - 上下文隔离：组织域只知道 TeacherInfo，不知道 User
 */
@Slf4j
@Service
public class OrgUserGatewayImpl implements OrgUserGateway {

    @Resource
    private UserDataProvider userDataProvider;

    @Resource
    private ParentProfileRepository parentProfileRepository;

    /**
     * 查询或创建教师用户
     *
     * 查询逻辑由 UserDataProvider 处理，此方法只做模型转换
     */
    @Override
    public TeacherInfo findOrCreateTeacher(String name, String phone) {
        User user = userDataProvider.findByPhone(phone)
                .orElseGet(() -> userDataProvider.createUser(name, phone));

        log.info("[ACL-Org] 教师用户操作完成: userId={}, phone={}", user.getId(), phone);
        return toTeacherInfo(user);
    }

    /**
     * 批量查询教师信息
     *
     * 查询逻辑由 UserDataProvider 处理，此方法只做模型转换
     */
    @Override
    public List<TeacherInfo> findTeachersByIds(List<Long> userIds) {
        List<User> users = userDataProvider.findByIds(userIds);
        return users.stream()
                .map(this::toTeacherInfo)
                .collect(Collectors.toList());
    }

    // ==================== 学生相关实现 ====================

    @Override
    public StudentInfo findOrCreateStudent(String name, String phone, String idCard) {
        User user = userDataProvider.findByPhone(phone)
                .map(existing -> {
                    // 角色校验：已存在的用户必须是 STUDENT
                    if (!"STUDENT".equals(existing.getRole())) {
                        throw new com.ai.edu.common.exception.BusinessException(
                            com.ai.edu.common.constant.ErrorCode.PARAM_ERROR,
                            "该手机号已被其他角色使用"
                        );
                    }
                    log.info("[ACL-Org] 复用已有学生用户: userId={}, phone={}", existing.getId(), phone);
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("[ACL-Org] 创建新学生用户: name={}, phone={}", name, phone);
                    return userDataProvider.createStudent(name, phone, idCard);
                });

        return toStudentInfo(user);
    }

    // ==================== 家长相关实现 ====================

    @Override
    public ParentInfo findOrCreateParent(String name, String phone) {
        User user = userDataProvider.findByPhone(phone)
                .map(existing -> {
                    // 角色校验：已存在的用户必须是 PARENT
                    if (!"PARENT".equals(existing.getRole())) {
                        throw new com.ai.edu.common.exception.BusinessException(
                            com.ai.edu.common.constant.ErrorCode.PARAM_ERROR,
                            "该手机号已被其他角色使用"
                        );
                    }
                    log.info("[ACL-Org] 复用已有家长用户: userId={}, phone={}", existing.getId(), phone);
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("[ACL-Org] 创建新家长用户: name={}, phone={}", name, phone);
                    return userDataProvider.createParent(name, phone);
                });

        return toParentInfo(user, ""); // relationship 在 bindStudentParents 时设置
    }

    @Override
    public void bindStudentParents(Long studentUserId, List<ParentBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            log.info("[ACL-Org] 无家长需要绑定: studentUserId={}", studentUserId);
            return;
        }

        List<ParentProfile> profiles = new ArrayList<>();
        for (ParentBinding binding : bindings) {
            ParentProfile profile = ParentProfile.create(
                    studentUserId,
                    binding.getParentUserId(),
                    binding.getRelationship()
            );
            profiles.add(profile);
        }

        parentProfileRepository.saveAll(profiles);
        log.info("[ACL-Org] 家长绑定完成: studentUserId={}, parentCount={}", studentUserId, profiles.size());
    }

    @Override
    public List<ParentInfo> findParentsByStudentUserId(Long studentUserId) {
        List<ParentProfile> profiles = parentProfileRepository.findByStudentUserId(studentUserId);
        if (profiles.isEmpty()) {
            return List.of();
        }

        List<Long> parentUserIds = profiles.stream()
                .map(ParentProfile::getParentUserId)
                .toList();

        List<User> parentUsers = userDataProvider.findByIds(parentUserIds);

        // 按 userId 索引用户
        var userMap = parentUsers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ParentInfo> result = new ArrayList<>();
        for (ParentProfile profile : profiles) {
            User parentUser = userMap.get(profile.getParentUserId());
            if (parentUser != null) {
                result.add(toParentInfo(parentUser, profile.getRelationship()));
            }
        }
        return result;
    }

    // ==================== 模型转换方法 ====================

    /**
     * 模型转换：用户域 User → 组织域 TeacherInfo
     *
     * 这是防腐层的核心职责：
     * - 只提取组织域需要的字段
     * - 隔离用户域的内部结构
     * - 用户域变更时只需修改此方法
     */
    private TeacherInfo toTeacherInfo(User user) {
        return new TeacherInfo(
                user.getId(),
                user.getRealName(),
                user.getPhone()
        );
    }

    /**
     * 模型转换：用户域 User → 组织域 StudentInfo
     *
     * 身份证处理：解密 → 脱敏 → 返回
     */
    private StudentInfo toStudentInfo(User user) {
        String maskedIdCard = "";
        if (user.getIdCard() != null && !user.getIdCard().isBlank()) {
            try {
                String decrypted = EncryptUtil.decrypt(user.getIdCard());
                maskedIdCard = EncryptUtil.maskIdCard(decrypted);
            } catch (Exception e) {
                log.warn("[ACL-Org] 身份证解密失败: userId={}", user.getId(), e);
                maskedIdCard = "***";
            }
        }

        return new StudentInfo(
                user.getId(),
                user.getRealName(),
                user.getPhone(),
                maskedIdCard
        );
    }

    /**
     * 模型转换：用户域 User → 组织域 ParentInfo
     */
    private ParentInfo toParentInfo(User user, String relationship) {
        return new ParentInfo(
                user.getId(),
                user.getRealName(),
                user.getPhone(),
                relationship
        );
    }
}