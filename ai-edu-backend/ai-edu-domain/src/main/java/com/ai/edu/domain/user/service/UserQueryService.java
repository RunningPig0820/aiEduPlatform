package com.ai.edu.domain.user.service;

import com.ai.edu.domain.user.model.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户查询服务（领域服务）
 * 提供用户查询能力，供其他域调用
 *
 * DDD领域服务：处理需要协调Repository的业务逻辑
 */
public interface UserQueryService {

    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户实体
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据ID查询用户
     * @param userId 用户ID
     * @return 用户实体
     */
    Optional<User> findById(Long userId);

    /**
     * 批量查询用户
     * @param userIds 用户ID列表
     * @return 用户列表
     */
    List<User> findByIds(List<Long> userIds);

    /**
     * 创建用户
     * @param name 姓名
     * @param phone 手机号
     * @return 用户ID
     */
    Long createUser(String name, String phone);
}