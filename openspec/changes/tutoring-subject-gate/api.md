# tutoring-subject-gate API 接口文档

> 基础路径: `/api/tutoring`
>
> 更新日期: 2026-08-19

---

## 目录

- [通用说明](#通用说明)
- [1. 非数学题跳过响应（SSE）](#1-非数学题跳过响应sse)
- [2. subject-classify（Java↔Python 内部，不对前端开放）](#2-subject-classifyjavapython-内部不对前端开放)
- [前端调用注意事项](#前端调用注意事项)

---

## 通用说明

对外前端接口**契约不变**——仍是 `meta / token / done` 三段 SSE 事件流。本 change 新增一种响应场景：**非数学题时系统跳过并返回「仅支持数学」提示**。学科判定在 Java↔Python 内部完成（`subject-classify`），前端不感知。

---

## 1. 非数学题跳过响应（SSE）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/tutoring/sessions`（拍题非数学，不建会话）/ `/api/tutoring/sessions/{sessionId}/messages`（换题非数学） |
| 需要登录 | 是（STUDENT） |
| Content-Type | 文字：`application/json`；图片：`multipart/form-data` |

**触发条件**：`subject-classify` 判定新题学科非 `math`（如 `physics` / `chemistry`）。

**行为**：跳过——不建/不续会话、不调数学 decide/generate、不落题目/掌握度/错误事件，前端收到提示流。

### 响应参数（SSE 事件序列）

```
event: meta,  data: {"sessionId":null, "status":"ACTIVE", "type":"hint", "roundCount":0}
event: token, data: {"content":"目前仅支持数学答疑，换一道数学题试试吧。"}
event: done,  data: {"sessionId":null, "status":"ACTIVE", "roundCount":0}
```

| 事件 | 字段 | 说明 |
|------|------|------|
| meta | sessionId | 拍题非数学为 `null`（未建会话）；换题非数学为原会话 ID |
| meta | type | 复用 `hint`（提示语类型，不新建 ActionType） |
| token | content | 固定提示语「目前仅支持数学答疑，换一道数学题试试吧。」 |
| done | status | `ACTIVE`（未结束） |

### 请求示例

**JavaScript (fetch) 消费 SSE：**

```javascript
const es = new EventSource('/api/tutoring/sessions');
es.onmessage = (e) => {
  const data = JSON.parse(e.data);
  if (e.event === 'meta')  currentType = data.type;        // hint
  if (e.event === 'token') appendToChat(data.content);     // "目前仅支持数学答疑..."
  if (e.event === 'done')  finishTurn(data);               // sessionId=null（未建会话）
};
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未登录或 Session 过期 |
| 10005 | 无权访问 | 会话不属于当前用户 |

> 非数学题**不报错**（不是错误码），走正常 SSE 返回提示流。

---

## 2. subject-classify（Java↔Python 内部，不对前端开放）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/tutoring/subject-classify`（Python stateless 端点） |
| 认证 | 内部 token（复用 llm-gateway internalToken 模式） |
| 开放对象 | **仅 Java 网关**，不对前端开放 |

**用途**：判定题目学科（decide 之前）。支持文本和图片。

### 请求参数

```json
{
  "content": "自由落体运动的问题…",
  "image_url": null
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | String | 否 | 题目文本（与 image_url 至少一个非空） |
| image_url | String | 否 | 题目图片 URL（COS） |

### 响应参数

```json
{
  "subject": "physics"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| subject | String | 闭集：`math` / `physics` / `chemistry` / `biology` / `other`；识别失败/异常 → 空（Java 按 math 放行） |

### 常见错误

| code | message | 说明 |
|------|---------|------|
| - | - | 不抛错：失败返回空 subject，Java 降级放行 |

---

## 前端调用注意事项

1. **学科判定对前端透明**：前端照常发题目（文字/图片），后端内部先判学科再决定走答疑还是「仅支持数学」。
2. **拍题非数学不建会话**：`meta.sessionId=null`，前端不能拿它做跳转；直接展示提示语即可。
3. **不消耗轮次**：非数学题不扣 20 轮上限，可继续换数学题。
4. **无落库**：非数学题不产生题目记录/掌握度/错误事件。
5. 若前端希望差异化展示（弹窗/图标提示「仅支持数学」），后续可在 meta 增加标记，本期不新增。

---

*文档生成时间: 2026-08-19*
