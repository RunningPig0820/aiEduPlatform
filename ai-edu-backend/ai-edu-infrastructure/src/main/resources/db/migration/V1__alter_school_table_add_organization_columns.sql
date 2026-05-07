-- V1__alter_school_table_add_organization_columns.sql
-- Add columns for school organization management: icon_url, institutional_type, stages, status, created_at, updated_at

-- Add icon_url column for school icon
ALTER TABLE t_school ADD COLUMN IF NOT EXISTS icon_url VARCHAR(500) DEFAULT NULL COMMENT '学校图标URL';

-- Add institutional_type column for school institution type (PUBLIC, PRIVATE, TRAINING_INSTITUTE)
ALTER TABLE t_school ADD COLUMN IF NOT EXISTS institutional_type VARCHAR(50) DEFAULT 'PUBLIC' COMMENT '学校性质类型：PUBLIC/PRIVATE/TRAINING_INSTITUTE';

-- Add stages column as JSON for school stages (PRIMARY, JUNIOR_HIGH, SENIOR_HIGH, UNIVERSITY)
ALTER TABLE t_school ADD COLUMN IF NOT EXISTS stages JSON DEFAULT NULL COMMENT '包含学段：JSON数组存储';

-- Add status column for school status
ALTER TABLE t_school ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '学校状态：ACTIVE/INACTIVE';

-- Add created_at timestamp
ALTER TABLE t_school ADD COLUMN IF NOT EXISTS created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- Add updated_at timestamp
ALTER TABLE t_school ADD COLUMN IF NOT EXISTS updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- Add index on name for faster lookup
CREATE INDEX IF NOT EXISTS idx_school_name ON t_school(name);

-- Add unique constraint on name (if not exists)
-- ALTER TABLE t_school ADD UNIQUE INDEX IF NOT EXISTS uniq_school_name (name);