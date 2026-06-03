-- 创建部门表
-- 数据库: ai_edu_org
-- 部门用于行政组织归属，支持树形层级结构

CREATE TABLE IF NOT EXISTS `t_department` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `school_id` BIGINT NOT NULL COMMENT '所属学校ID',
    `name` VARCHAR(100) NOT NULL COMMENT '部门名称',
    `parent_id` BIGINT DEFAULT NULL COMMENT '上级部门ID，NULL表示根节点',
    `department_path` VARCHAR(200) DEFAULT NULL COMMENT '部门路径，格式如 1_3_5，根节点为空',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '部门描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_school_id` (`school_id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_department_path` (`department_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- 创建唯一索引：同一学校下部门名称唯一
CREATE UNIQUE INDEX `idx_school_name` ON `t_department` (`school_id`, `name`) WHERE `is_deleted` = 0;