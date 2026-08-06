-- 学生知识点掌握度表
-- 位于学习域数据库 ai_edu_learning
-- 答疑功能归属学习域 bounded context（知识掌握度跟踪是学习域核心能力）
-- 以知识图谱 TextbookKP 节点 URI 为 key 幂等落库（student_id + kp_key 唯一）
-- mastery_level 0-100：mastered→75 / practicing→50 / struggling→25，取 max 单调不减（显式纠正例外下调）
-- evidence JSON 记录证据（命中步骤、错误事件 id 列表）

CREATE TABLE IF NOT EXISTS `t_student_kp_mastery` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`      BIGINT       NOT NULL COMMENT '学生ID',
    `kp_key`          VARCHAR(255) NOT NULL COMMENT '知识点 key（TextbookKP URI）',
    `kp_label`        VARCHAR(255) NOT NULL COMMENT '知识点名（冗余，便于展示）',
    `mastery_level`   INT          NOT NULL DEFAULT 0 COMMENT '掌握度 0-100',
    `evidence`        JSON         NULL COMMENT '证据（命中步骤、错误事件 id 列表）',
    `last_session_id` BIGINT       NULL COMMENT '最近一次答疑会话ID',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`      BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`     BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_student_kp` (`student_id`, `kp_key`) COMMENT '同一学生知识点唯一（UPSERT 幂等）',
    INDEX `idx_student` (`student_id`) COMMENT '按学生查掌握度（图谱叠加）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生知识点掌握度表';
