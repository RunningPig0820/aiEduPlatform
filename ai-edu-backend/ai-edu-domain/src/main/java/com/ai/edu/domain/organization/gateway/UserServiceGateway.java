package com.ai.edu.domain.organization.gateway;

import com.ai.edu.domain.organization.acl.TeacherInfo;

import java.util.List;

/**
 * 用户服务网关（防腐层 / Anti-Corruption Layer）
 *
 * 定义在组织域，用于隔离对用户域的调用。
 * - 接口定义在调用方域（组织域）
 * - 返回组织域自己的模型，不暴露用户域的实体
 * - 实现放在 Infrastructure 层，负责模型转换和数据源切换
 *
 * DDD 防腐层核心职责：
 * 1. 跨域调用隔离
 * 2. 模型转换（用户域 User → 组织域 TeacherInfo）
 * 3. 技术细节封装（数据源切换等）
 */
public interface UserServiceGateway {

    /**
     * 查询或创建教师用户
     *
     * 业务场景：创建教职工时，需要关联用户
     * - 用户已存在：返回现有用户信息
     * - 用户不存在：创建新用户并返回信息
     *
     * 注意：此方法涉及跨域写操作，当前规模下可接受。
     * 后续可演进为领域事件方式。
     *
     * @param name  姓名
     * @param phone 手机号
     * @return 教师信息（组织域模型）
     */
    TeacherInfo findOrCreateTeacher(String name, String phone);

    /**
     * 批量查询教师信息
     *
     * 业务场景：查询教职工列表时，需要聚合用户基本信息
     *
     * @param userIds 用户ID列表
     * @return 教师信息列表（组织域模型）
     */
    List<TeacherInfo> findTeachersByIds(List<Long> userIds);
}