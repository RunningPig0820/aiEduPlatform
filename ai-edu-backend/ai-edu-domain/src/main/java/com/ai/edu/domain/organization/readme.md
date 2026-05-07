# Organization Domain (组织域)

组织域负责管理教育平台中的组织结构，包括学校、年级、班级以及用户与组织的关联关系。

## 模块概览

```
┌─────────────────────────────────────────────────────────┐
│           Organization Domain (组织域)                    │
├─────────────────────────────────────────────────────────┤
│ 核心实体                                                  │
│   School (学校): 学校基本信息                              │
│   Grade (年级): 学校年级                                   │
│   Class (班级): 班级信息                                   │
│   StudentClass (学生班级): 学生-班级关联                    │
│   TeacherClass (教师班级): 教师-班级关联                    │
│   SchoolUserAssociation (学校用户关联): 用户-学校关联        │
│                                                          │
│ 值对象                                                    │
│   SchoolId: 学校标识                                       │
│   SchoolInstitutionalType: 学校性质 (公立/私立/培训机构)     │
│   SchoolStage: 学段 (小学/初中/高中/大学)                    │
│   SchoolUserRole: 用户在学校中的角色 (管理员/教师/学生/家长)   │
│   GradeLevel: 年级等级                                     │
│   ClassStatus: 班级状态                                    │
│                                                          │
│ 聚合根                                                    │
│   SchoolOrganizationAggregate: 学校组织聚合                 │
│   SchoolAggregate: 学校聚合                                │
│   ClassAggregate: 班级聚合                                 │
│                                                          │
│ 仓储接口                                                  │
│   SchoolRepository: 学校仓储                               │
│   SchoolUserRepository: 学校用户关联仓储                    │
│   GradeRepository: 年级仓储                                │
│   ClassRepository: 班级仓储                                │
│   StudentClassRepository: 学生班级仓储                      │
│   TeacherClassRepository: 教师班级仓储                      │
└─────────────────────────────────────────────────────────┘
```

## 学校组织架构

```
School (学校)
├── Grade (年级)
│   └── Class (班级)
│       ├── StudentClass (学生)
│       └── TeacherClass (教师)
│
└── SchoolUserAssociation (用户关联)
    ├── ADMIN (管理员)
    ├── TEACHER (教师)
    ├── STUDENT (学生)
    └── PARENT (家长)
```

## 核心业务规则

### 学校 (School)
- 学校名称必须唯一
- 学校必须至少有一个学段
- 支持多学段学校 (如九年一贯制)

### 学校用户关联 (SchoolUserAssociation)
- 用户可以关联多个学校
- 每个关联必须指定角色
- 角色包括: ADMIN, TEACHER, STUDENT, PARENT

### 班级 (Class)
- 班级必须归属于某个年级
- 班级名称在同一年级内必须唯一

## 学校权限控制

通过 `@SchoolScoped` 注解实现方法级别的学校权限校验:

```java
@SchoolScoped  // 校验用户是否关联目标学校
@GetMapping("/api/schools/{schoolId}/grades")
public ApiResponse<List<Grade>> getSchoolGrades(@PathVariable Long schoolId) {
    // 只有关联了该学校的用户才能访问
}

@SchoolScoped(requireAdmin = true)  // 要求管理员角色
@PostMapping("/api/schools/{schoolId}/grades/create")
public ApiResponse<Grade> createGrade(@PathVariable Long schoolId, @RequestBody CreateGradeCommand command) {
    // 只有学校管理员才能创建年级
}
```

### 权限校验流程

```
请求 → SchoolPermissionInterceptor
       │
       ├── 1. 从 Session 获取 userId
       ├── 2. 从 URL 提取 schoolId
       ├── 3. 查询用户在学校中的角色
       ├── 4. 校验角色是否满足要求
       └── 5. 设置 SchoolContextHolder
       │
       └ Controller 方法执行
       │
       └── afterCompletion: 清除 SchoolContextHolder
```

## 包结构

```
com.ai.edu.domain.organization/
├── model/
│   ├── entity/
│   │   ├── School.java
│   │   ├── Grade.java
│   │   ├── Class.java
│   │   ├── StudentClass.java
│   │   ├── TeacherClass.java
│   │   └── SchoolUserAssociation.java
│   ├── valueobject/
│   │   ├── SchoolInstitutionalType.java
│   │   ├── SchoolStage.java
│   │   ├── SchoolUserRole.java
│   │   ├── GradeLevel.java
│   │   ├── ClassStatus.java
│   │   ├── SchoolYear.java
│   │   └── StudentClassStatus.java
│   └── aggregate/
│       ├── SchoolOrganizationAggregate.java
│       ├── SchoolAggregate.java
│       └── ClassAggregate.java
├── repository/
│   ├── SchoolRepository.java
│   ├── SchoolUserRepository.java
│   ├── GradeRepository.java
│   ├── ClassRepository.java
│   ├── StudentClassRepository.java
│   └── TeacherClassRepository.java
```

## REST API

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/schools/create` | POST | 创建学校 |
| `/api/schools/{id}/update` | POST | 更新学校 |
| `/api/schools/{id}` | GET | 获取学校详情 |
| `/api/schools/list` | GET | 获取学校列表 (支持 type 参数筛选) |
| `/api/schools/{id}/delete` | POST | 删除学校 |
| `/api/schools/{schoolId}/users/add` | POST | 关联用户到学校 |
| `/api/users/{userId}/schools` | GET | 获取用户关联的学校 |
| `/api/schools/{schoolId}/users/{userId}/remove` | POST | 移除用户关联 |
| `/api/schools/{schoolId}/users/{userId}/check` | GET | 检查用户权限 |

## 数据库表

### school 表
```sql
CREATE TABLE school (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon_url VARCHAR(255),
    institutional_type VARCHAR(20),  -- PUBLIC, PRIVATE, TRAINING_INSTITUTE
    stages VARCHAR(100),              -- JSON 数组: ["PRIMARY", "JUNIOR_HIGH"]
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME,
    updated_at DATETIME
);
```

### school_user 表
```sql
CREATE TABLE school_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    school_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_type VARCHAR(20) NOT NULL,  -- ADMIN, TEACHER, STUDENT, PARENT
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_school_user (school_id, user_id)
);
```

## 与其他域的关系

- **User Domain**: 用户在学校中的角色通过 SchoolUserAssociation 管理
- **Homework Domain**: 作业归属于班级，班级归属于学校
- **Learning Domain**: 错题本和学习记录与班级关联

---

## 更新历史

### 2024-05
- 新增 SchoolUserAssociation 实体，支持用户-学校多对多关系
- 新增 SchoolUserRole 枚举: ADMIN, TEACHER, STUDENT, PARENT
- 实现 @SchoolScoped 注解和权限拦截器
- 创建学校管理 REST API