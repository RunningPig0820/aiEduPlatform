-- 学生题目记录表（掌握度事实源：每道题一条记录，可追溯）
-- 位于学习域数据库 ai_edu_learning
-- 题目采集全量落库（AI 答疑 source=ai / 题库 source=bank 预留），记录题目文本、LLM 原始题型名、聚集后 canonical、对错信号、引导轮数
-- canonical_label 可空（题型未识别 PENDING 时为空，信号照常采集，归属后回填聚合）
-- score = 生效分值（0.0/0.5/1.0，含 per-题型打折后），与掌握表累计平均聚合同源
-- session_id = 原题链接（AI 答疑会话 ID，可跳回看原题；无会话记录为 NULL 显示题目原文）
-- 掌握表（t_student_topic_mastery）为聚合结果，本表为事实源——改折扣系数/信号映射重算聚合即可，证据不丢

CREATE TABLE IF NOT EXISTS `t_student_question_record` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`            BIGINT       NOT NULL COMMENT '学生ID',
    `content`               TEXT         NOT NULL COMMENT '题目文本（该轮题目，非最后一条用户消息）',
    `source`                VARCHAR(10)  NOT NULL DEFAULT 'ai' COMMENT '来源：ai（AI 答疑）/ bank（题库，预留）',
    `topic_label`           VARCHAR(255) NOT NULL COMMENT 'LLM 原始题型名（弱标注，可能飘）',
    `canonical_label`       VARCHAR(255) NULL COMMENT '聚集后 canonical 题型名（可空=题型未识别 PENDING）',
    `score`                 DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '生效分值 0.00/0.50/1.00（含 per-题型打折后）',
    `hint_count`            INT          NOT NULL DEFAULT 0 COMMENT '引导轮数（roundCount）',
    `answer_request_count`  INT          NOT NULL DEFAULT 0 COMMENT '要答案次数（answerRequestCount）',
    `session_id`            BIGINT       NULL COMMENT '原题链接：AI 答疑会话 ID（可跳回看原题，无则显示题目原文）',
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
    `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`            BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`           BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_student` (`student_id`) COMMENT '按学生查题目记录（掌握度追溯/题目列表）',
    INDEX `idx_student_canonical` (`student_id`, `canonical_label`) COMMENT '按学生+canonical 查题目（掌握度页「查看题目」）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生题目记录表（掌握度事实源）';