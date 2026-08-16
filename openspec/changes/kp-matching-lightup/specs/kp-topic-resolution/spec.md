# kp-topic-resolution 能力规格

题型/知识点 label → TextbookKP URI 的解析管线，供答疑内嵌与外露接口复用。

## ADDED Requirements

### Requirement: 解析管线按序命中

解析器 SHALL 按顺序尝试：① kg-sync 镜像精确/LIKE 匹配 → ② 题型库按学生年级匹配 → ③ LLM 消歧（给定候选 label 列表选最匹配 + 置信度）→ ④ 低置信/歧义先学生澄清（可选）→ ⑤ 学生跳过或仍歧义则挂起。镜像命中 SHALL 优先于 LLM 调用。

#### Scenario: 镜像精确命中
- **WHEN** decide 输出 label「二元一次方程组」且 kg-sync 镜像存在同 label 节点
- **THEN** 解析器直接返回该节点 URI，不调用 LLM

#### Scenario: 镜像未命中但题型库有年级匹配
- **WHEN** label「鸡兔同笼」不在镜像，但题型库记录该题型分布：假设法(4-6 年级,38)、二元一次方程组(7-8 年级,21)，且当前学生为七年级
- **THEN** 解析器返回「二元一次方程组」URI，confidence 按占比加权

#### Scenario: 均未命中则走 LLM 消歧
- **WHEN** label 未命中镜像与题型库，解析器走两段式 LLM 消歧（生成候选名 + 镜像校验，见下）
- **THEN** LLM 返回最匹配 kp + 置信度，解析器按置信度决定 RESOLVED 或 PENDING

### Requirement: 冷启动 LLM 消歧（生成候选名 + 镜像校验）

题型库无先验且镜像名 LIKE 无召回时，解析器 SHALL 走两段式 LLM 消歧：① LLM 生成候选知识点名（给定题型 label + 年级上下文）；② Java 用镜像 exact/LIKE 校验候选名，命中才保留。SHALL NOT 返回镜像不存在的 kp（LLM 只生成 name 候选，最终 kp 必经镜像校验）。校验后：单候选 SHALL 直接 RESOLVED（冷启动标 WEAK）；多候选 SHALL 返回 PENDING 携带候选；零命中 SHALL 返回 PENDING 无候选。

#### Scenario: 题型名冷启动经 LLM 生成候选并校验
- **WHEN** label「鸡兔同笼」未命中镜像与题型库，LLM 生成候选名「二元一次方程组」「假设法」，镜像校验「二元一次方程组」命中、「假设法」未命中
- **THEN** 保留「二元一次方程组」作候选；单候选 RESOLVED 标 WEAK

#### Scenario: LLM 生成候选名全部未命中镜像
- **WHEN** LLM 生成的候选名镜像校验全不命中
- **THEN** 返回 PENDING 无候选，不返回任何镜像不存在的 kp

### Requirement: 年级锚参与解析

同一题型对不同年级的教材知识点归属不同，解析器 SHALL 以学生年级为主信号（强先验），图谱 URI 内嵌年级可用于候选 kp 的距离排序。年级不可得时 SHALL 降级为纯 LLM 消歧。

#### Scenario: 同题型不同年级归不同 kp
- **WHEN** 四年级学生遇「鸡兔同笼」且候选含假设法与二元一次方程组
- **THEN** 解析器优先返回「假设法」URI

#### Scenario: 跨年级薄弱可覆盖年级锚
- **WHEN** 七年级学生在鸡兔同笼题上持续表现 struggling，decide 诊断明确指向假设法逻辑
- **THEN** LLM 上下文 + 置信度覆盖年级锚，返回「假设法」URI（年级是强先验而非硬规则）

### Requirement: 低置信先澄清后挂起

解析结果置信度低于阈值时，解析器 SHALL 先给学生可选澄清（见「学生意图澄清」）；学生跳过或澄清后仍歧义时，SHALL 将 label 记为 PENDING 挂起，不产出可点亮掌握的 URI，同时记录观测供后续审核。

#### Scenario: 低置信先澄清后挂起
- **WHEN** LLM 消歧置信度 < 60
- **THEN** 解析器先呈现"你想学哪个"可选澄清；学生跳过则返回 PENDING 状态，不写入掌握度，观测落 `t_kp_derived_obs`(status=PENDING)

### Requirement: 学生意图澄清（可选）

解析低置信/歧义时，解析器 SHALL 向学生呈现可选的意图澄清——以学科概念选项（如「假设法 / 二元一次方程组 / 跳过」）询问"你想学哪个"，SHALL NOT 暴露内部 kp_uri。学生选择 SHALL 落一条 `t_kp_derived_obs(source=student_vote, confidence=中等)`；跳过 SHALL 视为弃权不产生信号。

#### Scenario: 学生澄清落票
- **WHEN** 七年级学生遇「鸡兔同笼」低置信，澄清题呈现「假设法 / 二元一次方程组 / 跳过」
- **THEN** 学生选「二元一次方程组」→ 落观测 source=student_vote，不暴露 kp_uri

#### Scenario: 跳过弃权
- **WHEN** 学生选「跳过」
- **THEN** 不产生 student_vote 观测，label 转 PENDING 挂起

### Requirement: 冷启动弱化（首条不直接点亮）

题型库无先验支撑时（冷启动首条），LLM 消歧结果 SHALL 标记 `status=WEAK`，不直接点亮、不直接进题型库先验；满足任一第二独立信号（同生做题结果佐证 / 第二名不同学生共现同 kp / 学生澄清投票达标）才转 RESOLVED。

#### Scenario: 冷启动首条弱化
- **WHEN** 题型库空，「鸡兔同笼」首条 LLM 消歧高置信命中假设法
- **THEN** 观测 status=WEAK，不点亮、不进题型库先验

#### Scenario: 第二信号转确定
- **WHEN** 第二名学生对「鸡兔同笼」也消歧到假设法
- **THEN** 该观测转 RESOLVED，进入题型库聚合

### Requirement: 解析结果落个体派生观测

每次成功/挂起的解析 SHALL 写入 `t_kp_derived_obs`（student_id + topic_label + kp_uri + confidence + source + status + 解析时年级快照）。同生 + 同题型 + 同 kp_uri 已存在时 SHALL 仅递增 `occurrence_count`，不重复建行。

#### Scenario: 同生再次遇到同题型
- **WHEN** 学生 A 第二次在答疑中产生「鸡兔同笼 → 二元一次方程组」解析
- **THEN** 不新增行，原行 `occurrence_count` +1

### Requirement: 解析接口外露

系统 SHALL 提供 `POST /api/kp/resolve`，输入 label（可带 student_grade），输出 `{uri, label, confidence, status}`，复用同一解析管线，供诊断与管理工具调用。

#### Scenario: 外露解析
- **WHEN** 调用方 POST `/api/kp/resolve` 携带 `{"label":"鸡兔同笼","student_grade":7}`
- **THEN** 返回解析结果，含 uri/confidence/status，且结果同样落观测
