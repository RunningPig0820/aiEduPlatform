package com.ai.edu.domain.organization.acl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 家长信息值对象（防腐层模型隔离）
 *
 * 定义在组织域，用于隔离用户域的 User 实体。
 * 组织域只知道这个简化的视图，不知道用户域的内部结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentInfo {

    /**
     * 用户ID（关联键）
     */
    private Long userId;

    /**
     * 家长姓名
     */
    private String name;

    /**
     * 家长手机号
     */
    private String phone;

    /**
     * 与学生关系类型（父亲/母亲/监护人等）
     */
    private String relationship;
}
