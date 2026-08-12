# AI 答疑 业务测试场景清单（端到端人工/联调验证）

> 面向**真实 Java↔Python 全链路**的业务场景验收，覆盖本次 `tutoring-agent-events` 变更（agent 事件协议、decide SSE、图片换题）及答疑完整业务流。
> 单测/组件级用例见 `test.md`；本文档是**用户可见行为**的验收清单。
>
> 更新日期: 2026-08-07 ｜ 判定标准: SSE 事件序列 `agent(guardrail) → meta → agent(generate) → token* → agent(memory) → done`，护栏拒绝**无 token 流**，错误码符合 api.md。

---

## 0. 业务场景总览

```
                    ┌────────────────────────────── 答疑业务流 ──────────────────────────────┐
                    │                                                                       │
  POST /sessions ──▶ │ 1. 发起会话（文字/图片）         │                                     │
  multipart/JSON    │ 2. decide（SSE: agent阶段→meta）  │   ── safety_flag? ──▶ TERMINATED  │
                    │ 3. Java 护栏校验                 │   ── end(无关/学习方法/非数学) ──▶ TERMINATED
                    │ 4. 落库副作用（掌握度/错误/round） │   ── 轮次护栏 round≥20 ──▶ ROUND_LIMIT
                    │ 5. COS 整写 transcript           │                                     │
                    │ 6. generate 流式 + SSE 透传       │   ◀── agent 事件注入点 ──           │
                    └──────────────────────────────────┴─────────────────────────────────────┘
```

| 场景组 | 入口 | 涉及本次变更 |
|--------|------|-------------|
| S1 会话生命周期 | start / sendMessage / request-answer / getSession / archive | agent 事件序列 |
| S2 agent 事件协议 | 所有 SSE 端点 | **guardrail/memory 注入、generate 中继** |
| S3 护栏规则 | decide 输出 → Java 校验 | guardrail 事件 detail |
| S4 图片 & 换题 | multipart start / sendImageMessage | 图片 COS、is_new_question→switch |
| S5 掌握度/记忆 | mastery_signals 落库 | **memory 事件** |
| S6 持久化/COS/Redis | archive / getSession | transcript/图片/缓存 |
| S7 健壮性与错误 | decide 空流/error、并发、频率 | **decide SSE 失败边界** |
| S8 OCR 前置（已弃用，接口保留） | POST /ocr | 无 |

---

## S1 会话生命周期

| # | 业务场景 | 操作 | 预期结果 |
|---|---------|------|---------|
| S1.1 | 文字发起会话 | 登录后 `POST /sessions` body `{message:"鸡兔同笼"}` | SSE 序列完整：`agent(guardrail 放行: hint)` → `meta(sessionId, type, roundCount)` → `agent(generate)` → `token*`（引导，不含答案）→ `agent(memory)` → `done`；DB 建会话 ACTIVE |
| S1.2 | 图片发起会话 | `POST /sessions` multipart `file`（新题目照片）| 图片上传 COS `tutoring/questions/{studentId}/{sessionId}/{yyyyMMdd-HHmmss-SSS}.ext`，作为首条消息进 history（带 image_url），SSE 正常返回引导 |
| S1.3 | 文字消息轮 | `POST /sessions/{id}/messages` body `{content:"..."}` | 追加消息 → decide → 引导推进（hint/approach），事件序列同 S1.1，roundCount 递增 |
| S1.4 | 图片消息轮（配图）| `POST /sessions/{id}/messages` multipart `file`（非新题，重复上传历史图）| 图片 URL 已在 history → 不触发换题（is_new_question=false），正常引导 |
| S1.5 | 请求答案（第 1 次）| `POST /sessions/{id}/request-answer` | 答案护栏：deny reveal → 降级 approach，`agent(guardrail)` detail=`"拒绝: reveal → 降级 approach"`，`meta.denied=reveal`，无完整答案 |
| S1.6 | 请求答案（第 2 次）| 再次 `request-answer` | 放行 reveal → 完整答案 + 收尾 `done(ARCHIVED, ANSWER_REVEALED)` |
| S1.7 | 断点恢复 | 中断后 `GET /sessions/{id}` | 返回会话状态/计数/最近 50 条消息 + transcript presigned URL |
| S1.8 | 主动归档 | `POST /sessions/{id}/archive` | endReason=ABANDONED，掌握度**不**提升，COS 终态写，Redis 清理，返回 transcript URL |
| S1.9 | 会话已结束后操作 | 对 TERMINATED/ARCHIVED 会话 `messages`/`request-answer` | 50003（会话已结束或已归档） |

## S2 agent 事件协议（本次变更核心）

| # | 业务场景 | 操作 | 预期结果 |
|---|---------|------|---------|
| S2.1 | 完整事件序列（文字）| 任一正常轮 | 严格顺序：`agent(guardrail)` → `meta` → `agent(generate)` → `token*` → `agent(memory)` → `done`；前端按 stage 渲染阶段 |
| S2.2 | 完整事件序列（图片/换题轮）| multipart 新题 | 同 S2.1 序列（换题轮同样注入 guardrail/memory 事件） |
| S2.3 | guardrail 事件放行 | decide=approach 等放行动作 | `agent(guardrail)` data=`{"level":"sub","stage":"guardrail","label":"安全把关","status":"done","detail":"放行: approach"}` |
| S2.4 | guardrail 事件拒绝降级 | 第 1 次请求答案 | detail=`"拒绝: reveal → 降级 approach"`，随后 `meta.type=approach` |
| S2.5 | generate agent 中继 | Python generate 流含 `agent(generate)` | Java 原样透传 `event: agent`（data 不变）；Python 的 meta/done **被丢弃**（不泄漏为假 token） |
| S2.6 | memory 事件流尾 | 正常轮含 mastery_signals | `agent(memory)` 在最后 token 后、`done` 前；detail 汇总信号（如 `"二元一次方程组 → 练习中"`）；无信号时 detail=null |
| S2.7 | 终止/轮次上限无 guardrail | 无关内容 / round≥20 | **无** `agent(guardrail)` 事件（无 generate 路径），走既有 meta(TERMINATED/ROUND_LIMIT) |
| S2.8 | decide 阶段事件不透传 | 观察 Python decide 的 perceive/analyze/plan/decide 事件 | Java **不透传**（block 取 meta 即结束），前端 guardrail 前显示通用"AI 思考中"即可（additive，前端容忍未知 stage） |
| S2.9 | SSE 未登录/越权 | 无 session 访问 SSE 端点 / TEACHER 访问 | 不开流，返回 `event: error`（401/20004） |

## S3 护栏规则（Java 确定性）

| # | 业务场景 | 触发 | 预期结果 |
|---|---------|------|---------|
| S3.1 | 答案护栏·未授权 | `type=reveal` 且 answer_request_count=0 | DENY + fallback=approach，count→1，`agent(guardrail)` detail 拒绝摘要 |
| S3.2 | 答案护栏·已授权 | `type=reveal` 且 count≥1 | ALLOW，count→2，收尾 ANSWER_REVEALED |
| S3.3 | 轮次护栏 | 引导类（hint/approach）且 round≥20 | DENY + fallback=end(ROUND_LIMIT)，固定话术"本轮答疑已达 20 轮上限…"，**无 generate**，无 guardrail 事件 |
| S3.4 | 安全护栏 | decide `safety_flag=true`（如自伤内容）| TERMINATED + 安全话术（Python 拦截），**无 token 流** |
| S3.5 | 换题护栏 | `type=switch` 放行 | 计数归零（round/answer 按新题重计），会话保持 ACTIVE |
| S3.6 | degraded 兜底 | Python 200 + ActionMeta(type=hint, degraded=true) | 按普通 hint 放行 + 记日志，不拦护栏，不使用 503 |
| S3.7 | 非法 type | decide 输出未知 type | 走默认 hint 放行，记日志，不阻断 |

## S4 图片 & 换题

| # | 业务场景 | 操作 | 预期结果 |
|---|---------|------|---------|
| S4.1 | 图片上传 COS 路径 | 图片发起/消息轮 | 路径恒为 `tutoring/questions/{studentId}/{sessionId}/{yyyyMMdd-HHmmss-SSS}.ext`，时间戳命名可排序，图片 URL 存进消息 image_url |
| S4.2 | 换题信号判定（Java 权威）| 对话中上传**新图**（URL 未在 history）| decide 请求带 `is_new_question=true` → Python 短路径返回 `type=switch` → Java `switchQuestion()` 重置计数 → 新题继续（事件序列完整） |
| S4.3 | 首问绝非换题 | 图片发起会话（S1.2）| 首条消息 `is_new_question=false`，Python 绝不能判 switch（B1 回归：实测首问返回 approach/hint 引导） |
| S4.4 | 非换题图片轮 | 上传历史重复图 | is_new_question=false（S1.4） |
| S4.5 | 图片格式校验 | 上传非 jpg/png/webp/bmp | 40006 或 SSE 错误事件（仅支持 jpg/png/webp/bmp 图片） |
| S4.6 | 图片消息 content 为空 | multipart 仅 file 无 content | 正常进答疑（null→"" 规范化，不 422） |

## S5 掌握度 / memory

| # | 业务场景 | 触发 | 预期结果 |
|---|---------|------|---------|
| S5.1 | 掌握度 UPSERT | decide 输出 mastery_signals（label 命中 URI）| `t_student_kp_mastery` 按 student+kp_key UPSERT，signal 分值单调不降 |
| S5.2 | 掌握度提升 | endReason=COMPLETED | 命中知识点 `raiseByCorrection()` 提升 75+ |
| S5.3 | label 未命中 | signal 的 kp_label 无对应 URI | 记日志不点亮，不阻断会话（kg 数据依赖） |
| S5.4 | 错误事件门控 | decide 原 type=hint/approach 且 eval.correct=false 且 error_type 非空 | 写 `t_tutoring_error_event`（含 emotion）；switch/end/reveal/concept 轮 correct=false 不写 |
| S5.5 | memory 事件单发 | 正常轮 | `agent(memory)` **仅 Java 发**（Python 已删占位，不双发）；detail 可视化掌握度信号 |
| S5.6 | mastery_signals 为空 | 普通轮无信号 | 跳过掌握度更新不报错；`agent(memory)` detail=null 仍发射 |

## S6 持久化 / COS / Redis

| # | 业务场景 | 操作 | 预期结果 |
|---|---------|------|---------|
| S6.1 | transcript 每轮整写 | 消息轮完成 | COS `tutoring/transcripts/{studentId}/{sessionId}.json` 幂等整写，含 user/ai 完整对话（AI 回复在流结束后追加） |
| S6.2 | transcript_url 回填 | 首次写后 | 会话表回填 objectKey，Redis 快照刷新，`GET` 返回 presigned URL |
| S6.3 | 归档终态写 | archive / 自然收尾 | COS 终态写 + Redis 清理 |
| S6.4 | 断点恢复 | Redis 消息在 | getSession 返回最近消息；Redis 过期则从 MySQL 查会话状态（提示学生重述题目） |

## S7 健壮性与错误边界

| # | 业务场景 | 触发 | 预期结果 |
|---|---------|------|---------|
| S7.1 | decide 空流/error | Python 返回 `event: error` 或空流（无 meta）| 50005"答疑决策服务暂不可用"；已有会话 → `meta + "网络波动，请重试" token + done`，**会话保持 ACTIVE** |
| S7.2 | decide 连接失败重试 | 未收到任何事件 | 重试 1 次；仍失败按 S7.1 |
| S7.3 | decide 含 reason 调试字段 | Python meta data 带未知字段 | 容忍解析（FAIL_ON_UNKNOWN_PROPERTIES=false），不报错 |
| S7.4 | generate 失败 | generate 流 error | 降级"网络波动"token（onErrorResume），不中断 done |
| S7.5 | 会话并发 | 同会话双发消息 | Redis 锁拒绝后发 → "会话繁忙，请稍后再试" |
| S7.6 | 创建频率 | 5 分钟内 > 上限 | 50004 创建会话过于频繁 |
| S7.7 | 未登录 | 无 session | 同步端点 401；SSE 端点 `event: error` |
| S7.8 | 输入校验 | start 空消息 / messages 空 content | 10001（同步）/ SSE 错误事件 |
| S7.9 | decide 超时 | meta 超 120s 未到 | 按 agent 调用失败处理（50005），会话保持 ACTIVE |

## S8 OCR 前置（已弃用，接口保留）

| # | 业务场景 | 操作 | 预期结果 |
|---|---------|------|---------|
| S8.1 | 拍题识别 | `POST /api/tutoring/ocr` 上传图片 | 代理 Python `/api/ocr/recognize` → `{text, confidence}`（注意：图像优先架构下 OCR 已弃用，识别质量不保证，接口仅保留） |
| S8.2 | OCR 开关 | `GET /api/tutoring/config` | 返回 `{ocrEnabled:true/false}`；false 时 POST /ocr → 50006"拍照识别未开启" |
| S8.3 | OCR 图片无效 | 空/非法格式 | 50006；Python 失败重试后 → 50005 |

---

## 优先级与状态

| 优先级 | 场景 | 状态 |
|--------|------|------|
| P0 | S1 会话生命周期 / S2 事件协议 / S3 护栏 / S4 换题 | 关键路径，改动后必测 |
| P1 | S5 掌握度 / S6 持久化 / S7 健壮性 | 答题链路稳定后阶段 2 |
| P2 | S8 OCR（已弃用）| 仅验证接口不回归 |

**已实测通过（2026-08-07 真实联调）**：S1.1 文字发起、S1.3 消息轮、S2.1 完整序列（guardrail→meta→generate→32×token→memory→done）、S2.3 guardrail 放行、S2.6 memory detail=`"二元一次方程组 → 练习中"`、S7.3 reason 字段容忍。

**2026-08-07 真实 E2E 自测（不 mock）**：S1.1 文字发起 / S1.3 消息轮 / S1.5 第 1 次 request-answer（拒绝 reveal→approach）/ S1.6 第 2 次（reveal + ARCHIVED/ANSWER_REVEALED）/ S1.7 断点恢复（transcript URL 可访问，9 条完整对话）/ S1.8 主动归档（ARCHIVED + Redis 清理 + COS 终态写）/ S1.9 已归档 50003 —— **全部通过**。

**S2 场景组（2026-08-07 真实 E2E 自测，全部通过）**：
- S2.1 完整序列（文字）：`agent(guardrail 放行: hint)` → `meta(sessionId=30, hint, round=1)` → `agent(generate)` → 27×token → `agent(memory: 一元一次方程求解 → practicing)` → `done` ✅
- S2.2 换题轮序列：图片上传触发 switch → `agent(guardrail 放行: switch)` → `meta(type=switch, roundCount=0)`（计数重置）→ `agent(generate)` → 20×token → `agent(memory)`（无信号 detail=null）→ `done` ✅
- S2.3 guardrail 放行格式：`{"level":"sub","stage":"guardrail","label":"安全把关","status":"done","detail":"放行: hint"}` ✅
- S2.4 拒绝降级：`detail="拒绝: reveal → 降级 approach"` + `meta(denied=reveal, type=approach, answerRequestCount=1)` ✅
- S2.5 generate 中继：`agent(generate)` 原样透传（Python 格式 detail=null）；Python 的 meta/done **零泄漏**为假 token ✅
- S2.6 memory 流尾：`agent(memory)` 在最后 token 后、done 前；detail 含掌握度信号（有信号时）✅
- S2.7 终止无 guardrail：安全内容 → `meta(TERMINATED)`，**无 guardrail/generate/token** ✅（注：无关/非数学带 end_reason 的 end 走收尾路径 ARCHIVED，见 B4，非终止路径）
- S2.8 decide 阶段不透传：前端仅收到 guardrail/generate/memory 三种 agent 事件，`perceive/analyze/plan/decide` **零泄漏** ✅
- S2.9 未登录/越权：未登录 → 统一 401 JSON（`code:10004`）；TEACHER 访问学生 SSE → `event: error {"code":"20004"}` 不开流 ✅

**S3 场景组（2026-08-07 真实 E2E 自测）**：
- S3.1 答案护栏·未授权：即 S1.5（reveal 未授权 → DENY + approach + count→1，guardrail detail 拒绝摘要）✅
- S3.2 答案护栏·已授权：即 S1.6（count≥1 → ALLOW + reveal + 收尾 ANSWER_REVEALED）✅
- S3.3 轮次护栏（round≥limit → ROUND_LIMIT）：**⚠️ 未能在本次触发** —— 见下方模型限流说明；已有 ai-tutoring change test.md **T2.3 真实实测记录**（round-limit 临时调 2 → round≥2 后 end(ROUND_LIMIT) + ARCHIVED）✅（历史实测）
- S3.4 安全护栏：即 S2.7（safety_flag → TERMINATED 无 token 流）✅
- S3.5 换题护栏：即 S2.2（switch 放行 → 计数归零，会话保持 ACTIVE）✅
- S3.6 degraded 兜底：**本次真实触发** —— Python 模型限流 429 四段管线全失败 → 返回 `type=hint, degraded=true` → Java 护栏按普通 hint 放行 + WARN 日志（"decide degraded=true，按普通 hint 放行"）+ generate 无正文（tokens=0）✅
- S3.7 非法 type：依赖 Python 输出非法 type，未真实触发（需 Python 侧构造）；既有单测覆盖默认 hint 放行（TutoringAppServiceTest）✅（单测覆盖）

### ⚠️ 模型端限流阻塞（2026-08-07 发现，需火山方舟控制台操作）

S3.3 自测期间 Python decide 持续返回 degraded，日志定位根因：**火山方舟账号 `2104786803` 达到 `doubao-seed-2-0-lite` 推理限额，模型服务被暂停**（`429 SetLimitExceeded`，Safe Experience Mode 触发）。Python 四段管线（function_calling/json_mode/文本）全部 429 → 兜底 `degraded=true` → Java 按 hint 放行。

**操作**：火山方舟控制台 → 模型开通页 → 调整/关闭"安全体验模式"限额后重测 S3.3。
**影响**：S3.3 轮次护栏真实触发暂不可行（degraded 路径绕过轮次护栏），其余 S3 场景已由前序自测或单测覆盖。

**S4 场景组（2026-08-07 真实 E2E 自测）**：
- S4.1 图片上传 COS 路径：图片发起 → `tutoring/questions/1/36/20260807-140456-492.png`（studentId/sessionId/时间戳命名）✅
- S4.2 换题信号判定（is_new_question→switch）：图片消息轮上传新图 → `meta(type=switch, roundCount=0)` 计数重置，**短路径不调 LLM（不受模型限流影响）** ✅
- S4.3 首问非换题：图片发起首问 `meta(type=hint, roundCount=1)`，**绝不是 switch** ✅
- S4.4 非换题图片轮：⚠️ **当前实现不可达** —— 见下方设计语义说明
- S4.5 图片格式校验：`.txt` → 50006"仅支持 jpg/png/webp/bmp 图片" ✅
- S4.6 图片消息 content 为空：`content=` 空 → 正常进答疑不 422（S4.2 同一请求验证）✅

**S5 场景组（2026-08-07 真实 E2E 自测，模型限流间歇恢复后完成）**：
- S5.1 掌握度 UPSERT：`INSERT INTO t_student_kp_mastery ... ON DUPLICATE KEY UPDATE`（SQL 日志）按 student+kp_key 幂等，updated_at 刷新 ✅
- S5.2 掌握度提升：COMPLETED 收尾轮 → mastery_signals `二元一次方程组→mastered` → level 75（信号映射 75，`raiseByCorrection` 保证 ≥75）✅
- S5.3 label 未命中：本轮模型返回的 label 均命中（0 次 warn）；未命中路径代码清晰（log.warn 不点亮）+ 单测覆盖（kpResolver 未命中返回 null）✅（单测覆盖）
- S5.4 错误事件门控：错误作答轮（correct=false + error_type="一元一次不等式求解移项计算错误" + 原始 type=hint）→ `INSERT INTO t_tutoring_error_event (student_id=1, session_id=40, kp_key=...01592, error_type, emotion=NEUTRAL, step_index=2, student_answer)` 真实写入 ✅
- S5.5 memory 事件单发：正常轮恰好 1 个 `agent(memory)`（无双发）✅
- S5.6 信号为空：换题轮（S2.2）mastery_signals=[] → memory 事件 detail=null 仍发射 ✅
- **信号映射确认**：mastered→75 / practicing→50 / struggling→25（MasterySignal.Level）

**S6 场景组（2026-08-07 真实 E2E 自测）**：
- S6.1 transcript 每轮整写：会话 41 多轮后 COS `tutoring/transcripts/1/41.json` = `[user 题目, ai 引导, user×2, ai]` 完整 user/ai 对话，AI 回复流结束后追加，幂等整写 ✅
- S6.2 transcript_url 回填：ACTIVE 会话 getSession 返回 presigned URL（`.../transcripts/1/41.json?q-sign...`），首次写即回填 ✅
- S6.3 归档终态写：archive → ARCHIVED + COS 终态写（status=ARCHIVED, 5 条完整）+ Redis 清理（recentMessages=0）✅
- S6.4 断点恢复：ACTIVE 会话 Redis 热存 → getSession 返回 4 条最近消息（user/ai/user/ai）；归档会话等效验证 Redis 过期路径（从 MySQL 查会话状态 + COS 取完整对话）✅

**S7 场景组（2026-08-07 真实 E2E 自测）**：
- S7.1/S7.2 decide 空流/连接失败：停 Python → 消息轮 decide 重试后仍失败 → 降级 `meta(ACTIVE, hint)` + token `"网络波动，请重试。"` + `done(ACTIVE)`，**会话保持 ACTIVE** ✅
- S7.3 decide 含 reason 调试字段：正常轮与 degraded 轮（`reason="结构化输出四段降级兜底"`）均容忍解析不报错（FAIL_ON_UNKNOWN_PROPERTIES=false）✅
- S7.4 generate 失败降级：`onErrorResume → "网络波动"` 逻辑在 TutoringAppServiceTest 单测覆盖（真实单点故障时 decide 先失败，generate 独立失败难构造）✅（单测覆盖）
- S7.5 会话并发：同会话双发 → 后发返回"会话繁忙，请稍后再试"（Redis 锁生效）✅
- S7.6 创建频率：连续创建第 4 个 → `50004`"创建会话过于频繁，请稍后再试"（limit=3/5min）✅
- S7.7 未登录：同步端点 → 统一 401 JSON（`code:10004`）✅
- S7.8 输入校验：start 空消息 → 400 + `10001`；sendMessage 空 content → `event: error 10003` ✅
- S7.9 decide 超时 120s：真实等待成本高，TutoringLlmClientTest mock 覆盖 `block(timeout)` 超时抛 TutoringAgentException（50005）✅（单测覆盖）

**S8 场景组（2026-08-07 真实 E2E 自测，OCR 接口保留）**：
- S8.1 拍题识别：POST /api/tutoring/ocr 上传 math.png → 代理 Python → `{text:"10.已知实数a>0,b>0,且a+b=1...", confidence:0.8814}`（含 LaTeX 公式）✅
- S8.2 OCR 开关：`GET /api/tutoring/config` → `{ocrEnabled:true}` ✅；`ocr.enabled=false` → 50006（单测 `ocr_disabledByConfig` 覆盖：不调 Python）✅
- S8.3 OCR 图片无效：`.txt` → 50006"仅支持 jpg/png/webp/bmp 图片"；空文件 → 50006"图片为空" ✅

**thinking 推理分片事件（2026-08-12，Python 侧决定不关思考、改流式透传推理）**：
- 决策：不关豆包思考模式（保留决策质量），改为 generate 流式吐 `event: thinking`（`{"content":"推理分片"}`，可多条）
- Java 改动：`buildStream` filter 放行 + 原样中继 thinking（不累积、不落库、不进 AI 回复）；decide 阶段 thinking 不透传（block 取 meta）
- **真实 E2E 验证**：`agent(guardrail)→meta→agent(generate)→62×thinking→29×token→agent(memory)→done`；thinking 为真实推理文本、token 为正文，二者不混淆
- 前端：可选渲染可折叠"思考过程"面板；不渲染也不影响（thinking 被忽略）
- 单测：`sendMessage_relaysGenerateThinkingEvents` 锁定透传 + 不累积
- **thinking 落库（2026-08-12，历史消息保留思考过程）**：`TutoringChatMessage` 加 `thinking` 字段（`ai(content, thinking)` 工厂）+ `buildStream` 累积 thinkingBuf + `doOnComplete` 带 thinking 落库 + `ChatMessageDTO.thinking` + `getSession` 映射 → **Redis 热存消息与 COS transcript 均含 ai 消息的 thinking**（archiver/cache 直接序列化该对象自动带）。**真实 E2E 验证**：getSession recentMessages ai 消息带完整推理文本、transcript ai 消息含 thinking、多轮各自落库、Python 容忍 thinking 字段无回归。单测：`sendMessage_relaysGenerateThinkingEvents` 断言 thinking 落库、`archive_writesAiThinking`、`getSession` thinking 映射，全绿。

### ⚠️ 设计语义发现：任何图片上传轮都判换题（S4.4 不可达）

换题判定 `isNewQuestion = !historyContainsImageUrl(history, 本次上传URL)`，而图片按 `yyyyMMdd-HHmmss-SSS` 时间戳命名 → **每次上传（即使同一文件）都生成全新 URL** → 恒不在 history → `isNewQuestion` 恒为 true → 任何图片上传轮都判换题。
- 含义：当前接口只接受图片文件，"配图消息"（带图但非换题）需前端**复用已上传图片 URL**（传 URL 而非文件），当前未支持 → S4.4 场景不可达。
- 若业务需要"上传配图不换题"，需改接口支持传既有 URL 或加"是否换题"显式标记（供产品决策，非缺陷）。

### ⚠️ 实测发现的缺陷

- **[BUG-A] 图片发起会话首条图片消息丢失**（S1.2 场景暴露）→ **已修复 + 复测通过（2026-08-07）**：
  - **现象**：图片 `start()` 后 transcript 首写 `[user 图片]` 正常；但发送一条消息轮后 transcript 变 `[user文字, ai]`，**首条图片消息被覆盖丢失**。
  - **根因**：图片路径 `start()` 需先落库拿 sessionId（组织图片 COS 路径）→ `orchestrate()` 的 `ensurePersisted()` 见 `session.getId()!=null` 直接 return，未把首条图片消息 appendMessage 进 Redis → 后续 `buildStream` doOnComplete 用 Redis 列表整写 transcript → 覆盖首条图片消息。
  - **影响**：① transcript 缺首条题目图；② 断点恢复 recentMessages 缺首条图；③ **后续 decide 上下文缺首条题目图**（sendMessage 用 Redis listMessages 组装 history，图片题后续轮次模型看不到题目）。
  - **修复**：图片路径 `start()` 落库后把 `history` 首条消息显式 appendMessage 进 Redis（`ensurePersisted` 因 id 非空跳过补录）；同时补 `ChatMessageDTO.imageUrl` 字段并在 `getSession` 映射，断点恢复前端可见图片 URL。补测试 `start_imageFirstMessagePersistedToCache`（断言首条消息带 image_url 入缓存）。
  - **复测**：真实 E2E 图片发起 → 消息轮后 transcript = `[user 图片, ai, user文字, ai]` 4 条完整（修复前图片丢失），getSession recentMessages 0→4 含首条图。
- **[GAP-B] 主动归档 endReason 未回显**（S1.8 场景）：`archive()` 调 `onEnd(ABANDONED)` 后 session 实体 endReason=ABANDONED 已设置，但 DTO 与 transcript JSON 均未映射 end_reason 字段 → 对外不透明（历史遗留，非本次变更引入）。

**回归参考**：ai-tutoring change test.md T1-T3 + E 系列（答案护栏/轮次护栏/换题/降级/越权/频率等）已实测通过；本清单与其合并即为答疑完整业务验收集。
