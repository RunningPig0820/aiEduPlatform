package com.ai.edu.infrastructure.integration.user;

import com.ai.edu.domain.organization.acl.TeacherInfo;
import com.ai.edu.domain.organization.gateway.OrgUserGateway;
import com.ai.edu.domain.user.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
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
}