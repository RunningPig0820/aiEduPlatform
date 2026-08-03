# AI 答疑 API 接口文档

> 基础路径: `/api/tutoring`
>
> 更新日期: 2026-08-03
> 认证: 全部接口需登录。`student_id`（即 userId）从 `HttpSession.getAttribute("userId")` 获取并校验 STUDENT 角色；请求体不传 student_id，服务端不信任客户端传入的身份。
> 拓扑: 前端 → Java 网关（认证/护栏/落库）→ Python 答疑 agent（decide/generate）。

---

## 通用响应结构

非流式接口统一返回：

```json
{
  "code": "00000",
  "message": "success",
  "data": { ... }
}
```

错误码：`40001`（会话不存在）、`40002`（会话已结束）、`40003`（创建会话过于频繁）、`40004`（Python 调用失败且重试后仍失败）。

---

## 1. 发起答疑会话

`POST /api/tutoring/sessions`

创建会话并返回首条引导（流式）。内部：安全预检 → 组装上下文 → Python `decide` → Java 护栏 → Python `generate`（流式）。

请求体：
```json
{ "message": "鸡兔同笼，共35头94脚，各几只？" }
```

响应（SSE 流式）：
```
event: meta, data: {"session_id":1001, "status":"ACTIVE", "type":"hint", "round_count":0}
event: token, data: {"content":"先找一下题目里的已知条件，你能列出来吗？"}
event: done,  data: {"session_id":1001, "status":"ACTIVE", "round_count":1}
```

**终止场景**（`meta` 事件即携带最终回复，无 token 流）：
- 无关内容 → `{"status":"TERMINATED", "type":"end", "reply":"我主要解答学科问题，请提出学习相关的内容"}`
- 学习方法 → `{"status":"TERMINATED", "type":"end", "reply":"<学习方法建议>"}`
- 非数学 → `{"status":"TERMINATED", "type":"end", "reply":"当前仅支持数学答疑"}`
- 安全命中 → `{"status":"TERMINATED", "type":"end", "reply":"该内容超出答疑范围..."}`

---

## 2. 发送学生回答（流式，类型先行）

`POST /api/tutoring/sessions/{sessionId}/messages`

发送一轮回答，SSE 返回 AI 回复。内部：追加消息 → 组装上下文 → Python `decide` → **Java 护栏校验动作** → 落库副作用 → Python `generate`（流式）→ 透传。

请求体：
```json
{ "content": "设鸡有x只，则兔有35-x只，2x + 4(35-x) = 94" }
```

SSE 事件序列：
```
event: meta, data: {"session_id":1001, "status":"ACTIVE", "type":"hint", "round_count":2,
                    "eval":{"correct":true,"emotion":"NEUTRAL","mastery_signals":[{"kp_label":"二元一次方程组","signal":"practicing"}]}}
event: token, data: {"content":"方程列出来了。如果考虑所有动物抬起两只脚，这个式子还成立吗？"}
event: done,  data: {"session_id":1001, "status":"ACTIVE", "round_count":2}
```

**护栏拒绝时**（无 token 流，meta 即降级结果）：
```
event: meta, data: {"session_id":1001, "status":"ACTIVE", "type":"approach",
                    "round_count":1, "denied":"reveal", "reason":"answer_count_insufficient"}
event: token, data: {"content":"思路：先设鸡为x、兔为y，根据头数列一个方程，根据脚数列第二个，联立求解。"}
event: done,  data: {"session_id":1001, "status":"ACTIVE", "round_count":2}
```

**收尾场景**（`done` 事件附带总结）：
```
event: done, data: {"session_id":1001, "status":"ARCHIVED",
                    "summary":{"knowledge_points":["二元一次方程组"],"weak_points":["代入消元法"]},
                    "end_reason":"COMPLETED"}
```

**错误码**：`40001` 会话不存在；`40002` 会话已结束/已归档。

---

## 3. 请求答案

`POST /api/tutoring/sessions/{sessionId}/request-answer`

学生显式请求答案（走答案护栏：第 1 次思路 / 第 2 次完整答案）。也可不调此接口——学生在消息里直接说"答案给我"由 `decide` 识别。

请求体：`{}`

响应（非流式，SSE 事件同接口 2）：
- 第 1 次 → `meta.type=approach`，`answer_request_count=1`，给思路
- 第 2 次 → `meta.type=reveal`，`answer_request_count=2`，给完整答案后收尾（end_reason=ANSWER_REVEALED）

**边界情况**：
- 会话已 ARCHIVED / TERMINATED → 40002，不计数
- 无活跃会话上下文 → 40001

---

## 4. 查询会话状态（断点恢复）

`GET /api/tutoring/sessions/{sessionId}`

返回会话状态、当前题目、计数、最近消息，供中断后恢复续聊。

响应：
```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "session_id": 1001,
    "status": "ACTIVE",
    "subject": "math",
    "question_content": "鸡兔同笼，共35头94脚，各几只？",
    "question_type": "APPLICATION",
    "round_count": 2,
    "answer_request_count": 0,
    "recent_messages": [
      {"role": "user", "content": "设鸡有x只...", "created_at": "2026-08-03T10:00:00"},
      {"role": "ai", "content": "方程列出来了。...", "created_at": "2026-08-03T10:00:05"}
    ]
  }
}
```

---

## 5. 结束并归档会话

`POST /api/tutoring/sessions/{sessionId}/archive`

学生主动结束会话：按 end_reason（ABANDONED）校正掌握度（不提升）、全文归档 COS、置 ARCHIVED。

请求体：`{}`

响应：
```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "session_id": 1001,
    "status": "ARCHIVED",
    "transcript_url": "https://cos-xxx.cos.ap-guangzhou.myqcloud.com/tutoring/transcripts/1001.json",
    "summary": {"knowledge_points": ["二元一次方程组"], "weak_points": ["代入消元法"]}
  }
}
```
> `transcript_url` 为 COS 签名 URL（短时有效）。**存储约定**：`t_tutoring_session.transcript_url` 存 COS **objectKey**（如 `tutoring/transcripts/1001.json`），读时 `FileStorageService.getUrl(objectKey)` 现生成签名 URL，避免死链接。

---

## 6. 拍照识别题目（OCR，前置步骤）

`POST /api/tutoring/ocr`

学生上传题目照片，识别为题目文本供确认/修改，再发起答疑。**OCR 是发起会话的前置步骤**（用户题目输入的唯一入口）。

请求体：`multipart/form-data`，字段 `file`（jpg/png）

响应：
```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "text": "鸡兔同笼，共35头94脚，各几只？",
    "confidence": 0.92
  }
}
```

前端流程：上传照片 → 拿到 `text` 展示给学生 → 学生确认/修改 → 将确认后的文本作为接口 1 的 `message` 发起会话。

**错误码**：`40005`（无效图片）；Python 识别失败且重试后仍失败 → `40004`。

---

## 7. 查询学生知识点掌握度（图谱叠加用）

`GET /api/students/{studentId}/mastery`

返回该学生全部已记录的知识点掌握度，前端按 `kp_key`（URI）叠加到知识图谱节点。

响应：
```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "student_id": 501,
    "items": [
      {"kp_key": "http://edukg.org/knowledge/3.1/textbook/xxx", "kp_label": "二元一次方程组", "mastery_level": 75, "updated_at": "2026-08-03T10:05:00"}
    ]
  }
}
```

---

## Java 内部接口（Java → Python，internal token）

> 这些是 Java 网关调 Python 答疑 agent 的接口，**不对前端开放**。认证用内部 token（复用 llm-gateway `internalToken` 模式），不在 api.md 对外契约中。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/tutoring/decide` | 非流式，输出 action 元数据（type/reason/eval/mastery_signals/new_question/end_reason/summary/safety_flag） |
| POST | `/api/tutoring/generate` | 流式 SSE，按已放行 action_type 输出正文 |
| POST | `/api/ocr/recognize` | 非流式，题目照片识别为 `{text, confidence}`（Java `POST /api/tutoring/ocr` 代理） |

契约细节见 `specs/ai-tutoring.md` 的 Python 端点契约章节。

---

## 错误码说明

| code | 含义 | 处理 |
|------|------|------|
| 00000 | 成功 | — |
| 40001 | 会话不存在 | 前端提示会话已失效 |
| 40002 | 会话已结束/已归档 | 引导发起新会话 |
| 40003 | 会话创建过于频繁（5 分钟 > 3 个） | 提示先完成当前答疑 |
| 40004 | Python decide/generate/OCR 调用失败且重试后仍失败 | 提示"网络波动，请重试"，会话状态不丢失 |
| 40005 | OCR 无效图片 / 识别失败 | 提示重新上传清晰照片 |
| 401 | 未登录 | 走统一认证 |

## 前端调用注意事项

1. **SSE 消费顺序**：先收 `meta`（类型先行，可据此渲染"给思路/给答案/引导"的 UI 骨架），再收 `token` 流，最后 `done`（状态 + eval + summary）。
2. **护栏拒绝时无 token**：`meta` 带 `denied` 字段，前端按 meta 的 type 渲染即可，无需处理 token 中断。
3. **不要自行计数答案次数**：`answer_request_count` 以服务端为准，前端仅渲染。
4. **断点恢复**：进入页面先调接口 4 查进行中会话，有则续聊，无则新建。
5. **图谱叠加**：知识图谱页调接口 6 拉取掌握度，按 `kp_key` 匹配节点 URI 渲染。
