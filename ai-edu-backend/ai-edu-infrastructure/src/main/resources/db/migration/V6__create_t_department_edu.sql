-- 创建教育部门扩展属性表
-- 存储行政班节点的教育特有属性：学段、年制、年级编码、入学年份
-- 通过 dept_id 反向关联 t_department
CREATE TABLE IF NOT EXISTS `t_department_edu` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dept_id`         BIGINT       NOT NULL COMMENT '关联 t_department.id',
    `school_id`       BIGINT       NOT NULL COMMENT '所属学校ID',
    `dept_type`       TINYINT      NOT NULL COMMENT '节点类型：3-学段，4-年级，5-班级',
    `stage_code`      VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '学段编码：PRIMARY/JUNIOR_HIGH/SENIOR_HIGH',
    `stage_year_code` VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '年制：4-小学六年制，5-初中三年制，7-高中三年制',
    `grade_code`      VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '年级编码 1-12',
    `enrollment_year` VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '入学年份',
    `created_by`      BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `created_on`      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_by`     BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modified_on`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_school_dept` (`school_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教育部门扩展属性表';
