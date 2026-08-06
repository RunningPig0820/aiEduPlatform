-- 答疑错误事件表
-- 位于学习域数据库 ai_edu_learning
-- 答疑功能归属学习域 bounded context（错误事件/错题是学习域核心数据）
-- 记录引导过程中学生对易错分支的选择/典型误解（eval.correct=false 时写入），形成结构化错误事件
-- emotion 存该轮情绪（F7 七态：NEUTRAL/CONFUSED/FRUSTRATED/ANXIOUS/CONFIDENT/INTERESTED/BORED）

CREATE TABLE IF NOT EXISTS `t_tutoring_error_event` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`     BIGINT       NOT NULL COMMENT '学生ID',
    `session_id`     BIGINT       NOT NULL COMMENT '答疑会话ID',
    `kp_key`         VARCHAR(255) NULL COMMENT '关联知识点（TextbookKP URI，可空）',
    `error_type`     VARCHAR(64)  NULL COMMENT 'eval 输出的错误类型',
    `emotion`        VARCHAR(16)  NULL COMMENT '该轮情绪（F7 七态）',
    `step_index`     INT          NULL COMMENT '出错步骤索引',
    `student_answer` TEXT         NULL COMMENT '学生原答',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`     BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`    BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_student_created` (`student_id`, `created_at`) COMMENT '按学生查错误历史/趋势'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑错误事件表';
