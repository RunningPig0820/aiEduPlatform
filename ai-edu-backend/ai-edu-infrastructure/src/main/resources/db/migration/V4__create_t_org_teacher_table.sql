-- 创建教职工关联关系表
-- 教职工本质是用户与部门的关联关系，不存储用户基本信息
CREATE TABLE t_org_teacher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    school_id BIGINT NOT NULL COMMENT '所属学校ID',
    user_id BIGINT NOT NULL COMMENT '关联用户域用户ID',
    department_id BIGINT NOT NULL COMMENT '所属行政部门ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人(登录用户ID)',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '最后修改人(登录用户ID)',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记(0:未删除 1:已删除)',
    UNIQUE INDEX idx_school_user (school_id, user_id) COMMENT '一个用户在一个学校只能有一条教职工记录',
    INDEX idx_department (department_id) COMMENT '部门索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教职工关联关系表';