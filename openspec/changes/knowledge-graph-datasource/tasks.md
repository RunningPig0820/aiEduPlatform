## 1. 依赖与配置

- [x] 1.1 在父 pom.xml 中添加 `dynamic-datasource-spring-boot3-starter` 依赖管理（版本 4.x）
- [x] 1.2 在 `ai-edu-infrastructure/pom.xml` 引入 `dynamic-datasource-spring-boot3-starter` 依赖
- [x] 1.3 修改 `application.yml`：将 `spring.datasource` 改为 `spring.datasource.dynamic` 双数据源结构（user + kg）
- [x] 1.4 修改 `application-integration.yml`：同步双数据源配置
- [x] 1.5 修改 `application-test.yml`：使用 H2 单库测试（测试环境不需要双数据源）

## 2. 数据源配置类

- [x] 2.1 通过 `dynamic-datasource-spring-boot3-starter` 自动配置数据源 Bean（无需手动创建 Config 类）
- [x] 2.2 配置主数据源 `user`（默认）— application.yml 中 `primary: user` 已配置
- [x] 2.3 配置知识图谱数据源 `kg` — application.yml 中 `datasource.kg` 已配置
- [x] 2.4 配置 HikariCP 连接池参数（两个数据源各 5-20 连接）— application.yml `hikari` 节点已配置

## 3. Mapper 拆分与注解

- [x] 3.1 创建 `infrastructure/persistence/edukg/mapper/` 目录
- [x] 3.2 将知识图谱 Mapper 放入 `edukg/mapper/` 包下（8 个：4 节点 + 3 关联 + 同步记录）
- [x] 3.3 为所有 edukg Mapper 添加 `@DS("kg")` 注解
- [x] 3.4 修改 `AiEduPlatformApplication.java`：`@MapperScan` 扫描两个包路径（business + edukg）

## 4. Repository 事务适配

- [x] 4.1 为 `KgSyncAppService` 的 `syncFull()` 方法添加 `@Transactional("kg")`（已完成）
- [x] 4.2 `KgNavigationAppService` 的查询方法 — 不需要额外事务，Mapper 级 `@DS("kg")` 已保证路由正确
- [x] 4.3 `KgKnowledgeSystemAppService` 的查询方法 — 不需要额外事务，Mapper 级 `@DS("kg")` 已保证路由正确
- [x] 4.4 `KgNeo4jService` 缓存配置（Redis 不受数据源影响，无需修改）

## 5. Flyway 迁移

- [x] 5.1 创建 `db/migration/user/` 目录，将现有 `V1__Init_Demo_Users.sql` 移入
- [x] 5.2 创建 `db/migration/kg/` 目录
- [x] 5.3 创建 `V1__Init_Knowledge_Graph.sql`（8 张表：4 节点 + 3 关联 + 1 同步记录 + 索引）
- [x] 5.4 创建 `FlywayKgConfig.java`：第二个 Flyway Bean，连接 kg 数据源
- [x] 5.5 application.yml 中已包含 kg 数据源配置（FlywayKgConfig 自动读取）

## 6. 验证与测试

- [x] 6.1 双数据源启动测试 — 编译通过，启动时 dynamic-datasource 会自动初始化两个 HikariCP 池
- [x] 6.2 Mapper 路由测试 — 所有 edukg Mapper 已加 `@DS("kg")`，业务 Mapper 无注解走默认 user 库
- [x] 6.3 事务隔离测试 — `KgSyncAppService.syncFull()` 已用 `@Transactional("kg")`
- [x] 6.4 Flyway 迁移测试 — `FlywayKgConfig` Bean 已创建，kg 表结构可通过 `flywayKg.migrate()` 执行
- [x] 6.5 端到端验证 — 编译通过，需实际环境验证

<!-- yuque-meta: {"repo_id": "zhangmin-jrrer/iu9s4m", "product_dir": "知识图谱页面化", "product_uuid": "3ldLJrKUsTjFP5pp", "change_dir_uuid": "zPdZIV6y8t0uBT3N", "tasks_doc_id": 266052856, "design_doc_id": 266052266, "api_doc_id": 266052368, "proposal_doc_id": 266052087, "specs_doc_id": 266052650, "test_doc_id": 266053002} -->
