-- =====================================================
-- AI教育平台 - 作业域 (Homework Context)
-- 表结构设计文档
-- =====================================================

USE ai_edu;

-- =====================================================
-- 1. 作业定义表 (老师发布的作业)
-- =====================================================
CREATE TABLE IF NOT EXISTS t_homework_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作业定义ID',
    title VARCHAR(200) NOT NULL COMMENT '作业标题',
    description TEXT COMMENT '作业描述',
    teacher_id BIGINT NOT NULL COMMENT '发布老师ID',
    class_id BIGINT COMMENT '目标班级ID',
    subject VARCHAR(50) COMMENT '学科',
    homework_type VARCHAR(50) DEFAULT 'NORMAL' COMMENT '作业类型: NORMAL/EXAM/PRACTICE',
    start_time TIMESTAMP COMMENT '开始时间',
    end_time TIMESTAMP COMMENT '截止时间',
    duration_minutes INT COMMENT '时长限制(分钟)',
    total_score INT DEFAULT 100 COMMENT '总分',
    allow_late_submit BOOLEAN DEFAULT TRUE COMMENT '是否允许迟交',
    allow_resubmit BOOLEAN DEFAULT FALSE COMMENT '是否允许重交',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/CLOSED',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_teacher (teacher_id),
    INDEX idx_class (class_id),
    INDEX idx_status (status),
    INDEX idx_subject (subject),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业定义表';

-- =====================================================
-- 2. 作业题目关联表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_homework_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    homework_definition_id BIGINT NOT NULL COMMENT '作业定义ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    score INT NOT NULL DEFAULT 10 COMMENT '该题分值',
    sort_order INT DEFAULT 0 COMMENT '题目顺序',
    is_required BOOLEAN DEFAULT TRUE COMMENT '是否必答',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_homework_question (homework_definition_id, question_id),
    INDEX idx_question (question_id),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_hq_homework FOREIGN KEY (homework_definition_id) REFERENCES t_homework_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_hq_question FOREIGN KEY (question_id) REFERENCES t_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业题目关联表';

-- =====================================================
-- 3. 作业提交表 (学生提交的作业，聚合根)
-- =====================================================
CREATE TABLE IF NOT EXISTS t_homework_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提交ID',
    homework_definition_id BIGINT NOT NULL COMMENT '作业定义ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    submit_time TIMESTAMP COMMENT '提交时间',
    submit_type VARCHAR(20) DEFAULT 'NORMAL' COMMENT '提交类型: NORMAL/LATE/RESUBMIT',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    total_score INT COMMENT '总分',
    ai_score INT COMMENT 'AI评分',
    teacher_score INT COMMENT '老师评分',
    final_score INT COMMENT '最终得分',
    feedback TEXT COMMENT '总体反馈',
    graded_by BIGINT COMMENT '批改老师ID',
    graded_at TIMESTAMP COMMENT '批改时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_student_homework (student_id, homework_definition_id),
    INDEX idx_student (student_id),
    INDEX idx_status (status),
    INDEX idx_homework_def (homework_definition_id),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_hs_homework FOREIGN KEY (homework_definition_id) REFERENCES t_homework_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_hs_student FOREIGN KEY (student_id) REFERENCES t_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_hs_grader FOREIGN KEY (graded_by) REFERENCES t_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';

-- =====================================================
-- 4. 作业答题详情表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_homework_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '答题ID',
    submission_id BIGINT NOT NULL COMMENT '提交ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    student_answer TEXT COMMENT '学生答案',
    answer_images JSON COMMENT '答题图片URL列表(JSON数组)',
    is_correct BOOLEAN COMMENT '是否正确',
    ai_score INT COMMENT 'AI得分',
    teacher_score INT COMMENT '老师评分',
    final_score INT COMMENT '最终得分',
    ai_feedback TEXT COMMENT 'AI反馈',
    teacher_feedback TEXT COMMENT '老师反馈',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_submission_question (submission_id, question_id),
    INDEX idx_question (question_id),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_ha_submission FOREIGN KEY (submission_id) REFERENCES t_homework_submission(id) ON DELETE CASCADE,
    CONSTRAINT fk_ha_question FOREIGN KEY (question_id) REFERENCES t_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业答题详情表';

-- =====================================================
-- 5. 作业模板表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_homework_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    name VARCHAR(200) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    subject VARCHAR(50) COMMENT '学科',
    teacher_id BIGINT NOT NULL COMMENT '创建老师ID',
    is_public BOOLEAN DEFAULT FALSE COMMENT '是否公开',
    use_count INT DEFAULT 0 COMMENT '使用次数',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_teacher (teacher_id),
    INDEX idx_subject (subject),
    INDEX idx_public (is_public),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_template_teacher FOREIGN KEY (teacher_id) REFERENCES t_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业模板表';

-- =====================================================
-- ER图关系说明
-- =====================================================
-- t_homework_definition (1) <-----> (N) t_homework_question
-- t_homework_definition (1) <-----> (N) t_homework_submission
-- t_homework_submission (1) <-----> (N) t_homework_answer
-- t_question (1) <-----> (N) t_homework_question
-- t_question (1) <-----> (N) t_homework_answer
-- t_homework_template (1) <-----> (N) t_homework_definition (可选)
--
-- 作业生命周期:
-- 1. 老师创建作业定义 -> DRAFT
-- 2. 发布作业 -> PUBLISHED
-- 3. 学生提交作业 -> 创建 t_homework_submission
-- 4. AI异步批改 -> PROCESSING -> COMPLETED/FAILED
-- 5. 老师复核/修改分数
-- 6. 作业截止 -> CLOSED
--
-- 作业类型说明:
-- | homework_type | 说明       |
-- |---------------|------------|
-- | NORMAL        | 普通作业   |
-- | EXAM          | 考试       |
-- | PRACTICE      | 练习       |