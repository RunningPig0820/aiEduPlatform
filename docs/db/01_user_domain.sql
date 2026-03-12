-- =====================================================
-- AI教育平台 - 用户域 (User Context)
-- 表结构设计文档
-- =====================================================

USE ai_edu_user;

-- =====================================================
-- 1. 用户表 (聚合根)
-- =====================================================
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    role VARCHAR(20) NOT NULL COMMENT '角色: STUDENT/TEACHER/PARENT/ADMIN',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_username (username),
    INDEX idx_role (role),
    INDEX idx_phone (phone),
    INDEX idx_enabled (enabled),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =====================================================
-- 2. 学生扩展信息表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_student_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学生档案ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    student_no VARCHAR(50) COMMENT '学号',
    grade VARCHAR(20) COMMENT '年级',
    school VARCHAR(100) COMMENT '学校',
    class_name VARCHAR(50) COMMENT '班级名称',
    enrollment_date DATE COMMENT '入学日期',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_user_id (user_id),
    UNIQUE KEY uk_student_no (student_no),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生扩展信息表';

-- =====================================================
-- 3. 老师扩展信息表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_teacher_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '老师档案ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    teacher_no VARCHAR(50) COMMENT '工号',
    title VARCHAR(50) COMMENT '职称',
    department VARCHAR(100) COMMENT '部门/院系',
    specialty TEXT COMMENT '专业领域',
    hire_date DATE COMMENT '入职日期',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_user_id (user_id),
    UNIQUE KEY uk_teacher_no (teacher_no),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老师扩展信息表';

-- =====================================================
-- 4. 家长扩展信息表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_parent_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '家长档案ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    occupation VARCHAR(100) COMMENT '职业',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家长扩展信息表';

-- =====================================================
-- 5. 学生-家长关联表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_student_parent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    student_id BIGINT NOT NULL COMMENT '学生用户ID',
    parent_id BIGINT NOT NULL COMMENT '家长用户ID',
    relationship VARCHAR(20) NOT NULL COMMENT '关系: FATHER/MOTHER/GUARDIAN/OTHER',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否主要监护人',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_student_parent (student_id, parent_id),
    INDEX idx_student (student_id),
    INDEX idx_parent (parent_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生-家长关联表';

-- =====================================================
-- ER图关系说明
-- =====================================================
-- t_user (1) <-----> (0..1) t_student_profile
-- t_user (1) <-----> (0..1) t_teacher_profile
-- t_user (1) <-----> (0..1) t_parent_profile
-- t_user (M) <-----> (N) t_user (通过 t_student_parent)
--
-- 角色说明:
-- | role    | 说明   | 扩展表              |
-- |---------|--------|---------------------|
-- | STUDENT | 学生   | t_student_profile   |
-- | TEACHER | 老师   | t_teacher_profile   |
-- | PARENT  | 家长   | t_parent_profile    |
-- | ADMIN   | 管理员 | -                    |
--
-- 关系类型说明:
-- | relationship | 说明     |
-- |--------------|----------|
-- | FATHER       | 父亲     |
-- | MOTHER       | 母亲     |
-- | GUARDIAN     | 监护人   |
-- | OTHER        | 其他     |