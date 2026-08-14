-- 知识点派生层三张表
-- 位于学习域数据库 ai_edu_learning
-- 权威图谱（Neo4j + kg-sync 镜像）零写入：派生层只借 kp_uri 结构，业务隔离
--
-- 1. t_kp_derived_obs    个体派生观测（学生 × 题型 → 知识点），长期尾·无限
-- 2. t_kp_question_type  聚合题型库主表（知识点的题型），精选·有限
-- 3. t_kp_question_type_kp 题型↔知识点 年级分布桶（1:N）

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
