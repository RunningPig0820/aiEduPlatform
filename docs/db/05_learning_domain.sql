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
-- 来源：Flyway V15__create_t_student_topic_mastery.sql + V18__alter_t_student_topic_mastery_add_source_train_count.sql
-- 注意：spring.flyway.enabled=false，需手动在 ai_edu_learning 库执行
-- 旧 t_student_kp_mastery 保留并行（无题型映射的知识点覆盖度回退旧表，过渡期）
-- V18 改造：mastery_level 语义 四档 0/25/50/75 → 连续百分比（累计平均正确率）；加 source/train_count
-- 历史行 train_count=1 平滑过渡（旧 mastery_level 作初始正确率，首次 applyScore 起累计）
-- =====================================================
CREATE TABLE IF NOT EXISTS `t_student_topic_mastery` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`      BIGINT       NOT NULL COMMENT '学生ID',
    `topic_key`       VARCHAR(255) NOT NULL COMMENT '归一化题型标识（鸡兔同笼）',
    `topic_label`     VARCHAR(255) NOT NULL COMMENT '题型展示名',
    `mastery_level`   INT          NOT NULL DEFAULT 0 COMMENT '掌握度 0-100（连续百分比=累计平均正确率；旧四档值作初始正确率）',
    `evidence`        JSON         NULL COMMENT '证据（命中步骤、错误事件 id 列表）',
    `last_session_id` BIGINT       NULL COMMENT '最近一次答疑会话ID',
    `source`          VARCHAR(10)  NOT NULL DEFAULT 'ai' COMMENT '来源：ai（AI 答疑）/ bank（题库，预留）',
    `train_count`     INT          NOT NULL DEFAULT 1 COMMENT '训练数（累计作答次数；历史行=1，旧 mastery_level 作初始正确率平滑过渡）',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`      BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`     BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_student_topic` (`student_id`, `topic_key`) COMMENT '同一学生题型唯一（UPSERT 幂等）',
    INDEX `idx_student` (`student_id`) COMMENT '按学生查题型掌握度（掌握度列表/覆盖度派生）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生题型掌握度表（mastery_level=累计平均正确率）';

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

-- =====================================================
-- 13. 学生题目记录表（掌握度事实源：每道题一条记录，可追溯）
-- 来源：Flyway V17__create_t_student_question_record.sql
-- 注意：spring.flyway.enabled=false，需手动在 ai_edu_learning 库执行
-- 题目采集全量落库（AI 答疑 source=ai / 题库 source=bank 预留）
-- topic_label 可空（V20 改：答疑 PENDING 题型未识别时 topic_label 与 canonical_label 同为 NULL，信号照常采集）
-- canonical_label 可空（题型未识别 PENDING 时为空，信号照常采集，归属后回填聚合）
-- score = 生效分值（0.0/0.5/1.0，含 per-题型打折后），与掌握表累计平均聚合同源
-- session_id = 原题链接（AI 答疑会话 ID，可跳回看原题；无会话记录为 NULL 显示题目原文）
-- 掌握表（t_student_topic_mastery）为聚合结果，本表为事实源——改折扣系数/信号映射重算聚合即可，证据不丢
-- =====================================================
CREATE TABLE IF NOT EXISTS `t_student_question_record` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`            BIGINT       NOT NULL COMMENT '学生ID',
    `content`               TEXT         NOT NULL COMMENT '题目文本（该轮题目，非最后一条用户消息）',
    `source`                VARCHAR(10)  NOT NULL DEFAULT 'ai' COMMENT '来源：ai（AI 答疑）/ bank（题库，预留）',
    `topic_label`           VARCHAR(255) NOT NULL COMMENT 'LLM 原始题型名（弱标注，可能飘）',
    `canonical_label`       VARCHAR(255) NULL COMMENT '聚集后 canonical 题型名（可空=题型未识别 PENDING）',
    `score`                 DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '生效分值 0.00/0.50/1.00（含 per-题型打折后）',
    `hint_count`            INT          NOT NULL DEFAULT 0 COMMENT '引导轮数（roundCount）',
    `answer_request_count`  INT          NOT NULL DEFAULT 0 COMMENT '要答案次数（answerRequestCount）',
    `session_id`            BIGINT       NULL COMMENT '原题链接：AI 答疑会话 ID（可跳回看原题，无则显示题目原文）',
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
    `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`            BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `modified_by`           BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人',
    `is_deleted`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_student` (`student_id`) COMMENT '按学生查题目记录（掌握度追溯/题目列表）',
    INDEX `idx_student_canonical` (`student_id`, `canonical_label`) COMMENT '按学生+canonical 查题目（掌握度页「查看题目」）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生题目记录表（掌握度事实源）';
-- =====================================================
-- V19：题目记录表 score 改为可空——analyze 贴题（无对错信号）score=null 表达「无信号」
-- 与「答错 score=0.00」区分：掌握表重算跳过 null（不拉低掌握度），SIG-007「不产生信号」语义落地
-- 来源：Flyway V19__alter_t_student_question_record_score_nullable.sql
-- 注意：spring.flyway.enabled=false，需手动在 ai_edu_learning 库执行
-- =====================================================
ALTER TABLE `t_student_question_record`
    MODIFY COLUMN `score` DECIMAL(3,2) NULL
    COMMENT '生效分值 0.00/0.50/1.00（含 per-题型打折后）；NULL=无信号（analyze 贴题，不参与掌握表聚合）';

-- =====================================================
-- V20：题目记录表 topic_label 改可空——答疑 PENDING（题型未识别）题目照常落库，信号不丢（SIG-006）
-- PENDING 语义与 canonical_label 一致（无题型名 → topic_label=NULL）
-- 来源：Flyway V20__alter_t_student_question_record_topic_label_nullable.sql
-- 注意：spring.flyway.enabled=false，需手动在 ai_edu_learning 库执行
-- =====================================================
ALTER TABLE `t_student_question_record`
    MODIFY COLUMN `topic_label` VARCHAR(255) NULL
    COMMENT 'LLM 原始题型名（弱标注，可能飘）；PENDING 题型未识别可为 NULL（与 canonical_label 同语义）';
