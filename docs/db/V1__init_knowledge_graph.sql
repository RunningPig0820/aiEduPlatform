-- ============================================
-- Knowledge Graph Tables
-- Neo4j → MySQL sync for textbook knowledge points
-- ============================================

USE `ai_edu_kg`;

-- ============================================
-- 节点主表（存储属性，URI 作为唯一标识）
-- ============================================

-- 教材表
CREATE TABLE IF NOT EXISTS `t_kg_textbook` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `uri` VARCHAR(512) NOT NULL UNIQUE COMMENT 'Neo4j URI 唯一标识',
    `label` VARCHAR(128) NOT NULL COMMENT '教材名称',
    `grade` VARCHAR(32) NOT NULL COMMENT '年级',
    `phase` VARCHAR(16) NOT NULL COMMENT '学段: primary/middle/high',
    `subject` VARCHAR(16) NOT NULL DEFAULT 'math' COMMENT '学科',
    `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态: active/deleted/merged',
    `merged_to_uri` VARCHAR(512) DEFAULT NULL COMMENT '被合并时指向新URI',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_by` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱-教材表';

-- 章节表
CREATE TABLE IF NOT EXISTS `t_kg_chapter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `uri` VARCHAR(512) NOT NULL UNIQUE COMMENT 'Neo4j URI 唯一标识',
    `label` VARCHAR(128) NOT NULL COMMENT '章节名称',
    `topic` VARCHAR(64) DEFAULT NULL COMMENT '专题',
    `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态: active/deleted/merged',
    `merged_to_uri` VARCHAR(512) DEFAULT NULL COMMENT '被合并时指向新URI',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_by` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱-章节表';

-- 小节表
CREATE TABLE IF NOT EXISTS `t_kg_section` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `uri` VARCHAR(512) NOT NULL UNIQUE COMMENT 'Neo4j URI 唯一标识',
    `label` VARCHAR(128) NOT NULL COMMENT '小节名称',
    `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态: active/deleted/merged',
    `merged_to_uri` VARCHAR(512) DEFAULT NULL COMMENT '被合并时指向新URI',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_by` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱-小节表';

-- 知识点表（全局存储，后续班级/学生通过关联表引用）
CREATE TABLE IF NOT EXISTS `t_kg_knowledge_point` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `uri` VARCHAR(512) NOT NULL UNIQUE COMMENT 'Neo4j URI 唯一标识',
    `label` VARCHAR(256) NOT NULL COMMENT '知识点名称',
    `difficulty` VARCHAR(16) DEFAULT NULL COMMENT '难度: easy/medium/hard',
    `importance` VARCHAR(16) DEFAULT NULL COMMENT '重要性: low/medium/high',
    `cognitive_level` VARCHAR(32) DEFAULT NULL COMMENT '认知层次: 记忆/理解/应用/分析',
    `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态: active/deleted/merged',
    `merged_to_uri` VARCHAR(512) DEFAULT NULL COMMENT '被合并时指向新URI',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_by` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱-知识点表';

-- ============================================
-- 层级关系表（固定结构，用于快速导航和进度统计）
-- ============================================

-- 教材 -> 章节
CREATE TABLE IF NOT EXISTS `t_kg_textbook_chapter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `textbook_uri` VARCHAR(512) NOT NULL COMMENT '教材URI',
    `chapter_uri` VARCHAR(512) NOT NULL COMMENT '章节URI',
    `order_index` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_by` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY `uk_textbook_chapter` (`textbook_uri`, `chapter_uri`),
    KEY `idx_kg_tc_chapter` (`chapter_uri`, `order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱-教材章节关联';

-- 章节 -> 小节
CREATE TABLE IF NOT EXISTS `t_kg_chapter_section` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `chapter_uri` VARCHAR(512) NOT NULL COMMENT '章节URI',
    `section_uri` VARCHAR(512) NOT NULL COMMENT '小节URI',
    `order_index` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_by` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY `uk_chapter_section` (`chapter_uri`, `section_uri`),
    KEY `idx_kg_cs_section` (`section_uri`, `order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱-章节小节关联';

-- 小节 -> 知识点 (TextbookKP)
CREATE TABLE IF NOT EXISTS `t_kg_section_kp` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `section_uri` VARCHAR(512) NOT NULL COMMENT '小节URI',
    `kp_uri` VARCHAR(512) NOT NULL COMMENT '知识点URI',
    `order_index` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_by` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY `uk_section_kp` (`section_uri`, `kp_uri`),
    KEY `idx_kg_sk_kp` (`kp_uri`, `order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱-小节知识点关联';

-- ============================================
-- 同步记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `t_kg_sync_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `sync_type` VARCHAR(16) NOT NULL COMMENT '同步类型: full (按需触发)',
    `scope` JSON DEFAULT NULL COMMENT '同步范围：{"subject":"math","grade":"一年级"} 等',
    `status` VARCHAR(16) NOT NULL COMMENT '状态: running/success/failed',
    `inserted_count` INT NOT NULL DEFAULT 0 COMMENT '新增数量',
    `updated_count` INT NOT NULL DEFAULT 0 COMMENT '更新数量',
    `status_changed_count` INT NOT NULL DEFAULT 0 COMMENT '状态变更数量（deleted/merged）',
    `reconciliation_status` VARCHAR(16) DEFAULT NULL COMMENT '对账结果: matched/mismatched',
    `reconciliation_details` JSON DEFAULT NULL COMMENT '对账详情：neo4j counts vs mysql counts',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `details` JSON DEFAULT NULL COMMENT '同步明细：各阶段耗时、异常 URI 列表',
    `started_at` DATETIME DEFAULT NULL,
    `finished_at` DATETIME DEFAULT NULL,
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_by` BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱-同步记录表';

-- ============================================
-- 索引
-- ============================================

-- 教材表查询索引
CREATE INDEX `idx_kg_textbook_grade` ON `t_kg_textbook` (`grade`);
CREATE INDEX `idx_kg_textbook_subject` ON `t_kg_textbook` (`subject`);
CREATE INDEX `idx_kg_textbook_phase` ON `t_kg_textbook` (`phase`);
CREATE INDEX `idx_kg_textbook_status` ON `t_kg_textbook` (`status`);

-- 章节表查询索引
CREATE INDEX `idx_kg_chapter_topic` ON `t_kg_chapter` (`topic`);
CREATE INDEX `idx_kg_chapter_status` ON `t_kg_chapter` (`status`);

-- 小节表查询索引
CREATE INDEX `idx_kg_section_status` ON `t_kg_section` (`status`);

-- 知识点表查询索引
CREATE INDEX `idx_kg_kp_status` ON `t_kg_knowledge_point` (`status`);
CREATE INDEX `idx_kg_kp_label` ON `t_kg_knowledge_point` (`label`(100));
CREATE INDEX `idx_kg_kp_difficulty` ON `t_kg_knowledge_point` (`difficulty`);
CREATE INDEX `idx_kg_kp_merged` ON `t_kg_knowledge_point` (`merged_to_uri`(100));

-- 关联表排序索引
CREATE INDEX `idx_kg_tc_chapter_order` ON `t_kg_textbook_chapter` (`chapter_uri`, `order_index`) ;
CREATE INDEX `idx_kg_cs_section_order` ON `t_kg_chapter_section` (`section_uri`, `order_index`) ;
CREATE INDEX `idx_kg_sk_kp_order` ON `t_kg_section_kp` (`kp_uri`, `order_index`) ;

-- 同步记录表查询索引
CREATE INDEX `idx_kg_sync_status` ON `t_kg_sync_record` (`status`, `started_at`);
