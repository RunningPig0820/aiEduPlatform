-- 教职工 (school_id, user_id) 唯一索引调整为普通索引
-- 背景：V4 建表时用 UNIQUE INDEX idx_school_user (school_id, user_id) 保证"一个用户在一个学校只能有一条教职工记录"。
-- 但 t_org_teacher 采用逻辑删除（is_deleted=1），软删除后重新添加同一教职工时：
--   软删除行仍占用唯一键 → INSERT 撞唯一约束 → DuplicateKeyException → HTTP 500。
-- 唯一性改由应用层保证：OrgTeacherRepository.findBySchoolIdAndUserId 查询时已过滤 is_deleted=false，
--   新增/修改教职工前据此校验，业务上仍保证"同一用户在同一学校至多一条未删除记录"。
-- 与测试库 schema.sql 的 idx_org_teacher_school_user（普通索引）保持一致。

ALTER TABLE t_org_teacher DROP INDEX idx_school_user;
ALTER TABLE t_org_teacher ADD INDEX idx_org_teacher_school_user (school_id, user_id) COMMENT '教职工所属学校索引（唯一性由应用层保证）';
