## Module Overview

知识图谱（Knowledge Graph）导航与同步模块。提供 Neo4j → MySQL 数据同步、知识点导航浏览、年级知识体系查询能力。前端 SPA 通过 REST API 读取 MySQL 数据。

## Bounded Context

KnowledgeGraph Domain

## Package Structure

```
com.ai.edu.domain.edukg/
├── model/
│   ├── entity/
│   │   ├── KgTextbook.java          # PK: uri (String)
│   │   ├── KgChapter.java
│   │   ├── KgSection.java
│   │   ├── KgKnowledgePoint.java
│   │   └── relation/
│   │       ├── KgTextbookChapter.java
│   │       ├── KgChapterSection.java
│   │       └── KgSectionKP.java
│   └── valueobject/
│       ├── KgDifficulty.java
│       ├── KgImportance.java
│       ├── KgCognitiveLevel.java
│       └── KgNodeStatus.java        # enum: active/deleted/merged
├── repository/                      # Repository interfaces
│   ├── KgTextbookRepository.java
│   ├── KgChapterRepository.java
│   ├── KgSectionRepository.java
│   ├── KgKnowledgePointRepository.java
│   ├── KgTextbookChapterRepository.java
│   ├── KgChapterSectionRepository.java
│   └── KgSectionKPRepository.java
└── service/
    └── KgDomainService.java

com.ai.edu.application.edukg/
├── KgSyncAppService.java            # 同步编排
└── KgNavigationAppService.java      # 导航查询编排

com.ai.edu.interface_.api/
└── KnowledgeGraphController.java    # /api/kg/**

com.ai.edu.infrastructure.edukg/
├── mapper/                          # MyBatis Mapper (带 @DS("kg"))
├── neo4j/
│   ├── Neo4jConfig.java
│   ├── Neo4jSyncService.java        # Neo4j → MySQL 同步
│   └── Neo4jRelationQueryService.java # 图谱关系查询
└── cache/
    └── KgRelationCacheManager.java  # Redis 缓存
```

## External Dependencies

| Dependency | Type | Interface | Required Fields |
|------------|------|-----------|-----------------|
| KnowledgeGraph Datasource | Infrastructure | @DS("kg") Mapper | ai_edu_kg 数据库可用 |
| Neo4j | External DB | bolt:// connection | Neo4j 服务可用（图谱关系查询） |
| Redis | Cache | Redisson client | 图谱关系查询短期缓存 (TTL 5min) |
| User Domain | Domain Service | UserService.validateLogin() | userId（可选，当前阶段不实现权限控制） |

## Provided Capabilities

### Domain Services

| Service | Method | Input | Output | Description |
|---------|--------|-------|--------|-------------|
| KgSyncService | syncFull() | { subject?, phase?, grade?, textbookUri? } | SyncResult | 从 Neo4j 同步数据到 MySQL（UPSERT） |
| KgNavigationService | getTextbookTree() | { textbookUri } | TextbookTreeDTO | 获取教材完整层级树 |
| KgNavigationService | getKnowledgePointDetail() | { kpUri } | KgKnowledgePointDetailDTO | 获取知识点详情（含2层父级） |
| KgNavigationService | getGradeKnowledgeSystem() | { grade } | GradeSystemDTO | 获取某年级完整知识体系 |
| KgRelationQueryService | getConceptRelations() | { uri } | RelationsDTO | 查询 Neo4j 图谱关系（含缓存） |

### REST APIs

#### 同步相关

| Method | Path | Auth | Input | Output | Description |
|--------|------|------|-------|--------|-------------|
| POST | /api/kg/sync/full | No (TODO: Auth) | SyncRequest | SyncResult | 触发全量/定向同步（下拉选项 code 值） |
| GET | /api/kg/sync/status | No | - | SyncStatus | 查询当前同步状态 |
| GET | /api/kg/sync/records | No | page, size | Page<SyncRecord> | 同步历史记录 |

#### 维度配置（下拉选择器）

| Method | Path | Auth | Input | Output | Description |
|--------|------|------|-------|--------|-------------|
| GET | /api/kg/dimensions/subjects | No | - | List<DimensionDTO> | 学科下拉选项 |
| GET | /api/kg/dimensions/grades | No | - | List<DimensionDTO> | 年级下拉选项 |
| GET | /api/kg/dimensions/phases | No | - | List<DimensionDTO> | 学段下拉选项 |

#### 导航相关（6 级：学科→年级→教材→章节→小节→知识点）

| Method | Path | Auth | Input | Output | Description |
|--------|------|------|-------|--------|-------------|
| GET | /api/kg/subjects | No | - | List\<String\> | 导航树根节点（学科列表） |
| GET | /api/kg/subjects/{subject}/grades | No | path: subject | List\<{uri,label}\> | 学科下的年级 |
| GET | /api/kg/grades/{grade}/textbooks | No | path: grade | List<TextbookDTO> | 年级下的教材 |
| GET | /api/kg/textbooks | No | grade?, subject? | List<TextbookDTO> | 教材列表 |
| GET | /api/kg/textbooks/{uri}/chapters | No | path: uri | List<ChapterTreeNode> | 教材章节树 |
| GET | /api/kg/sections/{uri}/points | No | path: uri | List<KgKnowledgePointDetailDTO> | 小节知识点 |
| GET | /api/kg/knowledge-points/{uri} | No | path: uri | KgKnowledgePointDetailDTO | 知识点详情 |
| GET | /api/kg/knowledge-points/{uri}/graph | No | path: uri | KgGraphDTO | 知识点图谱关系 |

#### 知识体系（无需登录）

| Method | Path | Auth | Input | Output | Description |
|--------|------|------|-------|--------|-------------|
| GET | /api/kg/system/grade/{grade} | No | path: grade | GradeSystemDTO | 年级知识体系 |
| GET | /api/kg/system/stats/{grade} | No | path: grade | StatsDTO | 年级知识点统计 |

#### 图谱关系查询（直接查 Neo4j）

| Method | Path | Auth | Input | Output | Description |
|--------|------|------|-------|--------|-------------|
| GET | /api/kg/concepts/{uri}/relations | No | path: uri | RelationsDTO | 概念关联（Neo4j+缓存） |
| GET | /api/kg/concepts/batch-relations | No | uris[] (query) | List<RelationsDTO> | 批量概念关联 |
| GET | /api/kg/neo4j/health | No | - | HealthDTO | Neo4j 健康状态 |

### Domain Events

| Event | Trigger | Payload | Consumers |
|-------|---------|---------|-----------|
| KgSyncCompleted | 同步完成 | { syncId, scope, insertedCount, updatedCount } | Learning Domain（知识点更新通知） |
| KgNodeStatusChanged | 节点状态变更 | { uri, oldStatus, newStatus, mergedToUri? } | Learning Domain（进度迁移） |

## Key Data Models

### Entity: KgKnowledgePoint

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| uri | String | 唯一标识（主键） | PK, VARCHAR(255) |
| label | String | 知识点名称 | NOT NULL |
| difficulty | KgDifficulty | 难度等级 | enum: easy/medium/hard |
| importance | KgImportance | 重要程度 | enum: low/medium/high |
| cognitiveLevel | KgCognitiveLevel | 认知层级 | enum: 记忆/理解/应用/分析 |
| status | KgNodeStatus | 节点状态 | enum: active/deleted/merged |
| mergedToUri | String | 被合并目标URI | nullable |

### DTO: KgKnowledgePointDetailDTO

| Field | Type | Description | Source |
|-------|------|-------------|--------|
| uri | String | 知识点URI | Entity.uri |
| label | String | 知识点名称 | Entity.label |
| difficulty | String | 难度 | Entity.difficulty |
| sectionUri | String | 父级小节URI | KgSectionKP.sectionUri |
| sectionLabel | String | 小节名称 | KgSection.label |
| chapterUri | String | 爷爷级章节URI | KgChapterSection.chapterUri |
| chapterLabel | String | 章节名称 | KgChapter.label |

### DTO: SyncRequest

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| subject | String | 学科（默认 math） | No |
| phase | String | 学段（primary/middle/high） | No |
| grade | String | 年级 | No |
| textbookUri | String | 指定教材URI | No |

### DTO: SyncResult

| Field | Type | Description |
|-------|------|-------------|
| syncId | Long | 同步记录ID |
| status | String | running/success/failed |
| insertedCount | int | 新增数 |
| updatedCount | int | 更新数 |
| statusChangedCount | int | 状态变更数 |
| reconciliationStatus | String | matched/mismatched |
| duration | long | 耗时（ms） |

## Cross-Module Interaction

```
  Frontend
    → KnowledgeGraphController (/api/kg/**)
      → KgSyncAppService
        → Neo4jSyncService (Neo4j → MySQL UPSERT)
          → KgTextbookMapper (@DS("kg")) → ai_edu_kg DB
        → KgNavigationAppService
          → KgKnowledgePointRepository (JPA query, status='active')
            → ai_edu_kg DB
          → Neo4jRelationQueryService (optional)
            → Neo4j bolt:// (with Redis cache)
```

```
  Learning Domain (Consumer)
    ← KgSyncCompleted Event
    ← KgNodeStatusChanged Event
    → Uses /api/kg/knowledge-points/{uri} to fetch KG details
```

## Implementation Notes

- **数据库**: ai_edu_kg（通过 knowledge-graph-datasource 模块的 @DS("kg") 路由）
- **URI 主键**: 所有 KG 主表使用 VARCHAR(255) URI 作为主键，非自增 ID
- **同步事务**: 整个同步流程（UPSERT + 关联重建 + 对账）在一个 @Transactional("kg") 内
- **查询过滤**: 导航/知识体系查询自动过滤 status='active'
- **进度查询例外**: 学习进度查询包含 deleted 节点，通过 isDeprecated 标识
- **Redis 缓存**: 图谱关系查询 key = `kg:neo4j:{uri}:{query_type}`, TTL = 300s
- **降级**: Neo4j 不可用时返回空关联，不影响 MySQL 导航
- **当前不实现权限**: 所有 API 无需登录，后续由 User Domain 补充
- **索引**: 已为 grade/subject/topic/status 等常用查询字段添加索引
