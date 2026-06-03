# LLM Gateway 测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证 `LlmApiController` 的所有业务场景，确保 LLM Gateway 集成功能的正确性和健壮性。

### 1.2 测试方式
- **集成测试**：直接注入 Controller，调用真实方法
- **Mock WebClient**：使用 MockWebServer 模拟 Python LLM 服务
- **Session 模拟**：使用 `MockHttpSession` 模拟用户登录状态

### 1.3 测试环境配置
- Profile: `test`
- Mock 服务：使用 MockWebServer 模拟 Python LLM Gateway
- Session：使用 `MockHttpSession` 模拟登录状态

---

## 2. 测试数据

| 参数 | 值 | 说明 |
|-----|-----|-----|
| TEST_USER_ID | 12345L | 测试用户 ID |
| TEST_MESSAGE | 你好，请介绍一下自己 | 测试消息 |
| TEST_SCENE | page_assistant | 测试场景 |
| TEST_PROVIDER | zhipu | 测试 Provider |
| TEST_MODEL | glm-4-flash | 测试模型 |
| TEST_SESSION_ID | sess_test_123 | 测试会话 ID |

---

## 3. 测试用例清单

### 3.1 同步对话接口 (POST /api/llm/chat)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| CHAT-001 | 正常对话-仅消息 | 用户已登录 | `{message: "你好"}` | 返回 AI 响应 |
| CHAT-002 | 正常对话-指定场景 | 用户已登录 | `{message: "你好", scene: "page_assistant"}` | 返回 AI 响应，使用场景默认模型 |
| CHAT-003 | 正常对话-指定模型 | 用户已登录 | `{message: "你好", provider: "zhipu", model: "glm-4-flash"}` | 返回 AI 响应，使用指定模型 |
| CHAT-004 | 正常对话-多轮 | 用户已登录 | `{message: "继续", sessionId: "sess_xxx"}` | 返回 AI 响应，保持上下文 |
| CHAT-005 | 异常-未登录 | 未登录 | `{message: "你好"}` | 返回 401 UNAUTHORIZED |
| CHAT-006 | 异常-消息为空 | 用户已登录 | `{message: ""}` | 返回 400 参数校验失败 |
| CHAT-007 | 异常-模型不在白名单 | 用户已登录 | `{message: "你好", provider: "xxx", model: "xxx"}` | 返回 60002 模型不允许调用 |
| CHAT-008 | 异常-LLM 服务不可用 | LLM 服务宕机 | `{message: "你好"}` | 返回 60001 LLM 服务不可用 |
| CHAT-009 | 异常-LLM 调用失败 | LLM 服务返回错误 | `{message: "你好"}` | 返回 60003 LLM 调用失败 |

### 3.2 流式对话接口 (POST /api/llm/chat/stream)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| STREAM-001 | 正常流式对话 | 用户已登录 | `{message: "你好"}` | 返回 SSE 事件流，包含 token 和 done 事件 |
| STREAM-002 | 流式对话-指定场景 | 用户已登录 | `{message: "你好", scene: "homework_grading"}` | 返回 SSE 事件流 |
| STREAM-003 | 异常-未登录 | 未登录 | `{message: "你好"}` | 返回 401 UNAUTHORIZED |
| STREAM-004 | 异常-流式错误 | LLM 服务返回错误 | `{message: "你好"}` | 返回 SSE error 事件 |

### 3.3 获取允许调用的模型列表 (GET /api/llm/allowed-models)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| MODEL-001 | 正常获取模型列表 | 无 | GET /api/llm/allowed-models | 返回模型列表，包含 defaultModel |
| MODEL-002 | 异常-LLM 服务不可用 | LLM 服务宕机 | GET /api/llm/allowed-models | 返回 60001 LLM 服务不可用 |

### 3.4 获取所有模型列表 (GET /api/llm/models)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| ALLMODEL-001 | 正常获取所有模型 | 无 | GET /api/llm/models | 返回按 Provider 分组的模型列表 |

### 3.5 获取场景列表 (GET /api/llm/scenes)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| SCENE-001 | 正常获取场景列表 | 无 | GET /api/llm/scenes | 返回场景列表，包含 code、defaultModel、description |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功 |
| 10001 | INVALID_PARAMS | 参数无效 |
| 10004 | UNAUTHORIZED | 未授权 |
| 60001 | LLM_SERVICE_UNAVAILABLE | LLM 服务不可用 |
| 60002 | LLM_MODEL_NOT_ALLOWED | 模型不允许调用 |
| 60003 | LLM_CALL_FAILED | LLM 调用失败 |
| 60004 | LLM_TIMEOUT | LLM 响应超时 |
| 60005 | LLM_INVALID_PARAMS | LLM 参数错误 |

---

## 5. 测试用例统计

| 模块 | 用例数量 |
|-----|---------|
| 同步对话接口 | 9 |
| 流式对话接口 | 4 |
| 获取允许调用的模型列表 | 2 |
| 获取所有模型列表 | 1 |
| 获取场景列表 | 1 |
| **总计** | **17** |

---

## 6. 测试执行顺序

测试按 `@Order` 注解指定的顺序执行：

```
100-109  : 同步对话接口测试
200-203  : 流式对话接口测试
300-301  : 获取允许调用的模型列表测试
400      : 获取所有模型列表测试
500      : 获取场景列表测试
```

---

## 7. 辅助方法

### 7.1 创建登录会话
```java
private MockHttpSession createLoginSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", TEST_USER_ID);
    session.setAttribute("username", "test_user");
    session.setAttribute("role", "STUDENT");
    return session;
}
```

### 7.2 模拟 LLM 服务成功响应
```java
private void mockLlmChatSuccess(MockWebServer server, String response) throws IOException {
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("{\"response\":\"" + response + "\",\"session_id\":\"sess_test\",\"model_used\":\"zhipu/glm-4-flash\",\"usage\":{\"total_tokens\":10}}"));
}
```

### 7.3 模拟 LLM 服务错误响应
```java
private void mockLlmError(MockWebServer server, int code, String message) throws IOException {
    server.enqueue(new MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody("{\"detail\":\"" + message + "\"}"));
}
```

### 7.4 模拟 SSE 流式响应
```java
private void mockLlmStreamSuccess(MockWebServer server) throws IOException {
    String sseResponse = "event: token\ndata: {\"content\":\"你好\"}\n\nevent: done\ndata: {\"model_used\":\"zhipu/glm-4-flash\",\"session_id\":\"sess_test\",\"usage\":{\"total_tokens\":10}}\n\n";
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(sseResponse));
}
```

---

## 8. 运行测试

```bash
# 运行 LLM 模块所有测试
cd ai-edu-backend && mvn test -pl ai-edu-interface -Dtest=LlmApiControllerTest

# 运行单个测试方法
mvn test -pl ai-edu-interface -Dtest=LlmApiControllerTest#testChatSuccess

# 运行集成测试（需要启动 Python LLM 服务）
mvn test -pl ai-edu-interface -Dtest=LlmApiControllerTest -Dspring.profiles.active=integration
```