-- AI 答疑会话表补标题列
-- 历史会话列表展示用标题（首条用户消息内容前 N 字，或兜底值）
-- 存量会话回填为空，前端用占位「答疑会话」

ALTER TABLE `t_tutoring_session`
    ADD COLUMN `title` VARCHAR(255) NULL COMMENT '会话标题（首条用户消息前~30字生成，历史列表展示；存量可空）' AFTER `question_type`;
