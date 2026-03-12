-- =====================================================
-- AI教育平台 - 系统域 (System Context)
-- 表结构设计文档
-- =====================================================

USE ai_edu;

-- =====================================================
-- 1. 通知消息表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    notification_type VARCHAR(50) NOT NULL COMMENT '通知类型: HOMEWORK/SYSTEM/REMINDER/EMOTION_ALERT',
    reference_type VARCHAR(50) COMMENT '关联类型',
    reference_id BIGINT COMMENT '关联ID',
    priority INT DEFAULT 5 COMMENT '优先级: 1-10',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    read_at TIMESTAMP COMMENT '阅读时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_user (user_id),
    INDEX idx_type (notification_type),
    INDEX idx_read (is_read),
    INDEX idx_created (created_at),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';

-- =====================================================
-- 2. 系统配置表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(50) DEFAULT 'STRING' COMMENT '配置类型: STRING/NUMBER/BOOLEAN/JSON',
    description VARCHAR(500) COMMENT '配置描述',
    is_public BOOLEAN DEFAULT FALSE COMMENT '是否公开',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_config_key (config_key),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- =====================================================
-- 3. 操作日志表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '操作用户名',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_desc VARCHAR(500) COMMENT '操作描述',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    request_method VARCHAR(10) COMMENT '请求方法: GET/POST/PUT/DELETE',
    request_url VARCHAR(500) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_code INT COMMENT '响应码',
    response_time_ms INT COMMENT '响应时间(毫秒)',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_user (user_id),
    INDEX idx_operation (operation_type),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created (created_at),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- =====================================================
-- 4. 字典类型表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_dict_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典类型ID',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典类型名称',
    description VARCHAR(500) COMMENT '描述',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_dict_type (dict_type),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- =====================================================
-- 5. 字典数据表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_dict_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典数据ID',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    description VARCHAR(500) COMMENT '描述',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_dict_type (dict_type),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_dd_type FOREIGN KEY (dict_type) REFERENCES t_dict_type(dict_type) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- =====================================================
-- 6. 文件资源表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_file_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    file_name VARCHAR(200) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(200) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_url VARCHAR(500) COMMENT '访问URL',
    file_type VARCHAR(50) COMMENT '文件类型: IMAGE/DOCUMENT/VIDEO/AUDIO',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    file_size BIGINT COMMENT '文件大小(字节)',
    bucket_name VARCHAR(100) COMMENT '存储桶名称',
    owner_id BIGINT COMMENT '所属用户ID',
    business_type VARCHAR(50) COMMENT '业务类型',
    business_id BIGINT COMMENT '业务ID',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    INDEX idx_owner (owner_id),
    INDEX idx_type (file_type),
    INDEX idx_business (business_type, business_id),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_file_owner FOREIGN KEY (owner_id) REFERENCES t_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资源表';

-- =====================================================
-- 7. 定时任务表
-- =====================================================
CREATE TABLE IF NOT EXISTS t_scheduled_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_group VARCHAR(50) DEFAULT 'DEFAULT' COMMENT '任务组',
    bean_name VARCHAR(100) NOT NULL COMMENT 'Bean名称',
    method_name VARCHAR(100) NOT NULL COMMENT '方法名称',
    params VARCHAR(500) COMMENT '参数',
    cron_expression VARCHAR(100) COMMENT 'Cron表达式',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/PAUSED',
    description VARCHAR(500) COMMENT '描述',
    last_execute_time TIMESTAMP COMMENT '上次执行时间',
    next_execute_time TIMESTAMP COMMENT '下次执行时间',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    modified_by BIGINT NOT NULL DEFAULT 0 COMMENT '修改人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    UNIQUE KEY uk_task_name (task_name, task_group),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务表';

-- =====================================================
-- ER图关系说明
-- =====================================================
-- t_notification (N) <-----> (1) t_user
-- t_dict_type (1) <-----> (N) t_dict_data
-- t_file_resource (N) <-----> (1) t_user
--
-- 通知类型说明:
-- | notification_type | 说明           | 触发场景                         |
-- |-------------------|----------------|----------------------------------|
-- | HOMEWORK          | 作业通知       | 老师发布作业、批改完成           |
-- | SYSTEM            | 系统通知       | 系统升级、维护公告               |
-- | REMINDER          | 提醒通知       | 作业即将截止、学习计划提醒       |
-- | EMOTION_ALERT     | 情绪预警       | 学生出现负面情绪，通知家长/老师  |
--
-- 推送方式:
-- 1. WebSocket 实时推送给在线用户
-- 2. 存入数据库供离线用户查看
-- 3. 可选: 接入短信/邮件通知
--
-- 文件类型说明:
-- | file_type | 说明     |
-- |-----------|----------|
-- | IMAGE     | 图片     |
-- | DOCUMENT  | 文档     |
-- | VIDEO     | 视频     |
-- | AUDIO     | 音频     |