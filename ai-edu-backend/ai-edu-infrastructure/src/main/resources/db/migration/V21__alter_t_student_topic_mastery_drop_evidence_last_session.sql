-- V21: 掌握表（t_student_topic_mastery）清理——删除 evidence + last_session_id。
-- 「记录最后一个对话」语义冗余：事实源 t_student_question_record 已含 session_id/content/score，
-- 掌握表只存聚合结果（mastery_level/source/train_count）。两列无接口消费
-- （getMastery 不暴露「最后会话」，「查看题目」走题目表 session_id 原题链接）。
-- Flyway 关闭，需在 ai_edu_learning 库手动执行。
ALTER TABLE `t_student_topic_mastery`
    DROP COLUMN `evidence`,
    DROP COLUMN `last_session_id`;
