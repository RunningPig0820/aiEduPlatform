## 1. decide agent 事件透传

- [x] 1.1 `TutoringAppService.orchestrate`：decide filter 从 `only thinking` 改为 `thinking + agent`（透传 Python decide 的 perceive/analyze/plan/decide 事件）✅ 已实现

## 2. ActionMeta 契约

- [x] 2.1 `ActionMeta` 新增 `reason`（String，Python 决策自由文本，可空）✅ 已实现
- [x] 2.2 `ActionMeta` 新增 `questionKps`（List<String>，`@JsonProperty("question_kps")`，可空）✅ 已实现

## 3. SseMetaDTO 契约

- [x] 3.1 `SseMetaDTO` 新增 `decideReason`（Python 决策自由文本，buildMeta 无条件 set，null ok；`reason` 保持护栏拒绝原因语义不动）✅ 已实现
- [x] 3.2 `SseMetaDTO` 新增 `questionKps`（List<String>）✅ 已实现
- [x] 3.3 `SseMetaDTO` 新增 `masterySignals`（List<SseMasterySignalDTO>，camelCase {kpLabel, signal}）✅ 已实现
- [x] 3.4 新建 `SseMasterySignalDTO`（camelCase，sse dto 包；**不复用** `MasterySignalItem`——其 `kpLabel` 标 `@JsonProperty("kp_label")` 会序列化成 snake_case）✅ 已实现

## 4. buildMeta

- [x] 4.1 `buildMeta` 带出 decideReason/questionKps/masterySignals；护栏拒绝时 set denied + reason（语义不变）✅ 已实现

## 5. 测试与文档

- [x] 5.1 `TutoringAppServiceTest.sendMessage_decideThinkingRelayedFirst` 更新：decide agent 事件透传断言（改 filter 后原断言序列会挂，须同步）✅ 已实现（42/42 绿）
- [x] 5.2 新增 meta 新字段断言（decideReason/questionKps/masterySignals）✅ 已实现（TutoringLlmClientTest 3/3 绿）
- [x] 5.3 `openspec/changes/tutoring-agent-events/api.md` 契约更新（decide agent 透传 + meta 新字段说明）✅ 已完成（2026-08-12）
