-- H2 测试数据库初始化脚本
-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- 学校表
CREATE TABLE IF NOT EXISTS t_school (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50),
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    address VARCHAR(500),
    school_type VARCHAR(50),
    description CLOB,
    created_by BIGINT NOT NULL DEFAULT 0,
    modified_by BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 年级表
CREATE TABLE IF NOT EXISTS t_grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50),
    grade_level INT,
    description CLOB,
    created_by BIGINT NOT NULL DEFAULT 0,
    modified_by BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 班级表
CREATE TABLE IF NOT EXISTS t_class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    grade VARCHAR(20) NOT NULL,
    school_year VARCHAR(20),
    class_type VARCHAR(50),
    description CLOB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0,
    modified_by BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 学生-班级关联表
CREATE TABLE IF NOT EXISTS t_student_class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    student_no VARCHAR(50),
    join_date DATE,
    leave_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0,
    modified_by BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 教师-班级关联表
CREATE TABLE IF NOT EXISTS t_teacher_class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    subject VARCHAR(50),
    is_head_teacher BOOLEAN NOT NULL DEFAULT FALSE,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0,
    modified_by BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 题目表
CREATE TABLE IF NOT EXISTS t_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content CLOB NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    knowledge_point_id BIGINT,
    knowledge_point_name VARCHAR(100),
    answer CLOB,
    analysis CLOB,
    options CLOB,
    created_by BIGINT
);

-- 知识点表
CREATE TABLE IF NOT EXISTS t_knowledge_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    subject VARCHAR(50),
    grade_level VARCHAR(20),
    description CLOB
);

-- 作业表
CREATE TABLE IF NOT EXISTS t_homework (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    question_id BIGINT,
    image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    score INT,
    feedback CLOB
);

-- 错题本表
CREATE TABLE IF NOT EXISTS t_error_book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    homework_id BIGINT,
    error_count INT NOT NULL DEFAULT 1,
    is_corrected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    last_error_at TIMESTAMP
);

-- 部门表
CREATE TABLE IF NOT EXISTS t_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    department_path VARCHAR(200),
    sort_order INT DEFAULT 0,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- 初始化测试学校数据
INSERT INTO t_school (id, name, school_type, is_deleted) VALUES (1, '测试学校', 'PRIMARY', FALSE);
INSERT INTO t_school (id, name, school_type, is_deleted) VALUES (2, '空学校', 'PRIMARY', FALSE);

-- 初始化测试用户数据（用于教职工关联）
INSERT INTO t_user (id, username, password, real_name, phone, role, enabled) VALUES
(1, 'teacher001', 'password123', '张三', '13800138001', 'TEACHER', TRUE),
(2, 'teacher002', 'password123', '李四', '13800138002', 'TEACHER', TRUE),
(3, 'teacher003', 'password123', '王五', '13800138003', 'TEACHER', TRUE);

-- 初始化测试部门数据
INSERT INTO t_department (id, school_id, name, parent_id, department_path, sort_order, is_deleted) VALUES
(1, 1, '教务处', NULL, '1', 1, FALSE),
(2, 1, '语文教研组', 1, '1_2', 1, FALSE),
(3, 1, '数学教研组', 1, '1_3', 2, FALSE);

-- 教职工关联关系表
CREATE TABLE IF NOT EXISTS t_org_teacher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL DEFAULT 0,
    modified_by BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- 索引
CREATE INDEX idx_department_school ON t_department(school_id);
CREATE INDEX idx_department_parent ON t_department(parent_id);
CREATE INDEX idx_department_path ON t_department(department_path);
CREATE UNIQUE INDEX idx_org_teacher_school_user ON t_org_teacher(school_id, user_id);
CREATE INDEX idx_org_teacher_department ON t_org_teacher(department_id);