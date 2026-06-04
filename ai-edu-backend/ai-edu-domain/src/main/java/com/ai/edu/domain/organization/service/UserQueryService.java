package com.ai.edu.domain.organization.service;

import java.util.List;
import java.util.Optional;

/**
 * 用户域查询服务（防腐层接口）
 * 由用户域实现，组织域通过此接口访问用户域数据
 */
public interface UserQueryService {

    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户基本信息
     */
    Optional<UserInfo> findByPhone(String phone);

    /**
     * 创建用户（返回用户ID）
     * @param name 用户姓名
     * @param phone 手机号
     * @return 用户ID
     */
    Long createUser(String name, String phone);

    /**
     * 批量查询用户基本信息
     * @param userIds 用户ID列表
     * @return 用户基本信息列表
     */
    List<UserInfo> findByIds(List<Long> userIds);

    /**
     * 用户基本信息DTO
     */
    record UserInfo(
            Long userId,
            String name,
            String phone
    ) {}
}