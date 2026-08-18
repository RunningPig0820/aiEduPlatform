-- V19: 题目记录表 score 改为可空——analyze 贴题（无对错信号）score=null 表达「无信号」，
-- 与「答错 score=0.00」区分：掌握表重算跳过 null（不拉低掌握度），SIG-007「不产生信号」语义落地。
-- Flyway 关闭，需在 learning 库手动执行：source 05_learning_domain.sql 或本文件。
ALTER TABLE `t_student_question_record`
    MODIFY COLUMN `score` DECIMAL(3,2) NULL
    COMMENT '生效分值 0.00/0.50/1.00（含 per-题型打折后）；NULL=无信号（analyze 贴题，不参与掌握表聚合）';
