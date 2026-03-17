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