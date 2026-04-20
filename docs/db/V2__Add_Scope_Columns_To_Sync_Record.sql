-- 同步记录表增加维度列（edition/subject/stage/grade）
-- 用于按年级拆分同步任务，支持细粒度锁和独立对账

ALTER TABLE t_kg_sync_record
    ADD COLUMN edition VARCHAR(100) DEFAULT NULL AFTER sync_type,
    ADD COLUMN subject VARCHAR(50)  DEFAULT NULL AFTER edition,
    ADD COLUMN stage VARCHAR(50)   DEFAULT NULL AFTER subject,
    ADD COLUMN grade VARCHAR(50)   DEFAULT NULL AFTER stage;

ALTER TABLE t_kg_sync_record
    ADD INDEX idx_scope (edition, subject, stage, grade);
