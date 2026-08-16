# kp-matching-lightup 实施任务

## 1. 数据层（Flyway + 领域模型 + 仓储）

- [x] 1.1 新增 Flyway 迁移（learning 库）：`t_kp_derived_obs`、`t_kp_question_type`、`t_kp_question_type_kp`，字段见 design §3（含 UNIQUE 约束、索引）
- [x] 1.2 learning 领域新增实体/值对象：`DerivedKpObs`、`QuestionType`、`QuestionTypeKp`（DDD 4 层，domain 纯 JPA）
- [x] 1.3 learning 领域新增仓储接口 + infra 层 MyBatis-Plus 实现（`DerivedKpObsRepository` 等）
- [x] 1.4 配置聚合阈值（`application.yml`：`ai-edu.kp.aggregation.candidate-students=3`、`candidate-hits=5`、`stable-students=10`、`confidence-threshold=60`）

## 2. 解析管线升级

- [x] 2.1 重写 `TutoringKpResolverImpl` 为管线：① 镜像精确/LIKE（保留现有）→ ② 题型库年级匹配 → ③ LLM 消歧 → ④ 低置信学生澄清（可选）→ ⑤ PENDING
- [x] 2.2 解析时获取学生年级（组织系统 学生→班级→年级；不可得降级纯 LLM 消歧）
- [x] 2.3 LLM 消歧接入：给定 label + 候选 kp label 列表（保证正确答案在候选内）→ 返回 kp + confidence（复用 llm-gateway，见 design Open Q2）
- [x] 2.4 解析结果写 `t_kp_derived_obs`（同生+同题型+同 kp 去重递增 occurrence_count）
- [x] 2.5 学生澄清交互：低置信/歧义生成学科概念选项（不暴露 kp_uri），学生选择落 `source=student_vote` 观测，跳过弃权
- [x] 2.6 冷启动弱化：题型库无先验时首条 LLM 消歧标 `WEAK`，不点亮；第二独立信号（做题结果/共现/学生投票）才转 RESOLVED
- [x] 2.7 obs 接入答疑主流程：`applyMasteryAndErrors` 升级 `resolve(label, studentId)`，掌握度取 status/confidence（不再硬编码 RESOLVED）

## 3. 题型库聚合（离线批处理 · 大数据归宿 → `batch` 包）

- [x] 3.0 离线逻辑剥离：聚合逻辑独立到 `application.service.batch` 包 + `package-info.java` 标注「逻辑归宿=大数据平台，当前后端 @Scheduled 过渡实现」（design Decision 11）
- [x] 3.1 聚合任务：扫描 obs → 按 topic_label 聚合并按 kp 拆分年级分布桶 → 达阈值建 CANDIDATE
- [x] 3.2 CANDIDATE 升 STABLE 逻辑（≥10 学生 + 近 30 天增长 + 审核），STABLE 条目可补 definition
- [x] 3.3 题型库分布桶 `ratio` 计算，供解析管线②作先验

## 4. 自动维护闭环（离线批处理 · 大数据归宿 → `batch` 包）

- [x] 4.1 冲突检测：WEAK 第二信号共现转正已实现；decide 诊断冲突/掌握度矛盾/做题结果矛盾 → 打 `CONFLICTED`（后续接入）
- [x] 4.2 周期重判任务（`@Scheduled`）：LLM 重判 → 高置信自动更新（obs 状态）+ 题型库统计回流（多模型交叉后续）
- [x] 4.3 仍歧义 → `HUMAN_REVIEW`（已实现）；掌握度错解析打 `MIGRATED`（后续，需掌握度表 schema 变更）

## 5. 接口层

- [x] 5.1 `MasteryItemDTO` 增加 `status`(RESOLVED/PENDING) + `confidence`，`StudentMasteryController` 返回
- [x] 5.2 新增 `POST /api/kp/resolve`（label + student_grade → {uri, label, confidence, status}）
- [x] 5.3 新增 `GET /api/kg/aliases/pending` + `POST /api/kg/aliases/pending/{id}/confirm`（ADMIN/TEACHER）
- [x] 5.4 新增 `GET /api/students/{id}/pending-kps`（学生端疑似观测清单，PENDING/WEAK，越权校验）

## 6. 前端：学生端图谱点亮

- [ ] 6.1 前端调用 `getStudentMastery`（当前定义未调用），叠加到图谱节点
- [ ] 6.2 `KnowledgeGraph` 组件支持掌握度档位着色（绿/黄/红 + 疑似虚线 + 待确认角标）
- [ ] 6.3 学生端图谱路由 + 页面（复用组件，与 admin 图谱页隔离）
- [ ] 6.4 答疑界面低置信时渲染"你想学哪个"可选澄清（A/B 概念选项 + 跳过）

## 7. 测试

- [x] 7.1 解析管线单测（镜像命中/题型库年级匹配/LLM 消歧/低置信挂起）
- [x] 7.2 观测去重计数 + 聚合阈值 + 维护重判单测
- [x] 7.3 接口测试（mastery 增强字段、resolve、pending 确认、权限）
- [x] 7.4 权威图零写入断言（聚合/维护后 Neo4j 与镜像无变更）

## 8. 前端学习报告数据契约（stage 字段 + 全量知识点分页 + 题型库分页）

- [x] 8.1 [①] `MasteryItemDTO` 增加 `stage`/`chapterLabel`/`sectionLabel`；新增值对象 `KgKpPlacement`（kpUri+stage+chapterLabel+sectionLabel）+ `KgKnowledgePointRepository.findPlacementByUris(List<String>)` 批量反查（Mapper 一条 LEFT JOIN：kp→section→chapter→textbook）
- [x] 8.2 [①] `TutoringAppService.getStudentMastery` 组装时批量反查 stage 填入（复用 8.1 的 findPlacementByUris，避免 N+1）
- [x] 8.3 [②] 新增 `POST /api/kg/knowledge-points`（按 stage 分页列教材知识点，返回 kpUri/kpLabel/stage/chapterLabel/sectionLabel + total/page/size）；Mapper 反向 JOIN（textbook[stage]→chapter→section→kp）+ COUNT
- [x] 8.4 [③a] `QuestionTypeRepository` 增加分页查询 `findPage`；新增 `GET /api/kp/question-types`（page/size，返回 id/topicLabel/status/hitCount + total）
- [x] 8.5 [③b] 新增 `GET /api/kp/question-types/{id}/knowledge-points`：`findByQuestionTypeId` + `kgKnowledgePointRepository.findByUris` 反查 kpLabel，返回 kpUri/kpLabel/gradeRange/ratio/hitCount
- [x] 8.6 [测试] 三接口 + stage 反查的单测/接口测试（分页边界、kp 无归属 stage=null、越权、kpLabel 反查）

## 9. 掌握度主体翻转：题型直接观测 + 知识点派生覆盖度

- [x] 9.1 [领域模型] 新增 `StudentTopicMastery` 实体（student_id + topic_key 唯一、mastery_level 四档、status/confidence）+ `TopicKey` 值对象 + `TopicKeyNormalizer` 归一化工具（全角半角/空白折叠/去末尾语气词标点）；`StudentTopicMasteryRepository` 接口 + `findByStudentId`/`upsert`
- [x] 9.2 [基础设施] `t_student_topic_mastery` Flyway 迁移（learning 库，UNIQUE(student_id, topic_key)）+ MyBatis-Plus 实现 + PO/Mapper；`TopicKeyNormalizer` 实现放 domain（纯函数，可单测）
- [x] 9.3 [应用] `applyMasteryAndErrors` 改写：`MasterySignalItem.kpLabel` 语义翻为题型 label → `topicKey = normalize(label)` 落 `t_student_topic_mastery`（UPSERT 取 max）；`kpResolver.resolve(label)` 仍产出 `kp_uri` 落 `t_kp_derived_obs`（供派生），不再写 `t_student_kp_mastery`
- [x] 9.4 [应用] 新增 `KpCoverageAppService`：计算知识点派生覆盖度 `coverage = clamp(Σ 题型掌握度 × ratio, 0, 75)`；ratio 优先 `t_kp_question_type_kp`、未聚合回退 obs 单观测（ratio=1）、无题型映射回退旧 `t_student_kp_mastery`
- [x] 9.5 [接口] `GET /api/students/{id}/mastery` 改返回题型掌握度（topicKey/topicLabel/masteryLevel/status/confidence/updatedAt）；新增 `GET /api/students/{id}/kp-coverage`（kpUri/kpLabel/coverage/masteryLevel/status/confidence + stage/chapterLabel/sectionLabel，越权校验同 mastery）
- [x] 9.6 [测试] 题型归一化（空白/全角半角/去末尾标点，保留「问题」后缀）、题型掌握度落库取 max、派生覆盖度（聚合 ratio / 未聚合单观测 / 无映射回退旧表 / clamp）、两接口（越权/空列表）

## 10. 冷启动 LLM 消歧 + 离线 LLM 聚合（题型库自我生长）

- [x] 10.1 [在线] `KpLlmDisambiguator` 改两段式：① LLM 生成候选知识点名（给定题型 label + 年级上下文）；② `kgKnowledgePointRepository.findByLabel`/`findByLabelLike` 回镜像校验，命中才保留；单候选→RESOLVED，多候选→PENDING 携带候选，零命中→PENDING 无候选
- [x] 10.2 [在线] `TutoringKpResolverImpl.resolve` ③ 分支不再依赖 `findByLabelLikeList` 预筛候选（改走 10.1 两段式）；冷启动首条仍标 WEAK（Decision 9）
- [x] 10.3 [离线] `KpQuestionTypeAggregationService` 加 LLM 自动关联：达阈值的题型，LLM 输入 obs 共现 `(kp_uri, 命中次数, 年级分布桶)` → 输出 kp 分布 ratio（归一化和=1）→ 建/更新 CANDIDATE
- [x] 10.4 [离线] 冷启动弱化：LLM 关联结果不直接 STABLE，第二独立信号（多生共现 / 投票达标 / 做题结果佐证）才升 STABLE 进解析先验（由既有 `promoteToStable` 门禁保证：聚合只产 CANDIDATE，STABLE 需 ≥10 学生 + 审核）
- [x] 10.5 [测试] 两段式消歧（生成+校验、校验不过丢弃、多候选澄清、LIKE 兜底、LLM 失败降级）、LLM 聚合建候选（归一化、剔除幻觉、失败降级）、WEAK 转正
