## 1. decide SSE 消费（BREAKING）

- [x] 1.1 `TutoringLlmClient.decide` 改 `bodyToFlux(SSE)` 过滤 `meta` 事件取 ActionMeta（`readActionMeta` + ObjectMapper）；空流/error（无 meta）→ `TutoringAgentException`
- [x] 1.2 decide 重试/超时语义验证：仅连接失败（未收到事件）重试 1 次；空流不重试；超时 = 等 meta 超时（`Mono.retry` 只在 error 触发；空流/error 正常完成无 meta → null → 抛异常，TutoringLlmClientTest 覆盖）

## 2. 编排层 agent 事件注入

- [x] 2.1 `TutoringAppService` 新增 `agentEvent(stage,label,status,detail)` 帮助方法 + guardrail/memory 阶段常量（`{level:"sub",...}`，LinkedHashMap + SSE_MAPPER 序列化）
- [x] 2.2 orchestrate 在护栏通过后、generate 前注入 `agent(guardrail)`（detail 含放行/拒绝降级摘要，`guardDetail`）
- [x] 2.3 `buildStream` 过滤器从 `.filter(token)` 改 `.filter(token||agent)`：token 累积 AI 回复 + 透传，agent 原样中继；流尾注入 `agent(memory)`（落库后收尾信号，`memoryDetail`）
- [x] 2.4 确认 terminate / round-limit 分支不注入 guardrail 事件（无 generate 路径），行为不变（回归测试验证）

## 3. 测试

- [x] 3.1 `TutoringLlmClientTest` decide SSE 解析测试（mock WebClient：agent+meta+done → 取 meta；agent+error / 空流 → 抛 TutoringAgentException），3 用例
- [x] 3.2 `TutoringAppServiceTest`：正常流事件序列含 `agent(guardrail)` 前置 + `agent(memory)` 流尾；generate 透传 agent 事件；护栏拒绝降级 guardrail detail 正确；memory detail 含掌握度信号（+2 新用例，6 处既有断言按新序列更新）
- [x] 3.3 回归：start/sendMessage/requestAnswer/terminate/round-limit 既有测试全绿（TutoringAppServiceTest 37 + TutoringControllerTest 13 + ContextAssembler 5 + Transcript 2）
- [x] 3.4 编译 + 全量测试（application/interface；Department 集成测试为存量环境失败，与本次改动无关）

## 4. 契约同步与联调

- [x] 4.1 api.md 更新 SSE 事件序列（`agent(guardrail) → meta → agent(generate) → token* → agent(memory) → done`）+ `event: agent` 格式（本变更 api.md 已写）
- [x] 4.2 与模型端联调：真实 decide/generate SSE 事件序列（文字发起/消息轮实测：guardrail→meta→generate→token*→memory→done；decide 含 reason 字段容忍）——已实测
- [x] 4.3 test.md 记录实测事件覆盖（guardrail/memory 注入、agent 中继、decide error 边界）——已记录 §9

## 5. decide thinking 响应式中继（D7 演进，2026-08-13）

> 实施对接说明见 [decide-thinking-integration.md](decide-thinking-integration.md)（3 文件改动清单 + 契约要点）。
> 注意：`TutoringLlmClient.java` / `TutoringLlmPort.java` 当前为 HEAD 旧 JSON 版 decide，需先按该文档恢复 SSE 解析再改流式。

- [ ] 5.1 `TutoringLlmPort.decide(ctx)` → `decideStream(ctx): Flux<ServerSentEvent<String>>`（返回 Python decide 原始事件流）
- [ ] 5.2 `TutoringLlmClient.decideStream` 去 `.block()` / 去 `.filter(meta)` / 去 retry（流式不可重试，与 generate 一致），原样透传全事件
- [ ] 5.3 `TutoringAppService.orchestrate` 改响应式管线：`Sinks.One<ActionMeta>` + `Flux.concat(decideThinking, tail)`——thinking 实时中继、meta 到达后走 `postDecide`（抽取护栏/副作用/持久化/guardrail+generate）
- [ ] 5.4 decide 失败处理：流中途失败 → 已有会话 `friendlyErrorStream`（50005），start 阶段重抛；`metaSink` 收 error/空流无 meta 均按失败
- [ ] 5.5 并发锁适配：`withSessionLock` 改订阅时取锁、`postDecide` 副作用完成后释放（`unlock` 参数 + `doFinally` 兜底），generate 仍在锁外
- [ ] 5.6 测试：`TutoringAppServiceTest` mock 改 `decideStream`；新增"thinking 先于 guardrail/meta 到达"时序用例；`TutoringLlmClientTest` decide 用例改流式断言
- [ ] 5.7 api.md 已更新（decide thinking 中继 + 序列前置）；联调验证 decide thinking 实时透传、不入库（刷新后仅 generate thinking）
