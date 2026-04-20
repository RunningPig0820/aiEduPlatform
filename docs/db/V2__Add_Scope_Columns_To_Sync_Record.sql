-- ============================================
-- V2: 同步记录表增加维度列（edition/subject/stage/grade）
-- 用于按年级拆分同步任务，支持细粒度锁和独立对账
-- ============================================

USE `ai_edu_kg`;

-- 1. 添加维度字段（替代原有 JSON scope 字段，便于查询）
ALTER TABLE `t_kg_sync_record`
    ADD COLUMN `edition` VARCHAR(100) DEFAULT NULL COMMENT '教材版本' AFTER `sync_type`,
    ADD COLUMN `subject` VARCHAR(50)  DEFAULT NULL COMMENT '学科' AFTER `edition`,
    ADD COLUMN `stage` VARCHAR(50)   DEFAULT NULL COMMENT '学段' AFTER `subject`,
    ADD COLUMN `grade` VARCHAR(50)   DEFAULT NULL COMMENT '年级' AFTER `stage`;

-- 2. 创建复合索引（支持按维度查询最近同步记录）
-- 用于 findLatestRunningByScope 和过期任务检测
ALTER TABLE `t_kg_sync_record`
    ADD INDEX `idx_scope` (`edition`, `subject`, `stage`, `grade`);

-- 3. 保留原 scope JSON 列向后兼容（可后续清理）
-- ALTER TABLE `t_kg_sync_record` DROP COLUMN `scope`;
