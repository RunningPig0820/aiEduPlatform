# question-type-mastery-backend API 接口文档

> 基础路径: `/api/students`
>
> 更新日期: 2026-08-17
>
> **BREAKING**：`GET /students/{id}/mastery` 响应契约变更（`masteryLevel` 离散四档 → 连续百分比，新增 `source`/`trainCount`）。前端需联调。

---

## 目录

- [通用响应结构](#通用响应结构)
- [1. 题型掌握度 getMastery（BREAKING 契约变更）](#1-题型掌握度-getmastery)
- [2. 按题型查题目列表](#2-按题型查题目列表)
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

## 1. 题型掌握度 getMastery

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/students/{studentId}/mastery` |
| 需要登录 | 是（STUDENT） |

**用途**：查询学生题型掌握度。**掌握度主体 = 题型**，`masteryLevel` 为 **0-100 连续百分比**（累计平均正确率，替代原离散四档 0/25/50/75），新增 `source`（来源）与 `trainCount`（训练数）供掌握度页列式展示。

### 路径参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | Long | 是 | 学生 ID（必须与会话 userId 一致，否则 10005 无权访问） |

### 响应参数

成功时 `data` 返回：

```json
{
  "studentId": 1001,
  "items": [
    {
      "topicKey": "鸡兔同笼",
      "topicLabel": "鸡兔同笼",
      "masteryLevel": 64,
      "source": "ai",
      "trainCount": 10,
      "status": "RESOLVED",
      "updatedAt": "2026-08-17T21:00:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| studentId | Long | 学生 ID |
| items | Array | 题型掌握度列表（仅已练**且已归属**的题型；未开始/未归属不出现在列表中） |
| items[].topicKey | String | 归一化题型 key（canonical，掌握表主键） |
| items[].topicLabel | String | 题型展示名（canonical） |
| items[].masteryLevel | Integer | **0-100 连续百分比（累计平均正确率）** |
| items[].source | String | 来源：`ai`（AI 答疑）/ `bank`（题库，预留） |
| items[].trainCount | Integer | 训练数（该题型累计作答次数） |
| items[].status | String | `RESOLVED`（已锚定，掌握表有行） |
| items[].updatedAt | DateTime | 最近更新时间 |

### 请求示例

**cURL:**
```bash
curl -X GET http://localhost:8080/api/students/1001/mastery \
  -H "Cookie: SESSION=<your-session>"
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/students/1001/mastery', { credentials: 'include' });
const result = await response.json();
if (result.code === '00000') {
  const items = result.data.items; // [{ topicLabel, masteryLevel(0-100), source, trainCount, status }]
  // 前端分桶：<25 待巩固 / 25-50 练习中 / 50-75 偏稳 / ≥75 已掌握
}
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未登录或 Session 过期 |
| 10005 | 无权访问 | 路径 studentId 与会话 userId 不一致 |

---

## 2. 按题型查题目列表

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/students/{studentId}/topics/{topicLabel}/questions` |
| 需要登录 | 是（STUDENT） |

**用途**：掌握度页「查看题目」跳转——返回该生该题型下全部题目记录（内容、对错信号、作答时间），可追溯「为什么是 64%」。

### 路径参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | Long | 是 | 学生 ID（须与会话一致） |
| topicLabel | String | 是 | 题型展示名（canonical），需 URL encode |

### 响应参数

成功时 `data` 返回：

```json
{
  "studentId": 1001,
  "topicLabel": "鸡兔同笼",
  "questions": [
    {
      "id": 5001,
      "content": "笼子里有鸡和兔共 35 个头，94 只脚…",
      "score": 0.5,
      "hintCount": 1,
      "answerRequestCount": 0,
      "sessionId": 888,
      "createdAt": "2026-08-17T20:30:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| studentId | Long | 学生 ID |
| topicLabel | String | 题型名（canonical） |
| questions | Array | 该题型下题目记录；**无记录返回空数组，不报错** |
| questions[].id | Long | 题目记录 ID |
| questions[].content | String | 题目文本 |
| questions[].score | Number | 生效分值 0.0 / 0.5 / 1.0（含打折后） |
| questions[].hintCount | Integer | 引导轮数 |
| questions[].answerRequestCount | Integer | 要答案次数 |
| questions[].sessionId | Long | **原题链接**——AI 答疑会话 ID，可跳回原会话看原题；无会话链接为 null（显示题目原文） |
| questions[].createdAt | DateTime | 作答时间 |

### 请求示例

**cURL:**
```bash
curl -X GET "http://localhost:8080/api/students/1001/topics/%E9%B8%A1%E5%85%94%E5%90%8C%E7%AC%BC/questions" \
  -H "Cookie: SESSION=<your-session>"
```

**JavaScript (fetch):**
```javascript
const response = await fetch(`/api/students/1001/topics/${encodeURIComponent(topicLabel)}/questions`, {
  credentials: 'include'
});
const result = await response.json();
if (result.code === '00000') {
  const questions = result.data.questions; // [{ content, score, hintCount, sessionId, createdAt }]
  // 空数组 → 显示空态，不报错
}
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未登录或 Session 过期 |
| 10005 | 无权访问 | 路径 studentId 与会话 userId 不一致 |

---

## 3. 批量聚集手动触发（ADMIN，非定时）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/kp/aggregation/topic-cluster` |
| 需要登录 | 是（ADMIN，`@PreAuthorize("hasRole('ADMIN')")`） |

**用途**：手动触发批量聚集——扫描题目表未归并/低置信题型名 → 全量向量聚类补归并 → 写别名表 → 重算掌握表聚合。**不做定时任务**（面试项目，聚合/维护全部按钮手动触发）。已归并的题型名幂等重算，不重复计掌握度。

### 请求参数

无（POST 空请求体）。

### 响应参数

成功时 `data` 返回：

```json
{
  "mergedCount": 3,
  "canonicalCount": 12,
  "message": "批量聚集完成"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| mergedCount | Integer | 本次归并的散名数 |
| canonicalCount | Integer | 聚集后 canonical 题型总数 |
| message | String | 结果提示 |

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/kp/aggregation/topic-cluster \
  -H "Cookie: SESSION=<admin-session>"
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未登录或 Session 过期 |
| 20004 | 无权限 | 非 ADMIN 角色 |

---

## 错误码说明

### 通用错误码 (1xxxx)

| code | message | 说明 |
|------|---------|------|
| 00000 | success | 成功 |
| 10001 | 参数错误 | 请求参数格式不正确 |
| 10002 | 实体不存在 | 请求的资源不存在 |
| 10004 | 未登录 | 用户未登录或 Session 过期 |
| 10005 | 无权访问 | 越权访问他人数据 |

---

## 前端调用注意事项

### 1. Session 管理

两个接口均需登录（STUDENT），请求必须 `credentials: 'include'`；路径 `studentId` 必须与会话一致，否则 `10005`。

### 2. `getMastery` BREAKING 契约（联调重点）

- `masteryLevel` **语义从离散四档（0/25/50/75）改为连续百分比 0-100**——前端分桶展示保留四档视觉：`<25 待巩固 / 25-50 练习中 / 50-75 偏稳 / ≥75 已掌握`。
- 新增 `source`（ai/bank）与 `trainCount`（训练数）——掌握度页加「来源」「训练数」列。
- 未开始题型不出现在 `items[]`（引导去 AI 答疑做题）。`items[]` 只含**已归属**（掌握表有行）题型——题目记录 canonical 未归属的不进掌握表，也不在 items 展示。
- **知识点总览页不再消费本接口做覆盖度**（本期题型↔知识点断联）。

### 3. 掌握度 = 累计平均正确率（可追溯）

每一行的 `masteryLevel` 背后是该题型 `trainCount` 道题的题目记录——「查看题目」命中接口 2，空态不报错。题目的 `score` 语义：直接答对 1.0 / **求助后答对 0.5（学生主动要过思路/答案，answerRequestCount≥1）** / 答错 0.0（含 per-题型前几题打折）。AI 主动给 hint 不降级（学生未求助直接答对 → 1.0）。

### 4. 空态

- 掌握度页无记录 → 引导去 AI 答疑做题。
- 题型有掌握记录但无题目证据 → 接口 2 返回空数组，前端显示空态不报错。

### 5. 原题链接

题目列表每项带 `sessionId`——前端「查看题目」可跳回 AI 答疑会话查看原题；`sessionId` 为 null 时展示 `content` 原文即可。

### 6. 题型动态聚集（canonical）

`topicKey`/`topicLabel` 均为**动态涌现**的 canonical——无题库预置分类，canonical 由第一条相似题创建，后续题目按「题型名向量」最近邻归并（题目向量本期不落库）。前端无需处理变体（「一元二次方程」与「解一元二次方程」共享同一行掌握度）。

### 7. 题型名归一（canonical）

`topicKey`/`topicLabel` 均为归一后 canonical——「一元二次方程」与「解一元二次方程」共享同一行掌握度，前端无需处理变体。

### 8. 前端联调契约（题型分析页 ↔ 掌握度）

- **analyze 返回 canonical**：`analyze-question` 返回的 `topicLabel` 是**动态聚集后的 canonical**（「解一元二次方程」→「一元二次方程」）——前端用它直接查 `getMastery`，能对上，不会误判「未开始」。
- **域 B 独立化（analyze 只到题型）**：`analyze-question` 识别题型后**不再自动关联知识点**——`knowledgePoints` 来自题型库权威分布（有则返回 / 无则空），不会因「无知识点」挂起 PENDING。题型↔知识点关联由 ADMIN 维护接口手动配（见 9）。
- **两态语义**（不能混）：
  - `items[]` 只有 `status=RESOLVED`（已归属，掌握表有行）→ `masteryLevel` = 累计平均正确率
  - 不在 `items[]` = **未开始或未归属** → 引导去 AI 答疑做题。题型识别失败（canonical 未归属）的题目记录**仍在题目表**（事实源完整、可追溯），只是不进掌握表、不在掌握度列表展示；后续批量聚集（`POST /api/kp/aggregation/topic-cluster`）扫描 `canonical_label` 为空 → 重新聚集回填 → 重算掌握表补上
- **masteryLevel 连续% 分桶** = 已掌握/练习中/待巩固（掌握表累计平均）
- **score 同源可追溯**：「查看题目」列表返回的 `score`（0.0/0.5/1.0，含打折）= 掌握表聚合用的同一个信号——「为什么 64%」点开题目列表能对上。

### 9. 题型↔知识点维护接口（ADMIN，域 B 独立逻辑）

> 域 B 独立化：入口只读查表，关联由维护接口写入（**替代「obs 共现自动聚合」**——`POST /api/kp/aggregation/run` 逻辑停用，不再用于入口关联）。

| 接口 | 说明 |
|------|------|
| `POST /api/kp/type/upsert` | 建/更新题型（topicLabel → CANDIDATE/STABLE） |
| `POST /api/kp/type/{id}/kp` | 绑定知识点分布（kpUri + ratio + gradeRange）→ `t_kp_question_type_kp` |
| 别名维护 | 变体题型名 → canonical（复用 `t_kp_question_type_alias`） |

> 演示用法：管理后台手动配几条「鸡兔同笼 → 鸡兔同笼问题(ratio 0.6) / 假设法(ratio 0.4)」→ 入口 `analyze-question` 命中题型库即返回权威分布；未命中返回「仅题型+canonical」，前端展示无关联态（不报错）。
