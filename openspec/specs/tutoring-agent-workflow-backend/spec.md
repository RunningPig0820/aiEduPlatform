# tutoring-agent-workflow-backend Specification

## Purpose
答疑 agent 工作流后端透传能力：Java 编排层将 Python decide 流的 `agent` 阶段事件（perceive/analyze/plan/decide）与 thinking 一起实时中继给前端；`ActionMeta`/`SseMetaDTO` 建模并带出 `reason`、`decideReason`、`questionKps`、`masterySignals` 等新字段，保持 decide 阶段事件时序稳定供前端 live 走查。

## Requirements

### Requirement: decide agent 事件透传

系统 SHALL 在 `TutoringAppService.orchestrate` 的 decide 消费链路中，将 Python decide 流里的 `agent` 事件（stage ∈ perceive/analyze/plan/decide）与 `thinking` 一起原样透传前端（filter 由 `only thinking` 改为 `thinking + agent`）。Python 的 `meta`/`done` 事件仍由 Java 消费/丢弃，不透传。

#### Scenario: decide 阶段 agent 事件实时中继
- **WHEN** Python decide 返回 SSE 流（perceive → analyze → plan → thinking* → decide → meta → done）
- **THEN** Java 原样中继 `event: agent`（perceive/analyze/plan/decide），顺序保持
- **THEN** Java 仍提取 meta 重建事件，Python 的 meta/done 不透传

#### Scenario: 换题短路无 thinking
- **WHEN** 换题短路（is_new_question）或降级兜底（degraded），decide 无 thinking 事件
- **THEN** `perceive/analyze/plan` agent 事件仍透传（Python 恒发）；`agent(decide)` **不发出**（只在 thinking 事件时发出，短路/降级仅 yield meta），仅 meta 仍到，前端无 thinking、无 `agent(decide)` 残留

### Requirement: ActionMeta 新增 reason 与 questionKps

系统 SHALL 在 `ActionMeta` 中建模 Python decide 输出的 `reason`（决策自由文本，String，可空）与 `question_kps`（题目涉及知识点，List\<String\>，可空，`@JsonProperty("question_kps")`），不再由宽容 ObjectMapper 静默丢弃。

#### Scenario: 解析 reason 与 questionKps
- **WHEN** Python decide meta data 含 `reason` 与 `question_kps` 字段
- **THEN** `ActionMeta` 解析出 `reason`/`questionKps` 字段值
- **WHEN** 字段缺失
- **THEN** 对应字段为 null，不影响其他字段解析与既有流程

### Requirement: SseMetaDTO decideReason + 新字段

系统 SHALL 在 `SseMetaDTO` 中：**不重定义 `reason`**（护栏拒绝原因语义不变，仅拒绝时 set）；新增 `decideReason`（Python 决策自由文本，buildMeta 无条件带出，可空）、`questionKps`（List\<String\>）、`masterySignals`（List\<SseMasterySignalDTO\>，camelCase `{kpLabel, signal}`）。

#### Scenario: 护栏拒绝时 meta 字段
- **WHEN** 护栏拒绝（如 reveal 未授权 → 降级 approach）
- **THEN** `meta` 携带 `denied`（原始 type）、`reason`（answerCountInsufficient 等）、`decideReason`（Python 理由）、`masterySignals`（若有）
- **THEN** `reason` 仅在拒绝时存在，放行轮为 null

#### Scenario: 护栏放行时 meta 字段
- **WHEN** 护栏放行
- **THEN** `meta` 携带 `type`（放行类型）、`decideReason`（Python 理由，可空）、`questionKps`（可空）、`masterySignals`（若有）
- **THEN** `denied`/`reason` 为 null

### Requirement: SseMasterySignalDTO camelCase 契约

系统 SHALL 新建 `SseMasterySignalDTO`（camelCase `{kpLabel, signal}`，sse dto 包），供 `SseMetaDTO.masterySignals` 使用。**不得**直接复用领域 `MasterySignalItem`（其 `kpLabel` 标 `@JsonProperty("kp_label")`，会序列化成 snake_case，不符合前端契约）。

#### Scenario: masterySignals 序列化 camelCase
- **WHEN** `buildMeta` 将 `action.getMasterySignals()` 映射为 `List<SseMasterySignalDTO>`
- **THEN** `meta.masterySignals` 序列化为 `[{"kpLabel": "...", "signal": "..."}]`（camelCase kpLabel）

### Requirement: buildMeta 带出新字段

系统 SHALL 在 `buildMeta` 中带出 `decideReason`（无条件 set `action.getReason()`）、`questionKps`、`masterySignals`（映射自 `action.getMasterySignals()`）；护栏拒绝时 set `denied` + `reason`（`reason` 语义与既有行为不变）。

#### Scenario: 全字段带出
- **WHEN** `buildMeta` 执行（放行或拒绝路径）
- **THEN** meta 含 decideReason（Python 理由）、questionKps、masterySignals 对应值；拒绝路径额外含 denied/denied 轮 reason（护栏 code）

### Requirement: decide 事件时序稳定（阶段二冻结契约）

前端阶段二 SENDING 期连续消费 decide 阶段 agent 事件做 live 走查，系统 SHALL 保持 decide 阶段事件时序稳定：`agent(perceive) → agent(analyze) → agent(plan) → agent(decide) → meta`，不得重排或丢失。此为本变更 filter 透传的既有保证，阶段二前端依赖此顺序（见"阶段二契约冻结"结论，后端无新增改动）。

#### Scenario: SENDING 期 live 走查按序消费
- **WHEN** 前端处于 SENDING 期，连续消费 decide 阶段 agent 事件
- **THEN** 事件按 perceive→analyze→plan→decide 顺序到达，meta 最后定型，无乱序/丢序导致"解析中"状态异常
