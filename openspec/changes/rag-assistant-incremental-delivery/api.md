# RAG 项目介绍助手 API 接口文档（按里程碑）

> 基础路径: `/api/rag/assistant`
>
> 更新日期: 2026-08-25
>
> 说明：本文档供**前端按里程碑对接**。SSE 事件时序在 M2 冻结，下游里程碑只补字段。功能需求/错误码语义查 `rag-project-intro-assistant` 变更。

---

## 目录

- [通用响应结构](#通用响应结构)
- [SSE 事件契约（M2 冻结）](#sse-事件契约m2-冻结)
- [按里程碑对接清单](#按里程碑对接清单)
- [1. 发起问答（SSE 流式）](#1-发起问答sse-流式)
- [2. 关闭对话](#2-关闭对话)
- [3. 断线补查](#3-断线补查)
- [4. 获取评估报告](#4-获取评估报告)
- [错误码说明](#错误码说明)
- [前端调用注意事项](#前端调用注意事项)

---

## 通用响应结构

SSE 问答为独立事件流；其余接口返回统一 JSON：

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

## SSE 事件契约（M2 冻结）

> 时序 `permission → intent → (clarify|switch) → rewrite → rerank → (boundary) → token* → done`，**不得重排、不得删除**。字段 camelCase。

| 事件 | 归属里程碑 | 核心字段 | 说明 |
|------|-----------|---------|------|
| `permission` | M1 | `allowed` | 角色门结果，学生放行 true |
| `intent` | M2 | `anchor` `category` `switchDetected` `ambiguous` | 意图分析（闭集标签 + 锚点） |
| `switch` | M2 | `from` `to` | 功能切换（下一轮重路由） |
| `clarify` | M6 | `message` `candidates` `default` | 澄清追问（歧义才发） |
| `rewrite` | M2 | `rewrittenQuery` | 改写后检索式 |
| `rerank` | M3 | `blocks[]`（blockId/title/summary/filePath/score） | RRF 精排 Top-K |
| `boundary` | M3 | `reason`（=low_confidence）`message` | 范围门拒答（唯一拒答路径） |
| `token` | M4 | `delta` | 生成逐块 |
| `done` | M2(桩)/M4(真) | `answer` `tokensUsage` `traceId`；M5 补 `quotedKeys`；M6 补 `suggestions` | 收尾（Java 重建，不透传 Python meta/done） |

---

## 按里程碑对接清单

| 里程碑 | 前端可对接 | 接口/事件 |
|--------|-----------|----------|
| M1 权限判断 | 403 页 / 学生放行 | `POST /api/rag/assistant/ask`（SSE，首事件 permission） |
| M2 意图+改写 | 阶段展示区、桩替答案 | SSE 事件 permission/intent/rewrite/done（桩替） |
| M3 召回+边界 | 召回块面板、边界话术 | SSE 事件 rerank/boundary |
| M4 生成+token | 流式回答、成本面板 | SSE 事件 token*/done（真生成）+ tokensUsage |
| M5 自我检查 | 引用高亮、评估报告 | done 补 quotedKeys；`GET /api/rag/assistant/eval/report` |
| M6 问题提示 | 开始引导 chips（定向 RAG）+ 结束引导 chips（含 RAG）+ 澄清 UI | SSE 事件 clarify；done 补 suggestions；`GET .../guide`（开始引导，非 SSE） |
| M7 会话收尾 | 关闭按钮、结算面板、断线补查 | `POST .../sessions/{sessionId}/close`；`GET .../turns/{traceId}` |

---

## 1. 发起问答（SSE 流式）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/rag/assistant/ask` |
| Content-Type | `text/event-stream` |
| 需要登录 | 是（仅 STUDENT，非学生固定 403） |

### 请求参数

**RequestBody**

```json
{
  "question": "这个项目的整体架构是什么？",
  "sessionId": "sess-001",
  "currentProject": "ai-tutoring",
  "topK": 3
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| question | String | 是 | 非空、≤500 字符 | 学生问题 |
| sessionId | String | 是 | 非空 | 会话 id |
| currentProject | String | 否 | - | 页面锚定模块（clarify default 绑定、switch 判定用） |
| topK | Integer | 否 | 默认 3 | RRF 精排回传块数 |

> **role 字段禁止出现在 body**：前端传 role 一律忽略，角色只认可信 session。

### 响应参数

SSE 事件流（前端用 `EventSource` 语义消费，事件见上表）。非流式模式（`Accept: application/json`）返回 done 等价 JSON。

### 请求示例

**cURL:**
```bash
curl -N -X POST http://localhost:8080/api/rag/assistant/ask \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -b "JSESSIONID=..." \
  -d '{"question":"这个项目的整体架构是什么？","sessionId":"sess-001","currentProject":"ai-tutoring","topK":3}'
```

**JavaScript (fetch + ReadableStream):**
```javascript
const resp = await fetch('/api/rag/assistant/ask', {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json', 'Accept': 'text/event-stream' },
  body: JSON.stringify({ question, sessionId, currentProject, topK: 3 })
});
const reader = resp.body.getReader();
const decoder = new TextDecoder();
// 逐事件解析 permission→intent→rewrite→rerank→token*→done
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 403 | 仅学生可访问此助手 | 非学生/角色缺失（固定响应体，SSE 前返回） |
| 10001 | 参数无效 | question 缺失/超长 |

---

## 2. 关闭对话

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/rag/assistant/sessions/{sessionId}/close` |
| Content-Type | `application/json` |
| 需要登录 | 是（仅 STUDENT） |

### 请求参数

**Path**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 会话 id |

### 响应参数

成功时 `data` 返回：

```json
{
  "closed": true,
  "rounds": 5,
  "sessionUsage": { "promptTokens": 1600, "completionTokens": 700, "cacheHitTokens": 300, "totalTokens": 2600 }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| closed | Boolean | 会话已关闭 |
| rounds | Integer | 累计轮数 |
| sessionUsage | Object | 会话累计 token 四字段（prompt/completion/cache_hit/total） |

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 403 | 仅学生可访问此助手 | 非学生/角色缺失 |
| 10002 | 会话不存在 | sessionId 无对应会话 |

> 幂等：已关闭会话再次 close 仍返回 closed=true + 当前累计值。

---

## 3. 断线补查

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/rag/assistant/turns/{traceId}` |
| 需要登录 | 是（仅 STUDENT） |

### 响应参数

成功时 `data` 返回：

```json
{
  "answer": "……",
  "quotedKeys": ["block-01"],
  "tokensUsage": { "promptTokens": 320, "completionTokens": 140, "cacheHitTokens": 0, "totalTokens": 460 },
  "suggestions": ["想了解RAG的整体架构吗？"]
}
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 403 | 仅学生可访问此助手 | 非学生/角色缺失 |
| 10002 | trace 不存在 | traceId 无对应轮次 |

---

## 4. 获取评估报告

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/rag/assistant/eval/report` |
| 需要登录 | 是（仅 STUDENT） |

### 响应参数

成功时 `data` 返回：

```json
{
  "version": "2026-08-24-e966ac",
  "count": 15,
  "hitAtK": 0.8,
  "qualityAvg": 4.2,
  "latencyAvgMs": 5599,
  "costAvgYuan": 0.0157
}
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10002 | 暂无评估报告 | 尚未跑过评测 |

---

## 5. 获取开始引导（RAG 定向，非 SSE）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/rag/assistant/guide` |
| Content-Type | `application/json` |
| 需要登录 | 是（仅 STUDENT） |

### 响应参数

成功时 `data` 返回：

```json
{
  "suggestions": [
    { "title": "想了解RAG的整体架构吗？", "direction": "architecture" },
    { "title": "想知道知识库数据是如何流转的吗？", "direction": "data_flow" },
    { "title": "想看看评测体系是怎么设计的吗？", "direction": "evaluation" }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| suggestions | Array | RAG 定向引导（定位/架构/数据流/评测/坑），1~3 条，静态池 0 token |

> **不占 SSE 时序**：会话开始无问答轮，guide 为页面级一次拉取，不进冻结的 `permission → ... → done` 序列。RAG 是始终在底层运行的引擎（非展示页模块），开始与结束引导都必须带上 RAG。

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 403 | 仅学生可访问此助手 | 非学生/角色缺失 |

---

## 错误码说明

| code | 说明 |
|------|------|
| 00000 | 成功 |
| 10001 | 参数无效（question 缺失/超长） |
| 10002 | 实体不存在（trace/会话/评估报告不存在） |
| 10004 | 未登录 |
| 403 | 角色非 STUDENT（固定响应体，非标准错误码） |

---

## 前端调用注意事项

### 1. Session 管理

- **角色只认 session**：前端禁止在 body 传 role；非学生直接收到固定 403，前端展示 403 页，不发起 RAG 请求。
- **携带 Cookie**：所有接口请求带 `credentials: 'include'`；开发环境配置 CORS 允许携带凭证。

### 2. SSE 事件消费

- 按冻结时序渲染阶段展示区，**M2 后事件顺序不变**，后续只增字段。
- `permission{allowed:true}` 是流程起点；非学生不会收到任何 SSE 事件（直接 403）。
- `done` 由 Java 重建，`quotedKeys`（M5 起）/`suggestions`（M6 起）为追加字段，前端渲染时按字段存在与否分支。
- **问题提示的 RAG 常驻**：进入页面先拉 `GET /api/rag/assistant/guide`（开始引导，定向 RAG）；每轮 `done.suggestions` 必含 ≥1 条 RAG 方向（RAG 是始终在底层运行的引擎，非展示页模块，任何模块的回答后都把话题带回 RAG）。

### 3. 关闭对话

- 学生点"结束对话"调 close → 拿到会话累计 token 结算；已关闭会话再 ask 收到固定话术、不耗 token。
- 断线后凭 `done.traceId` 调补查接口恢复单轮结果。

### 4. 参数校验

- `question` 非空且 ≤500 字符；`topK` 默认 3 可配；`role` 字段在 body 中应被忽略。

---

*文档生成时间: 2026-08-25*
