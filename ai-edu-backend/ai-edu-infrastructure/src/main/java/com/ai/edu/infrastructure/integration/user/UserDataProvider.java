package com.ai.edu.infrastructure.integration.user;

import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.service.UserService;
import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据提供者（基础设施层）
 *
 * 纯粹的用户域数据查询组件，不做任何模型转换。
 * 可被各防腐层复用，消除重复的查询逻辑。
 *
 * 职责：
 * - 调用用户域 UserService
 * - 切换数据源 @DS("user")
 * - 返回用户域原始对象（User）
 *
 * 注意：此组件仅在基础设施层内部使用，不暴露给应用层或领域层。
 */
@Slf4j
@Component
public class UserDataProvider {

    @Resource
    private UserService userService;

    /**
     * 根据手机号查询用户
     */
    @DS("user")
    public Optional<User> findByPhone(String phone) {
        return userService.findByPhone(phone);
    }

    /**
     * 根据ID查询用户
     */
    @DS("user")
    public Optional<User> findById(Long userId) {
        return userService.findById(userId);
    }

    /**
     * 批量查询用户
     */
    @DS("user")
    public List<User> findByIds(List<Long> userIds) {
        return userService.findByIds(userIds);
    }

    /**
     * 创建用户
     */
    @DS("user")
    public User createUser(String name, String phone) {
        Long userId = userService.createUser(name, phone);
        return userService.findById(userId).orElseThrow(() ->
            new IllegalStateException("用户创建后查询失败: userId=" + userId));
    }
}