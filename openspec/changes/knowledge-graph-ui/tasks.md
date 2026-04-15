## 1. 数据库与基础设施

- [ ] 1.1 设计 MySQL 表结构：4 张节点主表（uri 主键，status 枚举 + merged_to_uri）+ 3 张层级关联表（含 order_index，无软删除）+ 1 张同步记录表（含 scope/reconciliation_status/details JSON 字段）
- [ ] 1.2 创建 Flyway 迁移脚本 `V1__init_knowledge_graph.sql`
- [ ] 1.3 在 `ai-edu-infrastructure/pom.xml` 添加 `neo4j-java-driver` 依赖
- [ ] 1.4 在 `application.yml` 添加 Neo4j 连接配置
- [ ] 1.5 创建 `Neo4jConfig` 配置类，初始化 Neo4j Driver Bean（含连接池配置）
- [ ] 1.6 在 `ErrorCode.java` 增加知识图谱错误码段（7xxxx）
- [ ] 1.7 创建数据库索引：t_kg_textbook(grade/subject/phase) + t_kg_knowledge_point(status/label/difficulty) + 关联表排序索引

## 2. Domain 层建模

- [ ] 2.1 创建 `domain/edukg/model/entity/` 目录及 4 个节点实体类（KgTextbook, KgChapter, KgSection, KgKnowledgePoint），主键为 URI（String）
- [ ] 2.2 创建 `domain/edukg/model/entity/relation/` 目录及 3 个关联实体类（KgTextbookChapter, KgChapterSection, KgSectionKP），复合主键 + orderIndex
- [ ] 2.3 创建 `domain/edukg/model/entity/KgSyncRecord.java` 同步记录实体（含 scope/reconciliationStatus/details 字段）
- [ ] 2.4 创建 `domain/edukg/model/valueobject/` 枚举类（KgDifficulty, KgImportance, KgCognitiveLevel, KgNodeStatus）
- [ ] 2.5 创建 `domain/edukg/repository/` 目录及 7 个 Repository 接口（4 节点 + 3 关联）

## 3. Infrastructure 层实现

- [ ] 3.1 创建 `infrastructure/persistence/repository/` 目录下 JPA Repository 实现
- [ ] 3.2 创建 `infrastructure/persistence/neo4j/Neo4jKgSyncService.java` Neo4j 同步服务
- [ ] 3.3 实现 Neo4j 查询节点并映射为 MySQL Entity（按 URI）
- [ ] 3.4 实现 Neo4j 查询层级关系（CONTAINS/IN_UNIT，含 order_index）并映射为关联 Entity
- [ ] 3.5 实现 UPSERT 批量写入逻辑（ON DUPLICATE KEY UPDATE，按 URI）
- [ ] 3.6 实现关联表全量重建逻辑（每次同步先 DELETE 全部关联再 INSERT 新关联）
- [ ] 3.7 实现状态变更逻辑（MySQL 中有但 Neo4j 中无的标记 `status = 'deleted'`）
- [ ] 3.8 实现同步记录写入与更新逻辑（含 insertedCount/updatedCount/statusChangedCount/scope/reconciliationStatus 统计）
- [ ] 3.9 实现 URI 校验逻辑（非空/格式/重复检查，异常记录到同步日志并跳过）
- [ ] 3.10 实现同步对账校验逻辑（同步完成后对比 MySQL vs Neo4j 节点数/关联数）
- [ ] 3.11 创建 `infrastructure/cache/Neo4jRelationCacheService.java` Redis 缓存服务（TTL 300s）
- [ ] 3.12 创建 `infrastructure/neo4j/Neo4jRelationQueryService.java` Neo4j 图谱关系查询服务（含降级机制）
- [ ] 3.13 实现 Neo4j 健康检查逻辑（连接测试 + 响应时间）

## 4. Application 层服务

- [ ] 4.1 创建 `application/dto/kg/` 目录及响应 DTO（KgTextbookDTO, KgChapterDTO, KgSectionDTO, KgKnowledgePointDTO, KgGradeSystemDTO, KgGradeStatsDTO, KgSyncStatusDTO, KgBatchRelationsDTO）
- [ ] 4.2 创建 MapStruct 转换器 `KgConverter.java`
- [ ] 4.3 创建 `application/service/KgSyncAppService.java` 同步应用服务
- [ ] 4.4 创建 `application/service/KgNavigationAppService.java` 导航查询服务
- [ ] 4.5 创建 `application/service/KgKnowledgeSystemAppService.java` 知识体系服务
- [ ] 4.6 创建 `application/service/KgNeo4jService.java` Neo4j 查询服务（含缓存 + 降级）
- [ ] 4.7 实现 `syncFull(subject, phase, grade, textbookUri)` 全量/定向同步方法（**整个流程在 @Transactional 事务内**：同步锁、URI 校验、UPSERT、关联表重建、状态变更、对账校验）
- [ ] 4.8 实现 `getSyncStatus()` 同步状态查询（含 reconciliationStatus）
- [ ] 4.9 实现 `getSyncRecords()` 同步历史查询（含 scope/reconciliationStatus）
- [ ] 4.10 实现 `getTextbooks(subject, phase)` 教材列表查询
- [ ] 4.11 实现 `getChaptersByTextbook(textbookUri)` 章节树查询（JOIN 关联表，含 orderIndex，空章节过滤）
- [ ] 4.12 实现 `getKnowledgePointsBySection(sectionUri)` 知识点查询（JOIN 关联表）
- [ ] 4.13 实现 `getKnowledgePointDetail(kpUri)` 知识点详情（含 2 层父级）
- [ ] 4.14 实现 `getGradeSystem(grade, groupBy)` 年级知识体系
- [ ] 4.15 实现 `getGradeStats(grade)` 年级知识点统计
- [ ] 4.16 实现 `getNeo4jHealth()` Neo4j 健康检查
- [ ] 4.17 实现 `batchGetConceptRelations(uris)` 批量获取概念关联（Redis 缓存 + 降级）

## 5. Interface 层 API

- [ ] 5.1 创建 `interface_/api/KnowledgeGraphController.java`
- [ ] 5.2 实现 `POST /api/kg/sync/full` 同步接口（支持可选 subject/phase/grade/textbookUri 参数）
- [ ] 5.3 实现 `GET /api/kg/sync/status` 同步状态接口
- [ ] 5.4 实现 `GET /api/kg/sync/records` 同步历史接口
- [ ] 5.5 实现 `GET /api/kg/textbooks` 教材列表接口
- [ ] 5.6 实现 `GET /api/kg/textbooks/{uri}/chapters` 章节树接口（URI 需要 URL 编码）
- [ ] 5.7 实现 `GET /api/kg/sections/{uri}/points` 知识点列表接口
- [ ] 5.8 实现 `GET /api/kg/knowledge-points/{uri}` 知识点详情接口（返回 2 层父级）
- [ ] 5.9 实现 `GET /api/kg/system/grade/{grade}` 年级知识体系接口
- [ ] 5.10 实现 `GET /api/kg/system/stats/{grade}` 年级统计接口
- [ ] 5.11 实现 `GET /api/kg/neo4j/health` Neo4j 健康检查接口
- [ ] 5.12 实现 `POST /api/kg/concepts/batch-relations` 批量概念关联接口
- [ ] 5.13 为所有接口添加 `ApiResponse<T>` 统一包装和 CORS 配置

## 6. 单元测试

- [ ] 6.1 编写 Domain 层实体/枚举/关联表测试
- [ ] 6.2 编写 Neo4j 同步服务测试（Mock Neo4j Driver）
- [ ] 6.3 编写 AppService 层测试 `KgSyncAppServiceTest`（含事务回滚测试）
- [ ] 6.4 编写 AppService 层测试 `KgNavigationAppServiceTest`
- [ ] 6.5 编写 Controller 层 API 接口测试（MockMvc）
- [ ] 6.6 编写同步原子性测试（确保同步期间数据一致性，事务失败回滚）
- [ ] 6.7 编写软删除查询过滤测试
- [ ] 6.8 编写同步对账校验测试
- [ ] 6.9 编写 URI 校验测试（非法 URI/空 URI/重复 URI）
- [ ] 6.10 编写 Neo4j 缓存与降级测试
- [ ] 6.11 编写批量概念关联测试

## 7. 前端对接

> 后端负责 API 设计和 DTO 定义，前端同学根据 API 文档开发页面。

- [ ] 7.1 提供 API 接口文档（api.md）给前端同学
- [ ] 7.2 确认 URI 编码规范与前端约定一致
- [ ] 7.3 联调知识点导航页面
- [ ] 7.4 联调年级知识体系页面

## 8. 联调与集成

- [ ] 8.1 执行全量同步，验证 MySQL 数据正确性（节点 + 关联表）
- [ ] 8.2 联调教材树浏览功能（与前端）
- [ ] 8.3 联调知识点详情展示（验证 2 层父级返回）
- [ ] 8.4 联调年级知识体系页面（与前端）
- [ ] 8.5 联调年级统计卡片（与前端）
- [ ] 8.6 端到端流程测试：选择学科→选择教材→浏览章节→查看知识点详情→切换年级知识体系

<!-- yuque-meta: {"repo_id": "zhangmin-jrrer/iu9s4m", "product_dir": "知识图谱页面化", "product_uuid": "3ldLJrKUsTjFP5pp", "change_dir_uuid": "hORjXzINaEuK18r1", "tasks_doc_id": 265947003, "design_doc_id": 265946919, "api_doc_id": 265946772} -->
