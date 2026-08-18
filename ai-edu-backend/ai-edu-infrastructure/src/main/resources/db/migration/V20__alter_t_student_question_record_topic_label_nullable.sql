-- V20: 题目记录表 topic_label 改可空——答疑 PENDING（题型未识别，masterySignals 空）题目照常落库，
-- 信号不丢（SIG-006）；PENDING 语义与 canonical_label 一致（无题型名 → topic_label=NULL）。
-- V17 定义 topic_label NOT NULL，与 persistQuestionAttempt 落 null 冲突（真实 DB 插入报错），本迁移修复。
-- Flyway 关闭，需在 learning 库手动执行。
ALTER TABLE `t_student_question_record`
    MODIFY COLUMN `topic_label` VARCHAR(255) NULL
    COMMENT 'LLM 原始题型名（弱标注，可能飘）；PENDING 题型未识别可为 NULL（与 canonical_label 同语义）';
