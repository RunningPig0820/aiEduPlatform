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

- [ ] 5.1 `MasteryItemDTO` 增加 `status`(RESOLVED/PENDING) + `confidence`，`StudentMasteryController` 返回
- [ ] 5.2 新增 `POST /api/kp/resolve`（label + student_grade → {uri, label, confidence, status}）
- [ ] 5.3 新增 `GET /api/kg/aliases/pending` + `POST /api/kg/aliases/pending/{id}/confirm`（ADMIN/TEACHER）

## 6. 前端：学生端图谱点亮

- [ ] 6.1 前端调用 `getStudentMastery`（当前定义未调用），叠加到图谱节点
- [ ] 6.2 `KnowledgeGraph` 组件支持掌握度档位着色（绿/黄/红 + 疑似虚线 + 待确认角标）
- [ ] 6.3 学生端图谱路由 + 页面（复用组件，与 admin 图谱页隔离）
- [ ] 6.4 答疑界面低置信时渲染"你想学哪个"可选澄清（A/B 概念选项 + 跳过）

## 7. 测试

- [ ] 7.1 解析管线单测（镜像命中/题型库年级匹配/LLM 消歧/低置信挂起）
- [ ] 7.2 观测去重计数 + 聚合阈值 + 维护重判单测
- [ ] 7.3 接口测试（mastery 增强字段、resolve、pending 确认、权限）
- [ ] 7.4 权威图零写入断言（聚合/维护后 Neo4j 与镜像无变更）
