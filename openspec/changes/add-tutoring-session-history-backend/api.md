# AI 答疑会话历史 API 文档

> 基础路径: `/api/tutoring`
>
> 更新日期: 2026-08-13
> 本变更新增会话**列表**与**删除**接口，并让 COS transcript 消息携带工作流 meta。既有 `GET /sessions/{id}`（详情，返回签名 transcriptUrl + recentMessages）与 SSE 流式接口**均不变**。

---

## 目录

- [通用响应结构](#通用响应结构)
- [GET /api/tutoring/sessions（会话列表）](#1-get-apitutoringsessions会话列表)
- [DELETE /api/tutoring/sessions/{id}（删除会话）](#2-delete-apitutoringsessionsid删除会话)
- [COS transcript 消息结构（含 meta）](#3-cos-transcript-消息结构含-meta)
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

## 1. GET /api/tutoring/sessions（会话列表）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/tutoring/sessions` |
| Content-Type | `application/json` |
| 需要登录 | 是（STUDENT 角色） |

### 请求参数

**无请求体。** 学生 id 从鉴权上下文（Session）取，`user_id` 不信任客户端传入。

### 响应参数

成功时 `data` 返回该学生全部状态会话数组（`updated_at` 倒序，不含已删除）：

```json
[
  {
    "sessionId": 1001,
    "title": "鸡兔同笼怎么做",
    "status": "ACTIVE",
    "subject": "math",
    "questionType": "application",
    "roundCount": 3,
    "updatedAt": "2026-08-13T12:00:00",
    "archivedAt": null
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | Long | 会话 ID |
| title | String | 会话标题（首条用户消息前 ~30 字；存量/兜底可为空串或「图片题目」） |
| status | String | ACTIVE / ARCHIVED / TERMINATED |
| subject | String | 学科（本期恒 math） |
| questionType | String? | 题型（可空） |
| roundCount | Integer | 轮次计数 |
| updatedAt | String | 更新时间（yyyy-MM-dd'T'HH:mm:ss） |
| archivedAt | String? | 归档时间（可空） |

### 请求示例

**cURL:**
```bash
curl -X GET http://localhost:9627/api/tutoring/sessions \
  -H "Cookie: SESSION=..."
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/tutoring/sessions', {
  method: 'GET',
  credentials: 'include'
});
const result = await response.json();
// result.data => [{ sessionId, title, status, ... }]
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未登录或 Session 过期 |
| 20004 | 仅学生可访问 | 非 STUDENT 角色 |

---

## 2. DELETE /api/tutoring/sessions/{id}（删除会话）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `DELETE` |
| 接口路径 | `/api/tutoring/sessions/{sessionId}` |
| Content-Type | `application/json` |
| 需要登录 | 是（STUDENT 角色） |

### 请求参数

**Path**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| sessionId | Long | 是 | > 0 | 会话 ID |

**无请求体。** 归属校验：会话 `student_id` 与当前登录用户不一致返回 404（不泄露存在性）。

### 响应参数

软删除成功后返回成功空 `data`：

```json
{
  "code": "00000",
  "message": "success",
  "data": null
}
```

> 语义：软删（`is_deleted=1`）+ 清 Redis 缓存；COS transcript/题目图片**保留**（可恢复）。

### 请求示例

**cURL:**
```bash
curl -X DELETE http://localhost:9627/api/tutoring/sessions/1001 \
  -H "Cookie: SESSION=..."
```

**JavaScript (fetch):**
```javascript
const response = await fetch(`/api/tutoring/sessions/${sessionId}`, {
  method: 'DELETE',
  credentials: 'include'
});
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未登录或 Session 过期 |
| 20004 | 仅学生可访问 | 非 STUDENT 角色 |
| 50002 | 会话不存在 | 会话不存在 / 非本人会话 / 已软删除 |

---

## 3. COS transcript 消息结构（含 meta）

详情内容加载：前端调 `GET /sessions/{id}`（既有接口，不变）拿到**签名 `transcriptUrl`** 与 `status`，随后 `fetch` 该 URL 拉取 transcript JSON。消息项在既有 `role/content/image_url/thinking/created_at` 基础上**新增 meta 字段**（snake_case，可空）：

```json
{
  "session_id": 1001,
  "student_id": 501,
  "status": "ARCHIVED",
  "subject": "math",
  "created_at": "2026-08-13T11:00:00",
  "updated_at": "2026-08-13T12:00:00",
  "messages": [
    {
      "role": "user",
      "content": "鸡兔同笼怎么做",
      "image_url": null,
      "thinking": null,
      "created_at": "2026-08-13T11:00:01"
    },
    {
      "role": "ai",
      "content": "先假设全是鸡……",
      "image_url": null,
      "thinking": "设 x 只兔……",
      "created_at": "2026-08-13T11:00:10",
      "type": "approach",
      "denied": null,
      "decide_reason": "学生第一次要思路，给分步引导",
      "round": 1,
      "question_kps": ["鸡兔同笼", "二元一次方程组"],
      "eval": { "correct": false, "error_type": "equation_setup", "emotion": "CONFUSED", "exercise_complete": false },
      "status": "ACTIVE"
    }
  ]
}
```

### 消息项字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| role | String | user / ai |
| content | String | 消息内容 |
| image_url | String? | 图片消息 COS URL |
| thinking | String? | AI generate 思考过程（generate thinking 分片拼接；无思考轮/学生消息为 null） |
| created_at | String | 消息时间 |
| type | String? | **AI 消息生效类型**（hint/approach/reveal/concept/switch/end，护栏降级后）；用户消息为空 |
| denied | String? | 护栏拒绝时的原始请求类型（如 reveal）；无拒绝为空 |
| decide_reason | String? | Python 决策自由文本 |
| round | Integer? | 当前轮次 |
| question_kps | List\<String\>? | 题目涉及知识点 |
| eval | Object? | 评估 `{correct, error_type, emotion, exercise_complete}`（snake_case 内字段） |
| status | String? | 该轮会话状态（ACTIVE/ARCHIVED/TERMINATED） |

> **前端消费提示**：`toMessage` 已兼容 `decide_reason||decideReason`、`question_kps||questionKps`；`eval.exerciseComplete` 需兼容 snake_case `exercise_complete`（见设计 R2）。

### transcriptUrl 为空时的兜底

新会话仅首条用户消息（尚无 AI 回合）→ `transcriptUrl` 为空 → 前端回退 `GET /sessions/{id}` 的 `recentMessages`（Redis 热存）。

---

## 错误码说明

### 通用错误码

| code | message | 说明 |
|------|---------|------|
| 00000 | success | 成功 |
| 10004 | 未登录 | 未登录或 Session 过期 |
| 20004 | 仅学生可访问 | 非 STUDENT 角色 |

### 答疑错误码 (500xx)

| code | message | 说明 |
|------|---------|------|
| 50002 | 会话不存在 | 会话不存在 / 非本人 / 已删除 |
| 50003 | 会话已结束或已归档 | 对非 ACTIVE 会话发消息 |
| 50004 | 创建会话过于频繁 | 频率限制（窗口内 > 上限） |
| 50005 | 答疑服务暂不可用 | Python agent 调用失败（重试后） |
| 50006 | 仅支持 jpg/png/webp/bmp | 图片格式/OCR 无效 |

---

## 前端调用注意事项

### 1. Session 管理

所有接口需登录，请求必须携带 `credentials: 'include'`（Session Cookie）。列表/删除的学生 id 均从服务端 Session 取，前端**无需也不应传 user_id**。

### 2. 列表刷新

删除当前打开的会话后回到新建态并刷新列表；列表随 `updated_at` 变化倒序排列，会话有新轮次后需重新拉取。

### 3. 详情内容 = COS transcript

历史详情以 `GET /sessions/{id}` 的 `transcriptUrl`（短时签名）为主数据源，即时 fetch；签名过期则重调 `GET /sessions/{id}` 刷新。`transcriptUrl` 为空或拉取失败时回退 `recentMessages`，再失败回退 localStorage 离线兜底。

---

*文档生成时间: 2026-08-13*
