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

-- 知识点派生层三张表（H2 集成测试，对应生产 Flyway V14__create_kp_derived_tables）
CREATE TABLE IF NOT EXISTS t_kp_derived_obs (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id       BIGINT       NOT NULL,
    topic_label      VARCHAR(255) NOT NULL,
    kp_uri           VARCHAR(255),
    student_grade    INT,
    confidence       INT          NOT NULL DEFAULT 0,
    source           VARCHAR(32)  NOT NULL DEFAULT 'llm',
    status           VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    occurrence_count INT          NOT NULL DEFAULT 1,
    first_seen_at    TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       BIGINT       NOT NULL DEFAULT 0,
    modified_by      BIGINT       NOT NULL DEFAULT 0,
    is_deleted       TINYINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_student_topic_kp UNIQUE (student_id, topic_label, kp_uri)
);

CREATE TABLE IF NOT EXISTS t_kp_question_type (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_label  VARCHAR(255) NOT NULL,
    status       VARCHAR(32)  NOT NULL DEFAULT 'CANDIDATE',
    definition   VARCHAR(1024),
    hit_students INT          NOT NULL DEFAULT 0,
    hit_count    INT          NOT NULL DEFAULT 0,
    promoted_by  BIGINT,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   BIGINT       NOT NULL DEFAULT 0,
    modified_by  BIGINT       NOT NULL DEFAULT 0,
    is_deleted   TINYINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_topic_label UNIQUE (topic_label)
);

CREATE TABLE IF NOT EXISTS t_kp_question_type_kp (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_type_id BIGINT       NOT NULL,
    kp_uri           VARCHAR(255) NOT NULL,
    grade_range      VARCHAR(16),
    hit_students     INT          NOT NULL DEFAULT 0,
    hit_count        INT          NOT NULL DEFAULT 0,
    ratio            DECIMAL(5,4) NOT NULL DEFAULT 0,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       BIGINT       NOT NULL DEFAULT 0,
    modified_by      BIGINT       NOT NULL DEFAULT 0,
    is_deleted       TINYINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_type_kp UNIQUE (question_type_id, kp_uri)
);
