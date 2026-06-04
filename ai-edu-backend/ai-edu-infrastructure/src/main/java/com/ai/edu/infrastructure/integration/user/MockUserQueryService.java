package com.ai.edu.infrastructure.integration.user;

import com.ai.edu.domain.organization.service.UserQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户域查询服务 Mock 实现
 * 在用户域接口未就绪时使用，用于开发和测试
 * TODO: 用户域接口就绪后，替换为真实的 Feign 客户端实现
 */
@Slf4j
@Service
public class MockUserQueryService implements UserQueryService {

    // Mock 数据存储（用于测试）
    private final Map<String, UserInfo> phoneIndex = new ConcurrentHashMap<>();
    private final Map<Long, UserInfo> idIndex = new ConcurrentHashMap<>();
    private Long idGenerator = 1L;

    public MockUserQueryService() {
        // 初始化一些测试数据
        initTestData();
    }

    private void initTestData() {
        // 预置几个测试用户
        createUserInternal("张三", "13800138001");
        createUserInternal("李四", "13800138002");
        createUserInternal("王五", "13800138003");
    }

    @Override
    public Optional<UserInfo> findByPhone(String phone) {
        log.info("[Mock] 根据手机号查询用户: phone={}", phone);

        UserInfo user = phoneIndex.get(phone);
        return Optional.ofNullable(user);
    }

    @Override
    public Long createUser(String name, String phone) {
        log.info("[Mock] 创建用户: name={}, phone={}", name, phone);

        // 检查手机号是否已存在
        if (phoneIndex.containsKey(phone)) {
            throw new IllegalArgumentException("手机号已存在: " + phone);
        }

        return createUserInternal(name, phone);
    }

    @Override
    public List<UserInfo> findByIds(List<Long> userIds) {
        log.info("[Mock] 批量查询用户: userIds={}", userIds);

        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<UserInfo> result = new ArrayList<>();
        for (Long userId : userIds) {
            UserInfo user = idIndex.get(userId);
            if (user != null) {
                result.add(user);
            } else {
                log.warn("[Mock] 用户不存在: userId={}", userId);
            }
        }

        return result;
    }

    /**
     * 内部创建用户方法（不检查手机号唯一性）
     */
    private Long createUserInternal(String name, String phone) {
        Long userId = idGenerator++;
        UserInfo user = new UserInfo(userId, name, phone);

        phoneIndex.put(phone, user);
        idIndex.put(userId, user);

        log.info("[Mock] 用户创建成功: userId={}, name={}, phone={}", userId, name, phone);
        return userId;
    }

    /**
     * 清空 Mock 数据（用于测试）
     */
    public void clearMockData() {
        phoneIndex.clear();
        idIndex.clear();
        idGenerator = 1L;
    }

    /**
     * 获取所有 Mock 数据（用于测试）
     */
    public List<UserInfo> getAllMockData() {
        return new ArrayList<>(idIndex.values());
    }
}