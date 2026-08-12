## ADDED Requirements

### Requirement: decide SSE 流式消费（BREAKING）

系统 SHALL 将 `TutoringLlmClient.decide` 的消费方式从"读响应 JSON（`bodyToMono(ActionMeta)`）"改为"解析 SSE 流，过滤 `meta` 事件，取其 data 解析为 ActionMeta"。decide 响应格式为 `agent 阶段事件 → meta(ActionMeta) → done`。

#### Scenario: 正常 decide 返回 ActionMeta
- **WHEN** Java 调用 decide 且 Python 返回合法 SSE 流（含 meta 事件）
- **THEN** Java 从 meta 事件 data 解析出 ActionMeta，字段与流式化前一致（闭集 type/eval/mastery_signals 等）
- **THEN** decide 调用正常返回，编排层护栏逻辑不变

#### Scenario: 流中无 meta 事件（error 或空流）
- **WHEN** Python 返回 `event: error` 或空流（无 meta 事件）
- **THEN** Java 视为 agent 调用失败，抛 `TutoringAgentException`（对外 50005）
- **THEN** 会话保持 ACTIVE 不中断（已有会话降级"网络波动"）

#### Scenario: 重试语义
- **WHEN** decide 连接失败（未收到任何 SSE 事件）
- **THEN** 按配置重试 1 次（无副作用）
- **WHEN** 已收到事件后失败（空流/error）
- **THEN** 不重试，直接按失败处理

### Requirement: generate 中继 agent 事件

系统 SHALL 在 `TutoringAppService.buildStream` 中透传 Python generate 流的 `agent` 事件（与 `token` 一起中继给前端），供前端按协议渲染阶段。generate 流事件为 `meta(action_type) → agent(generate) → token* → done`。

#### Scenario: generate 正常流
- **WHEN** Python generate 返回含 agent 事件与 token 的 SSE 流
- **THEN** Java 透传 `agent` 事件（原样中继 `event: agent`）与 `token` 事件（累积 AI 回复）
- **THEN** Python 的 meta/done 事件被丢弃（Java 自建 meta/done）

### Requirement: 注入 guardrail 事件

系统 SHALL 在护栏审批通过后、调 generate 前，由 Java 发射 `event: agent`（stage=guardrail, label=安全把关, status=done）事件，透传给前端。

#### Scenario: 护栏放行
- **WHEN** 护栏审批通过（type=guardrail 相关动作放行）
- **THEN** Java 在 generate 前发射 `agent(guardrail)`，detail 含放行类型（如 "放行: hint"）

#### Scenario: 护栏拒绝降级
- **WHEN** 护栏拒绝（如 reveal 未授权 → 降级 approach）
- **THEN** Java 发射 `agent(guardrail)`，detail 含拒绝摘要与降级类型（如 "reveal 超限,降级 approach"）

### Requirement: 注入 memory 事件

系统 SHALL 在掌握度落库完成后，由 Java 发射 `event: agent`（stage=memory, label=记忆更新, status=done）事件。memory 事件由 Java 发（Python 已删占位，不会双发）。

#### Scenario: 记忆更新
- **WHEN** 掌握度落库完成（mastery_signals 解析 label→URI、`t_student_kp_mastery` 更新、transcript 归档）
- **THEN** Java 在 generate token 流结束后、done 前发射 `agent(memory)`，detail 汇总本轮 mastery 信号（如 "二元一次方程组 → 练习中"）
- **THEN** 前端收到完整的 `agent(guardrail) → meta → agent(generate) → token* → agent(memory) → done` 事件序列

### Requirement: agent 事件格式与阶段表

系统 SHALL 使用标准 agent 事件格式 `{level, stage, label, status, detail}` 发射 Java 侧的 guardrail/memory 事件，`level` 恒为 `sub`（`master` 预留），`stage` 属于标准阶段表（perceive/analyze/plan/tool/decide/generate/memory/guardrail）。

#### Scenario: 事件格式
- **WHEN** Java 发射 guardrail/memory 事件
- **THEN** 事件格式为 `event: agent` + data `{"level":"sub","stage":"guardrail|memory","label":"安全把关|记忆更新","status":"done","detail":"..."}`
- **THEN** data 可由前端按协议解析渲染
