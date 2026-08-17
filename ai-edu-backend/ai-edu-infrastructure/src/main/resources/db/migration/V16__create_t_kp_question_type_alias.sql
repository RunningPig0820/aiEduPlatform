-- 题型库变体别名表（相似题型名收敛到 canonical 题型）
-- 位于学习域数据库 ai_edu_learning
-- alias_label 唯一（uk_alias_label），聚合时按 kp 分布重叠判定变体后插入
-- 解析管线②/投票/聚合查询经别名命中同一 canonical 条目（findByTopicLabelOrAlias）
-- canonical 名只增不改：合并只加别名、不动主题名，避免破坏既有引用
-- 业务隔离于权威图谱，只存 MySQL ai_edu_learning

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
