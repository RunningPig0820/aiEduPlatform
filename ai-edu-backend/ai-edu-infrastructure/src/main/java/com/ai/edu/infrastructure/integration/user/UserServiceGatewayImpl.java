package com.ai.edu.infrastructure.integration.user;

import com.ai.edu.domain.organization.acl.TeacherInfo;
import com.ai.edu.domain.organization.gateway.UserServiceGateway;
import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.service.UserService;
import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户服务网关实现（防腐层）
 *
 * 实现组织域定义的 UserServiceGateway 接口。
 * 核心职责：
 * - 调用用户域 UserService
 * - 模型转换（用户域 User → 组织域 TeacherInfo）
 * - 数据源切换（@DS("user")）
 *
 * 防腐层模型隔离：组织域只看到 TeacherInfo，不知道 User 的存在。
 */
@Slf4j
@Service
public class UserServiceGatewayImpl implements UserServiceGateway {

    @Resource
    private UserService userService;

    /**
     * 查询或创建教师用户
     * 切换到用户域数据源，返回转换后的组织域模型
     */
    @Override
    @DS("user")
    public TeacherInfo findOrCreateTeacher(String name, String phone) {
        Optional<User> userOpt = userService.findByPhone(phone);

        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            log.info("[ACL] 用户已存在: userId={}, phone={}", user.getId(), phone);
        } else {
            Long userId = userService.createUser(name, phone);
            user = userService.findById(userId).orElseThrow();
            log.info("[ACL] 创建新用户: userId={}, name={}, phone={}", userId, name, phone);
        }

        return toTeacherInfo(user);
    }

    /**
     * 批量查询教师信息
     * 切换到用户域数据源，返回转换后的组织域模型
     */
    @Override
    @DS("user")
    public List<TeacherInfo> findTeachersByIds(List<Long> userIds) {
        List<User> users = userService.findByIds(userIds);
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