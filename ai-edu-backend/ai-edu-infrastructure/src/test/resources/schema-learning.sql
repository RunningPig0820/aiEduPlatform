-- AI 答疑会话表（H2 集成测试，learning 数据源 testdb_learning）
-- 对应生产 Flyway V9__create_t_tutoring_session + V13__alter_t_tutoring_session_add_title
-- is_deleted 用 TINYINT 对齐生产（逻辑删除配置 logic-delete-value=1/0 由测试类 @TestPropertySource 注入）
CREATE TABLE IF NOT EXISTS t_tutoring_session (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id           BIGINT       NOT NULL,
    subject              VARCHAR(32)  NOT NULL DEFAULT 'math',
    title                VARCHAR(255),
    question_type        VARCHAR(32),
    question_kind        VARCHAR(32),
    intent_category      VARCHAR(16),
    last_emotion         VARCHAR(16),
    status               VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    round_count          INT          NOT NULL DEFAULT 0,
    answer_request_count INT          NOT NULL DEFAULT 0,
    end_reason           VARCHAR(32),
    transcript_url       VARCHAR(512),
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,
    archived_at          TIMESTAMP,
    created_by           BIGINT       NOT NULL DEFAULT 0,
    modified_by          BIGINT       NOT NULL DEFAULT 0,
    is_deleted           TINYINT      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tutoring_student_status ON t_tutoring_session (student_id, status);
