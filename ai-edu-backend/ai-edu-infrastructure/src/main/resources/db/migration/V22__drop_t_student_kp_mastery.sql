-- V22: 删除旧知识点掌握表 t_student_kp_mastery——域 B 独立化后学生掌握题型
-- （掌握度主体=t_student_topic_mastery，getMastery/decide 快照均已切题型表），
-- 旧知识点掌握表无任何读写方（实体/Po/Mapper/仓储已随本版本代码删除）。
-- Flyway 关闭，需在 ai_edu_learning 库手动执行。
DROP TABLE IF EXISTS `t_student_kp_mastery`;
