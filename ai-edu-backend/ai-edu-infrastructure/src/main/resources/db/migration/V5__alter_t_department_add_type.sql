-- 部门表新增 department_type 字段
-- 用于区分普通组织部门和行政班节点
ALTER TABLE `t_department`
    ADD COLUMN `department_type` VARCHAR(20) NOT NULL DEFAULT 'ORG'
    COMMENT '部门类型：ORG-普通组织部门，ADMIN_CLASS-行政班节点'
    AFTER `department_path`;
