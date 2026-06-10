-- Organization Domain Tables (H2 Test)
CREATE TABLE IF NOT EXISTS t_school (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    address VARCHAR(200),
    school_type VARCHAR(20),
    icon_url VARCHAR(500),
    stages VARCHAR(200),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    description VARCHAR(500),
    created_by BIGINT DEFAULT 0,
    modified_by BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS t_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    department_path VARCHAR(200),
    department_type VARCHAR(20) DEFAULT 'ORG',
    sort_order INT DEFAULT 0,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS t_department_edu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_id BIGINT NOT NULL,
    school_id BIGINT NOT NULL,
    dept_type TINYINT NOT NULL,
    stage_code VARCHAR(20) DEFAULT '',
    stage_year_code VARCHAR(32) DEFAULT '',
    grade_code VARCHAR(20) DEFAULT '',
    enrollment_year VARCHAR(32) DEFAULT '',
    created_by BIGINT DEFAULT 0,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by BIGINT DEFAULT 0,
    modified_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- 创建测试学校数据
INSERT INTO t_school (id, name, status) VALUES (1, '测试学校', 'ACTIVE');
INSERT INTO t_school (id, name, status) VALUES (2, '空学校', 'ACTIVE');

-- 索引
CREATE INDEX idx_department_school ON t_department(school_id);
CREATE INDEX idx_department_parent ON t_department(parent_id);
CREATE INDEX idx_department_path ON t_department(department_path);