package com.ai.edu.infrastructure.integration.user;

import com.ai.edu.domain.organization.gateway.UserServiceGateway;
import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.service.UserService;
import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务网关实现（防腐层）
 *
 * 实现组织域定义的 UserServiceGateway 接口。
 * 调用用户域 UserService，负责：
 * - 数据源切换（@DS("user")）
 * - 用户域逻辑封装
 *
 * 防腐层隔离：组织域不知道用户域的具体实现。
 */
@Slf4j
@Service
public class UserServiceGatewayImpl implements UserServiceGateway {

    @Resource
    private UserService userService;

    /**
     * 查询或创建用户
     * 切换到用户域数据源
     */
    @Override
    @DS("user")
    public Long findOrCreateUser(String name, String phone) {
        Optional<User> userOpt = userService.findByPhone(phone);

        if (userOpt.isPresent()) {
            Long userId = userOpt.get().getId();
            log.info("[ACL] 用户已存在: userId={}, phone={}", userId, phone);
            return userId;
        } else {
            Long userId = userService.createUser(name, phone);
            log.info("[ACL] 创建新用户: userId={}, name={}, phone={}", userId, name, phone);
            return userId;
        }
    }

    /**
     * 批量查询用户
     * 切换到用户域数据源
     */
    @Override
    @DS("user")
    public List<User> findUsersByIds(List<Long> userIds) {
        return userService.findByIds(userIds);
    }
}