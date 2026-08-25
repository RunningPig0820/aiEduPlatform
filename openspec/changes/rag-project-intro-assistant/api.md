# RAG 项目介绍助手 API 接口文档

> 基础路径: `/api/rag/assistant`
>
> 更新日期: 2026-08-25
>
> 调用方: 前端学生页（需登录，Session 角色必须为 STUDENT）

---

## 目录

- [通用响应结构](#通用响应结构)
- [1. 发起问答（SSE 流式）](#1-发起问答sse-流式)
- [2. 发起问答（非流式）](#2-发起问答非流式)
- [3. 关闭对话（结算会话累计 token）](#3-关闭对话结算会话累计-token)
- [4. 断线补查单轮结果](#4-断线补查单轮结果)
- [5. 获取评估报告](#5-获取评估报告)
- [错误码说明](#错误码说明)
- [前端调用注意事项](#前端调用注意事项)

---

## 通用响应结构

所有非流式接口均返回统一的 JSON 格式：

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

> 边界回答、无权限、降级属于**正常业务结果**，在 SSE `done` 事件的 `reason` 字段以标志返回，不是错误码。角色非学生返回固定 403。

---

## 1. 发起问答（SSE 流式）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/rag/assistant/ask` |
| Content-Type | `application/json` |
| 需要登录 | 是（Session，角色须为 STUDENT） |
| 支持流式 | 是（`stream=true` 走 SSE） |

### 请求参数

**RequestBody**

```json
{
  "current_project": "ai-tutoring",
  "question": "这个项目的整体架构是什么？",
  "session_id": "sess-001",
  "history": [
    { "question": "RAG 是什么？", "answer": "……", "anchor": "ai-tutoring" }
  ],
  "trace_id": "trc-abc123",
  "stream": true,
  "top_k": 3
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| current_project | String | 否 | 覆盖模块 id（ai-tutoring/rag-system 等） | 页面锚定；缺省=全局模式。**仅作提示，角色以 session 为准** |
| question | String | 是 | 非空，长度 ≤ 500 | 学生问题 |
| session_id | String | 否 | 会话 id | 续接会话（锚点 + 轮次计数 + switch/clarify 判定） |
| history | Array | 否 | 最近 N 轮（默认 3），含 clarify 轮 | **Java 网关组装**传入（每轮过手 done 天然有）；供 intent/rewrite/clarify 兜底消费，Python 只读 |
| trace_id | String | 否 | 非空 | **Java 生成**传 Python；Python 贯穿日志并在 done 回显（两端 trace 一致） |
| stream | Boolean | 否 | 默认 false | true 时走 SSE 流式 |
| top_k | Integer | 否 | 1~5，默认 3 | RRF 精排块数（建议保持默认 3） |

> **permission 归属**：`permission` 事件仅由 Java 网关产出（角色门在 Java）；Python 侧 API 不产 permission（从 `intent` 开始）。Java 桥中继时从 Python 的 `intent` 事件开始转发，不消费/不透传 Python 侧任何 permission。

### SSE 事件序列

`Content-Type: text/event-stream`，事件按固定时序：

| 事件 | data 内容 | 说明 |
|------|----------|------|
| `permission` | `{role, allowed}` | 角色门结果（正常流恒为 allowed=true；非学生不会到 SSE） |
| `intent` | `{anchor, category, switchDetected, ambiguous, candidates, lockedSections, degraded}` | LLM 意图分类结果（anchor=模块路由 + lockedSections=节级加权两层并存；candidates=歧义候选模块，供 clarify） |
| `clarify` | `{message, candidates, default}` | 澄清轮，随后 `done` |
| `switch` | `{fromAnchor, toAnchor}` | 上下文切换，随后按新锚点 continue |
| `rewrite` | `{originalQuestion, rewrittenQuery}` | Query 改写结果 |
| `rerank` | `{blocks: [{blockId, title, summary, filePath, score}]}` | RRF 精排 Top-K 块（供引用面板，先灰显） |
| `boundary` | `{message, reason}` | 范围门低置信度过滤（reason=low_confidence），随后 `done` |
| `token` | `{text}` | 生成内容增量 |
| `done` | 完整结果（见下） | 流结束，携带 usage + quotedKeys + suggestions |

**`done` 事件 data 结构：**

```json
{
  "answer": "本系统采用 Java 网关编排 + Python 无状态 agent 的两段式架构……（AI答疑页§3）",
  "quotedKeys": ["block-01", "block-03"],
  "tokensUsage": {
    "promptTokens": 320,
    "completionTokens": 140,
    "cacheHitTokens": 0,
    "totalTokens": 460
  },
  "traceId": "trc-abc123",
  "suggestions": ["想了解RAG的整体架构吗？", "RRF融合算法有什么难点？"],
  "reason": null
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| answer | String | 生成答案（引用依据含于 quotedKeys） |
| quotedKeys | Array\<String\> | is_quoted 命中的精排块 blockId 集合（LCS 硬匹配，非 LLM 自述） |
| tokensUsage | Object | `{promptTokens, completionTokens, cacheHitTokens, totalTokens}`；cacheHit 估算时前端标注"估算" |
| traceId | String | 本轮 trace（断线后凭此补查） |
| suggestions | Array\<String\> | 完成后引导建议（1~3 条，LLM 生成，**必含 ≥1 条 RAG 方向**——RAG 始终带上，非并列模块）；开始引导另见 `GET /guide` |
| reason | String\|null | boundary=low_confidence / 超时降级=timeout，正常为 null |

### 请求示例

**cURL（SSE）:**
```bash
curl -X POST http://localhost:8080/api/rag/assistant/ask \
  -H "Content-Type: application/json" \
  -d '{"current_project":"ai-tutoring","question":"这个项目的整体架构是什么？","stream":true}'
```

**JavaScript (fetch，流式):**
```javascript
const response = await fetch('/api/rag/assistant/ask', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',   // 携带 Cookie（Session）
  body: JSON.stringify({ current_project: 'ai-tutoring', question: '这个项目的整体架构是什么？', stream: true })
});
const reader = response.body.getReader();
const decoder = new TextDecoder();
while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  const lines = decoder.decode(value).split('\n');
  for (const line of lines) {
    if (line.startsWith('data:')) {
      // 按 event 类型渲染：intent→阶段卡片 / rerank→引用面板(灰显) / token→正文 / done→高亮引用+成本+建议
    }
  }
}
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | Session 缺失或过期 |
| 403 | 仅学生可访问此助手 | 角色非 STUDENT（固定响应，不进 RAG 流程） |
| 10001 | 参数错误 | question 缺失/超长 |

---

## 2. 发起问答（非流式）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/rag/assistant/ask` |
| Content-Type | `application/json` |
| 需要登录 | 是（Session，角色须为 STUDENT） |

### 请求参数

同流式请求（`stream=false` 或省略）。

### 响应参数

成功时 `data` 返回（结构同流式 `done`，外加阶段摘要）：

```json
{
  "answer": "……",
  "quotedKeys": ["block-01"],
  "tokensUsage": { "promptTokens": 320, "completionTokens": 140, "cacheHitTokens": 0, "totalTokens": 460 },
  "traceId": "trc-abc123",
  "suggestions": ["……"],
  "reason": null,
  "stages": {
    "intent": { "anchor": "ai-tutoring", "category": "项目介绍", "switchDetected": false, "ambiguous": false },
    "rewrite": { "originalQuestion": "……", "rewrittenQuery": "……" },
    "rerank": [ { "blockId": "block-01", "title": "…", "summary": "…", "filePath": "docs/rag/ai-tutoring/03-架构…", "score": 0.82 } ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| stages.intent | Object | 意图分类结果（白盒展示） |
| stages.rewrite | Object | 改写前后对比 |
| stages.rerank | Array | 精排块（含 filePath 供"点击查看原文"） |
| stages.permission | Object | 角色门结果 |

### 常见错误

同流式。

---

## 3. 关闭对话（结算会话累计 token）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/rag/assistant/sessions/{sessionId}/close` |
| Content-Type | `application/json` |
| 需要登录 | 是（Session，角色须为 STUDENT） |

### 请求参数

**Path**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| sessionId | String | 是 | 会话 id | 要关闭的会话 |

### 响应参数

成功时 `data` 返回：

```json
{
  "sessionId": "sess-001",
  "closed": true,
  "rounds": 5,
  "sessionUsage": {
    "promptTokens": 1600,
    "completionTokens": 700,
    "cacheHitTokens": 200,
    "totalTokens": 2300
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | String | 会话 id |
| closed | Boolean | 是否已关闭（幂等：已关闭也返回 true） |
| rounds | Integer | 会话问答轮数 |
| sessionUsage | Object | 会话累计 token（`{promptTokens, completionTokens, cacheHitTokens, totalTokens}`） |

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/rag/assistant/sessions/sess-001/close \
  -H "Content-Type: application/json" \
  --cookie "SESSION=..."   # 携带登录 Cookie
```

**JavaScript (fetch):**
```javascript
const response = await fetch(`/api/rag/assistant/sessions/${sessionId}/close`, {
  method: 'POST',
  credentials: 'include'
});
const result = await response.json();
// result.data.sessionUsage 展示"本次对话总消耗"
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10002 | 会话不存在 | sessionId 无对应会话 |
| 10004 | 未登录 | Session 缺失或过期 |
| 403 | 仅学生可访问此助手 | 角色非 STUDENT |

---

## 4. 断线补查单轮结果

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/rag/assistant/turns/{traceId}` |
| Content-Type | `application/json` |
| 需要登录 | 是（Session，角色须为 STUDENT） |

### 响应参数

成功时 `data` 返回：

```json
{
  "traceId": "trc-abc123",
  "answer": "……",
  "quotedKeys": ["block-01"],
  "tokensUsage": { "promptTokens": 320, "completionTokens": 140, "cacheHitTokens": 0, "totalTokens": 460 },
  "suggestions": ["……"],
  "reason": null
}
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10002 | trace 不存在 | trace 超保留窗口或从未产生 |
| 10004 | 未登录 | Session 缺失或过期 |

---

## 5. 获取评估报告

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/rag/assistant/eval/report` |
| Content-Type | `application/json` |
| 需要登录 | 是（Session，角色须为 STUDENT） |

### 响应参数

成功时 `data` 返回：

```json
{
  "version": "2026-08-25-e966ac",
  "count": 15,
  "hitAt3": 0.80,
  "qualityAvg": 4.2,
  "avgLatencyMs": 5599,
  "avgCostYuan": 0.0157,
  "judgedRatio": 1.0
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| version | String | 语料版本（YYYY-MM-DD-sha1[:6]） |
| count | Integer | 评估集条数 |
| hitAt3 | Number | hit@3 平均 |
| qualityAvg | Number | 质量分平均（/5） |
| avgLatencyMs | Integer | 平均耗时 |
| avgCostYuan | Number | 单条平均成本（¥） |
| judgedRatio | Number | 判分成功率 |

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10002 | 暂无评估报告 | 尚未跑过评测 |
| 10004 | 未登录 | Session 缺失或过期 |

---

## 6. 获取开始引导（RAG 定向，非 SSE）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/rag/assistant/guide` |
| Content-Type | `application/json` |
| 需要登录 | 是（仅 STUDENT，非学生固定 403） |

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
| suggestions | Array | RAG 定向开始引导（定位/架构/数据流/评测/坑），1~3 条，静态池 0 token |

> **RAG 常驻**：RAG 是始终在底层运行的引擎（非展示页模块，学生无法导航到）。会话入口展示开始引导（定向 RAG）；每轮 `done.suggestions` 必含 ≥1 条 RAG 方向。guide 走非 SSE（会话开始无问答轮），不占冻结时序。

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 403 | 仅学生可访问此助手 | 非学生/角色缺失 |
| 10004 | 未登录 | Session 缺失或过期 |

---

## 错误码说明

### 通用错误码 (1xxxx)

| code | message | 说明 |
|------|---------|------|
| 00000 | success | 成功 |
| 10000 | 系统错误 | 服务器内部错误 |
| 10001 | 参数错误 | 请求参数格式不正确 |
| 10002 | 实体不存在 | 请求的资源不存在（trace 不存在 / 暂无评估报告） |
| 10004 | 未登录 | 用户未登录或 Session 过期 |

### 角色门 (固定 403)

| code | message | 说明 |
|------|---------|------|
| 403 | 仅学生可访问此助手 | 角色非 STUDENT（固定响应体，不进入 RAG 流程、不消耗 token） |

> 本模块无其它专用错误码：边界回答 / 无权限 / 降级均为正常业务结果（SSE `done.reason` 或 `data.reason` 标志位）。

---

## 前端调用注意事项

### 1. 认证与角色

- 使用 Spring Session + Redis，前端必须 `credentials: 'include'` 携带 Cookie。
- 角色只读自 session（后端），**前端不要在 body 传 role**——传了也会被忽略；非学生直接 403。
- 403 固定响应体"仅学生可访问此助手"，前端应引导切换学生账号。

### 2. 白盒事件渲染（关键）

- `intent` 到 `done` 的事件顺序即 RAG 链路顺序，前端按序渲染阶段卡片（权限→意图→改写→召回→生成）。
- `clarify` / `switch` / `boundary` 是**正常分支**，不是错误——按对应文案渲染，不要弹错误。
- `boundary`（low_confidence）出现时，展示"未找到关联文档，我尚未掌握"，并附当前 `rerank` 块（若有）。
- `clarify` 出现时，展示 candidates 让用户点选或输入明确功能名；`default` 提示为当前功能。

### 3. 引用面板（先灰后亮）

- `rerank` 事件到达即渲染块（灰显 + 折叠，点击 filePath "查看原文"）。
- `done` 的 `quotedKeys` 到达后，命中的块高亮展开、未命中的保持灰显折叠。
- `quotedKeys` 为空 → answer 已标注"引用未能精确匹配"，前端无需额外提示。

### 4. 成本与计费

- 流式在 `done` 取最终 `tokensUsage`（usage 流结束才返回）。
- `cacheHitTokens` 若为估算值，前端展示时标注"估算"。
- `suggestions` 为引导建议，渲染为可点击 chips，点击后作为新问题重走完整链路。

### 5. 断线与补查

- 前端断线后凭 `done` 之前收到的 `traceId` 调 `GET /turns/{traceId}` 补查该轮结果。
- 若 trace 已过期（10002），提示用户重发问题。

### 6. 关闭对话与结算

- 学生点击"结束对话"时调 `POST /sessions/{sessionId}/close`；若有在途流，前端同时取消 fetch（后端也会中止上游生成）。
- close 返回 `sessionUsage`（会话累计 token），前端展示"本次对话总消耗"（呼应 token 成本叙事）。
- 关闭后同 `session_id` 再提问 → 后端返回固定话术"本轮对话已结束，可开启新对话"，前端应引导开启新会话（新 session_id）。

---

*文档生成时间: 2026-08-25*
