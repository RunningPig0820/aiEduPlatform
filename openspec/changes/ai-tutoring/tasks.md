## 1. 数据库迁移（Flyway）

> **域归属（已定）**：AI 答疑作为**学习域 bounded context 内的功能模块**落地。三表物理位于 `ai_edu_learning` 数据库；实现代码放 `com.ai.edu.domain.learning` 下答疑子模块；持久化 Mapper 打 `@DS("learning")`（`application.yml` 需新增 `learning` 数据源，见 11.1）。

- [x] 1.1 `V9__create_t_tutoring_session.sql`: 创建 `t_tutoring_session`（student_id, subject, question_type, question_kind, intent_category, last_emotion, status, round_count, answer_request_count, end_reason, transcript_url, created_at, updated_at, archived_at, created_by, modified_by, is_deleted；**不存题目内容**），索引 (student_id, status)
- [x] 1.2 `V10__create_t_student_kp_mastery.sql`: 创建 `t_student_kp_mastery`（student_id, kp_key, kp_label, mastery_level, evidence JSON, last_session_id, created_at, updated_at, created_by, modified_by, is_deleted），UNIQUE(student_id, kp_key)，索引 (student_id)
- [x] 1.3 `V11__create_t_tutoring_error_event.sql`: 创建 `t_tutoring_error_event`（student_id, session_id, kp_key, error_type, emotion, step_index, student_answer, created_at, updated_at, created_by, modified_by, is_deleted），索引 (student_id, created_at)
- [x] 1.4 （不建消息表、不存题目内容——对话每轮实时整写 COS 恒完整，Redis 活跃期热存；换题只作事件重置计数）

## 2. Domain Layer - 值对象

- [x] 2.1 `TutoringState` 值对象（ACTIVE / ARCHIVED / TERMINATED，生命周期 3 态）
- [x] 2.2 `ActionType` 值对象（hint / approach / reveal / concept / switch / end，闭集）
- [x] 2.3 `EndReason` 值对象（COMPLETED / ANSWER_REVEALED / ABANDONED / ROUND_LIMIT）
- [x] 2.4 `KpKey` 值对象（包装 TextbookKP URI，校验非空）
- [x] 2.5 `MasterySignal` 值对象（kp_label + signal: mastered/practicing/struggling）
- [x] 2.6 `TutoringQuestionType` / `TutoringQuestionKind` 值对象（独立可扩展枚举，含 UNKNOWN）
- [x] 2.7 `TutoringEmotion` 值对象（**F7 七态**：NEUTRAL/CONFUSED/FRUSTRATED/ANXIOUS/CONFIDENT/INTERESTED/BORED，Python 输出方权威；不复用 learning 域 EmotionState）
- [x] 2.8 `TutoringConstants`：SESSION_ROUND_LIMIT=20、ANSWER_REQUEST_LIMIT=2、SESSION_CREATE_LIMIT=3、SESSION_CREATE_WINDOW_MINUTES=5、AGENT_RETRY=1、DECIDE_TIMEOUT / GENERATE_TIMEOUT / OCR_TIMEOUT

## 3. Domain Layer - 实体与聚合根

- [x] 3.1 `TutoringSession` 聚合根：字段（id, studentId, subject, questionType, questionKind, intentCategory, lastEmotion, status, roundCount, answerRequestCount, endReason, transcriptUrl, createdAt/updatedAt/archivedAt；**不记录题目内容**）；生命周期方法 `start()`、`terminate(reason)`；护栏方法 `recordRound()`（达 20 抛轮次上限）、`requestAnswer()`（返回第几次，仅在需要时计数）、`switchQuestion()`（仅重置计数，换题判定在 Python）、`complete(reason)`（置 ARCHIVED + endReason）、`setKpClassification(type,kind)`、`setLastEmotion(emotion)`
- [x] 3.2 `StudentKpMastery` 实体：`applySignal(signal)`（mastered→75/practicing→50/struggling→25 取 max；显式纠正例外下调）、`raiseByCorrection()`（COMPLETED 时提升 75+）
- [x] 3.3 `ErrorEvent` 实体：工厂 `create(studentId, sessionId, kpKey, errorType, emotion, stepIndex, studentAnswer)`

## 4. Domain Layer - 仓储接口

- [x] 4.1 `TutoringSessionRepository`：save、findById、findActiveByStudentId、updateTranscriptUrl
- [x] 4.2 `StudentKpMasteryRepository`：upsert（studentId+kpKey）、findByStudentId、findByStudentAndKp
- [x] 4.3 `ErrorEventRepository`：save、findByStudentId

## 5. Application Layer - DTO 与组装

- [x] 5.1 `StartTutoringCommand`（message）、`SendMessageCommand`（sessionId, content）、`RequestAnswerCommand`（sessionId）、`ArchiveSessionCommand`（sessionId）
- [x] 5.2 `TutoringSessionDTO`（sessionId, status, subject, questionType, roundCount, answerRequestCount, recentMessages, summary, transcriptUrl；**无 questionContent**——后端不记录题目，前端从 recent_messages 推断）、`ChatMessageDTO`（role/content/createdAt）
- [x] 5.3 `ActionMetaDTO`（type, eval, masterySignals, newQuestion, endReason, summary, safetyFlag, degraded）、`EvalDTO`（correct, errorType, emotion, exerciseComplete）、`MasteryDTO`、`SummaryDTO`；`reason` 不建模（Jackson 容忍未知字段）；**Python snake_case 契约经 @JsonProperty 映射**（mastery_signals/end_reason/new_question/safety_flag/error_type/exercise_complete/kp_label）
- [x] 5.4 `GuardResult`（ALLOW / DENY + deniedReason + fallbackType）
- [x] 5.5 `TutoringAssembler`：DTO ↔ 领域模型互转（toSessionDTO / toMasterySignals）

## 6. Application Layer - 护栏服务（核心）

- [x] 6.1 `TutoringGuardrailService.validate(action, session)` → GuardResult：
  - 答案护栏：`type=reveal` 且 `answer_request_count<1` → DENY + fallback=approach
  - 轮次护栏：引导类（hint/approach）且 `round_count>=20` → DENY + fallback=end(ROUND_LIMIT)
  - 换题护栏：`type=switch` → 归档旧题（ABANDONED）+ 重置计数（`onSwitch` 仅重置计数，会话保持 ACTIVE）
  - 收尾护栏：`type=end` → 按 endReason 校正掌握度 + 归档（`onEnd` 置 ARCHIVED + endReason，掌握度校正由编排服务执行）
- [x] 6.2 护栏拒绝后的降级逻辑：reveal 重决策仍 reveal → Java 直接降级固定思路话术 + count→1（`degradeRevealToApproach` + `FALLBACK_APPROACH_SPEECH` 常量）
- [x] 6.3 Python decide 结构化输出兜底：返回 **200 + ActionMeta(type=hint, degraded=true)** → Java 按普通 hint 放行 + 记日志（degraded=true 监控用），不拦护栏，不使用 503；safety_flag=true → DENY + fallback=end

## 7. Application Layer - 编排服务（Java 网关主导）

- [x] 7.1 `TutoringContextAssembler`：组装 decide 请求上下文（history 从 Redis、counters、masterySnapshot **带 kp_label**（Python label 接地用）、subject=math；**不含 current_question**——后端不记录题目，Python 从 history 推断）
- [x] 7.2 `TutoringLlmClient`（Infrastructure，WebClient 调 Python）：`decide(context)` → ActionMeta（非流式）；`generate(type, meta)` → Flux<ServerSentEvent>（流式）；`recognize(file)` → OcrResult（非流式）；decide/recognize 错误重试 1 次，generate 不可重试；宽容 ObjectMapper（FAIL_ON_UNKNOWN_PROPERTIES=false，容忍 reason 字段）
- [x] 7.3 `TutoringAppService.start()`：安全预检 → 组装上下文 → decide → 护栏 → 建会话 → generate 流式；终止场景（无关/学习方法/非数学/安全）直接回复置 TERMINATED（start 阶段终止不建会话）
- [x] 7.4 `TutoringAppService.sendMessage()`：追加消息到 Redis → 组装上下文 → decide → **护栏校验** → 落库副作用（掌握度/错误/情绪/round）→ **每轮对话实时整写 COS** → generate 流式 → SSE 透传；换题/收尾/轮次上限按护栏结果路由（deny reveal→approach 走 generate，deny round→固定话术无 generate）
- [x] 7.5 `TutoringAppService.requestAnswer()`：仅 ACTIVE 可调用；合成"请把答案给我"消息交由 decide+护栏处理（第 1 次 approach / 第 2 次 reveal）
- [x] 7.6 `TutoringAppService.getSession()`：断点恢复（含 recentMessages，transcript_url 转签名 URL）
- [x] 7.7 `TutoringAppService.archive()`：主动收尾（end_reason=ABANDONED，掌握度不提升 + COS 终态写 + 清 Redis）
- [x] 7.8 `TutoringAppService.getStudentMastery()`：查询掌握度（MasteryItemDTO 列表）
- [x] 7.9 会话创建频率限制：Redis 计数（5 分钟 > 3 个 → 50004）
- [x] 7.10 `TutoringTranscriptArchiver`：**每轮对话后整写 COS**（`tutoring/transcripts/{sessionId}.json`，脱敏钩子、幂等整写；首次写即回填 transcript_url）；会话结束终态写一次 → 清 Redis（FileStorageService 新增 `uploadToObjectKey` 幂等整写）

## 8. Infrastructure Layer - 持久化与基础设施

- [x] 8.1 PO：`TutoringSessionPO`（含 end_reason/transcript_url）、`StudentKpMasteryPO`（evidence JSON）、`ErrorEventPO`
- [x] 8.2 Mapper：`TutoringSessionMapper`、`StudentKpMasteryMapper`（INSERT ... ON DUPLICATE KEY UPDATE）、`ErrorEventMapper`；三件 Mapper 打 `@DS("learning")` 路由到 `ai_edu_learning`
- [x] 8.3 RepositoryImpl 三件套，桥接 PO ↔ 领域实体
- [x] 8.4 `TutoringKpResolver`：label → TextbookKP URI（kg-sync 镜像 `KgKnowledgePointPo`，subject=math；精确 → LIKE → 未命中记日志）
- [x] 8.5 `TutoringSessionCache`（Redis）：会话快照 + 完整消息列表（TTL 24h）+ 频率计数
- [x] 8.6 COS 实时写复用：确认 `FileStorageService`/`CosFileStorageServiceImpl` 配置（私有读、签名 URL），目录约定 `tutoring/transcripts/`

## 9. Interface Layer - REST API + SSE

- [x] 9.1 `TutoringController`：
  - `POST /api/tutoring/sessions`（start，SSE 类型先行流式）
  - `POST /api/tutoring/sessions/{sessionId}/messages`（sendMessage，SSE）
  - `POST /api/tutoring/sessions/{sessionId}/request-answer`
  - `GET /api/tutoring/sessions/{sessionId}`（断点恢复）
  - `POST /api/tutoring/sessions/{sessionId}/archive`
  - `GET /api/students/{studentId}/mastery`
  - `POST /api/tutoring/ocr`（multipart 图片 → 代理 Python `/api/ocr/recognize` → 返回 {text, confidence}；供前端确认/修改后进答疑）
- [x] 9.2 SSE 协议：`meta`（护栏已放行的 type + denied 字段）→ `token`（正文流）→ `done`（状态 + eval + summary）；护栏拒绝时**无 token 流**
- [x] 9.3 统一 `ApiResponse<T>` 包装 + 错误码（50002 会话不存在 / 50003 已结束 / 50004 创建频繁 / 50005 agent 失败 / 50006 OCR 无效——答疑归学习域 5xxxx，不与作业 4xxxx 冲突）
- [x] 9.4 内部 token 安全：Java↔Python 调用复用 llm-gateway `internalToken` 校验模式（decide/generate 端点仅内部可达）

## 10. Python 答疑 agent 契约（本仓库文档，实现另排期）

- [ ] 10.1 `docs/ai-tutoring-agent.md`：Python 独立答疑 agent 的 decide/generate 契约、action 元数据 schema、prompt 设计要点（苏格拉底原则、hint 禁答案、approach 只给思路、reveal 才给答案、换题/收尾识别）、type 先行流式协议（generate SSE：token/done）、decide 用快模型 / generate 用强模型的建议
- [ ] 10.2 Python 侧实现（`ai-edu-ai-service` 独立答疑模块，另行排期，不在本仓库）

## 11. 配置与收尾

- [x] 11.1 `application.yml` 新增 `ai-edu.tutoring` 配置（轮次/答案/频率/超时、Python decide/generate/OCR 端点地址、internal token、**`ocr.enabled` 开关**——关闭时前端隐藏拍照入口，仅手打/粘贴）；**新增 `learning` 数据源**（指向 `ai_edu_learning`，答疑 Mapper 经 `@DS("learning")` 使用）
- [x] 11.2 幂等与会话并发：同一会话并发消息的处理策略（Redis 锁或乐观锁）
- [ ] 11.3 确认 kg-ui 图谱叠加层新增掌握度数据源（读 `GET /api/students/{id}/mastery`）
- [ ] 11.4 确认认证网关对前端流式接口的登录态校验（Spring Security 放行 `/api/tutoring/**` 但校验 session）
