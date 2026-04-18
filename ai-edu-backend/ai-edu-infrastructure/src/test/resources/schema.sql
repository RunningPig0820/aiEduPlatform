-- Knowledge Graph Schema (H2 Test)
CREATE TABLE IF NOT EXISTS t_kg_textbook (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uri VARCHAR(500) NOT NULL,
    label VARCHAR(200),
    grade VARCHAR(50),
    stage VARCHAR(50),
    edition VARCHAR(100),
    subject VARCHAR(50),
    order_index INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'active',
    merged_to_uri VARCHAR(500),
    created_by BIGINT DEFAULT 0,
    modified_by BIGINT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS t_kg_chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uri VARCHAR(500) NOT NULL,
    label VARCHAR(200),
    topic VARCHAR(500),
    status VARCHAR(20) DEFAULT 'active',
    merged_to_uri VARCHAR(500),
    created_by BIGINT DEFAULT 0,
    modified_by BIGINT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS t_kg_section (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uri VARCHAR(500) NOT NULL,
    label VARCHAR(200),
    status VARCHAR(20) DEFAULT 'active',
    merged_to_uri VARCHAR(500),
    created_by BIGINT DEFAULT 0,
    modified_by BIGINT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS t_kg_knowledge_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uri VARCHAR(500) NOT NULL,
    label VARCHAR(200),
    difficulty VARCHAR(20),
    importance VARCHAR(20),
    cognitive_level VARCHAR(20),
    status VARCHAR(20) DEFAULT 'active',
    merged_to_uri VARCHAR(500),
    created_by BIGINT DEFAULT 0,
    modified_by BIGINT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS t_kg_textbook_chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    textbook_uri VARCHAR(500) NOT NULL,
    chapter_uri VARCHAR(500) NOT NULL,
    order_index INT DEFAULT 0,
    created_by BIGINT DEFAULT 0,
    modified_by BIGINT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS t_kg_chapter_section (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_uri VARCHAR(500) NOT NULL,
    section_uri VARCHAR(500) NOT NULL,
    order_index INT DEFAULT 0,
    created_by BIGINT DEFAULT 0,
    modified_by BIGINT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS t_kg_section_kp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_uri VARCHAR(500) NOT NULL,
    kp_uri VARCHAR(500) NOT NULL,
    order_index INT DEFAULT 0,
    created_by BIGINT DEFAULT 0,
    modified_by BIGINT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS t_kg_sync_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sync_type VARCHAR(50),
    scope VARCHAR(500),
    status VARCHAR(20),
    inserted_count INT DEFAULT 0,
    updated_count INT DEFAULT 0,
    status_changed_count INT DEFAULT 0,
    reconciliation_status VARCHAR(20),
    reconciliation_details VARCHAR(2000),
    error_message VARCHAR(2000),
    details VARCHAR(2000),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_by BIGINT DEFAULT 0,
    modified_by BIGINT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- Indexes
CREATE INDEX idx_kg_textbook_grade ON t_kg_textbook(grade);
CREATE INDEX idx_kg_textbook_subject ON t_kg_textbook(subject);
CREATE INDEX idx_kg_textbook_stage ON t_kg_textbook(stage);
CREATE INDEX idx_kg_chapter_status ON t_kg_chapter(status);
CREATE INDEX idx_kg_section_status ON t_kg_section(status);
CREATE INDEX idx_kg_kp_status ON t_kg_knowledge_point(status);
CREATE INDEX idx_tb_chapter_textbook ON t_kg_textbook_chapter(textbook_uri);
CREATE INDEX idx_ch_section_chapter ON t_kg_chapter_section(chapter_uri);
CREATE INDEX idx_sec_kp_section ON t_kg_section_kp(section_uri);
