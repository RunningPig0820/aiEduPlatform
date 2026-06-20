## 1. 通用层 — 加密工具

- [x] 1.1 在 `ai-edu-common` 模块创建 `EncryptUtil` 类，基于 AES-256 对称加密
- [x] 1.2 实现 `encrypt(plainText)` 方法：AES 加密 → Base64 编码
- [x] 1.3 实现 `decrypt(cipherText)` 方法：Base64 解码 → AES 解密
- [x] 1.4 实现 `maskIdCard(idCard)` 方法：保留前 6 后 6 位，中间脱敏
- [x] 1.5 在 `application.yml` 配置 `app.encrypt.aes-key` 密钥项（支持环境变量覆盖）
- [x] 1.6 编写 `EncryptUtil` 单元测试（加密→解密 往返验证 + 脱敏格式验证）

## 2. 数据库迁移

- [x] 2.1 创建 Flyway 迁移脚本 `V7__alter_t_user_add_id_card.sql`：ALTER TABLE t_user ADD COLUMN id_card VARCHAR(512)
- [x] 2.2 创建 Flyway 迁移脚本 `V8__create_t_parent_profile.sql`：包含 id, student_user_id, parent_user_id, relationship, is_primary, created_at, updated_at，以及 UNIQUE INDEX (student_user_id, parent_user_id)
- [x] 2.3 更新测试 `schema.sql`：t_user 表加 id_card 列，新建 t_parent_profile 表

## 3. 用户域 — User 实体扩展

- [x] 3.1 `User.java` 新增 `idCard` 字段（`@TableField("id_card")`），添加 `@Getter`
- [x] 3.2 新增 `User.createStudent(username, password, realName, phone, idCard)` 工厂方法，角色固定为 `"STUDENT"`
- [x] 3.3 扩展 `UserService.createUser` 方法签名为 `createUser(String name, String phone, String role, String idCard)`，支持角色参数化和可选 idCard
- [x] 3.4 `UserServiceImpl.createUser` 中按 role 参数创建不同角色用户（默认密码、默认用户名逻辑保持不变）
- [x] 3.5 `UserMapper` 无需修改（SELECT * 自动包含 id_card 列）

## 4. 用户域 — ParentProfile 实体与持久化

- [x] 4.1 创建 `ParentProfile` 实体（domain/user/model/entity/），字段：id, studentUserId(Long), parentUserId(Long), relationship(String), isPrimary(Boolean), createdAt, updatedAt
- [x] 4.2 实现 `ParentProfile.create(studentUserId, parentUserId, relationship)` 工厂方法
- [x] 4.3 创建 `ParentProfileRepository` 接口（domain/user/repository/）：saveAll, findByStudentUserId, findByParentUserId, deleteByStudentUserId
- [x] 4.4 创建 `ParentProfilePO` 持久化对象（infrastructure/persistence/user/po/）
- [x] 4.5 创建 `ParentProfileMapper`（infrastructure/persistence/user/mapper/），`@DS("user")`，继承 `BaseMapper<ParentProfilePO>`
- [x] 4.6 创建 `ParentProfileRepositoryImpl`（infrastructure/persistence/user/repository/），实现 PO ↔ Entity 转换

## 5. 组织域 — ACL 防腐层模型

- [x] 5.1 创建 `StudentInfo`（domain/organization/acl/）：userId(Long), name(String), phone(String), maskedIdCard(String)
- [x] 5.2 创建 `ParentInfo`（domain/organization/acl/）：userId(Long), name(String), phone(String), relationship(String)
- [x] 5.3 创建 `ParentBinding` 简单 VO（domain/organization/acl/）：parentUserId(Long), relationship(String) — 用于 Gateway 方法参数

## 6. 组织域 — Gateway 防腐层接口扩展

- [x] 6.1 `OrgUserGateway` 新增 `findOrCreateStudent(String name, String phone, String idCard)` 返回 `StudentInfo`
- [x] 6.2 `OrgUserGateway` 新增 `findOrCreateParent(String name, String phone)` 返回 `ParentInfo`
- [x] 6.3 `OrgUserGateway` 新增 `bindStudentParents(Long studentUserId, List<ParentBinding> bindings)` void 方法
- [x] 6.4 `OrgUserGateway` 新增 `findParentsByStudentUserId(Long studentUserId)` 返回 `List<ParentInfo>`
- [x] 6.5 `OrgUserGatewayImpl` 添加 4 个 stub 实现（真实逻辑待任务 7）

## 7. 基础设施层 — Gateway 实现扩展

- [x] 7.1 `UserDataProvider` 新增 `createStudent(String name, String phone, String idCard)` 方法：`@DS("user")`，调用 `EncryptUtil.encrypt(idCard)` 后传给 `userService.createUser(name, phone, "STUDENT", encryptedIdCard)`
- [x] 7.2 `UserDataProvider` 新增 `createParent(String name, String phone)` 方法：`@DS("user")`，调用 `userService.createUser(name, phone, "PARENT", null)`
- [x] 7.3 `OrgUserGatewayImpl.findOrCreateStudent()` 实现：查手机号 → 不存在则 createStudent → 角色校验 → 转 StudentInfo（含 maskIdCard）
- [x] 7.4 `OrgUserGatewayImpl.findOrCreateParent()` 实现：查手机号 → 不存在则 createParent → 角色校验 → 转 ParentInfo
- [x] 7.5 `OrgUserGatewayImpl.bindStudentParents()` 实现：遍历 bindings → `ParentProfile.create()` → `parentProfileRepository.saveAll()`
- [x] 7.6 `OrgUserGatewayImpl.findParentsByStudentUserId()` 实现：查 `ParentProfile` + 查 `User` + 合并为 `List<ParentInfo>`
- [x] 7.7 `OrgUserGatewayImpl` 模型转换方法：`toStudentInfo(User)`（含 decrypt + maskIdCard）、`toParentInfo(User, String relationship)`

## 8. 应用层 — DTO 与 Command

- [x] 8.1 创建 `CreateAdminClassStudentCommand`（application/dto/org/command/）：name, phone, idCard, studentNo, parents(List<ParentCommand>)
- [x] 8.2 创建 `ParentCommand`（独立类）：name, phone, relationship — 带 `@NotBlank` 校验
- [x] 8.3 `CreateAdminClassStudentCommand` 加 JSR-303 校验：name 非空、phone 正则 `^1[3-9]\d{9}$`、idCard 非空 18 位
- [x] 8.4 创建 `AdminClassStudentDTO`（application/dto/org/）：studentUserId, name, phone, maskedIdCard, studentNo, deptId/deptName, parents(List<ParentInfoDTO>), joinDate, status

## 9. 应用层 — AdminClassStudentAppService

- [x] 9.1 创建 `AdminClassStudentAppService`（application/service/org/），注入 `DepartmentRepository`、`DepartmentEduRepository`、`StudentClassRepository`、`OrgUserGateway`
- [x] 9.2 实现 `createStudent(Long schoolId, Long deptId, Long currentUserId, CreateAdminClassStudentCommand command)` 方法：
  - Step 1: `validateClassNode(deptId, schoolId)` — 查 Department + DepartmentEdu，校验 DeptEduType=CLASS 且 schoolId 匹配
  - Step 2: `gateway.findOrCreateStudent(name, phone, idCard)` — 创建/查询学生用户
  - Step 3: 遍历 `command.parents` → `gateway.findOrCreateParent(name, phone)` — 创建/查询家长用户，收集 bindings
  - Step 4: `createStudentClassRelation(schoolId, deptId, studentUserId, studentNo, currentUserId)` — 创建 StudentClass 关联，@DS("org") @Transactional
  - Step 5: `gateway.bindStudentParents(studentUserId, bindings)` — 保存家长关联
  - Step 6: `buildDTO()` — 聚合返回
- [x] 9.3 实现 `listStudentsByClass(Long deptId)` 查询方法：查 StudentClass → 批量查 User → 查 ParentProfile → 聚合
- [x] 9.4 实现 `validateClassNode(deptId, schoolId)` 私有方法，校验 Department 存在/类型/归属 + DepartmentEdu.deptType=CLASS
- [x] 9.5 实现 `createStudentRelationInTx()` 私有方法：检查重复 → StudentClass.create() → studentClassRepository.save()
- [x] 9.6 实现 `buildDTO()` 私有方法：合并 StudentClass + StudentInfo + ParentInfo 为 AdminClassStudentDTO
- [x] 9.7 新建 `ErrorCode`：ADMIN_CLASS_NODE_INVALID(80010)、STUDENT_ALREADY_IN_ADMIN_CLASS(80011)

## 10. 接口层 — REST API

- [x] 10.1 创建 `AdminClassStudentController`（interfaces/api/org/），`@RequestMapping("/api/auth/schools/{schoolId}/admin-classes")`
- [x] 10.2 实现 `POST /{deptId}/students` — 添加学生到行政班班级节点
- [x] 10.3 实现 `GET /{deptId}/students` — 查询行政班班级下的学生列表
- [x] 10.4 所有端点注入 SchoolId 校验（TODO: 暂时硬编码 currentUserId=1L，后续接登录上下文）

## 11. 编译验证

- [x] 11.1 执行 `mvn clean install -DskipTests` 确保所有模块编译通过
- [x] 11.2 修复编译问题（如有）
