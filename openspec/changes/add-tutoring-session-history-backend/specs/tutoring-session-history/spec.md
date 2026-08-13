## ADDED Requirements

### Requirement: 会话列表接口（按用户，全状态）

系统 MUST 提供 `GET /api/tutoring/sessions`，从鉴权上下文取当前学生 id，返回该学生**全部状态**（ACTIVE/ARCHIVED/TERMINATED，不含已删除）的会话列表，按 `updated_at` 倒序。列表项 MUST 包含 `sessionId`、`title`、`status`、`subject`、`questionType`、`roundCount`、`updatedAt`、`archivedAt`。

#### Scenario: 按用户取全状态列表

- **WHEN** 用户 A 发起两个会话（一个进行中、一个已归档）
- **THEN** `GET /sessions` 返回 2 条，已归档会话在列，最近更新的在前；不含其他用户的会话

#### Scenario: 已删除会话不返回

- **WHEN** 会话被软删除（is_deleted=1）
- **THEN** 列表不再返回该会话

### Requirement: 删除历史会话（软删除）

系统 MUST 提供 `DELETE /api/tutoring/sessions/{id}`：将 `t_tutoring_session` 行标记 `is_deleted=1`（软删除，数据可恢复），清除该会话 Redis 缓存（`messages:{id}` + `session:{id}`），COS 归档 transcript 与题目图片 MUST 保留。删除前 MUST 校验会话归属当前用户，`student_id` 不匹配返回 404。

#### Scenario: 删除会话后列表消失、数据保留

- **WHEN** 用户删除一个会话
- **THEN** 列表不再返回该会话；MySQL 行 `is_deleted=1`，COS transcript/图片仍存在，Redis 缓存被清除

#### Scenario: 软删会话详情不可见

- **WHEN** 会话已软删除
- **THEN** 详情 `GET /sessions/{id}` 按不存在处理（404）

#### Scenario: 越权删除被拒

- **WHEN** 用户 B 尝试删除用户 A 的会话
- **THEN** 返回 404，会话数据不受影响

### Requirement: 消息携带工作流 meta（随 Redis/COS 复原）

系统 MUST 让 `TutoringChatMessage` 携带 7 个可空 meta 字段（`type/denied/decide_reason/round/question_kps/eval/status`，snake_case 序列化）。AI 消息 append 时从 ActionMeta（含生效类型/护栏结果/会话状态）填充；用户消息 meta 为空。填充后的消息经 Redis 热存与 COS transcript 整写持久化，供前端从 COS 复原历史 ①-⑥ 工作流快照。

#### Scenario: AI 消息携带完整 meta

- **WHEN** 一轮问答完成，AI 回复 append 并整写 COS
- **THEN** AI 消息含 `type`（护栏生效类型）、`denied`（护栏拒绝时原始类型）、`decide_reason`、`round`、`question_kps`、`eval`、`status`（会话状态），与 live SSE meta 事件逐字段一致

#### Scenario: 护栏降级轮 meta 记录生效类型

- **WHEN** 学生请求答案被护栏降级（reveal → approach）
- **THEN** AI 消息 `type=approach`、`denied=reveal`，历史复原与 live 渲染一致

#### Scenario: 用户消息 meta 为空

- **WHEN** 学生消息 append
- **THEN** 该消息 7 个 meta 字段均为空，不携带工作流快照

#### Scenario: generate thinking 随 COS 复原

- **WHEN** AI 回复含思考过程（thinking 非空）
- **THEN** COS transcript 消息含完整 thinking 字段，历史复原可展示思考过程

### Requirement: 会话标题生成

系统 MUST 在 `t_tutoring_session` 提供 `title` 列，并在会话创建时从首条用户消息内容生成（取前约 30 字）；首条消息无正文（图片题）时使用兜底标题。存量会话 title 可为空。

#### Scenario: 文字首条消息生成标题

- **WHEN** 学生以文字发起会话
- **THEN** 会话行 `title` 为消息内容前 ~30 字，历史列表展示该标题

#### Scenario: 图片首条消息兜底标题

- **WHEN** 学生以纯图片发起会话（无正文）
- **THEN** 会话行 `title` 使用兜底值（subject+questionType 或「图片题目」），不阻断创建
