# AI 答疑 Agent 事件协议 API 对接文档

> 基础路径: `/api/tutoring`
>
> 更新日期: 2026-08-12（decide agent 透传 + meta 新字段）
> 本变更只改 **SSE 事件流**（新增 `event: agent`），接口路径/请求/错误码均不变。
> 配合模型端 `tutoring-agent-protocol`（decide 改 SSE 流式）。

---

## agent 事件协议（本次新增）

所有 agent 阶段事件格式统一：

```
event: agent
data: {"level":"sub","stage":"guardrail","label":"安全把关","status":"done","detail":"放行: hint"}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| level | String | `sub`（子 agent）/ `master`（主 agent，预留） |
| stage | String | 标准阶段：perceive/analyze/plan/tool/decide/generate/memory/guardrail |
| label | String | 前端展示文案（中文） |
| status | String | processing / done / error |
| detail | String? | 可选补充（决策摘要、拒绝原因等） |

---

## 1. 发起答疑会话 / 发送消息 / 请求答案（SSE 事件序列变更）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/tutoring/sessions` / `/api/tutoring/sessions/{sessionId}/messages` / `/api/tutoring/sessions/{sessionId}/request-answer` |
| Content-Type | `application/json` 或 `multipart/form-data`（图片题） |
| 需要登录 | 是 |

### 请求参数

不变（见 ai-tutoring api.md：`{message}` 文字 / multipart `file` 图片 / `{content}` 回答）。

### 响应（SSE 流，事件序列变更）

```
event: agent    {"level":"sub","stage":"perceive","label":"读题感知","status":"done","detail":"识别题目为二元一次方程组应用题"}   ← Python decide 读题感知（done）
event: agent    {"level":"sub","stage":"analyze","label":"解析意图","status":"processing"}                                        ← Python decide 解析学生意图中（processing）
event: agent    {"level":"sub","stage":"plan","label":"规划决策","status":"processing"}                                           ← Python decide 规划决策中（processing）
event: thinking {"content":"学生在回顾已知条件..."}                                                                                ← Python decide 推理分片（模型决策思考，可多条，实时透传，不入库）
event: thinking {"content":"..."}
event: agent    {"level":"sub","stage":"decide","label":"意图判定","status":"done","detail":"hint: 学生正常作答,推一步"}            ← Python 决策完成（meta 前）
event: agent    {"level":"sub","stage":"guardrail","label":"安全把关","status":"done","detail":"放行: hint"}                       ← Java 护栏通过后
event: meta     {"sessionId":1001,"status":"ACTIVE","type":"hint","roundCount":1,"decideReason":"学生正常作答,给一步引导","questionKps":["二元一次方程组"],"masterySignals":[{"kpLabel":"二元一次方程组","signal":"practicing"}],"eval":{...}}   ← Java 自建（含护栏已放行 type）
event: agent    {"level":"sub","stage":"generate","label":"生成中","status":"processing"}                                            ← Python generate
event: thinking {"content":"我在打磨符合要求的引导性反问..."}                                                                       ← Python 推理分片（模型思考，可多条，已落库）
event: thinking {"content":"..."}
event: token    {"content":"先找题目里的已知条件..."}
event: token    {"content":"..."}
event: agent    {"level":"sub","stage":"memory","label":"记忆更新","status":"done","detail":"二元一次方程组 → 练习中"}               ← Java 落库后
event: done     {"sessionId":1001,"status":"ACTIVE","roundCount":1,...}
```

### 事件序列说明

| 位置 | 事件 | 发射方 |
|------|------|--------|
| 1 | `agent(perceive)` | Python（decide 读题感知，done） |
| 2 | `agent(analyze)` | Python（decide 解析学生意图中，processing） |
| 3 | `agent(plan)` | Python（decide 规划决策中，processing） |
| 4 | `thinking*`(decide) | Python（**decide 决策推理分片**，`{"content":"..."}`，可多条，Java 实时中继、**不入库**） |
| 5 | `agent(decide)` | Python（决策完成，done，meta 前） |
| 6 | `agent(guardrail)` | Java（护栏通过后、generate 前） |
| 7 | `meta` | Java 自建（type=已放行类型，含 eval/roundCount；新增 `decideReason`/`questionKps`/`masterySignals`，见下节） |
| 8 | `agent(generate)` | Python |
| 9 | `thinking*`(generate) | Python（模型推理分片，`{"content":"..."}`，可多条，按 chunk 拼接展示） |
| 10 | `token*` | Python（正文流） |
| 11 | `agent(memory)` | Java（掌握度落库后收尾） |
| 12 | `done` | Java 自建 |

> **注意**：
> - **generate 的 thinking 事件**（2026-08-12 新增）：Python generate 在 token 前流式吐推理分片，Java **原样中继** + **累积落库**（`thinking` 字段：Redis 热存消息 + COS transcript，供历史消息"思考过程"展示）。前端可渲染可折叠"思考过程"面板（DeepSeek 风格，见下节）；不渲染也完全不影响现有逻辑（thinking 会被忽略）。
> - **decide 的 thinking 事件**（2026-08-13 新增，演进自原"不透传"）：Python decide 流式吐**决策推理分片**，Java **实时中继**前端（消除 decide 长等待黑盒，实测 17~48s）。**不入库**（仅实时透传，历史消息只保留 generate thinking）——若要 decide thinking 也落历史，另立 change。实现依赖 decide 消费从"同步 blockLast 取 meta"演进为"响应式中继 thinking + 提取 meta"（见 design.md D7）。
> - **decide 阶段的 agent 事件**（2026-08-12 新增，演进自原"不透传"）：Python decide 的 `perceive`/`analyze`/`plan`/`decide` agent 事件由 Java **原样透传**前端（decide filter 从 `only thinking` 放行为 `thinking + agent`），前端可用其驱动"本轮意图·解析意图…"live 状态（见 `tutoring-agent-workflow-backend` change）。`meta` 到达后定型为决策结果。
> - 护栏拒绝时（如 reveal 未授权）：`agent(guardrail)` detail 为"拒绝: reveal → 降级 approach"，随后 `meta.type=approach`（`denied=reveal`、`reason=answerCountInsufficient`、`decideReason=Python理由`），无 token 的降级话术由 generate 出。
> - 终止/轮次上限场景：无 guardrail 事件（无 generate 路径），走既有 meta(TERMINATED/ROUND_LIMIT) 流程。
> - 换题短路（is_new_question）/ 降级兜底（degraded）分支不吐 thinking（未调 LLM）；`perceive`/`analyze`/`plan` agent 事件仍恒发，`agent(decide)` **不发出**（只在 thinking 事件时发出，短路/降级仅 yield meta），仅 meta 仍到，行为不变。

### meta 事件新增字段（2026-08-12）

`meta`（Java 自建）在既有字段（sessionId/status/type/roundCount/answerRequestCount/eval/newQuestion/denied/degraded/reply）基础上新增：

| 字段 | 类型 | 说明 |
|------|------|------|
| `decideReason` | String? | Python 决策自由文本（如"学生正常作答,给一步引导"），每轮无条件带出（可空）。前端作"为什么"行的 hover 补充。 |
| `questionKps` | List<String>? | 题目涉及知识点（Python decide 读题顺手列，可空；空时前端显示占位"—"）。 |
| `masterySignals` | List\<Object\>? | 掌握度信号 `[{kpLabel, signal}]`（**camelCase**，signal: mastered/practicing/struggling）。前端从 `meta.masterySignals` 读取（`meta.eval.masterySignals` 恒 undefined）。 |

> **字段命名**：`reason`（护栏拒绝原因 `answerCountInsufficient`/`roundLimitExceeded`/`safetyFlagHit`，仅拒绝时 set）语义**不变**；`decideReason` 是新增的 Python 决策文本，两者语义不同、互不覆盖。前端只消费 `decideReason`。
> **camelCase 坑**：`masterySignals` 经后端新增 `SseMasterySignalDTO {kpLabel, signal}` 序列化，不能用领域 `MasterySignalItem`（其 `kpLabel` 标 `@JsonProperty("kp_label")`，会序列化成 `kp_label`）。

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 50005 | 答疑决策服务暂不可用 | decide 空流/error（Python 发 event: error 或连接失败重试后仍失败），前端提示"网络波动，请重试"，会话保持 |
| 50002 / 50003 | 会话不存在 / 已结束 | 引导发起新会话 |
| 401 | 未登录 | 走统一认证 |

---

## 错误码说明

错误码不变（见 ai-tutoring api.md）：00000/50002/50003/50004/50005/50006/401。

---

## 前端调用注意事项

### 1. 新增 `event: agent` 渲染

- 前端订阅 `agent` 事件，按 `stage`/`label`/`status` 渲染阶段标签或进度条（如"安全把关 ✓""生成中…""记忆更新 ✓"）
- `status=processing` → 显示进行中；`status=done` → 显示完成
- decide 阶段 agent（`perceive`/`analyze`/`plan`/`decide`）到达即意图解析 live（processing 显示"解析意图…"）；`agent(decide)` done + meta 到达后定型为决策结果
- 未识别的 stage 忽略即可（协议 additive，向后兼容）

### 2. 事件顺序依赖

- `agent(guardrail)` 在 `meta` 之前 → 前端可先渲染"安全把关"，再按 meta 的 type 渲染回复骨架
- `agent(memory)` 在 `done` 之前 → 前端在会话结束前看到"记忆更新"

### 3. SSE 消费顺序（完整）

`agent(perceive) → agent(analyze) → agent(plan) → thinking*(decide) → agent(decide) → agent(guardrail) → meta → agent(generate) → thinking* → token* → agent(memory) → done`

- 仍以 `meta` 的 type 为准（类型先行）
- `done` 带最终状态与 eval；`agent(memory)` 表示本轮学习成果已落库

### 4. 新增 `event: thinking` 渲染（AI 思考过程，DeepSeek 风格）

**背景**：模型思考模式下有两大段推理：**decide 决策思考**（17~48s，决定 action 类型）与 **generate 生成思考**（正文前的推理，可多条）。Python 两段都流式吐 `event: thinking`（`{"content":"分片"}`）。

- **generate thinking**：Java 原样中继 + **落库**（Redis 热存消息 + COS transcript，字段 `thinking`），历史消息刷新后仍可展示。
- **decide thinking**（2026-08-13 演进）：Java **实时中继**、**不入库**（历史只保留 generate thinking）。

**三种数据来源**：

| 数据 | 来源 | 时机 |
|------|------|------|
| decide thinking 分片（实时）| SSE `event: thinking`，data `{"content":"分片"}` | guardrail 事件前、首条学生消息发出后（decide 决策思考，17~48s） |
| generate thinking 分片（实时）| SSE `event: thinking`，data `{"content":"分片"}` | 每条 AI 回复的 generate 阶段，多条分片 |
| generate thinking 全文（历史）| `GET /sessions/{id}` → `recentMessages[].thinking` | 断点恢复/刷新后，ai 消息有值，user 消息 null |

**前端改动点**：

1. **实时接收（generate）**：SSE 加 `event: thinking` 分支 → 追加到**当前 AI 回复消息**的 thinking 缓存（与 token 归属同一条消息，非全局变量）：
   ```
   event: agent {stage:generate}  → 创建当前消息草稿，thinking=[]（折叠条显示"思考中…"）
   event: thinking {"content":"分片"} → 追加到当前草稿 thinking
   event: token {"content":"正文"}  → 追加正文（原逻辑不变）
   event: done                    → 消息定型：正文 + 完整 thinking
   ```
2. **实时接收（decide）**：decide thinking 到达时 AI 消息尚未创建（meta 才建），前端用**瞬态缓存**承接（SENDING 期显示实时思考条），meta 到达创建消息时把 decide thinking 注入消息 thinking 字段、随后清空缓存：
   ```
   event: thinking {"content":"分片"}（guardrail 前） → 追加到瞬态 decideThinking 缓存（思考条实时展示）
   event: meta / agent {stage:generate} → 创建 AI 消息，thinking=decideThinking（+ 后续 generate thinking 追加）
   ```
3. **历史渲染**：`recentMessages[].thinking`（ai 消息非空）→ 在正文上方渲染折叠条
4. **UI**：ai 消息且 thinking 非空 → 折叠条（"思考过程"，DeepSeek 风格，默认收起，展开显示全文，限高滚动）；**无 thinking 的消息（换题/降级轮、旧数据）不渲染折叠条**；SENDING 期 decide thinking 用复用 ThinkingPanel 的实时思考条展示
5. **多轮隔离**：每轮 thinking 独立，不串到上一轮

**验收**：发送消息后先见"思考过程"折叠条（实时分片填充）、正文随后出现；**首轮 decide 期间思考条实时增长（不再只有"AI 思考中…"占位）**；刷新后历史 ai 消息仍能看到 thinking（仅 generate 段）；换题/降级轮不显示折叠条。

---

*文档生成时间: 2026-08-07*
