-- 家长信息扩展表
-- 存储学生-家长的关联关系，位于用户域数据库 ai_edu_user
CREATE TABLE t_parent_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    student_user_id BIGINT NOT NULL COMMENT '学生用户ID',
    parent_user_id BIGINT NOT NULL COMMENT '家长用户ID',
    relationship VARCHAR(32) NOT NULL DEFAULT '' COMMENT '关系类型(父亲/母亲/监护人等)',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主要联系人(0:否 1:是)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人(登录用户ID)',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '最后修改人(登录用户ID)',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记(0:未删除 1:已删除)',
    UNIQUE INDEX idx_student_parent (student_user_id, parent_user_id) COMMENT '同一学生与家长唯一',
    INDEX idx_student (student_user_id) COMMENT '学生索引',
    INDEX idx_parent (parent_user_id) COMMENT '家长索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家长信息扩展表';
