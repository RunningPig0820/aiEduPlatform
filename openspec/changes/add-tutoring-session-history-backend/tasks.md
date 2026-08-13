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

## 5. Bugfix：会话拆分（前后端交接，计划源：前端 `bugfix-session-split.md`）

> 根因：① 前端只在 done 落 localStorage sessionId（generate 卡死→无 done→刷新丢 id→重复 startSession 建孤儿会话，前端已修 commit 873bd78）；
> ② 后端 `TutoringLlmClient.generate()` 曾漏配 `.timeout()`（已修 commit ff5008b，含回归测试）。
> 下述 B2/B3 为后端剩余任务。

- [x] B2. `TutoringAppService` 三处同步 `archiveTranscript`（COS 上传）改提交到**单线程 FIFO 调度器**，SSE 不等待 COS
  - [x] B2.1 注入字段 `Scheduler archiveScheduler`（默认 `Schedulers.newSingle("tutoring-archive", true)` daemon，`@Setter(PACKAGE)` 供测试注入）
  - [x] B2.2 helper `archiveAsync(session, messages, summary)` → `archiveScheduler.schedule(() -> archiveTranscript(...))`
  - [x] B2.3 `postDecide` / `endByRoundLimit` → `archiveAsync(session, history, summary)`（内存列表快照，不重读缓存）
  - [x] B2.4 `buildStream` doOnComplete → `archiveAsync(session, sessionCache.listMessages(id), summary)`（列表同步捕获，规避 clearCacheIfEnded 后异步重读为空）
  - [x] B2.5 测试：`TutoringAppServiceTest.@BeforeEach` 注入 `Schedulers.immediate()` 防现有 verify 竞态；新增 `start_archivesAsync_nonBlocking` 例 mock `Scheduler` 断言 `schedule(Runnable)` 被调用且 archive 未内联执行
  - [x] B2.6 回归 `mvn -pl ai-edu-application -am test` 全绿（190 例）
- [x] B3. 软删生产孤儿会话 id 85–96（12 个 ACTIVE、round_count=1、同题「鸡兔同笼」、无 AI 回复）
  - [x] B3.1 先读确认：id 84–97 区间（发现 84/96 已被软删、97 为新增）；整表仅 student_id=1 一个 dev 账号（97 行）
  - [x] B3.2 软删（用户确认「全删除，重新测试」）：`UPDATE t_tutoring_session SET is_deleted=1, updated_at=NOW() WHERE student_id=1 AND is_deleted=0` → 95 行
  - [x] B3.3 复查：student_id=1 alive 计数为 0；Redis 残留 `learning:tutoring:*` 51 个 key（session/messages/active 索引）全部清除，历史列表不再显示孤儿会话

## 6. COS transcript 改由后端代理获取（前端交接，方案见前端 `transcript-via-backend.md`）

> 动机：detail 下发签名 `transcriptUrl` 有权限风险（签名 30min 内可分享/抓包泄露、COS 路径暴露客户端）+ 依赖存储桶 CORS（已踩坑）。改后端服务端读 COS 透传，**前端零 COS 直连**；服务端→COS 无跨域，B4（CORS 配置）随之废弃。
>
> 目标契约（对齐前端）：
> - `GET /sessions/{id}`（detail）**移除 `transcriptUrl`**；`recentMessages/status/roundCount/answerRequestCount/summary/endReason` 等不变。
> - 新增 `GET /sessions/{id}/transcript` → `{ code:'00000', data:{ messages:[...] } }`；鉴权+归属校验失败（不存在/已软删/非本人）→ **50002**；COS 对象缺失 → `{ messages: [] }`（graceful，前端兜底 recentMessages）。
>
> **代码现状核查（2026-08-13）**：
> - `FileStorageService`/`CosFileStorageServiceImpl` 只有 upload/uploadToObjectKey/delete/getUrl/generatePresignedUrl，**无读对象内容方法**——前端方案假设「复用现有 COS client 读对象」，实际需**新增** `download`（getObject + `CosServiceException` 404 判 NoSuchKey → null）。
> - `resolveTranscriptUrl`（签名 URL 现生成）在 **`getSession` 行 250 与 `archive` 行 283 两处**被调用——契约「签名 URL 不再出现在任何响应里」要求**两处都移除**，不止 detail。
> - `transcriptUrl` 对内（`TutoringSession` 实体 / PO / DB `transcript_url` / Redis 快照）是归档 objectKey，**保留**；只移除 DTO 对外暴露。

- [x] T1. `FileStorageService` 接口 + `CosFileStorageServiceImpl` 新增 `download(objectKey) → byte[]`（`cosClient.getObject` 读内容；`CosServiceException` statusCode=404 → null，其余异常抛 `FILE_READ_FAILED`）
- [x] T2. `TutoringTranscriptArchiver` 新增 `readMessages(studentId, sessionId)`：key=`TRANSCRIPT_DIR/{studentId}/{sessionId}.json`（复用现有常量，与归档路径一致）→ `fileStorageService.download(key)` → objectMapper 反序列化根对象 → 取 `messages` 数组（`TutoringChatMessage` 反序列化已含 meta，`@JsonProperty` 对齐）；null/404 → 空列表
- [x] T3. `TutoringAppService` 新增 `getTranscript(studentId, sessionId)`：`loadSession(sessionId, studentId)` 归属校验（不存在/越权/已软删 → 50002）→ `transcriptArchiver.readMessages(session.getStudentId(), session.getId())` → 返回 messages；**对象缺失返回空列表（非 50002）**
- [x] T4. detail 移除 `transcriptUrl` 暴露：`TutoringSessionDTO` 删字段；`resolveTranscriptUrl` 方法删除；`getSession`（行 250）与 `archive`（行 283）两处调用删除；`TutoringAssembler` 行 42 `.transcriptUrl(...)` 删除；`TutoringConfig.transcriptUrlExpireMinutes`/`TutoringProperties` 若成死代码一并清理（可选）——**已清理**（interface/默认实现/常量/字段/4 个 yml key）
- [x] T5. `TutoringController` 新增 `GET /sessions/{sessionId}/transcript`：`TutoringAuth.requireStudent` → `tutoringAppService.getTranscript` → `ApiResponse<TranscriptDTO>`（`{ messages: [...] }`）；新建 `TutoringTranscriptDTO`（消息列表，含 meta）；Swagger 描述同步（去掉「经 transcriptUrl 拉 COS」表述）
- [x] T6. 单测：
  - [x] T6a. transcript 正常返回（消息含 meta，与 COS 序列化一致）——AppServiceTest.getTranscript_returnsMessages + ArchiverTest.readMessages_parsesMessagesWithMeta + ControllerTest.getTranscript_delegates
  - [x] T6b. COS 对象不存在 → `{ messages: [] }`（code 00000，**非 50002**）——getTranscript_objectMissing_returnsEmpty + readMessages_objectMissing_returnsEmpty
  - [x] T6c. 越权 / 会话不存在 / 已软删 → 50002——getTranscript_ownershipRejected / getTranscript_sessionNotFound
  - [x] T6d. detail（getSession）响应断言**不再含 transcriptUrl**——getSession_returnsRecentMessages 序列化断言不含该键
- [x] T7. 回归 `mvn -pl ai-edu-application -am test` 全绿（190 例）——**实跑 application 197 + interface 177 全绿（1 pre-existing skip），BUILD SUCCESS**；api.md/test.md 契约已同步（transcript 端点、detail 无 transcriptUrl、TRAN-001..004）

> 发布顺序（与前端对齐）：**前后端同批**。detail 先移除 transcriptUrl 时旧前端读 undefined → recentMessages 兜底（不崩）；新前端若后端接口未上 → 同样落 recentMessages 兜底。两侧就绪即恢复完整历史复原。
> 边界：B2 异步归档竞态（rounds 已走、COS 未写完）→ transcript 接口返回空 → 前端 recentMessages 兜底（与现状一致）；大 transcript 直接透传 COS JSON，无额外序列化成本。
