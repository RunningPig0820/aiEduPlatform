-- V2__create_school_user_table.sql
-- Create school_user association table for school permission scope

CREATE TABLE IF NOT EXISTS t_school_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    school_id BIGINT NOT NULL COMMENT '学校ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_type VARCHAR(50) NOT NULL COMMENT '用户在学校中的角色：ADMIN/TEACHER/STUDENT/PARENT',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',

    -- Indexes
    INDEX idx_school_user_school_id (school_id),
    INDEX idx_school_user_user_id (user_id),

    -- Unique constraint: one user can only have one role in one school
    UNIQUE INDEX uniq_school_user (school_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校用户关联表';

-- Add foreign key constraint (optional, may be omitted for flexibility)
-- ALTER TABLE t_school_user ADD CONSTRAINT fk_school_user_school FOREIGN KEY (school_id) REFERENCES t_school(id);
-- ALTER TABLE t_school_user ADD CONSTRAINT fk_school_user_user FOREIGN KEY (user_id) REFERENCES t_user(id);