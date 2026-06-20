-- t_user 表新增身份证号字段（AES 加密存储）
ALTER TABLE t_user ADD COLUMN id_card VARCHAR(512) COMMENT '身份证号(AES加密存储)' AFTER email;
