# LLM Gateway API 接口文档

> 基础路径: `/api/llm`
>
> 更新日期: 2026-03-24

---

## 目录

- [通用响应结构](#通用响应结构)
- [1. 同步对话](#1-同步对话)
- [2. 流式对话](#2-流式对话)
- [3. 获取允许调用的模型列表](#3-获取允许调用的模型列表)
- [4. 获取所有模型列表](#4-获取所有模型列表)
- [5. 获取场景列表](#5-获取场景列表)
- [错误码说明](#错误码说明)
- [前端调用注意事项](#前端调用注意事项)

---

## 通用响应结构

所有接口均返回统一的 JSON 格式：

```json
{
  "code": "00000",
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | String | 状态码，`00000` 表示成功，其他为错误码 |
| message | String | 提示信息 |
| data | Object | 业务数据，可能为 null |

---

## 1. 同步对话

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/llm/chat` |
| Content-Type | `application/json` |
| 需要登录 | 是 |

### 请求参数

**RequestBody**

```json
{
  "message": "请解释这个页面的功能",
  "scene": "page_assistant",
  "provider": "zhipu",
  "model": "glm-4-flash",
  "sessionId": "sess_abc123",
  "pageCode": "homework_list",
  "context": {
    "page_meta": { "title": "作业列表" }
  }
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| message | String | 是 | 最大 10000 字符 | 用户消息内容 |
| scene | String | 否 | 见场景列表 | 场景代码，用于自动选择模型 |
| provider | String | 否 | - | 指定 Provider，需配合 model 使用 |
| model | String | 否 | - | 指定模型名称，需配合 provider 使用 |
| sessionId | String | 否 | - | 会话 ID，用于多轮对话 |
| pageCode | String | 否 | - | 当前页面编码 |
| context | Object | 否 | - | 额外上下文信息 |

**scene 可选值**:

| 值 | 说明 | 默认模型 |
|----|------|---------|
| page_assistant | 页面助手 | glm-4-flash (免费) |
| homework_grading | 作业批改 | deepseek-chat |
| faq | 常见问题 | glm-4-flash (免费) |
| image_analysis | 图片分析 | glm-4.6v |
| content_generation | 内容生成 | deepseek-chat |
| math_tutor | 数学辅导 | qwen-math-turbo |

### 响应参数

成功时 `data` 返回：

```json
{
  "response": "这个页面是作业列表页面...",
  "sessionId": "sess_abc123",
  "modelUsed": "zhipu/glm-4-flash",
  "usage": {
    "promptTokens": 150,
    "completionTokens": 80,
    "totalTokens": 230
  },
  "toolCalls": []
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| response | String | AI 响应内容 |
| sessionId | String | 会话 ID |
| modelUsed | String | 实际使用的模型，格式: `{provider}/{model}` |
| usage | Object | Token 使用量统计 |
| usage.promptTokens | Long | 提示词 Token 数 |
| usage.completionTokens | Long | 生成 Token 数 |
| usage.totalTokens | Long | 总 Token 数 |
| toolCalls | Array | 工具调用列表 |

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:9627/api/llm/chat \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSION=xxx" \
  -d '{
    "message": "你好",
    "scene": "page_assistant"
  }'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/llm/chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    message: '你好',
    scene: 'page_assistant'
  })
});
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 用户未登录或 Session 过期 |
| 60001 | LLM 服务不可用 | Python 服务连接失败 |
| 60002 | 模型不允许调用 | 指定的模型不在白名单中 |
| 60003 | LLM 调用失败 | LLM 服务返回错误 |

---

## 2. 流式对话

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/llm/chat/stream` |
| Content-Type | `application/json` |
| 响应类型 | `text/event-stream` (SSE) |
| 需要登录 | 是 |

### 请求参数

同 [同步对话](#1-同步对话)

### 响应格式 (SSE)

```
event: token
data: {"content": "你好"}

event: token
data: {"content": "！"}

event: done
data: {"modelUsed": "zhipu/glm-4-flash", "sessionId": "xxx", "usage": {"totalTokens": 10}}

event: error
data: {"code": "60003", "message": "LLM 调用失败"}
```

| event | 说明 |
|-------|------|
| token | 内容片段，data 包含 `content` 字段 |
| done | 流结束，data 包含 `modelUsed`, `sessionId`, `usage` |
| error | 错误，data 包含 `code`, `message` |

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:9627/api/llm/chat/stream \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSION=xxx" \
  -d '{"message": "你好", "scene": "page_assistant"}'
```

**JavaScript (EventSource):**
```javascript
const eventSource = new EventSource('/api/llm/chat/stream', {
  headers: {
    'Content-Type': 'application/json'
  }
});

eventSource.addEventListener('token', (event) => {
  const data = JSON.parse(event.data);
  console.log('Token:', data.content);
});

eventSource.addEventListener('done', (event) => {
  const data = JSON.parse(event.data);
  console.log('Done:', data.modelUsed, data.usage);
  eventSource.close();
});

eventSource.addEventListener('error', (event) => {
  const data = JSON.parse(event.data);
  console.error('Error:', data.code, data.message);
  eventSource.close();
});
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 用户未登录或 Session 过期 |
| 60001 | LLM 服务不可用 | Python 服务连接失败 |
| 60002 | 模型不允许调用 | 指定的模型不在白名单中 |
| 60003 | LLM 调用失败 | LLM 服务返回错误 |

---

## 3. 获取允许调用的模型列表

**重要**: 调用时只能使用此列表中的模型！

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/llm/allowed-models` |
| 需要登录 | 否 |

### 响应参数

成功时 `data` 返回：

```json
{
  "allowedModels": [
    {
      "provider": "zhipu",
      "model": "glm-4-flash",
      "fullName": "zhipu/glm-4-flash",
      "displayName": "GLM-4-Flash",
      "free": true,
      "supportsTools": true,
      "supportsVision": false,
      "description": "免费模型，适合大多数场景"
    },
    {
      "provider": "deepseek",
      "model": "deepseek-chat",
      "fullName": "deepseek/deepseek-chat",
      "displayName": "DeepSeek Chat",
      "free": false,
      "supportsTools": true,
      "supportsVision": false,
      "description": "通用对话模型"
    }
  ],
  "defaultModel": "zhipu/glm-4-flash"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| allowedModels | Array | 允许调用的模型列表 |
| allowedModels[].provider | String | Provider 名称 |
| allowedModels[].model | String | 模型名称 |
| allowedModels[].fullName | String | 完整名称，格式: `{provider}/{model}` |
| allowedModels[].displayName | String | 显示名称 |
| allowedModels[].free | Boolean | 是否免费 |
| allowedModels[].supportsTools | Boolean | 是否支持工具调用 |
| allowedModels[].supportsVision | Boolean | 是否支持视觉 |
| allowedModels[].description | String | 模型描述 |
| defaultModel | String | 默认模型 |

### 请求示例

**cURL:**
```bash
curl http://localhost:9627/api/llm/allowed-models
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/llm/allowed-models');
const result = await response.json();
console.log(result.data.allowedModels);
```

---

## 4. 获取所有模型列表

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/llm/models` |
| 需要登录 | 否 |

### 响应参数

成功时 `data` 返回：

```json
{
  "providers": [
    {
      "name": "zhipu",
      "displayName": "智谱 AI",
      "models": [
        {
          "model": "glm-4-flash",
          "displayName": "GLM-4-Flash",
          "free": true
        }
      ]
    }
  ]
}
```

### 请求示例

**cURL:**
```bash
curl http://localhost:9627/api/llm/models
```

---

## 5. 获取场景列表

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/llm/scenes` |
| 需要登录 | 否 |

### 响应参数

成功时 `data` 返回：

```json
{
  "scenes": [
    {
      "code": "page_assistant",
      "defaultProvider": "zhipu",
      "defaultModel": "glm-4-flash",
      "description": "页面助手 - 解释当前页面内容"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| scenes | Array | 场景列表 |
| scenes[].code | String | 场景代码 |
| scenes[].defaultProvider | String | 默认 Provider |
| scenes[].defaultModel | String | 默认模型 |
| scenes[].description | String | 场景描述 |

### 请求示例

**cURL:**
```bash
curl http://localhost:9627/api/llm/scenes
```

---

## 错误码说明

### 通用错误码 (1xxxx)

| code | message | 说明 |
|------|---------|------|
| 00000 | success | 成功 |
| 10000 | 系统错误 | 服务器内部错误 |
| 10001 | 参数错误 | 请求参数格式不正确 |
| 10002 | 实体不存在 | 请求的资源不存在 |
| 10003 | 参数无效 | 参数校验失败 |
| 10004 | 未登录 | 用户未登录或 Session 过期 |

### LLM 模块错误码 (6xxxx)

| code | message | 说明 |
|------|---------|------|
| 60001 | LLM 服务不可用 | Python 服务连接失败 |
| 60002 | 模型不允许调用 | 指定的模型不在白名单中 |
| 60003 | LLM 调用失败 | LLM 服务返回错误 |
| 60004 | LLM 响应超时 | LLM 服务响应超时 |
| 60005 | LLM 参数错误 | 请求参数不符合 LLM 要求 |

---

## 前端调用注意事项

### 1. Session 管理

本系统使用 Spring Session + Redis 管理 Session，前端需要：

- **携带 Cookie**: 所有需要登录的接口，请求时必须携带 `credentials: 'include'`
- **跨域配置**: 开发环境需配置 CORS 允许携带凭证

```javascript
// fetch 请求示例
fetch('/api/llm/chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include', // 重要：携带 Cookie
  body: JSON.stringify({ message: '你好' })
});
```

### 2. SSE 流式响应处理

流式对话使用 Server-Sent Events (SSE)，前端需要：

- 使用 `EventSource` 或 `fetch` + `ReadableStream` 处理
- 注意处理 `error` 事件和连接断开重连
- 流式请求无法使用 EventSource POST，建议使用 fetch + ReadableStream

**推荐实现 (fetch + ReadableStream):**
```javascript
async function streamChat(message, onToken, onDone, onError) {
  const response = await fetch('/api/llm/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ message })
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop();

    for (const line of lines) {
      if (line.startsWith('event:')) {
        // 处理事件类型
      } else if (line.startsWith('data:')) {
        // 处理数据
      }
    }
  }
}
```

### 3. 模型选择建议

- **优先使用 scene**: 让后端自动选择合适的模型，减少前端复杂度
- **明确指定 provider + model**: 当需要使用特定模型时使用
- **获取 allowed-models**: 展示可用模型列表供用户选择

---

*文档生成时间: 2026-03-24*