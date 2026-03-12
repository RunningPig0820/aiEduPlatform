-- =====================================================
-- AI教育平台 - 视图定义
-- 便于数据统计和查询
-- =====================================================

USE ai_edu;

-- =====================================================
-- 1. 学生作业概览视图
-- =====================================================
CREATE OR REPLACE VIEW v_student_homework_overview AS
SELECT
    hs.id AS submission_id,
    hs.student_id,
    u.real_name AS student_name,
    hd.id AS homework_id,
    hd.title AS homework_title,
    hd.subject,
    hd.end_time,
    hs.status,
    hs.final_score,
    hs.submit_time,
    c.name AS class_name
FROM t_homework_submission hs
JOIN t_homework_definition hd ON hs.homework_definition_id = hd.id
JOIN t_user u ON hs.student_id = u.id
LEFT JOIN t_class c ON hd.class_id = c.id
WHERE hs.is_deleted = 0 AND hd.is_deleted = 0 AND u.is_deleted = 0;

-- =====================================================
-- 2. 学生知识点掌握情况视图
-- =====================================================
CREATE OR REPLACE VIEW v_student_mastery_overview AS
SELECT
    km.student_id,
    u.real_name AS student_name,
    kp.id AS knowledge_point_id,
    kp.name AS knowledge_point_name,
    kp.subject,
    km.mastery_level,
    km.correct_count,
    km.wrong_count,
    km.total_time_minutes,
    km.last_practice_at
FROM t_knowledge_mastery km
JOIN t_user u ON km.student_id = u.id
JOIN t_knowledge_point kp ON km.knowledge_point_id = kp.id
WHERE km.is_deleted = 0 AND u.is_deleted = 0 AND kp.is_deleted = 0;

-- =====================================================
-- 3. 错题统计视图
-- =====================================================
CREATE OR REPLACE VIEW v_error_statistics AS
SELECT
    eb.student_id,
    u.real_name AS student_name,
    kp.subject,
    kp.id AS knowledge_point_id,
    kp.name AS knowledge_point_name,
    COUNT(*) AS error_count,
    SUM(CASE WHEN eb.is_corrected = FALSE THEN 1 ELSE 0 END) AS uncorrected_count,
    AVG(eb.mastery_level) AS avg_mastery_level
FROM t_error_book eb
JOIN t_user u ON eb.student_id = u.id
JOIN t_question q ON eb.question_id = q.id
LEFT JOIN t_knowledge_point kp ON q.knowledge_point_id = kp.id
WHERE eb.is_deleted = 0 AND u.is_deleted = 0 AND q.is_deleted = 0
GROUP BY eb.student_id, u.real_name, kp.subject, kp.id, kp.name;

-- =====================================================
-- 4. 班级作业完成情况视图
-- =====================================================
CREATE OR REPLACE VIEW v_class_homework_completion AS
SELECT
    hd.id AS homework_id,
    hd.title,
    hd.class_id,
    c.name AS class_name,
    hd.subject,
    COUNT(DISTINCT sc.student_id) AS total_students,
    COUNT(DISTINCT hs.student_id) AS submitted_count,
    ROUND(COUNT(DISTINCT hs.student_id) * 100.0 / NULLIF(COUNT(DISTINCT sc.student_id), 0), 2) AS completion_rate,
    AVG(hs.final_score) AS avg_score
FROM t_homework_definition hd
JOIN t_class c ON hd.class_id = c.id
JOIN t_student_class sc ON sc.class_id = c.id AND sc.status = 'ACTIVE' AND sc.is_deleted = 0
LEFT JOIN t_homework_submission hs ON hs.homework_definition_id = hd.id AND hs.student_id = sc.student_id AND hs.is_deleted = 0
WHERE hd.status = 'PUBLISHED' AND hd.is_deleted = 0 AND c.is_deleted = 0
GROUP BY hd.id, hd.title, hd.class_id, c.name, hd.subject;

-- =====================================================
-- 5. 学生学习进度视图
-- =====================================================
CREATE OR REPLACE VIEW v_student_learning_progress AS
SELECT
    lp.student_id,
    u.real_name AS student_name,
    lp.id AS plan_id,
    lp.title AS plan_title,
    lp.subject,
    lp.start_date,
    lp.end_date,
    COUNT(lt.id) AS total_tasks,
    SUM(CASE WHEN lt.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_tasks,
    ROUND(SUM(CASE WHEN lt.status = 'COMPLETED' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(lt.id), 0), 2) AS progress_rate
FROM t_learning_plan lp
JOIN t_user u ON lp.student_id = u.id
LEFT JOIN t_learning_task lt ON lt.plan_id = lp.id AND lt.is_deleted = 0
WHERE lp.status = 'ACTIVE' AND lp.is_deleted = 0 AND u.is_deleted = 0
GROUP BY lp.student_id, u.real_name, lp.id, lp.title, lp.subject, lp.start_date, lp.end_date;

-- =====================================================
-- 6. 老师班级概览视图
-- =====================================================
CREATE OR REPLACE VIEW v_teacher_class_overview AS
SELECT
    tc.teacher_id,
    u.real_name AS teacher_name,
    tc.class_id,
    c.name AS class_name,
    c.grade,
    c.school_year,
    tc.subject,
    tc.is_head_teacher,
    COUNT(DISTINCT sc.student_id) AS student_count
FROM t_teacher_class tc
JOIN t_user u ON tc.teacher_id = u.id
JOIN t_class c ON tc.class_id = c.id
LEFT JOIN t_student_class sc ON sc.class_id = c.id AND sc.status = 'ACTIVE' AND sc.is_deleted = 0
WHERE tc.status = 'ACTIVE' AND tc.is_deleted = 0 AND u.is_deleted = 0 AND c.is_deleted = 0
GROUP BY tc.teacher_id, u.real_name, tc.class_id, c.name, c.grade, c.school_year, tc.subject, tc.is_head_teacher;

-- =====================================================
-- 7. AI任务统计视图
-- =====================================================
CREATE OR REPLACE VIEW v_ai_task_statistics AS
SELECT
    DATE(created_at) AS task_date,
    task_type,
    COUNT(*) AS total_count,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
    AVG(CASE WHEN status = 'COMPLETED'
        THEN TIMESTAMPDIFF(SECOND, started_at, completed_at)
        ELSE NULL END) AS avg_duration_seconds
FROM t_ai_task
WHERE is_deleted = 0
GROUP BY DATE(created_at), task_type;

-- =====================================================
-- 8. 学习情绪趋势视图
-- =====================================================
CREATE OR REPLACE VIEW v_emotion_trend AS
SELECT
    student_id,
    DATE(created_at) AS record_date,
    emotion_state,
    COUNT(*) AS occurrence_count,
    AVG(confidence) AS avg_confidence
FROM t_emotion_record
WHERE is_deleted = 0
GROUP BY student_id, DATE(created_at), emotion_state;