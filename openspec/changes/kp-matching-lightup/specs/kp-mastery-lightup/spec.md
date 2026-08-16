# kp-mastery-lightup 能力规格

掌握度主体翻转（题型直接观测、知识点派生）后的图谱点亮：题型掌握度落库、知识点派生覆盖度、学生端图谱页、挂起审核管理面。

## ADDED Requirements

### Requirement: 题型掌握度落库

掌握度信号 SHALL 以题型为粒度落库到 `t_student_topic_mastery`（`student_id + topic_key` 唯一，`topic_key` = 归一化题型名）。同生再次遇到同题型 SHALL 取 max 单调不减（同旧 KP 掌握度策略）。`masteryLevel` SHALL 为四档（0/25/50/75），并携带 `status`(RESOLVED/PENDING) 与 `confidence`。

#### Scenario: 题型掌握度落库
- **WHEN** 学生答疑产生「鸡兔同笼」掌握度信号（mastered=75）
- **THEN** `t_student_topic_mastery` 按 `topic_key`(归一化"鸡兔同笼") UPSERT，masteryLevel=75，status=RESOLVED

#### Scenario: 同题型取 max 单调不减
- **WHEN** 该生再次遇「鸡兔同笼」信号为 practicing=50
- **THEN** 不降分，masteryLevel 保持 75

### Requirement: 题型掌握度查询接口

`GET /api/students/{id}/mastery` SHALL 返回该生全部题型掌握度，每项含 `topicKey`、`topicLabel`、`masteryLevel`、`status`、`confidence`、`updatedAt`。路径 `studentId` 必须等于会话 userId，否则越权。

#### Scenario: 返回题型掌握度
- **WHEN** 学生查询自己的掌握度
- **THEN** 每项为题型粒度（topicKey/topicLabel + 四档 masteryLevel + status/confidence）

#### Scenario: 越权查询
- **WHEN** 路径 studentId ≠ 会话 userId
- **THEN** 返回权限不足

### Requirement: 知识点派生覆盖度计算

知识点掌握度 SHALL NOT 直接观测，改为从题型掌握度派生：`coverage(kp) = clamp(Σ_{题型→kp} (题型掌握度 × ratio), 0, 75)`。ratio 来源：优先 `t_kp_question_type_kp.ratio`（聚合后跨学生分布）；题型未聚合时用 `t_kp_derived_obs` 该生单观测（ratio 隐式 1）。无题型映射的知识点 SHALL 回退旧 `t_student_kp_mastery`（过渡期）。

#### Scenario: 聚合题型按 ratio 派生
- **WHEN** 学生「鸡兔同笼」掌握度 75，题型库「鸡兔同笼→二元一次方程组」ratio=0.8
- **THEN** 「二元一次方程组」coverage = 75 × 0.8 = 60，masteryLevel 离散为 intermediate(50)

#### Scenario: 未聚合题型按单观测派生
- **WHEN** 学生「相遇问题」掌握度 50，题型库尚无该题型，obs 记录相遇问题→相遇问题 kp 单观测
- **THEN** 该 kp coverage = 50 × 1 = 50

#### Scenario: 无题型映射回退旧 KP 掌握度
- **WHEN** 某知识点仅有旧 `t_student_kp_mastery` 记录、无任何题型映射
- **THEN** coverage 回退取旧 KP masteryLevel

### Requirement: 知识点派生覆盖度查询接口

系统 SHALL 提供 `GET /api/students/{id}/kp-coverage`，返回该生知识点派生覆盖度，每项含 `kpUri`、`kpLabel`、`coverage`(连续 0-75)、`masteryLevel`(离散四档)、`status`、`confidence`、`stage`、`chapterLabel`、`sectionLabel`。`kpLabel` 从 kg 镜像反查，`stage`/`chapterLabel`/`sectionLabel` 从 kp 归属教材反查（无归属为 null）。

#### Scenario: 返回知识点覆盖度
- **WHEN** 学生查询知识点派生覆盖度
- **THEN** 每项含 coverage（连续）与 masteryLevel（离散）及学段/章节归属

#### Scenario: 无归属知识点 stage 为空
- **WHEN** 某知识点未挂到小节/章节
- **THEN** 该项 stage=null，kpUri/kpLabel/coverage 仍正常返回

### Requirement: 学生端图谱按派生覆盖度点亮

学生端知识图谱页 SHALL 按 `node.id`(URI) 与覆盖度 `kpUri` 匹配着色：masteryLevel≥75 绿 / 50 黄 / 25 红；无覆盖度节点中性灰。学生端图谱页 SHALL 复用现有图谱组件，SHALL NOT 影响 admin 图谱页。

#### Scenario: 掌握节点点亮
- **WHEN** 学生在七年级图谱查看，其「二元一次方程组」覆盖度离散档 75
- **THEN** 该节点以绿色"掌握"档位渲染

#### Scenario: 学生端与管理员端隔离
- **WHEN** 学生访问知识图谱路由
- **THEN** 仅看到点亮视图，不暴露同步管理/系统统计等管理功能

### Requirement: 疑似薄弱节点可见不点亮

对解析 PENDING 或低置信的题型/知识点，图谱 SHALL 以"疑似待确认"视觉渲染（虚线 + 待确认角标），SHALL NOT 将其写入掌握度或按确认薄弱（红）着色。

#### Scenario: 疑似节点渲染
- **WHEN** 学生答疑产生「鸡兔同笼」PENDING 观测且该题型映射的知识点存在于当前图谱
- **THEN** 对应节点以虚线 + 待确认角标渲染，区别于确认薄弱（红）

### Requirement: 挂起审核管理面

系统 SHALL 提供管理接口：`GET /api/kg/aliases/pending` 列出挂起观测（HUMAN_REVIEW/PENDING），`POST /api/kg/aliases/pending/{id}/confirm` 确认其 kp_uri。确认后 SHALL 更新观测状态并回流题型库统计。仅 ADMIN/TEACHER 可访问。

#### Scenario: 人工确认挂起题型
- **WHEN** 管理员将挂起的「鸡兔同笼」确认归属假设法 URI
- **THEN** 观测转 RESOLVED，题型库假设法分布桶命中数增加，后续解析命中假设法
