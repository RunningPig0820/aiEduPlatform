-- AI 答疑会话表
-- 位于学习域数据库 ai_edu_learning
-- 答疑功能归属学习域 bounded context（掌握度/错误事件是学习域核心数据）
-- 存储答疑会话的结构化业务数据（对话每轮实时整写 COS 恒为完整对话；Redis 活跃期热存；不建消息表）
-- 不持久化题目内容——current_question 为 Redis 瞬时字段，换题只作事件（重置计数）
-- 会话仅保留生命周期 3 态：ACTIVE / ARCHIVED / TERMINATED
-- 无流程状态机——round_count / answer_request_count 为护栏计数器（round ≤ 20）

CREATE TABLE IF NOT EXISTS `t_tutoring_session` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `student_id`          BIGINT       NOT NULL COMMENT '学生ID（网关注入）',
    `subject`             VARCHAR(32)  NOT NULL DEFAULT 'math' COMMENT '学科（本期恒为 math）',
    `question_type`       VARCHAR(32)  NULL COMMENT '题型（答疑侧独立可扩展枚举）',
    `question_kind`       VARCHAR(32)  NULL COMMENT '题类（计算/应用/证明）',
    `intent_category`     VARCHAR(16)  NULL COMMENT '意图类别：ACADEMIC/GUIDANCE/UNRELATED（MVP 保留可空）',
    `last_emotion`        VARCHAR(16)  NULL COMMENT '最近一轮情绪（F7 七态，Python 输出方权威）',
    `status`              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态：ACTIVE/ARCHIVED/TERMINATED',
    `round_count`         INT          NOT NULL DEFAULT 0 COMMENT '轮次（≤20，换题时重置）',
    `answer_request_count` INT         NOT NULL DEFAULT 0 COMMENT '要答案次数（第 1 次思路/第 2 次答案）',
    `end_reason`          VARCHAR(32)  NULL COMMENT '收尾原因：COMPLETED/ANSWER_REVEALED/ABANDONED/ROUND_LIMIT',
    `transcript_url`      VARCHAR(512) NULL COMMENT 'COS 对话归档 objectKey（首次实时写时回填）',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（=会话开始）',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `archived_at`         DATETIME     NULL COMMENT '归档时间',
    `created_by`          BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`         BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_student_status` (`student_id`, `status`) COMMENT '按学生查活跃会话/历史会话'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 答疑会话表';
