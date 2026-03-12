-- =====================================================
-- AI教育平台 - 初始化数据
-- =====================================================

USE ai_edu;

-- =====================================================
-- 1. 默认管理员账号
-- =====================================================
-- 密码: admin123 (BCrypt加密)
INSERT INTO t_user (username, password, real_name, role, enabled, created_by) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z9qA2Qv.C3p4Xl.j5O7QP6Sy', '系统管理员', 'ADMIN', TRUE, 0);

-- =====================================================
-- 2. 系统默认配置
-- =====================================================
INSERT INTO t_system_config (config_key, config_value, config_type, description, is_public, created_by) VALUES
('system.name', 'AI教育平台', 'STRING', '系统名称', TRUE, 0),
('system.version', '1.0.0', 'STRING', '系统版本', TRUE, 0),
('ai.scoring.threshold', '0.8', 'NUMBER', 'AI评分置信度阈值', FALSE, 0),
('ai.emotion.alert.threshold', '0.7', 'NUMBER', '情绪预警置信度阈值', FALSE, 0),
('homework.auto.close.days', '7', 'NUMBER', '作业自动关闭天数', FALSE, 0),
('notification.retention.days', '30', 'NUMBER', '通知消息保留天数', FALSE, 0),
('log.retention.days', '90', 'NUMBER', '操作日志保留天数', FALSE, 0),
('file.max.size.mb', '50', 'NUMBER', '单文件最大大小(MB)', TRUE, 0),
('homework.max.submit.count', '3', 'NUMBER', '作业最大提交次数', FALSE, 0);

-- =====================================================
-- 3. AI模型默认配置
-- =====================================================
INSERT INTO t_ai_model_config (model_name, model_type, provider, config_json, is_enabled, priority, created_by) VALUES
('qwen-turbo', 'LLM', 'QWEN', '{"temperature": 0.7, "max_tokens": 2000, "model": "qwen2.5-turbo"}', TRUE, 1, 0),
('qwen-7b-local', 'LLM', 'LOCAL', '{"temperature": 0.7, "max_tokens": 2000}', FALSE, 2, 0),
('paddle-ocr', 'OCR', 'LOCAL', '{"lang": "ch", "use_angle_cls": true}', TRUE, 1, 0),
('emotion-classifier', 'EMOTION', 'LOCAL', '{"model": "default"}', TRUE, 1, 0),
('text-embedding', 'EMBEDDING', 'QWEN', '{"model": "text-embedding-v2"}', TRUE, 1, 0);

-- =====================================================
-- 4. 字典类型初始化
-- =====================================================
INSERT INTO t_dict_type (dict_type, dict_name, description, status, created_by) VALUES
('subject', '学科', '学科类型', 'ACTIVE', 0),
('difficulty', '难度', '题目难度等级', 'ACTIVE', 0),
('question_type', '题型', '题目类型', 'ACTIVE', 0),
('homework_status', '作业状态', '作业状态流转', 'ACTIVE', 0),
('emotion_state', '情绪状态', '学习情绪状态', 'ACTIVE', 0),
('relationship', '亲属关系', '学生家长关系', 'ACTIVE', 0);

-- =====================================================
-- 5. 字典数据初始化
-- =====================================================
-- 学科
INSERT INTO t_dict_data (dict_type, dict_label, dict_value, sort_order, status, created_by) VALUES
('subject', '数学', 'MATH', 1, 'ACTIVE', 0),
('subject', '语文', 'CHINESE', 2, 'ACTIVE', 0),
('subject', '英语', 'ENGLISH', 3, 'ACTIVE', 0),
('subject', '物理', 'PHYSICS', 4, 'ACTIVE', 0),
('subject', '化学', 'CHEMISTRY', 5, 'ACTIVE', 0),
('subject', '生物', 'BIOLOGY', 6, 'ACTIVE', 0),
('subject', '历史', 'HISTORY', 7, 'ACTIVE', 0),
('subject', '地理', 'GEOGRAPHY', 8, 'ACTIVE', 0),
('subject', '政治', 'POLITICS', 9, 'ACTIVE', 0);

-- 难度
INSERT INTO t_dict_data (dict_type, dict_label, dict_value, sort_order, status, created_by) VALUES
('difficulty', '简单', 'EASY', 1, 'ACTIVE', 0),
('difficulty', '中等', 'MEDIUM', 2, 'ACTIVE', 0),
('difficulty', '困难', 'HARD', 3, 'ACTIVE', 0);

-- 题型
INSERT INTO t_dict_data (dict_type, dict_label, dict_value, sort_order, status, created_by) VALUES
('question_type', '单选题', 'SINGLE_CHOICE', 1, 'ACTIVE', 0),
('question_type', '多选题', 'MULTIPLE_CHOICE', 2, 'ACTIVE', 0),
('question_type', '填空题', 'FILL_IN_BLANK', 3, 'ACTIVE', 0),
('question_type', '简答题', 'SHORT_ANSWER', 4, 'ACTIVE', 0),
('question_type', '论述题', 'ESSAY', 5, 'ACTIVE', 0);

-- 作业状态
INSERT INTO t_dict_data (dict_type, dict_label, dict_value, sort_order, status, created_by) VALUES
('homework_status', '草稿', 'DRAFT', 1, 'ACTIVE', 0),
('homework_status', '已发布', 'PUBLISHED', 2, 'ACTIVE', 0),
('homework_status', '已关闭', 'CLOSED', 3, 'ACTIVE', 0);

-- 情绪状态
INSERT INTO t_dict_data (dict_type, dict_label, dict_value, sort_order, status, created_by) VALUES
('emotion_state', '积极正向', 'POSITIVE', 1, 'ACTIVE', 0),
('emotion_state', '中性', 'NEUTRAL', 2, 'ACTIVE', 0),
('emotion_state', '挫败', 'FRUSTRATED', 3, 'ACTIVE', 0),
('emotion_state', '困惑', 'CONFUSED', 4, 'ACTIVE', 0),
('emotion_state', '焦虑', 'ANXIOUS', 5, 'ACTIVE', 0),
('emotion_state', '厌倦', 'BORED', 6, 'ACTIVE', 0);

-- 亲属关系
INSERT INTO t_dict_data (dict_type, dict_label, dict_value, sort_order, status, created_by) VALUES
('relationship', '父亲', 'FATHER', 1, 'ACTIVE', 0),
('relationship', '母亲', 'MOTHER', 2, 'ACTIVE', 0),
('relationship', '监护人', 'GUARDIAN', 3, 'ACTIVE', 0),
('relationship', '其他', 'OTHER', 4, 'ACTIVE', 0);

-- =====================================================
-- 6. 学科知识点示例
-- =====================================================
-- 数学一级知识点
INSERT INTO t_knowledge_point (name, code, parent_id, subject, grade_level, description, sort_order, created_by) VALUES
('数与代数', 'MATH-001', NULL, 'MATH', NULL, '数学核心领域之一', 1, 0),
('图形与几何', 'MATH-002', NULL, 'MATH', NULL, '数学核心领域之一', 2, 0),
('统计与概率', 'MATH-003', NULL, 'MATH', NULL, '数学核心领域之一', 3, 0);

-- 数学二级知识点 (数与代数下属)
INSERT INTO t_knowledge_point (name, code, parent_id, subject, grade_level, description, sort_order, created_by)
SELECT '有理数', 'MATH-001-01', id, 'MATH', 'GRADE_7', '有理数及其运算', 1, 0
FROM t_knowledge_point WHERE code = 'MATH-001';

INSERT INTO t_knowledge_point (name, code, parent_id, subject, grade_level, description, sort_order, created_by)
SELECT '一元一次方程', 'MATH-001-02', id, 'MATH', 'GRADE_7', '一元一次方程的解法与应用', 2, 0
FROM t_knowledge_point WHERE code = 'MATH-001';

-- 语文一级知识点
INSERT INTO t_knowledge_point (name, code, parent_id, subject, grade_level, description, sort_order, created_by) VALUES
('阅读理解', 'CHINESE-001', NULL, 'CHINESE', NULL, '语文核心能力', 1, 0),
('写作', 'CHINESE-002', NULL, 'CHINESE', NULL, '语文核心能力', 2, 0),
('文言文', 'CHINESE-003', NULL, 'CHINESE', NULL, '语文核心能力', 3, 0);

-- 英语一级知识点
INSERT INTO t_knowledge_point (name, code, parent_id, subject, grade_level, description, sort_order, created_by) VALUES
('词汇', 'ENGLISH-001', NULL, 'ENGLISH', NULL, '英语基础', 1, 0),
('语法', 'ENGLISH-002', NULL, 'ENGLISH', NULL, '英语基础', 2, 0),
('阅读', 'ENGLISH-003', NULL, 'ENGLISH', NULL, '英语技能', 3, 0),
('写作', 'ENGLISH-004', NULL, 'ENGLISH', NULL, '英语技能', 4, 0);

-- =====================================================
-- 7. 题目标签示例
-- =====================================================
INSERT INTO t_question_tag (name, color, created_by) VALUES
('高频考点', '#FF5722', 0),
('易错题', '#E91E63', 0),
('压轴题', '#9C27B0', 0),
('基础题', '#4CAF50', 0),
('拓展题', '#2196F3', 0),
('真题', '#FF9800', 0);

-- =====================================================
-- 8. 定时任务初始化
-- =====================================================
INSERT INTO t_scheduled_task (task_name, task_group, bean_name, method_name, cron_expression, status, description, created_by) VALUES
('清理过期通知', 'CLEANUP', 'notificationCleanupTask', 'cleanExpired', '0 0 2 * * ?', 'ACTIVE', '每天凌晨2点清理过期通知', 0),
('关闭超时作业', 'HOMEWORK', 'homeworkTask', 'closeExpired', '0 0 * * * ?', 'ACTIVE', '每小时检查并关闭超时作业', 0),
('生成学习报告', 'REPORT', 'learningReportTask', 'generateWeekly', '0 0 3 ? * MON', 'ACTIVE', '每周一凌晨3点生成上周学习报告', 0),
('AI任务超时处理', 'AI', 'aiTaskTask', 'handleTimeout', '0 */5 * * * ?', 'ACTIVE', '每5分钟检查AI任务超时', 0);