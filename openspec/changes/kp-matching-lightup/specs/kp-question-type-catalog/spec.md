# kp-question-type-catalog 能力规格

从答疑数据里沉淀"知识点的题型库"：个体派生观测 → 共现聚合 → 审核稳定，业务隔离，权威图谱零写入。

## ADDED Requirements

### Requirement: 个体派生观测可修正

`t_kp_derived_obs` 的每条观测 SHALL 记录 source（llm/mirror/catalog/curated/student_vote）、confidence、status（NEW/WEAK/RESOLVED/CONFLICTED/READJUDICATED/HUMAN_REVIEW），且 SHALL 允许被维护闭环或人工更新（可修正、可溯源）。

#### Scenario: 观测被修正
- **WHEN** 维护闭环将某观测从「二元一次方程组」重判为「假设法」
- **THEN** 该行 kp_uri 更新为假设法 URI，status 置 READJUDICATED，保留首次记录时间

### Requirement: 共现聚合进候选题型库

当同一 `topic_label` 的去重学生数 ≥ 阈值（默认 3）且总命中 ≥ 5 时，系统 SHALL 在 `t_kp_question_type` 创建 CANDIDATE 条目，并按 kp 拆分写入 `t_kp_question_type_kp` 年级分布桶（各带 hit_students/hit_count/ratio）。阈值 SHALL 可配置。学生票（source=student_vote）SHALL 与其他观测同等计入聚合统计，仅当同一方向 ≥3 名去重学生一致时才进候选（票数稀释防恶意）。

#### Scenario: 题型达到候选阈值
- **WHEN** 「鸡兔同笼」被 ≥3 名不同学生命中且总命中 ≥5
- **THEN** 题型库生成 CANDIDATE 条目，并写入 kp 分布（假设法、二元一次方程组各一桶）

#### Scenario: 未达阈值不聚合
- **WHEN** 「牛吃草」仅 1 名学生命中 1 次
- **THEN** 题型库不创建条目，仅保留个体观测

### Requirement: 审核升级为稳定题型

CANDIDATE 条目在去重学生数 ≥ 10 且近 30 天仍增长时 SHALL 可被审核升为 STABLE（"知识点的题型"），审核可补 definition。STABLE 条目的 kp 分布 SHALL 作为解析先验被复用。

#### Scenario: 稳定题型进入解析先验
- **WHEN** 「鸡兔同笼」升级为 STABLE 且七年级学生再次解析该 label
- **THEN** 解析管线按该条目的七年级分布桶返回「二元一次方程组」先验

### Requirement: 权威图谱零写入

派生层（个体观测、题型库）SHALL 只存 MySQL `ai_edu_learning`，SHALL NOT 向 Neo4j 或 kg-sync 镜像写入任何节点/边/行。权威图谱保持只读，派生层仅以 `kp_uri` 借用其结构。

#### Scenario: 聚合不写权威图
- **WHEN** 题型库创建/更新任何条目或分布桶
- **THEN** Neo4j 与 `t_kg_knowledge_point` 均无新增/修改

### Requirement: 自动维护闭环

系统 SHALL 以周期任务执行维护：扫描冲突/低置信/分布异常观测 → 用「年级锚 + 题型库先验 + LLM」重判 → 仅对高置信重判自动更新（观测 + 题型库统计 + 先验漂移）；低置信或仍歧义 SHALL 置 HUMAN_REVIEW 进入人工待确认队列。错误信号 SHALL 自动来自：decide 诊断与观测冲突、掌握度矛盾、年级分布异常、低置信、做题结果矛盾（obs 归到某 kp 但该生用另一知识点解对了同类题）。

#### Scenario: 冲突观测被重判
- **WHEN** decide 诊断学生卡在假设法，但该观测记了二元一次方程组（CONFLICTED）
- **THEN** 维护任务重判后高置信则更新为假设法并回流题型库统计，低置信则置 HUMAN_REVIEW

#### Scenario: 修正回流先验
- **WHEN** 一批观测从二元一次方程组修正为假设法
- **THEN** 题型库「鸡兔同笼」的假设法分布桶 hit_students 增加，后续解析先验随之偏向假设法
