# kp-question-analysis 能力规格

智能练习下题型分析的后端支撑：贴题/拍题 → 题目理解（题型名）→ 关联知识点清单；学生确认关联（复用 vote）；纯分析不写观测；题型库浏览干净无重复。

## ADDED Requirements

### Requirement: 贴题分析题型与知识点

系统 SHALL 提供 `POST /api/kp/analyze-question { text }`，输入题目文本，经题目理解（LLM 识别候选题型名）与解析管线，返回题型及关联知识点清单 `{ topicLabel, status, confidence, knowledgePoints: [{kpUri, kpLabel, gradeRange, ratio}], candidates }`，体现「这道题考了哪些知识点」。

#### Scenario: 题型库命中返回全分布
- **WHEN** 题目文本经题目理解识别出题型名，且该题型名命中题型库（canonical 或别名）
- **THEN** 返回 status=RESOLVED，knowledgePoints 为该题型全部关联知识点分布（kpLabel 从 kg 镜像按 kpUri 反查，含年级分布 gradeRange 与占比 ratio）

#### Scenario: 题型库未命中走解析管线
- **WHEN** 识别出的题型名未命中题型库
- **THEN** 复用解析管线只读解析：命中返回单条关联知识点（ratio=1）；未命中返回 PENDING

#### Scenario: 拍题分析
- **WHEN** 学生上传题目照片
- **THEN** 先经 `POST /api/tutoring/ocr` 识别为文本，再触发 analyze-question

### Requirement: 纯分析不写观测

analyze-question SHALL 只读不写 `t_kp_derived_obs`（浏览行为不产生学习信号）；仅学生确认（vote）SHALL 才写观测。

#### Scenario: 分析不产生观测
- **WHEN** 学生调用 analyze-question 分析题目
- **THEN** `t_kp_derived_obs` 无新增行

### Requirement: 低置信挂起携带澄清候选

题目理解失败或解析 PENDING 时，analyze-question SHALL 返回 PENDING（不报错），携带澄清候选 `candidates`；无候选时 SHALL 返回空列表。

#### Scenario: 题目理解失败降级
- **WHEN** LLM 题目理解不可用或未识别出题型名
- **THEN** 返回 status=PENDING，knowledgePoints 为空，candidates 为空或解析候选，不抛错

#### Scenario: 多候选歧义挂起
- **WHEN** 候选题型名多个且均未命中题型库，解析 PENDING
- **THEN** 返回 PENDING + candidates（含首个候选的澄清候选列表）

### Requirement: 学生确认关联（复用 vote）

题型分析结果 SHALL 支持学生点「确认关联」，复用 `POST /api/kp/vote { topicLabel, selectedLabel }` 落 source=student_vote 个人观测（不改全局），跨学生达阈值后由聚合任务沉淀题型库。

#### Scenario: 确认关联落票
- **WHEN** 学生点某关联知识点的「确认关联」
- **THEN** 调 vote 落 STUDENT_VOTE 观测，成功后该条标「已确认」，并提示「已记录，将参与题型整理」

#### Scenario: vote 失败不静默
- **WHEN** vote 返回错误（如候选知识点不在镜像）
- **THEN** 提示失败原因，状态复位可重试，不静默

### Requirement: 题型库浏览干净

题型分析视图的题型库浏览 SHALL 复用 `GET /api/kp/question-types` 与 `GET /api/kp/question-types/{id}/knowledge-points`，且相似题型名变体已合并到 canonical（别名），学生看到的题型库 SHALL 无重复条目。

#### Scenario: 变体题型已合并
- **WHEN** 题型库存在 canonical「鸡兔同笼问题」且别名「鸡兔同笼」
- **THEN** 题型库浏览仅展示 canonical 一条，变体在解析/确认时按别名命中同一条目

### Requirement: 学生端不暴露管理功能

analyze-question 与题型库浏览 SHALL NOT 暴露管理端审核/全局修改能力（管理端审核独立功能，本期不做）。

#### Scenario: 与管理端隔离
- **WHEN** 学生访问题型分析
- **THEN** 仅展示单题分析 + 题型库浏览 + 个人确认，不出现审核/全局维护入口
