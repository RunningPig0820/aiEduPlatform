## Module Overview

Knowledge Graph 数据源双数据源架构模块。通过引入 `dynamic-datasource-spring-boot3-starter` 实现 `ai_edu_user`（业务库）和 `ai_edu_kg`（知识图谱库）的物理隔离。

## Bounded Context

KnowledgeGraph Domain

## Package Structure

```
com.ai.edu.infrastructure/
├── config/
│   └── DynamicDataSourceConfig.java        # 双数据源配置
├── persistence/
│   ├── mapper/                              # 业务 Mapper → user 库（默认）
│   │   └── UserMapper.java
│   └── edukg/
│       └── mapper/                          # KG Mapper → kg 库（@DS("kg")）
│           ├── KgTextbookMapper.java
│           ├── KgChapterMapper.java
│           ├── KgSectionMapper.java
│           ├── KgKnowledgePointMapper.java
│           └── ...
└── flyway/
    ├── KgFlywayConfig.java                  # KG Flyway 自定义 Bean
    └── db/migration/
        ├── user/                            # 业务库迁移脚本
        └── kg/                              # KG 库迁移脚本
```

## External Dependencies

| Dependency | Type | Interface | Required Fields |
|------------|------|-----------|-----------------|
| MySQL | Database | JDBC Connection | `ai_edu_user` + `ai_edu_kg` 两个数据库 |
| dynamic-datasource | Library | @DS("kg") | Spring Boot 3 compatible version (4.x) |
| Flyway | Library | Flyway migration | 需要自定义第二个 Flyway Bean 用于 kg 库 |

## Provided Capabilities

### Domain Services

N/A - 此模块是基础设施层变更，不直接暴露 Domain Service。

### REST APIs

N/A - 此模块不新增 REST API，不影响现有 API。

### Domain Events

N/A

## Key Data Models

### Database Tables (in `ai_edu_kg`)

7 张知识图谱表部署到 `ai_edu_kg` 数据库：

| Table | Description | Primary Key |
|-------|-------------|-------------|
| t_kg_textbook | 教材表 | uri (VARCHAR) |
| t_kg_chapter | 章节表 | uri (VARCHAR) |
| t_kg_section | 小节表 | uri (VARCHAR) |
| t_kg_knowledge_point | 知识点表 | uri (VARCHAR) |
| t_kg_textbook_chapter | 教材→章节关联 | (textbook_uri, chapter_uri) |
| t_kg_chapter_section | 章节→小节关联 | (chapter_uri, section_uri) |
| t_kg_section_kp | 小节→知识点关联 | (section_uri, kp_uri) |
| t_kg_sync_record | 同步记录表 | id (BIGINT AUTO_INCREMENT) |

### Configuration

```yaml
spring.datasource.dynamic:
  primary: user
  strict: true
  datasource:
    user: { url: ${USER_DB_URL}, ... }
    kg: { url: ${KG_DB_URL}, ... }
```

## Cross-Module Interaction

```
  Application Layer (KgSyncAppService)
    → @Transactional("kg")
      → KgTextbookMapper (@DS("kg"))
        → ai_edu_kg Database

  Business Layer (UserAppService)
    → @Transactional (default)
      → UserMapper (no @DS, default user)
        → ai_edu_user Database
```

## Implementation Notes

- **数据源路由**: 通过 Mapper 级别的 `@DS("kg")` 注解实现，非动态切换
- **事务隔离**: `@Transactional("kg")` 绑定到 kg 数据源，默认绑定到 user
- **跨库 JOIN**: 不支持，知识图谱与业务表通过 URI 引用，无物理外键
- **Mapper 扫描**: `@MapperScan` 拆分为两个，按包路径分别绑定
- **连接池**: HikariCP，user 和 kg 各 5-20 连接，共 40 连接上限
- **Code Review 检查项**: 所有 `edukg/mapper/` 下的 Mapper 必须带 `@DS("kg")`
