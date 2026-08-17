-- =====================================================
-- AI教育平台 - 学习域 (Learning Context)
-- 表结构设计文档
-- =====================================================

USE ai_edu_learning;

-- =====================================================
-- 1. 错题本表 (聚合根)
-- =====================================================
CREATE TABLE IF NOT EXISTS t_error_book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '错题记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    homework_answer_id BIGINT COMMENT '来源答题ID',
    error_count INT NOT NULL DEFAULT 1 COMMENT '错误次数',
    correct_count INT DEFAULT 0 COMMENT '订正正确次数',
    is_corrected BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已订正',
    last_error_at TIMESTAMP COMMENT '最近错误时间',
    corrected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '订正时间',
    mastery_level INT DEFAULT 0 COMMENT '掌握程度: 0-100',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_student_question (student_id, question_id),
    INDEX idx_student (student_id),
    INDEX idx_question (question_id),
    INDEX idx_corrected (is_corrected),
    INDEX idx_mastery (mastery_level),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- =====================================================
-- 2. 知识点掌握程度表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_knowledge_mastery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '掌握记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    knowledge_point_id BIGINT NOT NULL COMMENT '知识点ID',
    mastery_level INT NOT NULL DEFAULT 0 COMMENT '掌握程度: 0-100',
    correct_count INT DEFAULT 0 COMMENT '正确次数',
    wrong_count INT DEFAULT 0 COMMENT '错误次数',
    total_time_minutes INT DEFAULT 0 COMMENT '总学习时长(分钟)',
    last_practice_at TIMESTAMP COMMENT '最近练习时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_student_knowledge (student_id, knowledge_point_id),
    INDEX idx_student (student_id),
    INDEX idx_knowledge (knowledge_point_id),
    INDEX idx_mastery (mastery_level),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点掌握程度表';

-- =====================================================
-- 3. 学习情绪记录表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_emotion_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '情绪记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    session_type VARCHAR(20) NOT NULL COMMENT '会话类型: HOMEWORK/PRACTICE/REVIEW',
    session_id BIGINT COMMENT '关联会话ID(如作业ID)',
    question_id BIGINT COMMENT '触发题目ID',
    emotion_state VARCHAR(20) NOT NULL COMMENT '情绪状态: POSITIVE/NEUTRAL/FRUSTRATED/CONFUSED/ANXIOUS/BORED',
    confidence DECIMAL(5,4) COMMENT '置信度: 0-1',
    trigger_context TEXT COMMENT '触发上下文',
    ai_suggestion TEXT COMMENT 'AI建议',
    is_notified BOOLEAN DEFAULT FALSE COMMENT '是否已通知家长/老师',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_student (student_id),
    INDEX idx_session (session_type, session_id),
    INDEX idx_emotion (emotion_state),
    INDEX idx_created (created_at),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习情绪记录表';

-- =====================================================
-- 4. 学习计划表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_learning_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学习计划ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    title VARCHAR(200) NOT NULL COMMENT '计划标题',
    description TEXT COMMENT '计划描述',
    subject VARCHAR(50) COMMENT '学科',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/COMPLETED/CANCELLED',
    progress INT DEFAULT 0 COMMENT '进度: 0-100',
    ai_generated BOOLEAN DEFAULT FALSE COMMENT '是否AI生成',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_student (student_id),
    INDEX idx_status (status),
    INDEX idx_date_range (start_date, end_date),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划表';

-- =====================================================
-- 5. 学习计划任务表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_learning_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    plan_id BIGINT NOT NULL COMMENT '计划ID',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description TEXT COMMENT '任务描述',
    knowledge_point_id BIGINT COMMENT '关联知识点',
    task_type VARCHAR(50) DEFAULT 'PRACTICE' COMMENT '任务类型: PRACTICE/REVIEW/ERROR_CORRECTION',
    target_count INT COMMENT '目标数量(如题目数)',
    completed_count INT DEFAULT 0 COMMENT '已完成数量',
    due_date DATE COMMENT '截止日期',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/IN_PROGRESS/COMPLETED',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_plan (plan_id),
    INDEX idx_knowledge (knowledge_point_id),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划任务表';

-- =====================================================
-- 6. 学习记录表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_learning_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学习记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    record_type VARCHAR(20) NOT NULL COMMENT '记录类型: HOMEWORK/PRACTICE/REVIEW/ERROR_CORRECTION',
    reference_id BIGINT COMMENT '关联ID',
    knowledge_point_id BIGINT COMMENT '知识点ID',
    subject VARCHAR(50) COMMENT '学科',
    duration_minutes INT COMMENT '学习时长(分钟)',
    question_count INT COMMENT '题目数量',
    correct_count INT COMMENT '正确数量',
    score INT COMMENT '得分',
    accuracy DECIMAL(5,2) COMMENT '正确率: 0-100',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_student (student_id),
    INDEX idx_type (record_type),
    INDEX idx_knowledge (knowledge_point_id),
    INDEX idx_subject (subject),
    INDEX idx_created (created_at),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';

-- =====================================================
-- 7. 学习报告表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_learning_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    report_type VARCHAR(50) NOT NULL COMMENT '报告类型: DAILY/WEEKLY/MONTHLY/SUBJECT',
    subject VARCHAR(50) COMMENT '学科',
    period_start DATE NOT NULL COMMENT '统计开始日期',
    period_end DATE NOT NULL COMMENT '统计结束日期',
    total_time_minutes INT COMMENT '总学习时长',
    total_questions INT COMMENT '总题目数',
    correct_rate DECIMAL(5,2) COMMENT '正确率',
    knowledge_summary JSON COMMENT '知识点掌握概况(JSON)',
    improvement_suggestions TEXT COMMENT '改进建议',
    ai_generated BOOLEAN DEFAULT TRUE COMMENT '是否AI生成',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_student (student_id),
    INDEX idx_type (report_type),
    INDEX idx_period (period_start, period_end),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习报告表';

-- =====================================================
-- ER图关系说明
-- =====================================================
-- t_error_book 记录学生错题，支持重复错误累计
-- t_knowledge_mastery 追踪每个知识点的掌握程度
-- t_emotion_record 记录学习过程中的情绪变化
-- t_learning_plan (1) <-----> (N) t_learning_task 学习计划与任务
-- t_learning_record 记录学习行为日志
-- t_learning_report 定期生成的学习分析报告
--
-- 学习数据流转:
-- 1. 作业批改完成 -> 更新 t_error_book (错题)
-- 2. 错题记录变化 -> 更新 t_knowledge_mastery (知识点掌握度)
-- 3. 答题过程 -> 记录 t_emotion_record (情绪识别)
-- 4. 学习行为 -> 记录 t_learning_record (学习统计)
-- 5. 定期生成 -> t_learning_report (学习报告)
--
-- 情绪状态说明:
-- | emotion_state | 说明     |
-- |---------------|----------|
-- | POSITIVE      | 积极正向 |
-- | NEUTRAL       | 中性     |
-- | FRUSTRATED    | 挫败     |
-- | CONFUSED      | 困惑     |
-- | ANXIOUS       | 焦虑     |
-- | BORED         | 厌倦     |

-- =====================================================
-- 8-10. 知识点派生层三张表（AI 答疑 · 题型↔知识点匹配点亮）
-- 来源：Flyway V14__create_kp_derived_tables.sql
-- 注意：spring.flyway.enabled=false，需手动在 ai_edu_learning 库执行
-- 权威图谱（Neo4j + kg-sync 镜像）零写入：派生层只借 kp_uri 结构，业务隔离
-- =====================================================

-- 8. 个体派生观测表（学生 × 题型 → 知识点，长期尾·无限）
CREATE TABLE IF NOT EXISTS `t_kp_derived_obs` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`       BIGINT       NOT NULL COMMENT '学生ID',
    `topic_label`      VARCHAR(255) NOT NULL COMMENT 'AI 题型/知识点原文（鸡兔同笼）',
    `kp_uri`           VARCHAR(255) NULL COMMENT '解析结果（TextbookKP URI），PENDING 时为空',
    `student_grade`    INT          NULL COMMENT '解析时学生年级（快照）',
    `confidence`       INT          NOT NULL DEFAULT 0 COMMENT '解析置信度 0-100',
    `source`           VARCHAR(32)  NOT NULL DEFAULT 'llm' COMMENT '来源：llm/mirror/catalog/curated/student_vote',
    `status`           VARCHAR(32)  NOT NULL DEFAULT 'NEW' COMMENT '状态：NEW/WEAK/RESOLVED/CONFLICTED/READJUDICATED/HUMAN_REVIEW',
    `occurrence_count` INT          NOT NULL DEFAULT 1 COMMENT '同生+同题型+同URI 累计次数（去重，非重复行）',
    `first_seen_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次记录时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`       BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`      BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_student_topic_kp` (`student_id`, `topic_label`, `kp_uri`) COMMENT '同生同题型同知识点去重（PENDING 时 kp_uri 为空，去重在应用层）',
    INDEX `idx_student` (`student_id`) COMMENT '按学生查观测',
    INDEX `idx_topic_label` (`topic_label`) COMMENT '按题型聚合'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个体派生观测表（学生×题型→知识点）';

-- 9. 聚合题型库主表（知识点的题型，精选·有限）
CREATE TABLE IF NOT EXISTS `t_kp_question_type` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `topic_label`  VARCHAR(255) NOT NULL COMMENT '题型名（UNIQUE）',
    `status`       VARCHAR(32)  NOT NULL DEFAULT 'CANDIDATE' COMMENT '状态：CANDIDATE/STABLE',
    `definition`   TEXT         NULL COMMENT 'LLM/人工 补的定义（可空）',
    `hit_students` INT          NOT NULL DEFAULT 0 COMMENT '去重学生数',
    `hit_count`    INT          NOT NULL DEFAULT 0 COMMENT '总命中次数',
    `promoted_by`  BIGINT       NULL COMMENT '首个触发学生（溯源）',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`   BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`  BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_topic_label` (`topic_label`) COMMENT '题型唯一',
    INDEX `idx_status` (`status`) COMMENT '按状态查（聚合/审核）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聚合题型库主表';

-- 10. 题型↔知识点 年级分布桶表（1:N）
CREATE TABLE IF NOT EXISTS `t_kp_question_type_kp` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `question_type_id` BIGINT       NOT NULL COMMENT '题型主表ID',
    `kp_uri`           VARCHAR(255) NOT NULL COMMENT '对应知识点 URI',
    `grade_range`      VARCHAR(16)  NULL COMMENT '该 kp 覆盖年级段（4-6）',
    `hit_students`     INT          NOT NULL DEFAULT 0 COMMENT '该分布桶去重学生数',
    `hit_count`        INT          NOT NULL DEFAULT 0 COMMENT '该分布桶命中次数',
    `ratio`            DECIMAL(5,4) NOT NULL DEFAULT 0 COMMENT '该 kp 占比（先验用）',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`       BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`      BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_type_kp` (`question_type_id`, `kp_uri`) COMMENT '同题型同知识点唯一桶',
    INDEX `idx_question_type` (`question_type_id`) COMMENT '按题型查分布',
    INDEX `idx_kp_uri` (`kp_uri`) COMMENT '按知识点反向找题型（错题/变式题消费方）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题型↔知识点 年级分布桶表';

-- =====================================================
-- 11. 学生题型掌握度表（掌握度主体翻转：题型直接观测，知识点派生）
-- 来源：Flyway V15__create_t_student_topic_mastery.sql
-- 注意：spring.flyway.enabled=false，需手动在 ai_edu_learning 库执行
-- 旧 t_student_kp_mastery 保留并行（无题型映射的知识点覆盖度回退旧表，过渡期）
-- =====================================================
CREATE TABLE IF NOT EXISTS `t_student_topic_mastery` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`      BIGINT       NOT NULL COMMENT '学生ID',
    `topic_key`       VARCHAR(255) NOT NULL COMMENT '归一化题型标识（鸡兔同笼）',
    `topic_label`     VARCHAR(255) NOT NULL COMMENT '题型展示名',
    `mastery_level`   INT          NOT NULL DEFAULT 0 COMMENT '掌握度 0-100（四档 0/25/50/75）',
    `evidence`        JSON         NULL COMMENT '证据（命中步骤、错误事件 id 列表）',
    `last_session_id` BIGINT       NULL COMMENT '最近一次答疑会话ID',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`      BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`     BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_student_topic` (`student_id`, `topic_key`) COMMENT '同一学生题型唯一（UPSERT 幂等）',
    INDEX `idx_student` (`student_id`) COMMENT '按学生查题型掌握度（掌握度列表/覆盖度派生）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生题型掌握度表';

-- =====================================================
-- 12. 题型库变体别名表（相似题型名收敛到 canonical 题型）
-- 来源：Flyway V16__create_t_kp_question_type_alias.sql
-- 注意：spring.flyway.enabled=false，需手动在 ai_edu_learning 库执行
-- alias_label 唯一；聚合时按 kp 分布重叠判定变体后插入；findByTopicLabelOrAlias 命中同一 canonical
-- =====================================================
CREATE TABLE IF NOT EXISTS `t_kp_question_type_alias` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `alias_label`      VARCHAR(255) NOT NULL COMMENT '变体题型名（鸡兔同笼），UNIQUE',
    `question_type_id` BIGINT       NOT NULL COMMENT 'canonical 题型ID（t_kp_question_type.id）',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`       BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`      BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_alias_label` (`alias_label`) COMMENT '变体题型名唯一（UPSERT 幂等）',
    INDEX `idx_question_type` (`question_type_id`) COMMENT '按 canonical 题型查别名'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题型库变体别名表';