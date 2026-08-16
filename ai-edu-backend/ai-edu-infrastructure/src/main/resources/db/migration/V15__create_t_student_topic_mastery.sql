-- 学生题型掌握度表（掌握度主体翻转：题型直接观测，知识点派生）
-- 位于学习域数据库 ai_edu_learning
-- 以归一化题型名（topic_key）为 key 幂等落库（student_id + topic_key 唯一）
-- mastery_level 0-100：mastered→75 / practicing→50 / struggling→25，取 max 单调不减
-- 知识点掌握度不再直接观测，改为「题型掌握度 × 题型→知识点 ratio」运行时派生
-- 旧 t_student_kp_mastery 保留并行（无题型映射的知识点覆盖度回退旧表，过渡期）

CREATE TABLE IF NOT EXISTS `t_student_topic_mastery` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`      BIGINT       NOT NULL COMMENT '学生ID',
    `topic_key`       VARCHAR(255) NOT NULL COMMENT '归一化题型标识（鸡兔同笼）',
    `topic_label`     VARCHAR(255) NOT NULL COMMENT '题型展示名',
    `mastery_level`   INT          NOT NULL DEFAULT 0 COMMENT '掌握度 0-100（四档 0/25/50/75）',
    `evidence`        JSON         NULL COMMENT '证据（命中步骤、错误事件 id 列表）',
    `last_session_id` BIGINT       NULL COMMENT '最近一次答疑会话ID',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`      BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`     BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_student_topic` (`student_id`, `topic_key`) COMMENT '同一学生题型唯一（UPSERT 幂等）',
    INDEX `idx_student` (`student_id`) COMMENT '按学生查题型掌握度（掌握度列表/覆盖度派生）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生题型掌握度表';
