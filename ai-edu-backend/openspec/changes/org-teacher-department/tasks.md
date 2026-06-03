## 1. 数据库表结构

- [x] 1.1 创建 Flyway 迁移脚本 V3__create_t_department_table.sql

## 2. Domain 层 - 值对象

- [x] 2.1 创建 DepartmentId 值对象
- [x] 2.2 创建 DepartmentQueryParam 查询参数值对象

## 3. Domain 层 - 实体

- [x] 3.1 创建 Department 实体（含 create/update 工厂方法，department_path 计算）

## 4. Domain 层 - 仓储接口

- [x] 4.1 创建 DepartmentRepository 仓储接口

## 5. Infrastructure 层 - PO

- [x] 5.1 创建 DepartmentPO 持久化对象

## 6. Infrastructure 层 - Mapper

- [x] 6.1 创建 DepartmentMapper（MyBatis-Plus）

## 7. Infrastructure 层 - 仓储实现

- [x] 7.1 创建 DepartmentRepositoryImpl 仓储实现（含 department_path 查询）

## 8. Application 层 - DTO/Command

- [x] 8.1 创建 DepartmentDTO
- [x] 8.2 创建 CreateDepartmentCommand
- [x] 8.3 创建 UpdateDepartmentCommand
- [x] 8.4 创建 DepartmentQueryCommand

## 9. Application 层 - 服务

- [x] 9.1 创建 DepartmentAppService 应用服务
- [x] 9.2 实现 createDepartment 方法（含 department_path 计算）
- [x] 9.3 实现 updateDepartment 方法（含 department_path 更新）
- [x] 9.4 实现 deleteDepartment 方法
- [x] 9.5 实现 getDepartmentById 方法
- [x] 9.6 实现 getDepartmentTree 方法

## 10. Interface 层 - Controller

- [x] 10.1 创建 DepartmentController
- [x] 10.2 实现创建部门接口 POST /schools/{schoolId}/departments/create
- [x] 10.3 实现更新部门接口 POST /schools/{schoolId}/departments/{id}/update
- [x] 10.4 实现删除部门接口 POST /schools/{schoolId}/departments/{id}/delete
- [x] 10.5 实现获取部门详情接口 GET /schools/{schoolId}/departments/{id}
- [x] 10.6 实现获取部门树接口 GET /schools/{schoolId}/departments

## 11. 测试与验证

- [x] 11.1 编译验证通过
- [ ] 11.2 启动应用验证接口可访问（需要配置 Neo4j/Redis 或排除相关服务）
- [ ] 11.3 测试创建根级部门（依赖测试环境配置）
- [ ] 11.4 测试创建子部门（department_path 正确）
- [ ] 11.5 测试部门树查询

## 12. 后续补充（依赖用户域）

- [ ] 12.1 创建 t_teacher_department 表
- [ ] 12.2 创建 TeacherDepartment 实体和仓储
- [ ] 12.3 实现教师-部门关联接口