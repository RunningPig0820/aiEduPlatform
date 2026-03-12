-- 演示账号数据初始化
-- 密码为 123456 的 BCrypt 加密值

INSERT INTO t_user (id, username, password, real_name, phone, email, role, enabled)
VALUES
    (1, 'student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '演示学生', '13800138001', 'student@demo.com', 'STUDENT', true),
    (2, 'teacher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '演示老师', '13800138002', 'teacher@demo.com', 'TEACHER', true),
    (3, 'parent', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '演示家长', '13800138003', 'parent@demo.com', 'PARENT', true)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    real_name = VALUES(real_name),
    phone = VALUES(phone),
    role = VALUES(role),
    enabled = VALUES(enabled);