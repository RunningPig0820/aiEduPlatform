package com.ai.edu.domain.organization.acl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教师信息值对象（防腐层模型隔离）
 *
 * 定义在组织域，用于隔离用户域的 User 实体。
 * 组织域只知道这个简化的视图，不知道用户域的内部结构。
 *
 * ACL 模型隔离原则：
 * 1. 只暴露组织域真正需要的字段
 * 2. 组织域不依赖用户域的 User 实体
 * 3. 用户域变更时，只需修改 Gateway 实现，不影响组织域
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInfo {

    /**
     * 用户ID（关联键）
     */
    private Long userId;

    /**
     * 教师姓名
     */
    private String name;

    /**
     * 教师手机号
     */
    private String phone;
}