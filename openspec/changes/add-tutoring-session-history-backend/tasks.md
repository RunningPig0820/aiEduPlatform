## 1. DDL 与域模型

- [x] 1.1 Flyway `V13__alter_t_tutoring_session_add_title.sql`：`ALTER TABLE t_tutoring_session ADD COLUMN title VARCHAR(255) NULL`
- [x] 1.2 `TutoringChatMessage` 增加 7 个可空 meta 字段（type/denied/decide_reason/round/question_kps/eval/status，`@JsonProperty` snake_case）
- [x] 1.3 `TutoringSession` 域实体加 `title`（restore 参数 + 生成入口）；`TutoringSessionPo` 加 `title` 列映射与 from/toEntity 转换

## 2. 消息 meta 填充

- [x] 2.1 `TutoringAppService.buildStream.doOnComplete`（行 616）AI 消息 append 时填 meta：type=allowedType、denied=guard 拒绝时原始类型、decide_reason=action.reason、round=session.roundCount、question_kps=action.questionKps、eval=action.eval、status=session.status
- [x] 2.2 验证：append 后 `archiveTranscript` 重写 COS 消息含 meta；`TutoringSessionCacheImpl` Redis 往返序列化不丢字段

## 3. 列表 / 删除 / 标题

- [x] 3.1 `TutoringSessionMapper` 加 `selectListByStudentId`（全状态 + `is_deleted=false` + `ORDER BY updated_at DESC`）+ 软删方法（`deleteById` 逻辑删或显式 `UPDATE is_deleted=1`）
- [x] 3.2 `TutoringSessionRepository`/Impl 加 `findListByStudentId` 与软删封装
- [x] 3.3 新增列表项 DTO `TutoringSessionListItemDTO`（sessionId/title/status/subject/questionType/roundCount/updatedAt/archivedAt）
- [x] 3.4 `TutoringAppService.listSessions(studentId)`：按 studentId 查列表 → 列表项 DTO
- [x] 3.5 `TutoringAppService.deleteSession(studentId, sessionId)`：`loadSession` 归属校验 → 软删 → `sessionCache.clear` → 返回
- [x] 3.6 `TutoringAppService.start()`：首条用户消息生成 title（前 ~30 字，图片题兜底）并随会话落库
- [x] 3.7 `TutoringController`：`GET /sessions`、`DELETE /sessions/{id}`（均 `TutoringAuth.requireStudent`）

## 4. 验证与契约确认

- [x] 4.1 单测：AI 消息 meta 填充与序列化、列表（全状态/排除软删/按用户隔离/倒序）、删除（软删+Redis 清+归属校验+越权拒绝）、title 生成
- [x] 4.2 Python 契约验证（R1）：确认加 meta 后的 `DecideContext.history` 项 Python Pydantic 容忍（未开 `extra="forbid"`），decide 不中断
- [x] 4.3 契约确认：`eval.exerciseComplete` 命名（R2）与前端对齐；列表项 `sessionId` 字段名与前端 `listSessions` 对齐
- [x] 4.4 集成验证：新会话一轮 → COS transcript 含 meta；列表/删除 E2E；历史工作流复原与 live 一致
  - [x] 4.4a H2 自测（本仓）：`TutoringSessionRepositoryIntegrationTest`（7 例）——save insert/update、findById 聚合复原（title/计数/状态）、列表全状态+排除软删+按用户隔离+倒序、softDelete→is_deleted=1 各处不可见、updateTranscriptUrl、findActiveByStudentId。新增 `schema-learning.sql` + `TutoringInfrastructureConfig`（@DS 需 `AopAutoConfiguration`）
  - [x] 4.4b live 环境残留：真实 COS transcript 含 meta、列表/删除 HTTP E2E、历史工作流复原与 live 一致（需起 MySQL/Redis/COS/Python）
