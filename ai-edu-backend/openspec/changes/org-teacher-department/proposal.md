## Why

当前学校组织管理只有学校(School)和班级(Class)实体，缺少部门(Department)这一中间层组织结构。实际学校运作中，教师归属于各个行政部门（如教务处、学生处、总务处），需要部门管理来支持：
- 教师组织架构管理
- 按部门分配任务/权限
- 按部门统计数据

## What Changes

### 新增功能（本期）
- 部门管理：创建、更新、删除、查询部门
- 部门树形结构：支持多级部门（parent_id + department_path）
- 部门仅用于行政组织归属（无类型字段）

### 后续补充（依赖用户域）
- 教师-部门关联：多对多关系管理
- 部门成员管理：添加/移除教师

### 暂不实现
- 部门负责人
- 部门成员角色（组长等）
- 部门状态（启用/停用）
- 年级组、学科组（放入任职任课模块）

## Capabilities

### New Capabilities
- `department-management`: 部门 CRUD，树形结构（行政部门）

### Modified Capabilities
- 无（新增模块，不修改现有能力）

### Pending Capabilities（依赖用户域）
- `teacher-department-association`: 教师-部门多对多关联管理（需先完成用户创建接口）

## Impact

### 数据库变更（本期）
- 新增 `t_department` 表（仅行政部门）

### 数据库变更（后续）
- 新增 `t_teacher_department` 关联表

### 代码变更（本期）
- Domain 层：新增 Department 实体、值对象
- Application 层：新增 DepartmentAppService
- Infrastructure 层：新增 DepartmentRepositoryImpl、Mapper、PO
- Interface 层：新增 DepartmentController

### 受影响模块
- 组织域