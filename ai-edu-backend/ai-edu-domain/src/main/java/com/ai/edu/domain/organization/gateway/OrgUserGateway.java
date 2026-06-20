package com.ai.edu.domain.organization.gateway;

import com.ai.edu.domain.organization.acl.ParentBinding;
import com.ai.edu.domain.organization.acl.ParentInfo;
import com.ai.edu.domain.organization.acl.StudentInfo;
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
 * 2. 模型转换（用户域 User → 组织域 ACL 模型）
 * 3. 技术细节封装（数据源切换等）
 */
public interface OrgUserGateway {

    // ==================== 教师相关 ====================

    /**
     * 查询或创建教师用户
     *
     * 业务场景：创建教职工时，需要关联用户
     * - 用户已存在：返回现有用户信息
     * - 用户不存在：创建新用户并返回信息
     *
     * @param name  姓名
     * @param phone 手机号
     * @return 教师信息（组织域模型）
     */
    TeacherInfo findOrCreateTeacher(String name, String phone);

    /**
     * 批量查询教师信息
     *
     * @param userIds 用户ID列表
     * @return 教师信息列表（组织域模型）
     */
    List<TeacherInfo> findTeachersByIds(List<Long> userIds);

    // ==================== 学生相关 ====================

    /**
     * 查询或创建学生用户
     *
     * 业务场景：创建行政班学生时，需要关联学生用户
     * - 用户已存在且角色为 STUDENT：返回现有用户信息
     * - 用户不存在：创建 STUDENT 角色用户（含加密后的身份证号）
     * - 用户已存在但角色非 STUDENT：抛出异常
     *
     * @param name   姓名
     * @param phone  手机号
     * @param idCard 身份证号（明文，由 Gateway 实现层负责加密）
     * @return 学生信息（含脱敏身份证号）
     */
    StudentInfo findOrCreateStudent(String name, String phone, String idCard);

    // ==================== 家长相关 ====================

    /**
     * 查询或创建家长用户
     *
     * 业务场景：创建行政班学生时，需要绑定家长
     * - 用户已存在且角色为 PARENT：返回现有用户信息
     * - 用户不存在：创建 PARENT 角色用户
     * - 用户已存在但角色非 PARENT：抛出异常
     *
     * @param name  姓名
     * @param phone 手机号
     * @return 家长信息（组织域模型）
     */
    ParentInfo findOrCreateParent(String name, String phone);

    /**
     * 绑定学生与家长的关联关系
     *
     * 业务场景：学生和所有家长创建完成后，建立 t_parent_profile 关联
     *
     * @param studentUserId 学生用户ID
     * @param bindings      家长绑定列表（含家长用户ID和关系类型）
     */
    void bindStudentParents(Long studentUserId, List<ParentBinding> bindings);

    /**
     * 查询学生绑定的家长列表
     *
     * 业务场景：查询行政班学生列表时，聚合家长信息
     *
     * @param studentUserId 学生用户ID
     * @return 家长信息列表（组织域模型）
     */
    List<ParentInfo> findParentsByStudentUserId(Long studentUserId);
}