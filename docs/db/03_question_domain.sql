-- =====================================================
-- AI教育平台 - 题目域 (Question Context)
-- 表结构设计文档
-- =====================================================

USE ai_edu;

-- =====================================================
-- 1. 知识点表 (树形结构)
-- =====================================================
CREATE TABLE IF NOT EXISTS t_knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识点ID',
    name VARCHAR(100) NOT NULL COMMENT '知识点名称',
    code VARCHAR(50) COMMENT '知识点编码',
    parent_id BIGINT COMMENT '父知识点ID',
    subject VARCHAR(50) NOT NULL COMMENT '学科: MATH/CHINESE/ENGLISH/PHYSICS/CHEMISTRY/BIOLOGY/HISTORY/GEOGRAPHY/POLITICS',
    grade_level VARCHAR(20) COMMENT '适用年级',
    description TEXT COMMENT '知识点描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_parent (parent_id),
    INDEX idx_subject (subject),
    INDEX idx_grade (grade_level),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_kp_parent FOREIGN KEY (parent_id) REFERENCES t_knowledge_point(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点表';

-- =====================================================
-- 2. 题目表 (聚合根)
-- =====================================================
CREATE TABLE IF NOT EXISTS t_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    content TEXT NOT NULL COMMENT '题目内容',
    question_type VARCHAR(20) NOT NULL COMMENT '题型: SINGLE_CHOICE/MULTIPLE_CHOICE/FILL_IN_BLANK/SHORT_ANSWER/ESSAY',
    difficulty VARCHAR(20) NOT NULL COMMENT '难度: EASY/MEDIUM/HARD',
    subject VARCHAR(50) NOT NULL COMMENT '学科',
    knowledge_point_id BIGINT COMMENT '关联知识点ID',
    knowledge_point_name VARCHAR(100) COMMENT '知识点名称(冗余)',
    answer TEXT COMMENT '标准答案',
    analysis TEXT COMMENT '解析',
    source VARCHAR(200) COMMENT '题目来源',
    ai_generated BOOLEAN DEFAULT FALSE COMMENT '是否AI生成',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/ARCHIVED',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_knowledge_point (knowledge_point_id),
    INDEX idx_difficulty (difficulty),
    INDEX idx_type (question_type),
    INDEX idx_subject (subject),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_question_kp FOREIGN KEY (knowledge_point_id) REFERENCES t_knowledge_point(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- =====================================================
-- 3. 题目选项表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_question_option (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '选项ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    option_label VARCHAR(10) NOT NULL COMMENT '选项标签: A/B/C/D/E/F',
    option_content VARCHAR(1000) NOT NULL COMMENT '选项内容',
    is_correct BOOLEAN DEFAULT FALSE COMMENT '是否正确答案',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES t_question(id) ON DELETE CASCADE,
    UNIQUE KEY uk_question_label (question_id, option_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项表';

-- =====================================================
-- 4. 题目标签表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_question_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    color VARCHAR(20) COMMENT '标签颜色',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_name (name),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目标签表';

-- =====================================================
-- 5. 题目-标签关联表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_question_tag_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_question_tag (question_id, tag_id),
    INDEX idx_tag (tag_id),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_qtr_question FOREIGN KEY (question_id) REFERENCES t_question(id) ON DELETE CASCADE,
    CONSTRAINT fk_qtr_tag FOREIGN KEY (tag_id) REFERENCES t_question_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目-标签关联表';

-- =====================================================
-- 6. 题目附件表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_question_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    attachment_type VARCHAR(20) NOT NULL COMMENT '附件类型: IMAGE/AUDIO/VIDEO',
    file_name VARCHAR(200) COMMENT '文件名',
    file_url VARCHAR(500) NOT NULL COMMENT '文件URL',
    file_size BIGINT COMMENT '文件大小(字节)',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_question (question_id),
    INDEX idx_type (attachment_type),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_att_question FOREIGN KEY (question_id) REFERENCES t_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目附件表';

-- =====================================================
-- ER图关系说明
-- =====================================================
-- t_knowledge_point (1) <-----> (N) t_knowledge_point (自关联树形结构)
-- t_knowledge_point (1) <-----> (N) t_question
-- t_question (1) <-----> (N) t_question_option
-- t_question (1) <-----> (N) t_question_attachment
-- t_question (M) <-----> (N) t_question_tag (通过 t_question_tag_relation)
--
-- 题型说明:
-- | question_type       | 说明         |
-- |---------------------|--------------|
-- | SINGLE_CHOICE       | 单选题       |
-- | MULTIPLE_CHOICE     | 多选题       |
-- | FILL_IN_BLANK       | 填空题       |
-- | SHORT_ANSWER        | 简答题       |
-- | ESSAY               | 论述题       |
--
-- 难度说明:
-- | difficulty | 说明   |
-- |------------|--------|
-- | EASY       | 简单   |
-- | MEDIUM     | 中等   |
-- | HARD       | 困难   |