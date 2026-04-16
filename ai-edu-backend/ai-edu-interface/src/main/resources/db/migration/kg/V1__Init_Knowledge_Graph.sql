-- 知识图谱数据库初始化
-- 4 张节点主表 + 3 张层级关联表 + 1 张同步记录表

-- ==================== 节点主表 ====================

-- 1. 教材表
CREATE TABLE IF NOT EXISTS `t_kg_textbook` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uri` VARCHAR(500) NOT NULL,
    `label` VARCHAR(200) NOT NULL,
    `grade` VARCHAR(50) NOT NULL,
    `phase` VARCHAR(50) NOT NULL,
    `subject` VARCHAR(50) NOT NULL,
    `status` VARCHAR(20) DEFAULT 'active',
    `merged_to_uri` VARCHAR(500) DEFAULT NULL,
    `created_by` BIGINT DEFAULT 0,
    `modified_by` BIGINT DEFAULT 0,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uri` (`uri`(255)),
    KEY `idx_grade` (`grade`),
    KEY `idx_subject` (`subject`),
    KEY `idx_phase` (`phase`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 章节表
CREATE TABLE IF NOT EXISTS `t_kg_chapter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uri` VARCHAR(500) NOT NULL,
    `label` VARCHAR(200) NOT NULL,
    `topic` VARCHAR(100) DEFAULT NULL,
    `status` VARCHAR(20) DEFAULT 'active',
    `merged_to_uri` VARCHAR(500) DEFAULT NULL,
    `created_by` BIGINT DEFAULT 0,
    `modified_by` BIGINT DEFAULT 0,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uri` (`uri`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 小节表
CREATE TABLE IF NOT EXISTS `t_kg_section` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uri` VARCHAR(500) NOT NULL,
    `label` VARCHAR(200) NOT NULL,
    `status` VARCHAR(20) DEFAULT 'active',
    `merged_to_uri` VARCHAR(500) DEFAULT NULL,
    `created_by` BIGINT DEFAULT 0,
    `modified_by` BIGINT DEFAULT 0,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uri` (`uri`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 知识点表
CREATE TABLE IF NOT EXISTS `t_kg_knowledge_point` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uri` VARCHAR(500) NOT NULL,
    `label` VARCHAR(200) NOT NULL,
    `difficulty` VARCHAR(20) DEFAULT NULL,
    `importance` VARCHAR(20) DEFAULT NULL,
    `cognitive_level` VARCHAR(20) DEFAULT NULL,
    `status` VARCHAR(20) DEFAULT 'active',
    `merged_to_uri` VARCHAR(500) DEFAULT NULL,
    `created_by` BIGINT DEFAULT 0,
    `modified_by` BIGINT DEFAULT 0,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uri` (`uri`(255)),
    KEY `idx_label` (`label`),
    KEY `idx_difficulty` (`difficulty`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 层级关联表 ====================

-- 5. 教材-章节关联表
CREATE TABLE IF NOT EXISTS `t_kg_textbook_chapter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `textbook_uri` VARCHAR(500) NOT NULL,
    `chapter_uri` VARCHAR(500) NOT NULL,
    `order_index` INT DEFAULT 0,
    `created_by` BIGINT DEFAULT 0,
    `modified_by` BIGINT DEFAULT 0,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_textbook_uri` (`textbook_uri`(255)),
    KEY `idx_chapter_uri` (`chapter_uri`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 章节-小节关联表
CREATE TABLE IF NOT EXISTS `t_kg_chapter_section` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `chapter_uri` VARCHAR(500) NOT NULL,
    `section_uri` VARCHAR(500) NOT NULL,
    `order_index` INT DEFAULT 0,
    `created_by` BIGINT DEFAULT 0,
    `modified_by` BIGINT DEFAULT 0,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_chapter_uri` (`chapter_uri`(255)),
    KEY `idx_section_uri` (`section_uri`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 小节-知识点关联表
CREATE TABLE IF NOT EXISTS `t_kg_section_kp` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `section_uri` VARCHAR(500) NOT NULL,
    `kp_uri` VARCHAR(500) NOT NULL,
    `order_index` INT DEFAULT 0,
    `created_by` BIGINT DEFAULT 0,
    `modified_by` BIGINT DEFAULT 0,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_section_uri` (`section_uri`(255)),
    KEY `idx_kp_uri` (`kp_uri`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 同步记录表 ====================

-- 8. 同步记录表
CREATE TABLE IF NOT EXISTS `t_kg_sync_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sync_type` VARCHAR(20) NOT NULL DEFAULT 'full',
    `scope` JSON DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'running',
    `inserted_count` INT DEFAULT 0,
    `updated_count` INT DEFAULT 0,
    `status_changed_count` INT DEFAULT 0,
    `reconciliation_status` VARCHAR(20) DEFAULT NULL,
    `reconciliation_details` TEXT DEFAULT NULL,
    `error_message` TEXT DEFAULT NULL,
    `details` JSON DEFAULT NULL,
    `started_at` DATETIME DEFAULT NULL,
    `finished_at` DATETIME DEFAULT NULL,
    `created_by` BIGINT DEFAULT 0,
    `modified_by` BIGINT DEFAULT 0,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_started_at` (`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
