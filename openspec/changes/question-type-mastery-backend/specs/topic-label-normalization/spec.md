# topic-label-normalization Spec

题型名动态聚集——字符级规则 + 向量库最近邻（题型名向量）→ canonical；**零锚点动态涌现**（canonical 由第一条相似题创建，无题库也能跑通），掌握表 key 源头不裂行。

## ADDED Requirements

### Requirement: 字符级规则归一

系统 SHALL 在聚集的第一阶段使用字符级规则（零成本）收敛高频变体：前缀/后缀剥离（「解X→X」「求X→X」「X问题→X」）、编辑距离 ≤1、既有 `TopicKeyNormalizer`（全角→半角/空白折叠/去末尾标点）。

#### Scenario: 前缀变体归一
- **WHEN** 题型名为「解一元二次方程」，规则库含「解X→X」
- **THEN** 系统归一为「一元二次方程」

#### Scenario: 近字变体归一
- **WHEN** 题型名与现有 canonical 编辑距离 ≤1
- **THEN** 系统归一到该 canonical

#### Scenario: 后缀变体归一
- **WHEN** 题型名为「相遇问题」，canonical 为「相遇问题」（「问题」为固有部分）
- **THEN** 系统保留「问题」后缀不剥离（剥离会丢题型语义）

### Requirement: 向量库最近邻聚集（题型名向量单信号）

系统 SHALL 在字符规则未命中后使用向量库查最近邻：**题型名向量**（本期只存题型名向量，题目向量不落库——Python 契约对齐 Non-Goals），top-1 相似度 ≥ 高阈值 → 归一到该 canonical 并写别名表；中阈值区间 → 进候选 LLM 仲裁；未命中 → 建新。

#### Scenario: 题型名向量命中归并（近义变体）
- **WHEN** 新题题型名「鸡兔同笼问题」与已有 canonical「鸡兔同笼」题型名向量相似度 ≥ 阈值
- **THEN** 系统归一到「鸡兔同笼」，并写别名表（鸡兔同笼问题 → 鸡兔同笼）

#### Scenario: 题型名向量命中归并（语义同型）
- **WHEN** 新题题面不同但题型名向量与已有 canonical 相似度 ≥ 阈值（汽车轮子版鸡兔同笼）
- **THEN** 系统归一到该 canonical

#### Scenario: 中阈值进候选 LLM 仲裁
- **WHEN** 题型名向量相似度落在中阈值区间（如「相遇问题」vs「行程问题」）
- **THEN** 系统进候选，LLM 仲裁是否同题型，同则归并否则建新

#### Scenario: 相似度不足不聚集
- **WHEN** 新题与最近邻题型名向量相似度 < 阈值
- **THEN** 系统不归并，走建新 canonical 路径

### Requirement: 未命中建新 canonical（无锚点冷启动）

系统 SHALL 在规则与向量最近邻均未命中时，将题型名作为新 canonical 建题型并入库题型名向量，供后续聚集。**无题库/无预置分类时锚点由第一条相似题动态创建。**

#### Scenario: 首题建锚
- **WHEN** 学生第 1 条题无任何近邻（向量桶空）
- **THEN** 系统创建新 canonical（首见题型名），入库题型名向量

#### Scenario: 新题型建库
- **WHEN** 题型名既无字符规则命中、向量库亦无 ≥ 阈值最近邻
- **THEN** 系统创建新 canonical 题型，并 embedding 入库题型名向量

### Requirement: 批量聚集兜底

系统 SHALL 提供**手动触发的批量聚集**（按钮，非定时——面试项目不做 @Scheduled）：扫描题目表未归并/低置信题型名，全量向量聚类补归并，canonical 名以首见名/最高频名兜底（手动触发时 LLM 归纳规范名），重算掌握表聚合。

#### Scenario: 散名批量归并
- **WHEN** 历史题目表存在多个近义散名（「鸡兔同笼/鸡兔同笼问题/假设法」）未归并，管理员点批量聚集
- **THEN** 批量聚类把它们并到同一 canonical，重算掌握表

#### Scenario: canonical 规范名归纳
- **WHEN** 批量聚集完成后组内有多个成员名
- **THEN** 系统以首见名/最高频名兜底，手动触发时用 LLM 归纳规范名

### Requirement: 落库前聚集（掌握表 key 源头归一）

系统 SHALL 在所有题型名入口（`decide` 的 `mastery_signals` label、`analyze-question` 结果）落库前统一过聚集 post-process（动态锚定 canonical），掌握表 key 使用 canonical，源头不裂行。

#### Scenario: decide 信号锚定后落掌握表
- **WHEN** `decide` 输出 `mastery_signals` label「解一元二次方程」，聚集 canonical 为「一元二次方程」
- **THEN** 系统以 canonical「一元二次方程」为掌握表 key 落库，「解一元二次方程」与「一元二次方程」共享同一行掌握度

#### Scenario: 聚集失败兜底
- **WHEN** 向量库不可用且字符规则未命中
- **THEN** 系统回退原样落库（保留原始题型名），不阻塞主链路
