# parent-profile-management Specification

## Purpose
家长档案管理能力：提供 `t_parent_profile` 表存储学生-家长的关联关系（关系类型、主联系人等），在用户域提供 `ParentProfile` 实体与 `ParentProfileRepository` 仓储接口及 MyBatis-Plus 实现，支持批量保存、按学生/按家长查询绑定关系。

## Requirements

### Requirement: 家长信息扩展表

系统 SHALL 提供 `t_parent_profile` 表，存储学生-家长的关联关系，包括关系类型、是否主联系人等信息。

#### Scenario: 创建学生-家长关联
- **WHEN** 学生添加时传入家长列表
- **AND** 家长用户已创建或已存在
- **THEN** 系统在 `t_parent_profile` 表插入记录，包含 student_user_id、parent_user_id、relationship

#### Scenario: 同一学生-家长对唯一
- **WHEN** 尝试为同一学生和同一家长重复创建关联
- **THEN** 系统通过 UNIQUE INDEX (student_user_id, parent_user_id) 阻止重复

#### Scenario: 按学生查询绑定的家长
- **WHEN** 查询某学生的家长信息
- **THEN** 系统返回该学生在 `t_parent_profile` 中的所有关联记录，每项包含家长信息（姓名、手机号）和关系类型

#### Scenario: 按家长查询绑定的学生
- **WHEN** 查询某家长绑定的所有学生
- **THEN** 系统返回该家长在 `t_parent_profile` 中的所有关联记录

### Requirement: ParentProfile 实体与仓储

系统 SHALL 在用户域提供 `ParentProfile` 实体、`ParentProfileRepository` 仓储接口及其 MyBatis-Plus 实现。

#### Scenario: 批量保存家长关联
- **WHEN** 调用 `ParentProfileRepository.saveAll(profiles)`
- **THEN** 系统批量插入多条家长关联记录

#### Scenario: 按学生用户ID查询家长关联
- **WHEN** 调用 `ParentProfileRepository.findByStudentUserId(studentUserId)`
- **THEN** 系统返回该学生所有绑定的家长关联记录
