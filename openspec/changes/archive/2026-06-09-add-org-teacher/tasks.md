## 1. 数据库迁移

- [x] 1.1 创建 Flyway 迁移脚本 V4__create_t_org_teacher_table.sql
- [x] 1.2 定义表结构：id, school_id, user_id, department_id, created_by, modified_by 等字段
- [x] 1.3 添加学校+用户唯一索引（idx_school_user）、部门索引（idx_department）

## 2. 领域层 - 实体与值对象

- [x] 2.1 创建 OrgTeacherId 值对象
- [x] 2.2 创建 OrgTeacher 实体（只包含 userId、departmentId、schoolId、createdBy、modifiedBy 等字段）
- [x] 2.3 实现 OrgTeacher 工厂方法（create、fromPO）
- [x] 2.4 实现 OrgTeacher 更新方法（updateDepartment）
- [x] 2.5 创建 OrgTeacherQueryParam 值对象

## 3. 领域层 - 仓储接口

- [x] 3.1 创建 OrgTeacherRepository 接口
- [x] 3.2 定义 save、findById、findBySchoolId、findByDepartmentId、queryPage、deleteById 方法
- [x] 3.3 定义 findBySchoolIdAndUserId 方法（用于唯一性校验）

## 4. 基础设施层 - 持久化

- [x] 4.1 创建 OrgTeacherPO 持久化对象
- [x] 4.2 创建 OrgTeacherMapper（MyBatis-Plus）
- [x] 4.3 实现 OrgTeacherRepositoryImpl（Entity 与 PO 转换）
- [x] 4.4 添加 @DS("org") 数据源注解

## 5. 跨域集成 - 用户域接口

- [x] 5.1 创建 UserQueryService 接口（防腐层）
- [x] 5.2 定义 findByPhone、createUser、findByIds 方法
- [x] 5.3 创建 UserInfo DTO（接收用户域返回的基本信息）
- [x] 5.4 实现用户域 Feign 客户端（或 Mock 实现）

## 6. 应用层 - DTO 与 Command

- [x] 6.1 创建 OrgTeacherDTO（聚合返回数据：userId + departmentId + 用户基本信息）
- [x] 6.2 创建 CreateOrgTeacherCommand（添加请求：name, phone, departmentId）
- [x] 6.3 创建 UpdateOrgTeacherCommand（更新请求：departmentId）
- [x] 6.4 创建 OrgTeacherQueryParamDTO（查询参数）

## 7. 应用层 - 服务

- [x] 7.1 创建 OrgTeacherAppService 应用服务
- [x] 7.2 实现 createOrgTeacher 方法：
  - 调用 userQueryService.findByPhone 查询用户
  - 不存在则调用 userQueryService.createUser 创建用户
  - 创建 OrgTeacher(userId, departmentId)
- [x] 7.3 实现 listOrgTeachers 聚合查询方法：
  - 查询组织域关联关系
  - 批量调用 userQueryService.findByIds 获取用户基本信息
  - 合并返回完整信息
- [x] 7.4 实现 getOrgTeacher、updateOrgTeacher、deleteOrgTeacher 方法

## 8. 接口层 - REST API

- [x] 8.1 创建 OrgTeacherController
- [x] 8.2 实现 POST /api/org/teachers 添加接口（提交姓名、手机号、部门ID）
- [x] 8.3 实现 GET /api/org/teachers/{id} 查询详情接口（聚合返回完整信息）
- [x] 8.4 实现 GET /api/org/teachers 列表查询接口（按学校、按部门，聚合返回完整信息）
- [x] 8.5 实现 PUT /api/org/teachers/{id} 更新接口（只修改所属部门）
- [x] 8.6 实现 DELETE /api/org/teachers/{id} 删除接口（只删除关联关系）

## 9. 编译验证

- [x] 9.1 执行 mvn clean install -DskipTests 编译验证