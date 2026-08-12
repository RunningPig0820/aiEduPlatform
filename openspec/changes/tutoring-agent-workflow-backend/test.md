# AI 答疑 Agent 工作流后端契约 测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证 Java 侧两类契约变更：①decide 阶段 agent 事件透传（orchestrate filter）；②meta 新字段（decideReason/questionKps/masterySignals）与 `reason` 语义保持（护栏拒绝原因不变）。确保既有答疑逻辑（护栏/落库/SSE 序列）不回归。

### 1.2 测试方式
- **单元测试（Mock）**：`TutoringAppServiceTest`（mock LlmPort/仓储，真实护栏）——decide 透传序列断言 + meta 新字段断言
- **契约测试**：`ActionMetaContractTest`（JSON 序列化/反序列化，reason/questionKps/masterySignals）
- 与模型端联调（real）：真实 decide SSE（perceive/analyze/plan/decide 透传 + question_kps）

### 1.3 测试环境配置
- Profile: `test`
- Mock WebClient / Mockito 注入
- 与 `tutoring-agent-events` 测试基线一致

---

## 2. 测试数据

| 参数 | 值 | 说明 |
|-----|-----|------|
| STUDENT_ID | 501L | 学生 ID |
| SESSION_ID | 1001L | 会话 ID |
| PY_REASON | "学生第 1 次明确要求答案" | Python 决策理由（→ meta.decideReason） |
| QUESTION_KPS | ["鸡兔同笼","二元一次方程组"] | 题目知识点 |
| GUARD_REASON | answerCountInsufficient | 护栏拒绝原因（→ meta.reason） |

---

## 3. 测试用例

### TC-1.1: decide 阶段 agent 事件透传
- **Priority**: High
- **Given**: mock decide 流 `thinking × 2 → agent(decide) → meta → done`
- **When**: `service.sendMessage(...)` 订阅
- **Then**: 事件序列为 `thinking → thinking → agent(decide 透传) → agent(guardrail) → meta → token → agent(memory) → done`
- **Ref**: spec#decide-agent-事件透传

### TC-1.2: decide agent 全 stage 透传
- **Priority**: Medium
- **Given**: mock decide 流 `agent(perceive) → agent(analyze) → agent(plan) → agent(decide) → meta → done`
- **When**: 订阅
- **Then**: 四个 agent 事件按序透传，Python meta/done 不透传
- **Ref**: spec#decide-agent-事件透传

### TC-1.3: 换题/降级轮——无 thinking、无 agent(decide)
- **Priority**: Medium
- **Given**: mock decide 流 `agent(perceive) → agent(analyze) → agent(plan) → meta → done`（短路/降级：`agent(decide)` 只在 thinking 事件时发出，短路仅 yield meta）
- **When**: 订阅
- **Then**: `perceive/analyze/plan` 仍透传，**无 thinking、无 `agent(decide)`**；meta 仍重建到达，前端按 meta.type 定型，无"解析中"残留
- **Ref**: spec#decide-agent-事件透传 / spec#换题短路无 thinking

### TC-2.1: ActionMeta 解析 reason/questionKps
- **Priority**: High
- **Given**: decide meta data 含 `reason` + `question_kps` 字段
- **When**: `readActionMeta(data)`
- **Then**: `action.getReason()` / `action.getQuestionKps()` 解析成功；缺失时为 null
- **Ref**: spec#actionmeta-新增-reason-与-questionkps

### TC-3.1: 护栏拒绝轮 meta 字段（decideReason 定案）
- **Priority**: High
- **Given**: decide type=reveal，answerRequestCount=0（第 1 次要答案）
- **When**: 订阅 sendMessage
- **Then**: meta 为 `type=approach` + `denied=reveal` + `reason=answerCountInsufficient` + `decideReason=<Python理由>` + `answerRequestCount=1`
- **Ref**: spec#ssemeta-decideReason-新字段

### TC-3.2: 护栏放行轮 meta 字段
- **Priority**: High
- **Given**: decide type=hint，护栏放行
- **When**: 订阅
- **Then**: meta 为 `type=hint` + `decideReason=<Python理由>` + `questionKps`/`masterySignals` 透传（若有）；`denied`/`reason` 为 null
- **Ref**: spec#ssemeta-decideReason-新字段

### TC-4.1: masterySignals 序列化 camelCase
- **Priority**: High
- **Given**: decide meta 含 `mastery_signals=[{kp_label, signal}]`
- **When**: buildMeta 产出 meta 并序列化
- **Then**: `meta.masterySignals` 为 `[{"kpLabel":"鸡兔同笼","signal":"struggling"}]`（**kpLabel camelCase**，非 kp_label）
- **Ref**: spec#ssemasterysignaldto-camelcase-契约

### TC-5.1: questionKps 空值透传
- **Priority**: Medium
- **Given**: Python 未下发 `question_kps`（后端先部署）
- **When**: buildMeta
- **Then**: `meta.questionKps` 为 null，前端显示占位"—"，不阻断流程
- **Ref**: spec#buildmeta-带出新字段

### TC-6.1: 回归——既有 SSE 序列
- **Priority**: High
- **Given**: 既有 start/sendMessage/requestAnswer/换题/终止/轮次上限场景
- **When**: 全量 `TutoringAppServiceTest` + `TutoringControllerTest`
- **Then**: 无回归（护栏/落库/事件顺序不变；仅 decide agent 事件新增透传）
- **Ref**: spec#decide-agent-事件透传

---

## 4. 覆盖目标

- spec 全部 5 项 Requirement 至少 1 个用例。
- 回归：`tutoring-agent-events` 既有用例全绿（重点 `sendMessage_decideThinkingRelayedFirst` 更新后通过）。
- 联调：真实 decide SSE 验证 perceive/analyze/plan/decide 透传 + question_kps 生效。
