package com.ai.edu.domain.organization.gateway;

import com.ai.edu.domain.user.model.entity.User;

import java.util.List;

/**
 * 用户服务网关（防腐层 / Anti-Corruption Layer）
 *
 * 定义在组织域，用于隔离对用户域的调用。
 * 组织域通过此接口获取用户信息，不直接依赖用户域的 Domain Service。
 *
 * DDD 防腐层原则：
 * 1. 接口定义在调用方域（组织域）
 * 2. 实现放在 Infrastructure 层，负责技术细节（数据源切换等）
 * 3. 只暴露调用方域真正需要的方法，保持接口精简
 */
public interface UserServiceGateway {

    /**
     * 查询或创建用户
     *
     * 业务场景：创建教职工时，需要关联用户
     * - 用户已存在：返回现有用户ID
     * - 用户不存在：创建新用户并返回ID
     *
     * @param name  姓名
     * @param phone 手机号
     * @return 用户ID
     */
    Long findOrCreateUser(String name, String phone);

    /**
     * 批量查询用户信息
     *
     * 业务场景：查询教职工列表时，需要聚合用户基本信息
     *
     * @param userIds 用户ID列表
     * @return 用户实体列表
     */
    List<User> findUsersByIds(List<Long> userIds);
}