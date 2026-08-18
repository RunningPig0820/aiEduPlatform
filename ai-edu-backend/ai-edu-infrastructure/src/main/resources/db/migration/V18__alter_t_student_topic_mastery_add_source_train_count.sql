-- t_student_topic_mastery 掌握表改造（设计 Decision 1/2：加 source/train_count，mastery_level 语义置信度 → 正确率累计）
-- 位于学习域数据库 ai_edu_learning
-- 加列非删列，回滚无损；历史行 train_count=1 平滑过渡（旧 mastery_level 作初始正确率，首次 applyScore 起累计）
-- source 默认 'ai'（历史数据全为 AI 答疑产出）；mastery_level 语义注释随迁移说明更新（旧四档值即初始正确率）

ALTER TABLE `t_student_topic_mastery`
    ADD COLUMN `source`      VARCHAR(10) NOT NULL DEFAULT 'ai' COMMENT '来源：ai（AI 答疑）/ bank（题库，预留）' AFTER `last_session_id`,
    ADD COLUMN `train_count` INT         NOT NULL DEFAULT 1 COMMENT '训练数（累计作答次数；历史行=1，旧 mastery_level 作初始正确率平滑过渡）' AFTER `source`;

ALTER TABLE `t_student_topic_mastery`
    MODIFY COLUMN `mastery_level` INT NOT NULL DEFAULT 0 COMMENT '掌握度 0-100（连续百分比=累计平均正确率；旧四档值作初始正确率）';