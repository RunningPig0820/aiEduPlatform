# admin-class-student-management Specification

## Purpose
行政班学生管理能力：支持将学生添加到行政班树的班级节点（Department with DeptEduType=CLASS），学生需填写姓名、手机号、身份证号、学号并可同时绑定多个家长；身份证号 AES 加密存储、查询脱敏返回；支持按行政班班级聚合查询学生列表（含脱敏身份证与家长信息）。

## Requirements

### Requirement: 学生可以添加到行政班班级节点

系统 SHALL 支持将学生添加到行政班树的班级节点（Department with DeptEduType=CLASS），学生需要填写姓名、手机号、身份证号、学号，并可同时绑定多个家长。

#### Scenario: 成功添加学生（学生用户已存在）
- **WHEN** 用户填写学生姓名、手机号、身份证号后提交
- **AND** 用户域存在该手机号的 STUDENT 角色用户
- **THEN** 系统复用已有学生用户，创建 StudentClass 关联关系，返回成功响应

#### Scenario: 成功添加学生（学生用户不存在）
- **WHEN** 用户填写学生姓名、手机号、身份证号后提交
- **AND** 用户域不存在该手机号的用户
- **THEN** 系统在用户域创建 STUDENT 角色用户（含 AES 加密的身份证号），再创建 StudentClass 关联关系

#### Scenario: 成功添加学生并绑定家长
- **WHEN** 用户提交学生信息的同时传入家长列表（姓名、手机号、关系类型）
- **AND** 部分家长手机号未注册
- **THEN** 系统为未注册的家长自动创建 PARENT 角色用户，为所有家长创建 t_parent_profile 关联记录

#### Scenario: 学生手机号被其他角色占用
- **WHEN** 用户提交学生手机号
- **AND** 用户域已存在该手机号但角色不是 STUDENT
- **THEN** 系统拒绝操作，返回错误提示 "该手机号已被其他角色使用"

#### Scenario: 家长手机号被其他角色占用
- **WHEN** 用户提交家长手机号
- **AND** 用户域已存在该手机号但角色不是 PARENT
- **THEN** 系统拒绝操作，返回错误提示 "家长手机号已被其他角色使用"

#### Scenario: 学生已在同一行政班中
- **WHEN** 用户尝试将学生添加到已存在的行政班
- **AND** 该学生已有的 StudentClass 记录 classId 等于目标行政班 Department.id
- **THEN** 系统拒绝操作，返回错误提示 "学生已在该行政班中"

#### Scenario: 目标班级节点无效
- **WHEN** 用户提交添加请求
- **AND** deptId 对应的 Department 不存在或 `DeptEduType` 不是 CLASS
- **THEN** 系统拒绝操作，返回错误提示 "行政班节点无效"

### Requirement: 学生身份证加密存储

系统 SHALL 对学生身份证号进行 AES 加密后存储，查询时脱敏返回。加密逻辑封装在基础设施层。

#### Scenario: 添加学生时加密身份证
- **WHEN** 系统创建学生用户
- **THEN** 身份证号在 `UserDataProvider` 层经过 `EncryptUtil.encrypt()` 加密后存入 `User.idCard` 字段

#### Scenario: 查询学生时脱敏返回
- **WHEN** 系统查询学生信息返回给前端
- **THEN** 身份证号在 ACL 模型转换层经过解密 + 脱敏处理后返回（如 "110101****1234"），前端不直接拿到加密密文

### Requirement: 行政班学生信息聚合查询

系统 SHALL 支持查询行政班班级下的学生列表，聚合返回学生基本信息（含脱敏身份证）和家长信息。

#### Scenario: 按行政班班级查询学生列表
- **WHEN** 用户请求某行政班班级（deptId）的学生列表
- **THEN** 系统返回该班级下所有学生，每个学生包含：StudentClass 关联信息 + 学生 User 基本信息（姓名、手机号、脱敏身份证、学号）+ 绑定家长列表

#### Scenario: 查询结果不包含已删除学生
- **WHEN** 用户请求学生列表
- **THEN** 系统过滤 is_deleted=true 的 StudentClass 记录，只返回在读学生
