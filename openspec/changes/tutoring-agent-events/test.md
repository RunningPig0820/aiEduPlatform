# AI 答疑 Agent 事件协议 测试用例设计

## 1. 测试概述

> **业务场景验收**（真实 Java↔Python 端到端、按业务流程组织）见 `scenarios.md`（S1-S8）；本文档为单元/组件级测试用例。

### 1.1 测试目标
验证 Java 侧接入 agent 事件协议的三处改动：decide SSE 消费（BREAKING）、generate 中继 agent 事件、guardrail/memory 事件注入。确保既有答疑业务逻辑（护栏/落库/SSE）不回归。

### 1.2 测试方式
- **单元测试（Mock）**：`TutoringLlmClientTest`（mock WebClient 返回 SSE 流）、`TutoringAppServiceTest`（mock LlmPort/仓储，真实护栏）
- **Controller 测试**：`TutoringControllerTest`（SSE 事件序列断言）
- 与模型端联调（real）：真实 decide/generate SSE 事件序列

### 1.3 测试环境配置
- Profile: `test`
- Mock WebClient / Mockito 注入
- Session：`MockHttpSession` 模拟（role=STUDENT）

---

## 2. 测试数据

| 参数 | 值 | 说明 |
|-----|-----|------|
| STUDENT_ID | 501L | 学生 ID |
| SESSION_ID | 1001L | 会话 ID |
| KP_URI | http://edukg.org/kp/1 | 知识点 URI |
| GUARDRAIL_STAGE | guardrail | 阶段 |
| MEMORY_STAGE | memory | 阶段 |

---

## 3. 测试用例清单

### 3.1 decide SSE 解析（TutoringLlmClient）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| DECIDE-001 | 正常流取 meta | mock WebClient 返回 `agent→meta→done` | DecideContext | 返回 meta 解析的 ActionMeta，type 正确 |
| DECIDE-002 | 流中无 meta（error） | mock 返回 `agent→error` | DecideContext | 抛 TutoringAgentException（50005） |
| DECIDE-003 | 空流 | mock 返回空 SSE | DecideContext | 抛 TutoringAgentException |
| DECIDE-004 | meta 解析失败 | mock 返回 meta 但 data 非法 JSON | DecideContext | 抛 TutoringAgentException |
| DECIDE-005 | 连接失败重试 | mock 首次 error 后成功 | DecideContext | 重试 1 次后返回 ActionMeta |

### 3.2 编排层 agent 事件（TutoringAppService）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| AGENT-001 | 正常轮事件序列 | decide=hint，generate 出 token | sendMessage | SSE 序列：agent(guardrail)→meta→agent(generate)→token→agent(memory)→done |
| AGENT-002 | guardrail detail（放行） | decide=hint | sendMessage | agent(guardrail) detail 含"放行: hint" |
| AGENT-003 | guardrail detail（拒绝降级） | decide=reveal 未授权 | sendMessage | agent(guardrail) detail 含"拒绝: reveal → 降级 approach"，meta.type=approach |
| AGENT-004 | generate 中继 agent 事件 | Python generate 流含 agent 事件 | sendMessage | agent 事件原样透传（event=agent，data 不变） |
| AGENT-005 | memory 事件在流尾 | 正常轮 | sendMessage | agent(memory) 在 token 后、done 前，detail 含掌握度信号（有信号时） |
| AGENT-006 | 终止场景无 guardrail | decide=end（无关内容） | sendMessage | 无 agent(guardrail) 事件，走既有 TERMINATED 流程 |
| AGENT-007 | 轮次上限无 guardrail | round≥20 护栏拒绝 | sendMessage | 无 agent(guardrail)，走既有 ROUND_LIMIT 流程 |
| AGENT-008 | generate 中继 thinking 事件 | Python generate 流含 thinking 分片 | sendMessage | thinking 原样透传（event=thinking, data={"content":...}）；不累积进 AI 回复；token 后序正常 |

### 3.3 回归

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| REG-001 | start 文字会话 | 登录 | start("鸡兔同笼") | meta→token→done 正常，round 落库 |
| REG-002 | 图片换题 | 登录，ACTIVE 会话 | 新图 multipart | is_new_question→switch→计数重置，事件序列含 agent(guardrail) |
| REG-003 | 请求答案第 2 次 | answer_count=1 | requestAnswer | reveal 放行，end=ANSWER_REVEALED |
| REG-004 | 未登录访问 | 无 session | POST /sessions | 401 |
| REG-005 | Python 调用失败 | decide 抛异常 | sendMessage | friendlyErrorStream（meta+"网络波动"+done），会话保持 ACTIVE |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 50005 | TUTORING_AGENT_FAILED | decide 空流/error/连接失败（对外） |
| 50002 | TUTORING_SESSION_NOT_FOUND | 会话不存在 |
| 50003 | TUTORING_SESSION_ENDED | 会话已结束 |
| 401 | UNAUTHORIZED | 未登录 |

---

## 5. 测试用例统计

| 模块 | 用例数量 |
|-----|---------|
| decide SSE 解析 | 5 |
| 编排层 agent 事件 | 7 |
| 回归 | 5 |
| **总计** | **17** |

---

## 6. 测试执行顺序

```
100-105  : TutoringLlmClient decide SSE 解析
200-206  : TutoringAppService agent 事件
300-304  : 回归（既有行为不破坏）
```

---

## 7. 辅助方法

### 7.1 mock WebClient 返回 SSE 流（decide）
```java
private void mockDecideSse(String... eventData) {
    Flux<ServerSentEvent<String>> flux = Flux.fromArray(eventData)
        .map(d -> ServerSentEvent.<String>builder().event("agent").data(d).build())
        .concatWith(Flux.just(ServerSentEvent.<String>builder()
            .event("meta").data("{\"type\":\"hint\",\"eval\":{...}}").build()))
        .concatWith(Flux.just(ServerSentEvent.<String>builder().event("done").data("{}").build()));
    when(webClient.post()).thenReturn(RequestBodyUriSpec...);
}
```

### 7.2 创建登录会话
```java
private HttpSession createStudentSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", STUDENT_ID);
    session.setAttribute("role", "STUDENT");
    return session;
}
```

---

## 8. 运行测试

```bash
cd ai-edu-backend
mvn test -pl ai-edu-application -Dtest='TutoringAppServiceTest,TutoringLlmClientTest'
mvn test -pl ai-edu-interface -Dtest='TutoringControllerTest'
```

---

## 9. 真实联调实测（2026-08-07，模型端已改 SSE）

与模型端联调实测通过，Java 侧事件注入/中继验证：

| 场景 | 实测事件序列 | 结果 |
|------|-------------|------|
| 文字发起会话 | `agent(guardrail 放行: hint)` → `meta` → `agent(generate)` → 32×`token` → `agent(memory)` → `done` | ✅ |
| 发送消息（答对）| 同序，`agent(memory)` detail=`"二元一次方程组 → practicing"`（掌握度信号落库可视化）| ✅ |
| decide 含 `reason` 调试字段 | meta 解析容忍未知字段，不报错（`FAIL_ON_UNKNOWN_PROPERTIES=false`）| ✅ |

**发现并修复的 bug**：初始 `JSON_MAPPER = new ObjectMapper()`（默认 FAIL_ON_UNKNOWN_PROPERTIES=true）无法解析 Python 的 `reason` 调试字段 → 已改配置容忍未知字段（与既有契约一致），TutoringLlmClientTest 锁定该行为。

**thinking 事件中继（2026-08-12）**：Python generate 新增 `event: thinking`（推理分片，不关思考、流式透传模型推理过程）。Java `buildStream` filter 放行 + 原样中继 thinking；**实测真实 E2E**：`agent(guardrail)→meta→agent(generate)→62×thinking→29×token→agent(memory)→done`，thinking 内容为模型真实推理（如"我正在打磨符合要求的引导性反问…"），token 为最终正文，二者不混淆；thinking 不累积进 AI 回复落库。TutoringAppServiceTest 新增 `sendMessage_relaysGenerateThinkingEvents`。

**thinking 落库（2026-08-12，历史消息保留思考过程）**：`TutoringChatMessage` 加 `thinking` 字段 + `buildStream` 累积 + `doOnComplete` 落库 + `ChatMessageDTO.thinking` 断点恢复回显。真实 E2E：getSession recentMessages ai 消息带完整推理文本、transcript ai 消息含 thinking、多轮各自落库、Python 容忍 thinking 字段无回归。单测：`sendMessage_relaysGenerateThinkingEvents`（thinking 落库）、`archive_writesAiThinking`、`getSession` thinking 映射。

**单元测试**：TutoringAppServiceTest 39 + TutoringLlmClientTest 3 + TutoringControllerTest 13 + ContextAssembler 5 + TranscriptArchiver 3 全绿。

