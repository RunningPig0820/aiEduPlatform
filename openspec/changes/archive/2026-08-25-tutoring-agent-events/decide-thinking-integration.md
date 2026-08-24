# decide thinking 流式透传改动点（给后端）

> 本文档是 D7（design.md）决策的**实施对接说明**，给后端同学直接用。
> 对应 tasks.md 任务组 5（5.1~5.7）。前端对接见前端 change 的 `frontend-thinking-integration.md`。
> 状态：方案已定，代码未实施（当前 `TutoringLlmClient.java` / `TutoringLlmPort.java` 为 HEAD 旧 JSON 版 decide，需先按本清单改）。

---

## 一、问题背景

当前 `TutoringAppService.orchestrate` 第 343 行 `ActionMeta action = llmPort.decide(ctx)` 是**同步阻塞**的（WebClient `.block(decideTimeout)`）。首轮 decide 实测 17~48s，期间整个 SSE 响应尚未开始返回，前端收不到任何字节，只显示"AI 思考中…"占位。

而 Python decide 端（`decider.py`）**已经在流式吐 `event: thinking`**（`reasoning_content` 逐 delta），全被 `TutoringLlmClient.decide` 的 `.filter(e -> "meta".equals(e.event()))` 滤掉。

**目标**：让 decide 也流式——SSE 响应在 decide 一开始就建立，Python decide 的 thinking 事件实时透传前端，meta 到达后再走既有护栏 → 副作用 → generate。

---

## 二、改动清单（3 个文件）

### 1. `TutoringLlmPort`（ai-edu-domain）

`ActionMeta decide(DecideContext context)` → `Flux<ServerSentEvent<String>> decideStream(DecideContext context)`。返回 Python decide **原始事件流**（thinking*/agent*/meta/done 全保留，由编排层决定消费哪些）。只有 `orchestrate` 一处调用，直接替换签名。

```java
/**
 * 决策流（流式 SSE）：返回 Python decide 原始事件流（thinking*/agent*/meta/done 全保留），
 * 由编排层决定消费哪些（实时中继 thinking + 提取 meta）。
 *
 * <p>实现内部<b>不重试</b>（流式不可重试，重试会重发已透传的 thinking，失败由编排层降级）。
 */
Flux<ServerSentEvent<String>> decideStream(DecideContext context);
```

### 2. `TutoringLlmClient`（ai-edu-infrastructure）

`decideStream` 实现——复用现有 WebClient 调用，删掉 `.filter(meta).map(...).next().block(...)` 与 `.retry()`，直接 `return tutoringWebClient.post()...bodyToFlux(...)`。流式不可重试（与 generate 一致，失败由编排层降级）。

```java
@Override
public Flux<ServerSentEvent<String>> decideStream(DecideContext context) {
    log.info("[tutoring] decideStream 调用, history={}, round={}, answerReq={}, isNewQuestion={}",
            context.getHistory() == null ? 0 : context.getHistory().size(),
            context.getRoundCount(), context.getAnswerRequestCount(), context.isNewQuestion());
    return tutoringWebClient.post()
            .uri(config().decidePath())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM)          // 新增：必须声明 SSE
            .bodyValue(context)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .doOnNext(event -> log.trace("[tutoring] decide SSE: {}", event.data()))
            .onErrorResume(e -> {
                log.error("[tutoring] decide 调用失败: {}", e.getMessage(), e);
                return Flux.error(new TutoringAgentException("答疑决策服务暂不可用", e));
            });
}
```

> 注意：删 `.retry(config().agentRetry())`——流式不可重试（重试会重发已透传的 thinking 分片）。

### 3. `TutoringAppService.orchestrate`（ai-edu-application）——核心重构

**现状**（sync）：`orchestrate` 第 339~395 行：① decide 同步（L343）→ ② 安全终止（L345）→ ③ 护栏校验（L350）→ ④ 终止类 end（L353）→ ⑤ 轮次护栏拒绝（L358）→ ⑥ 放行 type（L363）→ ⑦ ensurePersisted（L368）→ ⑧ 落库副作用（L371~375）→ ⑨ archiveTranscript（L377）→ ⑩ `Flux.concat(agentEvent(guardrail), buildStream(...))`（L382~385）。

**新**（响应式管线）：

```java
private Flux<ServerSentEvent<String>> orchestrate(TutoringSession session, List<TutoringChatMessage> history,
                                                  boolean isNewQuestion) {
    List<StudentKpMastery> masteryList = masteryRepository.findByStudentId(session.getStudentId());
    DecideContext ctx = contextAssembler.buildDecideContext(session, history, masteryList, isNewQuestion);

    Sinks.One<ActionMeta> metaSink = Sinks.one();
    Flux<ServerSentEvent<String>> decideThinking = llmPort.decideStream(ctx)
            .doOnNext(e -> {
                if ("meta".equals(e.event())) metaSink.tryEmitValue(readActionMeta(e.data()));
                else if ("error".equals(e.event())) metaSink.tryEmitError(new TutoringAgentException("答疑决策服务暂不可用"));
            })
            .filter(e -> "thinking".equals(e.event()));   // 只中继 thinking，agent/done 丢弃

    Mono<Flux<ServerSentEvent<String>>> tail = metaSink.asMono()
            .map(action -> postDecide(session, action, history, unlock));  // 护栏+副作用+generate

    return Flux.concat(decideThinking, Mono.from(tail).flatMapMany(f -> f))
            .onErrorResume(e -> handleDecideFailure(session, e));
}
```

**时序**：`thinking*(decide) → agent(guardrail) → meta → agent(generate) → thinking*(generate) → token* → agent(memory) → done`。`Flux.concat` 保证 thinking 段先流完（decide 结束）才进 tail。

- **postDecide**：抽取现 `orchestrate` 第 345~385 行（安全终止 / 护栏校验 / 轮次护栏 / 放行 type / ensurePersisted / applySideEffects / archiveTranscript）→ 返回 `Flux.concat(agent(guardrail), buildStream(...))`，仍是同步代码。
- decide 的 agent 事件（perceive/analyze/plan/decide）仍不透传，`filter` 只放行 thinking。（⚠️ 2026-08-12 **已演进**：filter 放行 `thinking + agent`，见 `tutoring-agent-workflow-backend`）
- **错误**：decide 流中途失败 → `onErrorResume` → 已有会话 `friendlyErrorStream`（50005）；start 阶段（session.id==null）重抛由接口层映射。
- **并发锁**：`withSessionLock` 现为同步持锁（decide+副作用后释放、generate 在锁外）。流式化后 decide 在 Flux 内执行 → 锁改为**订阅时获取、postDecide 副作用完成后释放**（`unlock` 参数传入 orchestrate，错误路径 `doFinally` 兜底）。

---

## 三、契约要点（前端依赖）

| 项 | 值 |
|----|-----|
| 新事件序列 | `thinking*(decide) → agent(guardrail) → meta → agent(generate) → thinking*(generate) → token* → agent(memory) → done` |
| decide thinking | 实时中继，**不入库**（历史 thinking 只存 generate 段） |
| decide agent 事件 | 仍不透传（perceive/analyze/plan/decide 丢弃）。（⚠️ 2026-08-12 **已演进**：透传，见 `tutoring-agent-workflow-backend`） |
| 后端未演进时 | 前端 decideThinking 恒空、思考条不渲染，行为与改造前一致（向前兼容） |

---

## 四、不改的

- `ActionMeta` 契约内容、护栏/副作用/持久化/终止逻辑
- generate 段的 thinking 中继与落库（已工作）
- Python 端零改动（`decider.py` 已在流式吐 thinking）

---

## 五、实施顺序建议

1. `TutoringLlmPort` + `TutoringLlmClient`：改端口签名 + `decideStream` 实现（编译验证）
2. `TutoringAppService`：`orchestrate` 响应式重构 + `postDecide` 抽取 + 并发锁适配
3. 单测：`TutoringAppServiceTest` mock 改 `decideStream` + 新增"thinking 先于 guardrail/meta 到达"时序用例；`TutoringLlmClientTest` decide 用例改流式断言
4. 与模型端联调：真实 decide/generate SSE 事件序列（decide thinking 实时透传、不入库）
