# AI 答疑完整流程：编排链路 / 引导-vs-答疑判定 / 边界护栏

> 一次学生消息的完整链路：**组装上下文 → Python decide 判意图 → Java 护栏校验授权 → 落库副作用 → Python generate 流式出正文 → SSE 透传前端**。
> 代码主档：`TutoringAppService.orchestrate` / `postDecide` / `buildStream`、`TutoringGuardrailService`、`TutoringContextAssembler`、`TutoringLlmClient`；Python `prompts.py`。

---

## 一、端到端流程（一次消息从发出到回显）

### 1.1 三个入口

| 入口 | 触发 | 动作 |
|------|------|------|
| `start` | 发起答疑（文字/图片题目） | 会话创建频率限制 → 建 session（图片先落库拿 id）→ 首条消息进 history → `orchestrate`（is_new_question=false） |
| `sendMessage` | 回答 / 追问 / 贴新题图 | 并发锁（`withSessionLockReactive`）→ 追加消息 → Java 判定换题信号 → `orchestrate` |
| `requestAnswer` | 点"要答案" | 并发锁 → 合成 `user("请把答案给我")` → `orchestrate` |

### 1.1b OCR 前置（拍题识别，入口层分支）

```
前端选图 → POST /api/tutoring/ocr → Python 识别 {text, confidence}
  ├─ 识别结果 → 学生「确认/修改」→ 确认后进答疑(带识别文本)
  └─ 或「直接传题」→ 绕过识别,图片直接作为题目进答疑
```
- 依赖 `ocr.enabled` 开关（关闭时前端隐藏拍照入口，仅手打/粘贴）
- 超时 30s；无效图片 → 50006；识别服务失败重试后仍失败 → 50005
- 识别结果**必须经学生确认/修改后再进答疑**（质量依赖识别服务，防止错误文本直接作答）

### 1.2 orchestrate 响应式管线（核心）

```
学生消息
  │
  ▼
[1] 组装 DecideContext            history + round_count + answer_request_count
                                  + mastery_snapshot(带 label) + subject_hint=math + is_new_question
  │
  ▼
[2] Python decide（SSE 流，decideStream）
       · meta 事件 → ActionMeta（Sinks.One，宽容 ObjectMapper 容忍 reason 等未知字段）
       · error 事件 / 流结束仍无 meta → 判失败（不重试）
       · 注：2026-08 全关思考（thinking: disabled），无 thinking 事件；decide 实测 ~1.5s
  │   meta 到达
  ▼
[3] postDecide（同步，仍持锁）
       1) safety_flag=true          → terminate（无 token 流）
       2) Java 护栏 validate         → GuardResult（答案护栏 / 轮次护栏 / 安全护栏）
       3) 终止类 end（type=end+end_reason空）→ terminate（TERMINATED + 直接回复 summary）
       4) 轮次护栏拒绝（fallback=end）→ endByRoundLimit（固定话术，无 generate）
       5) 定 allowedType             → 放行 type，拒绝用 fallbackType（如 reveal→approach）
       6) ensurePersisted            → 新会话落库 + 消息入 Redis
       7) applySideEffects           → 情绪/答案计数/换题/收尾/轮次/掌握度/错误事件
       8) archiveTranscript          → 每轮实时整写 COS（回填 transcript_url）
       ── 副作用结束，释放并发锁 ──
  │
  ▼
[4] SSE 事件流（generate 在锁外）
       agent(guardrail) → meta → agent(generate)
       → token*（AI 回复累积）
       → agent(memory) → done
  │   流结束后
  ▼
[5] AI 回复落库 Redis → 重新整写 COS（恒完整对话）→ 会话结束则清 Redis
       （2026-08 全关思考，无 thinking 落库；generate 实测 ~1.2s）
```

### 1.3 错误路径

| 场景 | 行为 |
|------|------|
| decide 失败，start 阶段（session 未建） | 重抛 → 接口层映射 50005 |
| decide 失败，已有会话 | `meta + "网络波动，请重试" + done`，会话保持 ACTIVE 可重试 |
| generate 失败（流中） | 单条 `"网络波动，请重试"` token 兜底，会话不中断 |

### 1.4 前端

`readSSE` 按事件逐条分发（token/meta/done/agent/error/thinking），无缓冲无节流。

**思考面板兼容策略（2026-08 关思考后，前端 D10）**：前端 `thinkingActive` 由 thinking 分片驱动（`handleThinking` 追加分片时置位），**不再由 `agent(generate)` 置位**：

- 关思考（现状）：无 `thinking` 事件 → "思考过程"面板 + decide 实时思考条**自动隐藏**，无空占位
- 重开思考（未来）：首片到达自动激活面板 + 打字机逐字 reveal，**前端零改动恢复**
- 主进度展示 = AgentStages 阶段 chips（安全把关 / 生成中 / 记忆更新），不依赖 thinking

**decide 阶段 agent 事件不透传**：前端 chips 只显示 `guardrail / generate / memory` 三阶段；decide 的 `perceive / analyze / plan / decide` agent 事件**从不中继前端**（Java 只提取 meta + 中继 thinking）。

**断点恢复 / 对账**（挂载与 loadSession 复用）：

```
本地快照秒渲染(localStorage) → getSession 服务端对账
  ├─ serverMsgs 多于本地 → 全量替换
  ├─ 否则逐条合并(image_url 真实 URL / thinking)
  ├─ 会话失效 50002 → 降级历史回看,提示"可发起新会话"
  └─ 其他错误 → 保留本地快照,维持本地 ACTIVE
```

---

## 二、为什么知道是"引导思路"还是"答疑"

**双层机制：Python decide 判意图（想干什么）+ Java 护栏判授权（有没有资格要答案）。**

### 2.1 第一层：Python decide 判意图 → 6 个动作闭集

| type | 含义 | 何时选（prompts.py） |
|------|------|---------------------|
| `hint` | 一条引导性反问（零步骤零答案） | **默认**。学生正常作答/轻微卡住，推一步 |
| `approach` | 思路步骤大纲（步骤名+关键公式，无最终数值） | 学生**明确求助/卡住**（"我不会""太难了""给个思路"） |
| `reveal` | 完整解答（逐步过程 + 最终答案） | 学生明确要答案（是否放行由 Java 护栏决定） |
| `concept` | 澄清/追问 | 输入过简或模糊但**与学习相关**（"我不会""老师你好"） |
| `switch` | 换题 | 学生贴出新题（Java `is_new_question=true` 信号 → Python 短路返回，不调 LLM） |
| `end` | 收尾（联动 end_reason） | 独立解出 / 放弃 / 内容无关 / 轮次上限 |

**关键 prompt 规则**：
- **先想一步原则**：默认 `hint`，仅学生明确求助/卡住才升 `approach`——不一上来就交完整路径。
- **首条消息**：默认 `hint`，**绝不能 `switch`**（无旧题可换）；明确求助才 `approach`/`concept`。
- **exercise_complete 硬规则**：学生回答正确且独立解出 → 强制 `end` + `end_reason=COMPLETED`。
- **终止型无关 vs 澄清型模糊**：完全无关（闲聊/非数学）→ `end` 终止；相关但过简 → `concept` 澄清不终止。

### 2.2 第二层：Java 护栏判授权 → 答案护栏

```
学生"把答案给我" → decide 判 reveal
  └─ 护栏: answer_request_count < 1（还没要过）?
        ├─ 是 → 拒绝，降级 approach（第 1 次：给思路大纲），计数 → 1
        └─ 否 → 放行 reveal（第 2 次：给完整解答），并收尾 ANSWER_REVEALED（防止反复要答案）
```

### 2.3 合起来：一条学生消息最终走向

```
学生消息
 ├─ 正常作答           → hint（推一步）                  → round+1
 ├─ 卡住/求助          → approach（思路大纲）            → round+1
 ├─ "把答案给我"第 1 次 → reveal 被护栏拦 → approach      → 答案计数 1
 ├─ "把答案给我"第 2 次 → reveal 放行 → 完整解答           → 收尾 ANSWER_REVEALED
 ├─ 过简/模糊          → concept（澄清，不终止不耗轮）     → 无计数
 ├─ 贴新题            → switch（换题，计数重置）          → 无计数
 ├─ 独立解出          → end/COMPLETED                   → 掌握度提升 75+
 ├─ 无关内容          → end（TERMINATED，直接回复 summary）→ 终止
 └─ 达 20 轮          → 轮次护栏强制 end/ROUND_LIMIT      → 固定话术
```

> **前端单向门（第 2 次要答案）**：服务端护栏之外，前端在 `answer_request_count >= 1` 时先弹**确认弹窗**再发"要答案"请求——两次确认才放行完整解答，形成"服务端护栏 + 前端确认"的双重拦截。
> **换题信号细节**：`is_new_question=true` 仅"新上传题目图"那一轮置位，后续答题轮不置；Java 侧 `historyContainsImageUrl` 权威判定新图 URL 是否首现（新 URL = 换题）。

---

## 三、边界设置（护栏全景）

### 3.1 Java 侧

| 边界 | 值 | 位置 | 作用 |
|------|----|------|------|
| 答案护栏 | `answer_request_count < 1` 拦 `reveal` → `approach` | GuardrailService | 第 1 次思路 / 第 2 次答案 |
| 轮次护栏 | `round_count ≥ 20` 强制收尾 `ROUND_LIMIT` | GuardrailService + 常量 | hint/approach 每轮 +1，达顶固定话术无 generate |
| 安全护栏 | `safety_flag=true` → 终止 | GuardrailService | decide 检测涉险内容，Java 终止，无 token 流 |
| 终止规则 | `type=end` 且 `end_reason` 空 → TERMINATED | postDecide `isTerminationEnd` | 无关/学习方法/非数学，直接回复 summary |
| 收尾联动 | `end_reason` 4 态 | GuardrailService.onEnd | COMPLETED 提升掌握度；ANSWER_REVEALED/ABANDONED/ROUND_LIMIT 不提升 |
| 换题 | `is_new_question=true` → switch | sendMessage + postDecide | 计数重置，旧题不点亮 |
| 会话创建频率 | 3 次 / 5 分钟 | ensureCreateAllowed | 超限 50004 |
| 并发锁 | Redis `SET NX EX`，TTL 45s | withSessionLockReactive | 串行化 decide+副作用；generate 在锁外；幂等释放 |
| decide 超时 | 10s（实测 ~1.5s） | LlmClient `.timeout()` | 防挂起 |
| generate 超时 | 常量 60s | LlmClient **未挂 `.timeout()`**（dead config，待接） | 流式正文。2026-08 全关思考后 generate 实测 ~1.2s（文字）/ ~3-5s（看图），建议挂上 `.timeout()`（如 15s）防模型挂起无限等待。**⚠️ 已知技术债：当前 generate 无超时保护，模型挂起会无限等待——优先补齐** |
| OCR 超时 | 30s | LlmClient `.block()` | 拍题降级路径 |
| 重试 | decide/ocr 重试 1 次 | 常量 + LlmClient | generate 流式不可重试 |
| 降级话术 | reveal 被拒后重决策仍 reveal → 固定思路话术 | GuardrailService.degradeRevealToApproach | 不依赖 LLM 的兜底 |
| 结构化兜底 | `degraded=true` → 按普通 hint 放行 + 记日志 | GuardrailService.validate | 不用 503。2026-08 Python 侧修复 content 兜底 + emotion 归一化后 degraded 触发率大幅下降 |
| 非法 type | 未知/null → 默认 hint | ActionType.fromCodeOrDefault | 不阻断 |
| OCR 开关 | `ocr.enabled` | ocr() + getTutoringConfig | 关闭时前端隐藏拍照入口 |

### 3.2 Python 侧（prompts.py）

- **先想一步原则**：默认 hint，明确求助才 approach。
- **首条消息规则**：默认 hint，绝不能 switch。
- **终止型 vs 澄清型**：无关 → end；相关但过简 → concept。
- **exercise_complete 硬规则**：独立解出 → end/COMPLETED。
- **换题短路**：Java `is_new_question` 信号 → 直接 `type=switch`，不调 LLM。

---

## 四、数据流（三层存储）

| 层 | 内容 |
|----|------|
| Redis | 活跃会话 + 完整消息列表（TTL 24h，**不记录题目内容**）；并发锁 key |
| MySQL | `t_tutoring_session` / `t_student_kp_mastery` / `t_error_event`（learning 库，Mapper `@DS("learning")`；**无消息表**） |
| COS | 对话每轮实时整写 `tutoring/transcripts/{studentId}/{sessionId}.json`（含 AI 回复；2026-08 全关思考后无 thinking）；题目图 `tutoring/questions/{studentId}/{sessionId}/`；transcript_url 为 objectKey，对外读时现生成签名 URL（30min） |
