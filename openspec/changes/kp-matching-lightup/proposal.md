## Why

AI 答疑已经能识别学生薄弱的知识点（decide 输出 `question_kps` / `mastery_signals`，是自由文本标签），但目前 label→URI 解析只做「精确匹配 → LIKE 模糊 → 未命中即丢弃」(`TutoringKpResolverImpl`)。大量真实题型（如"鸡兔同笼"）是题目考点而非教材知识点，落不到图谱，导致掌握度落库、图谱点亮整条链路断裂——学生答疑产生的数据无法可视化，答疑与知识图谱"两张皮"。

需要建立一个**派生层**：① 把 AI 题型解析到教材知识点 URI（可跨年级、可纠错）；② 从答疑数据里**共现沉淀**出"知识点的题型库"（业务隔离、零污染权威图谱）；③ 把掌握度在知识图谱上**点亮**，形成"答疑 → 掌握度 → 图谱点亮"的完整闭环。

## What Changes

- **解析管线升级**（取代现 `TutoringKpResolverImpl` 的精确→LIKE→丢弃）：`年级锚 + 别名表 + LLM 消歧 + 统计先验`，低置信/歧义先给学生可选澄清，跳过才进挂起队列。
- **新增个体派生表** `t_kp_derived_obs`：每生每题型 → URI 的观测记录，可修正、可溯源、同生同题型去重计数。
- **新增聚合题型库** `t_kp_question_type`：跨学生共现沉淀"知识点的题型"，带**年级分布**，CANDIDATE→审核→STABLE。
- **新增自动维护闭环**：冲突检测（decide 诊断 vs 派生标注 / 掌握度矛盾 / 年级分布异常）→ 自动重判 → 修正回流为先验 → 全体学生受益。
- **信任模型**：LLM 主裁判（默认解析/重判）+ 学生意图信号（低置信"你想学哪个"可选澄清，`source=student_vote`）+ 人工边界仲裁（仅极少数边界，找懂学科的人）；LLM 判断强制接客观信号校准，防自证循环。
- **掌握度 DTO 增强**：`MasteryItemDTO` 增加 `status`(RESOLVED/PENDING) 与 `confidence`，前端可渲染"疑似待确认"态。
- **新增接口**：`POST /api/kp/resolve`（解析外露）、`GET/POST /api/kg/aliases/pending`（挂起审核）。
- **学生端图谱页**：复用现有 `KnowledgeGraph` 组件开学生路由，按 `node.id(uri)` 叠加掌握度点亮（绿/黄/红 + 疑似虚线态）。
- **权威图谱零写入**：Neo4j 与 kg-sync MySQL 镜像保持只读，派生层只借 `kp_uri` 走权威图结构。

## Capabilities

### New Capabilities

- `kp-topic-resolution`: 题型/知识点 label → TextbookKP URI 的解析管线（年级锚、别名表、LLM 消歧、统计先验、挂起队列），供答疑内嵌与外露接口复用。
- `kp-question-type-catalog`: 个体派生观测 + 聚合题型库 + 自动维护闭环，从答疑数据里沉淀"知识点的题型"，业务隔离。
- `kp-mastery-lightup`: 图谱掌握度点亮：掌握度数据增强、学生端图谱页、挂起审核管理面。

### Modified Capabilities

<!-- 现有 spec 中无掌握度/答疑 spec；student_grade 契约变更属于设计决策，不涉及既有 spec 行为变更 -->

## Impact

- **Backend**（`ai-edu-backend`）：
  - `TutoringKpResolverImpl` 重写为管线式解析（infrastructure + domain 端口升级）。
  - 新增领域模型 + 2 张表（`t_kp_derived_obs`、`t_kp_question_type`）+ Flyway 迁移（learning 库）。
  - 掌握度 DTO 增加 `status`/`confidence`（`MasteryItemDTO`）。
  - 新增 `KpResolutionController` / 审核接口；`DecideRequest` 增加 `student_grade`（开放决策，见 design）。
  - 维护闭环为周期任务（Spring `@Scheduled`）。
- **Frontend**（`aiEduPlatformFront`）：
  - 学生端图谱页（新路由）+ `KnowledgeGraph` 组件复用 + 消费 `getStudentMastery`（当前定义了但无人调用）。
  - 答疑界面低置信时渲染"你想学哪个"可选澄清（A/B 概念选项 + 跳过）。
- **Database**：`ai_edu_learning` 新增 2 表；**Neo4j / kg-sync 镜像零写入**。
- **依赖**：LLM 消歧复用现有 `llm-gateway` / Python 能力；学生年级依赖组织系统（开放决策）。
