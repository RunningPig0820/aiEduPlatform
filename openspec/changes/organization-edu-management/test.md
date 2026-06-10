# 行政班管理 测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证 `AdminClassController` 的所有业务场景，包括 Domain 层值对象/实体/聚合根、Application 层服务、Infrastructure 层持久化和 Interface 层 API 的正确性和健壮性。

### 1.2 测试方式
- **单元测试**：Domain 层值对象、实体、聚合根
- **集成测试**：Application 层服务 + Infrastructure 层持久化
- **接口测试**：Interface 层 Controller API

### 1.3 测试环境配置
- Profile: `test`
- 数据库：H2 内存数据库或开发数据库（事务回滚）
- Session：使用 `MockHttpSession` 模拟

---

## 2. 测试数据

| 参数 | 值 | 说明 |
|-----|-----|-----|
| TEST_SCHOOL_ID | 1 | 测试学校 ID |
| TEST_DEPT_ID_STAGE | 100 | 测试学段节点 ID |
| TEST_DEPT_ID_GRADE | 101 | 测试年级节点 ID |
| TEST_DEPT_ID_CLASS | 102 | 测试班级节点 ID |
| TEST_STAGE_CODE | PRIMARY | 测试学段编码 |
| TEST_STAGE_YEAR | 4 | 测试年制（小学六年制） |
| TEST_GRADE_CODE | 1 | 测试年级编码 |
| TEST_ENROLLMENT_YEAR | 2024 | 测试入学年份 |

---

## 3. 测试用例清单

### 3.1 Domain Layer - Value Objects

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| DOM-001 | DepartmentType 创建 ORG | 无 | `DepartmentType.of("ORG")` | 返回 ORG 类型实例 |
| DOM-002 | DepartmentType 创建 ADMIN_CLASS | 无 | `DepartmentType.of("ADMIN_CLASS")` | 返回 ADMIN_CLASS 类型实例 |
| DOM-003 | DepartmentType 无效值 | 无 | `DepartmentType.of("INVALID")` | 抛出 IllegalArgumentException |
| DOM-004 | DeptEduType 创建 学段(3) | 无 | `DeptEduType.of(3)` | 返回 STAGE 类型，isStage()=true |
| DOM-005 | DeptEduType 创建 年级(4) | 无 | `DeptEduType.of(4)` | 返回 GRADE 类型，isGrade()=true |
| DOM-006 | DeptEduType 创建 班级(5) | 无 | `DeptEduType.of(5)` | 返回 CLASS 类型，isClass()=true |
| DOM-007 | DeptEduType 无效值(0) | 无 | `DeptEduType.of(0)` | 抛出 IllegalArgumentException |
| DOM-008 | StageYearCode 创建 小学六年制(4) | 无 | `StageYearCode.of("4")` | 返回实例，getYearCount()=6 |
| DOM-009 | StageYearCode 创建 初中三年制(5) | 无 | `StageYearCode.of("5")` | 返回实例，getYearCount()=3 |
| DOM-010 | StageYearCode 创建 高中三年制(7) | 无 | `StageYearCode.of("7")` | 返回实例，getYearCount()=3 |
| DOM-011 | StageYearCode 无效值 | 无 | `StageYearCode.of("99")` | 抛出 IllegalArgumentException |

### 3.2 Domain Layer - Entity

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| ENT-001 | DepartmentEdu 创建学段节点 | 无 | deptId, schoolId, deptType=3, stageCode, stageYearCode | gradeCode 和 enrollmentYear 为空 |
| ENT-002 | DepartmentEdu 创建年级节点 | 无 | deptId, schoolId, deptType=4, stageCode, stageYearCode, gradeCode, enrollmentYear | 所有教育字段填充 |
| ENT-003 | DepartmentEdu 创建班级节点 | 无 | deptId, schoolId, deptType=5, stageCode, stageYearCode, gradeCode, enrollmentYear | 所有教育字段填充 |
| ENT-004 | Department 默认 departmentType | 无 | 新建 Department | departmentType = ORG |
| ENT-005 | Department 软删除 | 已保存的 Department | department.delete() | isDeleted = true |
| ENT-006 | DepartmentEdu 级联软删除 | 已保存的 DepartmentEdu | edu.delete() | isDeleted = true |

### 3.3 Domain Layer - Aggregate

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| AGG-001 | AdminClassAggregate 创建学段节点 | 无 | name, schoolId, stageCode, stageYearCode | Department 和 DepartmentEdu 正确初始化 |
| AGG-002 | AdminClassAggregate 创建年级子节点 | 存在学段父节点 | name, schoolId, parentDept, gradeCode, enrollmentYear | parentId 正确设置，departmentPath 计算 |
| AGG-003 | AdminClassAggregate 创建班级子节点 | 存在年级父节点 | name, schoolId, parentDept, gradeCode, enrollmentYear | deptType=5，继承父级 stageCode |
| AGG-004 | 设置自身为父节点 | 已存在的聚合 | setParent(self) | 抛出 IllegalArgumentException |

### 3.4 Infrastructure Layer - Persistence

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| PER-001 | DepartmentEdu 保存和查询 | 无 | save(DepartmentEdu) → findByDeptId(deptId) | 返回保存的实体 |
| PER-002 | DepartmentEdu 按学校查询 | 已保存多条记录 | findBySchoolId(schoolId) | 返回该学校所有记录 |
| PER-003 | DepartmentEdu 删除 | 已保存记录 | deleteByDeptId(deptId) | is_deleted = 1 |
| PER-004 | Department 保存带 departmentType | 无 | save(Department with ADMIN_CLASS) | 数据库中 department_type = ADMIN_CLASS |
| PER-005 | Department 查询过滤类型 | 已保存 ORG 和 ADMIN_CLASS | findBySchoolIdAndType(schoolId, ADMIN_CLASS) | 仅返回 ADMIN_CLASS 类型 |

### 3.5 Application Layer - AdminClassAppService

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| SVC-001 | 创建学段节点 | 无 | CreateAdminClassNodeCommand(deptType=3) | Department + DepartmentEdu 同时创建 |
| SVC-002 | 创建年级节点 | 学段节点已存在 | CreateAdminClassNodeCommand(deptType=4, parentId) | 年级挂在学段下，departmentPath 正确 |
| SVC-003 | 创建班级节点 | 年级节点已存在 | CreateAdminClassNodeCommand(deptType=5, parentId) | 班级挂在年级下 |
| SVC-004 | 事务回滚：DepartmentEdu 创建失败 | 模拟 edu 保存异常 | CreateAdminClassNodeCommand | Department 也不保存，数据一致性 |
| SVC-005 | 查询学校行政班树 | 已创建完整三层结构 | getNodeTree(schoolId) | 返回三层嵌套 children 结构 |
| SVC-006 | 更新节点信息 | 节点已存在 | UpdateAdminClassNodeCommand | name、gradeCode 等字段更新 |
| SVC-007 | 删除叶子节点 | 班级节点无子节点 | deleteNode(deptId) | Department 和 DepartmentEdu 同时软删除 |
| SVC-008 | 删除非叶子节点 | 年级节点有子班级 | deleteNode(gradeDeptId) | 抛出异常："存在子节点" |

### 3.6 Interface Layer - API

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| API-001 | POST /api/admin-classes 创建学段 | 无 | {name:"小学部", deptType:3, stageCode:"PRIMARY"} | code=00000, 返回完整节点 |
| API-002 | POST /api/admin-classes 创建年级 | 学段存在 | {name:"2024级", deptType:4, parentId:100, gradeCode:"1"} | code=00000, children 包含新年级 |
| API-003 | POST /api/admin-classes 创建班级 | 年级存在 | {name:"1班", deptType:5, parentId:101} | code=00000 |
| API-004 | POST /api/admin-classes 参数缺失 | 无 | {name:"小学部"} (缺 deptType) | code=10003 |
| API-005 | POST /api/admin-classes 无效 deptType | 无 | {name:"x", deptType:99} | code=10003 |
| API-006 | GET /api/admin-classes?schoolId=1 | 已创建行政班树 | schoolId=1 | code=00000, data 为树形数组 |
| API-007 | GET /api/admin-classes?schoolId=1 (空学校) | 学校无行政班 | schoolId=999 | code=00000, data 为空数组 |
| API-008 | GET /api/admin-classes 缺少 schoolId | 无 | 不带 schoolId 参数 | code=10003 |
| API-009 | GET /api/admin-classes/100 | 节点存在 | deptId=100 | code=00000, 返回完整详情 |
| API-010 | GET /api/admin-classes/999 | 节点不存在 | deptId=999 | code=10002 |
| API-011 | PUT /api/admin-classes/100 | 节点存在 | {name:"新名称"} | code=00000, name 已更新 |
| API-012 | PUT /api/admin-classes/999 | 节点不存在 | {name:"x"} | code=10002 |
| API-013 | DELETE /api/admin-classes/102 | 节点无子节点 | deptId=102 | code=00000 |
| API-014 | DELETE /api/admin-classes/100 | 节点有子节点 | deptId=100 | code=20003 |
| API-015 | DELETE /api/admin-classes/999 | 节点不存在 | deptId=999 | code=10002 |
| API-016 | 未登录访问任何接口 | 无 Session | 任意接口 | code=10004 |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功 |
| 10000 | SYSTEM_ERROR | 系统内部错误 |
| 10001 | PARAM_ERROR | 请求参数格式错误 |
| 10002 | NOT_FOUND | 实体不存在 |
| 10003 | INVALID_PARAMS | 参数校验失败 |
| 10004 | UNAUTHORIZED | 未登录或 Session 过期 |
| 20001 | PARENT_NOT_FOUND | 父节点不存在 |
| 20002 | STAGE_ALREADY_EXISTS | 学段已存在 |
| 20003 | HAS_CHILDREN | 存在子节点无法删除 |
| 20004 | CREATE_ADMIN_CLASS_FAILED | 创建行政班事务失败 |

---

## 5. 测试用例统计

| 模块 | 用例数量 |
|-----|---------|
| Domain - Value Objects | 11 |
| Domain - Entity | 6 |
| Domain - Aggregate | 4 |
| Infrastructure - Persistence | 5 |
| Application - Service | 8 |
| Interface - API | 16 |
| **总计** | **50** |

---

## 6. 测试执行顺序

测试按 `@Order` 注解指定的顺序执行：

```
100-110  : Domain Value Objects 测试
200-205  : Domain Entity 测试
300-303  : Domain Aggregate 测试
400-404  : Infrastructure Persistence 测试
500-507  : Application Service 测试  
600-615  : Interface API 测试
```

---

## 7. 辅助方法

### 7.1 创建测试行政班数据
```java
private Department createTestDepartment(Long schoolId, String name, DepartmentType type, Long parentId) {
    Department dept = Department.createRoot(SchoolId.of(schoolId), name, 0, null);
    dept.setDepartmentType(type);
    if (parentId != null) {
        dept.updateParent(parentDepartment);
    }
    return departmentRepository.save(dept);
}

private DepartmentEdu createTestDepartmentEdu(Long deptId, Long schoolId, DeptEduType deptType,
                                               StageCode stageCode, StageYearCode stageYearCode,
                                               String gradeCode, String enrollmentYear) {
    DepartmentEdu edu = DepartmentEdu.create(deptId, schoolId, deptType, stageCode, stageYearCode);
    if (gradeCode != null) edu.setGradeCode(gradeCode);
    if (enrollmentYear != null) edu.setEnrollmentYear(enrollmentYear);
    return departmentEduRepository.save(edu);
}
```

### 7.2 创建登录会话
```java
private MockHttpSession createLoginSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 1L);
    session.setAttribute("username", "admin");
    return session;
}
```

### 7.3 构建三层测试树
```java
private void buildTestTree(Long schoolId) {
    Department stage = createTestDepartment(schoolId, "小学部", DepartmentType.ADMIN_CLASS, null);
    createTestDepartmentEdu(stage.getIdValue(), schoolId, DeptEduType.STAGE, 
        StageCode.of("PRIMARY"), StageYearCode.of("4"), null, null);
    
    Department grade = createTestDepartment(schoolId, "2024级", DepartmentType.ADMIN_CLASS, stage.getIdValue());
    createTestDepartmentEdu(grade.getIdValue(), schoolId, DeptEduType.GRADE,
        StageCode.of("PRIMARY"), StageYearCode.of("4"), "1", "2024");
    
    Department clazz = createTestDepartment(schoolId, "1班", DepartmentType.ADMIN_CLASS, grade.getIdValue());
    createTestDepartmentEdu(clazz.getIdValue(), schoolId, DeptEduType.CLASS,
        StageCode.of("PRIMARY"), StageYearCode.of("4"), "1", "2024");
}
```

---

## 8. 运行测试

```bash
# 运行所有组织域测试
cd ai-edu-backend && mvn test -pl ai-edu-interface -Dtest="*AdminClass*"

# 运行单个测试类
mvn test -pl ai-edu-interface -Dtest=AdminClassControllerTest

# 运行单个测试方法
mvn test -pl ai-edu-interface -Dtest=AdminClassControllerTest#testCreateStageNode

# 运行 Domain 层单元测试
mvn test -pl ai-edu-domain -Dtest="*DepartmentType*"
```
