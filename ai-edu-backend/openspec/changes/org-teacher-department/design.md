## Context

当前组织域已有 School 和 Class 实体，缺少 Department 这一中间层。教师管理需要部门概念来组织教师归属。本次新增 Department 模块，遵循现有 DDD 4层架构。

## Goals / Non-Goals

**Goals:**
- 设计部门(Department)表结构，支持树形层级
- 设计教师-部门关联(TeacherDepartment)表结构
- 遵循 DDD 架构，在组织域新增部门相关实体、值对象、仓储
- 提供部门 CRUD API 和教师-部门关联管理 API

**Non-Goals:**
- 部门负责人功能（暂不实现）
- 部门成员角色（组长等）
- 部门状态管理
- 权限控制（按部门分配权限）

## Decisions

### D1: 部门表结构设计

**表名**: `t_department`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| school_id | BIGINT | 所属学校ID |
| name | VARCHAR(100) | 部门名称 |
| parent_id | BIGINT | 上级部门ID，NULL 表示根节点 |
| department_path | VARCHAR(200) | 部门路径，格式如 `1_3_5`，根节点为空 |
| sort_order | INT | 排序序号 |
| description | VARCHAR(500) | 部门描述 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除标记 |

**说明**: 部门仅用于**行政组织归属**，年级组/学科组放入任职任课模块，因此去掉 type 字段。

**路径字段说明**:
- `department_path` 存储从根节点到当前节点的完整路径（不含当前节点ID）
- 格式：父节点ID用 `_` 分隔，如 `1_3` 表示当前节点的路径是 根节点1 → 节点3 → 当前节点
- 根节点：`department_path` 为空字符串或 NULL
- **查询子部门**: `WHERE department_path LIKE '1_3_%' OR department_path = '1_3'`
- **查询祖先部门**: 解析 department_path 获取所有祖先ID

**层级约束**:
- 部门类型仅有 **ADMIN（行政部门）**
- 行政部门内部可以有层级：如教务处 → 教务办公室
- 年级组、学科组不在此模块实现，放入**任职任课模块**

**备选方案**:
- 方案A：单表 + parent_id（树形结构）
- 方案B：闭包表（closure table）
- 方案C：路径枚举（path enumeration）

**选择**: 结合方案A + 方案C，parent_id 维护直接父子关系，department_path 支持快速路径查询，同时约束 type 一致性。

**理由**: 学校部门层级通常不超过3层，路径字段查询性能优秀，无需递归查询子树。类型约束保证三种维度独立管理。

### D2: 教师-部门关联表设计

**表名**: `t_teacher_department`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| teacher_id | BIGINT | 教师ID |
| department_id | BIGINT | 部门ID |
| created_at | DATETIME | 加入时间 |
| is_deleted | TINYINT | 逻辑删除标记 |

**约束**: (teacher_id, department_id) 唯一索引，防止重复关联

### D3: DDD 包结构

遵循现有组织域包结构：

```
com.ai.edu.domain.organization/
├── model/
│   ├── entity/
│   │   ├── Department.java
│   │   └── TeacherDepartment.java
│   ├── valueobject/
│   │   └── DepartmentId.java
│   └── repository/
│   │   ├── DepartmentRepository.java
│   │   └── TeacherDepartmentRepository.java
```

### D4: API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/schools/{schoolId}/departments/create` | 创建部门 |
| POST | `/api/auth/schools/{schoolId}/departments/{id}/update` | 更新部门 |
| GET | `/api/auth/schools/{schoolId}/departments/{id}` | 获取部门详情 |
| GET | `/api/auth/schools/{schoolId}/departments` | 获取部门树（按层级） |
| POST | `/api/auth/schools/{schoolId}/departments/{id}/delete` | 删除部门 |
| POST | `/api/auth/schools/{schoolId}/departments/{deptId}/teachers/add` | 添加教师到部门 |
| POST | `/api/auth/schools/{schoolId}/departments/{deptId}/teachers/{teacherId}/remove` | 移除教师 |
| GET | `/api/auth/schools/{schoolId}/departments/{deptId}/teachers` | 获取部门教师列表 |

## Risks / Trade-offs

### R1: 部门路径更新复杂性
- **风险**: 更新部门层级时，需同步更新该部门及所有子部门的 department_path
- **缓解**: 部门层级调整频率低，更新时批量更新子部门路径；限制层级深度 ≤5

### R2: 删除部门时教师关联数据处理
- **风险**: 删除部门时，教师-部门关联如何处理？
- **缓解**: 采用逻辑删除，关联数据也逻辑删除；物理删除前需检查是否有教师关联

### R3: 教师ID来源
- **风险**: 教师实体在用户域(User)，跨域引用需要协调
- **缓解**: 使用 TeacherId 值对象（Long 类型），不直接依赖用户域实体；用户域提供教师查询接口