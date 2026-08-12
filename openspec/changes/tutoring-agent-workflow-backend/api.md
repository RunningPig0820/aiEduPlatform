# AI 答疑 Agent 工作流 后端契约变更 API 文档

> 基础路径: `/api/tutoring`
>
> 更新日期: 2026-08-12
> 本变更只改 **SSE 事件流契约**（decide agent 事件透传 + meta 新字段），接口路径/请求/错误码均不变。
> 配合前端 `show-tutoring-agent-workflow`（Agent 工作流面板）与后端 `tutoring-agent-events`（既有 agent 事件协议）。

---

## 1. decide 阶段 agent 事件透传（事件序列变更）

`POST /api/tutoring/decide` 响应流中，Python 发出的 decide 阶段 agent 事件（`perceive` / `analyze` / `plan` / `decide`）由 Java **原样透传**前端（原实现只中继 `thinking`，agent 事件被丢弃）。

前端收到的 decide 段事件序列：

```
agent(perceive) → agent(analyze processing) → agent(plan processing) → [thinking*] → agent(decide)
```

> 注：Python 的 `meta`/`done` 事件仍由 Java 消费/丢弃（meta 提取重建，done 丢弃），不透传前端。

### 各 stage 含义

| stage | status | 含义 |
|-------|--------|------|
| `perceive` | done | 读题感知完成 |
| `analyze` | processing | 解析学生意图中 |
| `plan` | processing | 规划决策中 |
| `decide` | done | 决策完成（meta 前到达） |

前端按 stage 显示"解析意图…"处理中状态；`meta` 到达后定型为决策结果。

> **换题/降级轮**：Python `perceive/analyze/plan` 恒发，仅**无 thinking、无 `agent(decide)`**（`agent(decide)` 只在 thinking 事件时发出，短路/降级仅 yield meta），**仅 meta 仍到**。前端"解析中"状态以"decide 阶段 agent（analyze/plan）已到、meta 未到"为判据，不依赖 thinking 也不依赖 `agent(decide)`。

---

## 2. meta 事件新增字段

`event: meta`（Java 自建）在既有字段（sessionId/status/type/roundCount/answerRequestCount/eval/newQuestion/denied/degraded/reply）基础上新增/调整：

### 2.1 字段命名定案：`decideReason`（Python 理由）+ `reason` 保持护栏语义

| 字段 | 语义 | 变更 |
|------|------|------|
| `reason` | **护栏拒绝原因**（`answerCountInsufficient` / `roundLimitExceeded` / `safetyFlagHit`），仅护栏拒绝时 set | 语义**不变**（buildMeta 拒绝路径 set） |
| `decideReason`（新） | **Python 决策自由文本**（如"学生第 1 次明确要求答案"），buildMeta 无条件带出，可空。前端作"为什么"行的 hover 补充 | 新增 |

> `reason`（护栏 code）与 `decideReason`（Python 文本）语义清晰可分：前端只消费 `decideReason`（hover），`reason` 仅调试用。`denied`（原始请求 type，如 reveal）保留，前端主文案由 `denied` + `answerRequestCount` 确定性推导，不依赖两个 reason 字段。

### 2.2 新增字段

```json
{
  "type": "approach",
  "denied": "reveal",
  "reason": "answerCountInsufficient",
  "decideReason": "学生第 1 次明确要求答案",
  "answerRequestCount": 1,
  "questionKps": ["鸡兔同笼", "二元一次方程组"],
  "masterySignals": [{ "kpLabel": "鸡兔同笼", "signal": "struggling" }]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `reason` | String? | 护栏拒绝原因（answerCountInsufficient/roundLimitExceeded/safetyFlagHit），仅拒绝时 |
| `decideReason` | String? | Python 决策自由文本（hover 补充） |
| `questionKps` | List<String>? | 题目涉及知识点（Python decide 读题顺手列，可空；空时前端显示占位"—"） |
| `masterySignals` | List\<Object\>? | 掌握度信号 `[{kpLabel, signal}]`（**camelCase**，signal: mastered/practicing/struggling）。前端从 `meta.masterySignals` 读取（不再读 `meta.eval.masterySignals`——该字段恒 undefined） |

> **camelCase 坑**：`masterySignals` 不能用领域 `MasterySignalItem`（其 `kpLabel` 标 `@JsonProperty("kp_label")`），必须经新建的 `SseMasterySignalDTO {kpLabel, signal}` 序列化，否则前端拿到的会是 `kp_label`。

---

## 3. 完整事件序列（变更后）

```
agent(perceive) → agent(analyze) → agent(plan) → [thinking*] → agent(decide)
    → agent(guardrail) → meta → agent(generate) → [thinking*] → token* → agent(memory) → done
```

- `agent(guardrail)` 在 `meta` 之前（既有顺序，不变）。
- 护栏拒绝时（如 reveal 未授权）：`agent(guardrail)` detail 为"拒绝: reveal → 降级 approach"，随后 `meta.type=approach`，`denied=reveal`、`reason=answerCountInsufficient`、`decideReason=Python理由`。

---

## 常见错误

不变（见 `tutoring-agent-events/api.md`）：00000/50002/50003/50004/50005/50006/401。
