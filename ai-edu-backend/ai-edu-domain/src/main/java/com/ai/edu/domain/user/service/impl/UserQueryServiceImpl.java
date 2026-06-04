package com.ai.edu.domain.user.service.impl;

import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.repository.UserRepository;
import com.ai.edu.domain.user.service.UserQueryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 用户查询服务实现（领域服务）
 *
 * DDD领域服务：
 * 1. 属于用户域
 * 2. 协调Repository完成业务逻辑
 * 3. 提供跨域调用接口
 */
@Slf4j
@Service
public class UserQueryServiceImpl implements UserQueryService {

    @Resource
    private UserRepository userRepository;

    @Override
    public Optional<User> findByPhone(String phone) {
        log.info("用户域查询服务：根据手机号查询用户 phone={}", phone);
        return userRepository.findByPhone(phone);
    }

    @Override
    public Optional<User> findById(Long userId) {
        log.info("用户域查询服务：根据ID查询用户 userId={}", userId);
        return userRepository.findById(userId);
    }

    @Override
    public List<User> findByIds(List<Long> userIds) {
        log.info("用户域查询服务：批量查询用户 count={}", userIds.size());
        return userRepository.findByIds(userIds);
    }

    @Override
    public Long createUser(String name, String phone) {
        log.info("用户域查询服务：创建用户 name={}, phone={}", name, phone);

        // 检查手机号是否已存在
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("手机号已存在: " + phone);
        }

        // 生成用户名（基于手机号）
        String username = "user_" + phone.substring(phone.length() - 4);
        // 默认密码
        String password = "password123";

        // 创建用户实体
        User user = User.create(username, password, name, phone, "TEACHER");

        // 保存用户
        User savedUser = userRepository.save(user);

        log.info("用户创建成功 userId={}", savedUser.getId());
        return savedUser.getId();
    }
}