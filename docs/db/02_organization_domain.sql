-- =====================================================
-- AI教育平台 - 组织域 (Organization Context)
-- 表结构设计文档
-- =====================================================

USE ai_edu_org;

-- =====================================================
-- 1. 学校表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_school (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学校ID',
    name VARCHAR(200) NOT NULL COMMENT '学校名称',
    code VARCHAR(50) COMMENT '学校编码',
    province VARCHAR(50) COMMENT '省份',
    city VARCHAR(50) COMMENT '城市',
    district VARCHAR(50) COMMENT '区县',
    address VARCHAR(500) COMMENT '详细地址',
    school_type VARCHAR(50) COMMENT '学校类型: PRIMARY/JUNIOR_HIGH/HIGH_SCHOOL',
    description TEXT COMMENT '学校描述',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_code (code),
    INDEX idx_province (province),
    INDEX idx_city (city),
    INDEX idx_school_type (school_type),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校表';

-- =====================================================
-- 2. 班级表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
    school_id BIGINT COMMENT '所属学校ID',
    name VARCHAR(100) NOT NULL COMMENT '班级名称',
    code VARCHAR(50) COMMENT '班级编码',
    grade VARCHAR(20) NOT NULL COMMENT '年级',
    school_year VARCHAR(20) COMMENT '学年，如2024-2025',
    class_type VARCHAR(50) COMMENT '班级类型: NORMAL/EXPERIMENTAL/INTERNATIONAL',
    description TEXT COMMENT '班级描述',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/GRADUATED/ARCHIVED',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_school (school_id),
    INDEX idx_grade (grade),
    INDEX idx_school_year (school_year),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- =====================================================
-- 3. 学生-班级关联表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_student_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    student_id BIGINT NOT NULL COMMENT '学生用户ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    student_no VARCHAR(50) COMMENT '班级内学号',
    join_date DATE COMMENT '加入日期',
    leave_date DATE COMMENT '离开日期',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/GRADUATED/TRANSFERRED',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_student_class (student_id, class_id),
    INDEX idx_class (class_id),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生-班级关联表';

-- =====================================================
-- 4. 老师-班级关联表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_teacher_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    teacher_id BIGINT NOT NULL COMMENT '老师用户ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    subject VARCHAR(50) COMMENT '任教科目',
    is_head_teacher BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否班主任',
    start_date DATE COMMENT '开始任教日期',
    end_date DATE COMMENT '结束任教日期',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_teacher_class (teacher_id, class_id),
    INDEX idx_class (class_id),
    INDEX idx_head_teacher (is_head_teacher),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老师-班级关联表';

-- =====================================================
-- 5. 年级表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_grade (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '年级ID',
    school_id BIGINT COMMENT '所属学校ID',
    name VARCHAR(50) NOT NULL COMMENT '年级名称',
    code VARCHAR(50) COMMENT '年级编码',
    grade_level INT COMMENT '年级序号: 1-12',
    description TEXT COMMENT '年级描述',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_school (school_id),
    INDEX idx_grade_level (grade_level),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年级表';

-- =====================================================
-- ER图关系说明
-- =====================================================
-- t_school (1) <-----> (N) t_class
-- t_school (1) <-----> (N) t_grade
-- t_class (1) <-----> (N) t_student_class
-- t_class (1) <-----> (N) t_teacher_class
-- t_user (student) (1) <-----> (N) t_student_class
-- t_user (teacher) (1) <-----> (N) t_teacher_class
--
-- 年级级别说明:
-- | grade_level | 说明       |
-- |-------------|------------|
-- | 1-6         | 小学1-6年级|
-- | 7-9         | 初中1-3年级|
-- | 10-12       | 高中1-3年级|
--
-- 学校类型说明:
-- | school_type  | 说明     |
-- |--------------|----------|
-- | PRIMARY      | 小学     |
-- | JUNIOR_HIGH  | 初中     |
-- | HIGH_SCHOOL  | 高中     |
--
-- 班级状态说明:
-- | status   | 说明         |
-- |----------|--------------|
-- | ACTIVE   | 活跃班级     |
-- | GRADUATED| 已毕业       |
-- | ARCHIVED | 已归档       |