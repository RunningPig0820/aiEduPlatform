-- =====================================================
-- AI教育平台 - AI处理域 (AI Context)
-- 表结构设计文档
-- =====================================================

USE ai_edu_ai;

-- =====================================================
-- 1. AI任务表 (异步任务队列)
-- =====================================================
CREATE TABLE IF NOT EXISTS t_ai_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型: OCR/SCORING/ANALYSIS/RECOMMENDATION/EMOTION',
    reference_type VARCHAR(50) NOT NULL COMMENT '关联类型: HOMEWORK/QUESTION/ERROR_BOOK/LEARNING',
    reference_id BIGINT NOT NULL COMMENT '关联ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/FAILED',
    priority INT DEFAULT 5 COMMENT '优先级: 1-10 (数字越小优先级越高)',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retry INT DEFAULT 3 COMMENT '最大重试次数',
    input_data JSON COMMENT '输入数据',
    output_data JSON COMMENT '输出数据',
    error_message TEXT COMMENT '错误信息',
    model_name VARCHAR(100) COMMENT '使用的模型名称',
    tokens_used INT COMMENT '消耗Token数',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_task_type (task_type),
    INDEX idx_reference (reference_type, reference_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority, created_at),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务表';

-- =====================================================
-- 2. AI模型配置表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_ai_model_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_type VARCHAR(50) NOT NULL COMMENT '模型类型: LLM/OCR/EMOTION/EMBEDDING',
    provider VARCHAR(50) COMMENT '提供商: OPENAI/QWEN/LOCAL',
    endpoint VARCHAR(500) COMMENT 'API端点',
    api_key_encrypted VARCHAR(500) COMMENT '加密的API密钥',
    config_json JSON COMMENT '模型配置(JSON)',
    cost_per_1k_tokens DECIMAL(10,4) COMMENT '每1k token成本',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    priority INT DEFAULT 1 COMMENT '优先级',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_model_name (model_name),
    INDEX idx_model_type (model_type),
    INDEX idx_enabled (is_enabled),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- =====================================================
-- 3. AI对话记录表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '对话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    conversation_type VARCHAR(50) NOT NULL COMMENT '对话类型: TUTORING/QA/EXPLANATION',
    title VARCHAR(200) COMMENT '对话标题',
    context JSON COMMENT '对话上下文',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/ARCHIVED',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_user (user_id),
    INDEX idx_type (conversation_type),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';

-- =====================================================
-- 4. AI对话消息表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '对话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: USER/ASSISTANT/SYSTEM',
    content TEXT NOT NULL COMMENT '消息内容',
    tokens_used INT COMMENT '消耗Token数',
    model_name VARCHAR(100) COMMENT '使用的模型',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_conversation (conversation_id),
    INDEX idx_role (role),
    INDEX idx_created (created_at),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';

-- =====================================================
-- 5. AI推荐记录表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_ai_recommendation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    recommendation_type VARCHAR(50) NOT NULL COMMENT '推荐类型: QUESTION/KNOWLEDGE/PLAN',
    target_id BIGINT COMMENT '推荐目标ID',
    target_type VARCHAR(50) COMMENT '推荐目标类型',
    reason TEXT COMMENT '推荐理由',
    score DECIMAL(5,2) COMMENT '推荐得分',
    is_accepted BOOLEAN COMMENT '是否被采纳',
    feedback VARCHAR(500) COMMENT '用户反馈',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_student (student_id),
    INDEX idx_type (recommendation_type),
    INDEX idx_accepted (is_accepted),
    INDEX idx_created (created_at),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI推荐记录表';

-- =====================================================
-- ER图关系说明
-- =====================================================
-- t_ai_task (N) <-----> (1) t_ai_model_config
-- t_ai_conversation (1) <-----> (N) t_ai_message
-- t_ai_conversation (N) <-----> (1) t_user
-- t_ai_recommendation (N) <-----> (1) t_user
--
-- AI任务处理流程:
-- 1. 任务创建:
--    - 学生提交作业 -> 创建 OCR 任务 (识别手写)
--    - OCR完成 -> 创建 SCORING 任务 (AI评分)
--    - 评分完成 -> 创建 ANALYSIS 任务 (学习分析)
--
-- 2. 任务消费 (Python AI Service):
--    - 轮询 PENDING 状态任务 (按优先级排序)
--    - 更新状态为 PROCESSING
--    - 调用对应 AI 模型处理
--    - 写入 output_data，更新状态为 COMPLETED/FAILED
--
-- 3. 回调处理 (Java Backend):
--    - 监听 MQ 或定时查询已完成任务
--    - 根据 reference_type 更新对应业务数据
--    - 推送 WebSocket 通知给用户
--
-- 任务类型说明:
-- | task_type      | 说明           | 输入                    | 输出                      |
-- |----------------|----------------|-------------------------|---------------------------|
-- | OCR            | 图片文字识别   | image_url               | recognized_text           |
-- | SCORING        | AI评分        | question, answer        | score, feedback           |
-- | ANALYSIS       | 学习分析      | student_id, records     | learning_report           |
-- | RECOMMENDATION | 智能推荐      | student_id, weak_points | recommended_questions     |
-- | EMOTION        | 情绪识别      | behavior_data           | emotion_state             |
--
-- 模型类型说明:
-- | model_type | 说明         |
-- |------------|--------------|
-- | LLM        | 大语言模型   |
-- | OCR        | 文字识别     |
-- | EMOTION    | 情绪识别     |
-- | EMBEDDING  | 向量嵌入     |