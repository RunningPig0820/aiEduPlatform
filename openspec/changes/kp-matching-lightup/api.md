# 知识点匹配与点亮 API 接口文档

> 更新日期: 2026-08-13
>
> 涉及模块：`kp-topic-resolution`（解析）、`kp-mastery-lightup`（点亮/审核）、`kp-question-type-catalog`（派生层）。

---

## 目录

- [通用响应结构](#通用响应结构)
- [1. 题型解析 resolve](#1-题型解析-resolve)
- [2. 学生掌握度 mastery（增强）](#2-学生掌握度-mastery增强)
- [3. 挂起清单 pending](#3-挂起清单-pending)
- [4. 挂起确认 confirm](#4-挂起确认-confirm)
- [5. 全量知识点分页 knowledge-points](#5-全量知识点分页-knowledge-points)
- [6. 题型库分页 question-types](#6-题型库分页-question-types)
- [7. 题型关联知识点 question-types-kp](#7-题型关联知识点-question-types-kp)
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

## 1. 题型解析 resolve

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/kp/resolve` |
| Content-Type | `application/json` |
| 需要登录 | 是（Session） |

将 AI 识别的题型/知识点 label 解析到教材知识点 URI，复用答疑内嵌的同一解析管线（镜像 → 题型库年级匹配 → LLM 消歧 → PENDING）。低置信返回 `status=PENDING`，**不报错**。

### 请求参数

**RequestBody**

```json
{
  "label": "鸡兔同笼",
  "student_grade": 7
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| label | String | 是 | 非空 | AI 识别的题型/知识点原文 |
| student_grade | Integer | 否 | 1-12 | 学生年级，用于年级锚（缺省走纯 LLM 消歧） |

### 响应参数

成功时 `data` 返回：

```json
{
  "label": "鸡兔同笼",
  "uri": "http://edukg.org/knowledge/3.1/kp/math#renjiao-g2s-...",
  "kpLabel": "假设法",
  "confidence": 88,
  "status": "RESOLVED"
}
```

`status=PENDING` 时：

```json
{
  "label": "牛吃草",
  "uri": null,
  "kpLabel": null,
  "confidence": 40,
  "status": "PENDING"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| label | String | 原 label |
| uri | String/null | 解析出的 TextbookKP URI；PENDING 为 null |
| kpLabel | String/null | 命中知识点名（冗余展示） |
| confidence | Integer | 置信度 0-100 |
| status | String | `RESOLVED` / `PENDING` |

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/kp/resolve \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSION=<session>" \
  -d '{"label":"鸡兔同笼","student_grade":7}'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/kp/resolve', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({ label: '鸡兔同笼', student_grade: 7 })
});
const result = await response.json(); // { code, message, data: { uri, status, ... } }
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未携带 Session |
| 10001 | 参数错误 | label 为空 |

---

## 2. 学生掌握度 mastery（增强）

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/students/{studentId}/mastery` |
| Content-Type | — |
| 需要登录 | 是（路径 studentId 必须等于 Session userId） |

图谱点亮数据源：返回该学生全部已记录知识点掌握度，图谱按 `kpKey`(URI) 匹配节点渲染。

### 请求参数

**Path**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | Long | 是 | 学生 ID，必须与会话 userId 一致 |

### 响应参数

成功时 `data` 返回：

```json
{
  "studentId": 101,
  "items": [
    {
      "kpKey": "http://edukg.org/knowledge/3.1/kp/math#renjiao-g2s-...",
      "kpLabel": "二元一次方程组",
      "masteryLevel": 75,
      "status": "RESOLVED",
      "confidence": 92,
      "stage": "middle",
      "chapterLabel": "二元一次方程组",
      "sectionLabel": "8.1 二元一次方程组",
      "updatedAt": "2026-08-12T10:30:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| studentId | Long | 学生 ID |
| items | Array | 掌握度列表 |
| items[].kpKey | String | 知识点 URI（图谱节点匹配键） |
| items[].kpLabel | String | 知识点名 |
| items[].masteryLevel | Integer | 掌握度 0-100 |
| items[].status | String | `RESOLVED`（确定）/ `PENDING`（疑似待确认） |
| items[].confidence | Integer | 解析置信度 0-100 |
| items[].stage | String/null | 学段 primary/middle/high（从 kpKey 反查归属教材；无归属为 null） |
| items[].chapterLabel | String/null | 归属章节名（无归属为 null） |
| items[].sectionLabel | String/null | 归属小节名（无归属为 null） |
| items[].updatedAt | String | 更新时间 |

### 请求示例

**cURL:**
```bash
curl http://localhost:8080/api/students/101/mastery \
  -H "Cookie: SESSION=<session>"
```

**JavaScript (fetch):**
```javascript
const response = await fetch(`/api/students/${studentId}/mastery`, { credentials: 'include' });
const result = await response.json();
const uriToItem = Object.fromEntries(result.data.items.map(i => [i.kpKey, i]));
// 渲染：node.id 命中 kpKey 即按档位着色
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未携带 Session |
| 20004 | 权限不足 | 非 STUDENT 角色，或路径 studentId ≠ 会话 userId |

---

## 3. 挂起清单 pending

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kg/aliases/pending` |
| Content-Type | — |
| 需要登录 | 是（仅 ADMIN / TEACHER） |

列出挂起的派生观测（PENDING / HUMAN_REVIEW），供审核补别名。审核后可回流题型库与解析先验。

### 请求参数

无。

### 响应参数

成功时 `data` 返回：

```json
{
  "items": [
    {
      "id": 1001,
      "topicLabel": "鸡兔同笼",
      "studentId": 202,
      "studentGrade": 7,
      "confidence": 40,
      "status": "HUMAN_REVIEW",
      "kpUri": null,
      "firstSeenAt": "2026-08-11T09:00:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| items[].id | Long | 观测 ID（确认用） |
| items[].topicLabel | String | 题型 label |
| items[].studentId | Long | 来源学生 |
| items[].studentGrade | Integer/null | 解析时年级 |
| items[].confidence | Integer | 置信度 |
| items[].status | String | `PENDING` / `HUMAN_REVIEW` |
| items[].kpUri | String/null | 若有候选归属 |
| items[].firstSeenAt | String | 首次记录时间 |

### 请求示例

**cURL:**
```bash
curl http://localhost:8080/api/kg/aliases/pending \
  -H "Cookie: SESSION=<admin-session>"
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/kg/aliases/pending', { credentials: 'include' });
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未携带 Session |
| 20004 | 权限不足 | 非 ADMIN/TEACHER |

---

## 4. 挂起确认 confirm

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/kg/aliases/pending/{id}/confirm` |
| Content-Type | `application/json` |
| 需要登录 | 是（仅 ADMIN / TEACHER） |

确认挂起观测归属的知识点 URI。确认后观测转 `RESOLVED`，题型库对应分布桶命中数增加，后续解析优先命中该 kp。

### 请求参数

**Path**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 观测 ID（来自 pending 清单） |

**RequestBody**

```json
{
  "kp_uri": "http://edukg.org/knowledge/3.1/kp/math#renjiao-g2s-..."
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| kp_uri | String | 是 | 必须为 kg 镜像中存在的 URI | 确认归属的知识点 |

### 响应参数

成功时 `data` 返回：

```json
{
  "updated": true,
  "status": "RESOLVED"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| updated | Boolean | 是否更新 |
| status | String | 更新后观测状态 |

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/kg/aliases/pending/1001/confirm \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSION=<admin-session>" \
  -d '{"kp_uri":"http://edukg.org/knowledge/3.1/kp/math#renjiao-g2s-..."}'
```

**JavaScript (fetch):**
```javascript
const response = await fetch(`/api/kg/aliases/pending/${id}/confirm`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({ kp_uri: 'http://edukg.org/...' })
});
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未携带 Session |
| 20004 | 权限不足 | 非 ADMIN/TEACHER |
| 10001 | 参数错误 | kp_uri 为空或镜像中不存在 |
| 50007 | 观测不存在 | id 不存在或已处理 |

---

## 5. 全量知识点分页 knowledge-points

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/kg/knowledge-points` |
| Content-Type | `application/json` |
| 需要登录 | 是（Session，学生/教师/管理员均可） |

按学段分页列教材知识点，供学生端"知识点总览"知识地图底图（学段→章节→知识点分组）。数据源 kg 镜像只读。

### 请求参数

**RequestBody**

```json
{ "stage": "middle", "page": 1, "size": 20 }
```

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| stage | String | 是 | — | 学段：primary / middle / high |
| page | Integer | 否 | 1 | 页码（从 1 起） |
| size | Integer | 否 | 20 | 每页条数（上限 100） |

### 响应参数

```json
{
  "items": [
    {
      "kpUri": "http://edukg.org/knowledge/3.1/kp/math#...",
      "kpLabel": "二元一次方程组",
      "stage": "middle",
      "chapterLabel": "二元一次方程组",
      "sectionLabel": "8.1 二元一次方程组"
    }
  ],
  "total": 532,
  "page": 1,
  "size": 20
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| items[].kpUri | String | 知识点 URI |
| items[].kpLabel | String | 知识点名 |
| items[].stage | String | 学段 |
| items[].chapterLabel | String/null | 归属章节名（无归属为 null） |
| items[].sectionLabel | String/null | 归属小节名（无归属为 null） |
| total | Long | 该学段知识点总数 |
| page / size | Integer | 回显分页参数 |

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未携带 Session |
| 10001 | 参数错误 | stage 非 primary/middle/high |

---

## 6. 题型库分页 question-types

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kp/question-types` |
| 需要登录 | 是（Session） |

分页列出聚合题型库条目，供学生端"题型分析"页。

### 请求参数（Query）

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 20 | 每页条数（上限 100） |

### 响应参数

```json
{
  "items": [
    { "id": 1, "topicLabel": "鸡兔同笼", "status": "STABLE", "hitCount": 42 }
  ],
  "total": 128,
  "page": 1,
  "size": 20
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| items[].id | Long | 题型 ID |
| items[].topicLabel | String | 题型名 |
| items[].status | String | `CANDIDATE` / `STABLE` |
| items[].hitCount | Integer | 总命中次数 |
| total | Long | 题型总数 |

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未携带 Session |

---

## 7. 题型关联知识点 question-types-kp

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kp/question-types/{id}/knowledge-points` |
| 需要登录 | 是（Session） |

查某题型的关联知识点分布（题型→知识点，kpLabel 从 kg 镜像反查）。

### 请求参数（Path）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 题型 ID |

### 响应参数

```json
[
  {
    "kpUri": "http://edukg.org/knowledge/3.1/kp/math#...",
    "kpLabel": "二元一次方程组",
    "gradeRange": "7-8",
    "ratio": 0.8,
    "hitCount": 34
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| kpUri | String | 知识点 URI |
| kpLabel | String | 知识点名（kg 镜像反查） |
| gradeRange | String/null | 该 kp 覆盖年级段 |
| ratio | Double | 该 kp 占比 |
| hitCount | Integer | 该分布桶命中次数 |

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 未携带 Session |
| 10002 | 实体不存在 | 题型 ID 不存在 |

---

## 错误码说明

### 通用错误码 (1xxxx)

| code | message | 说明 |
|------|---------|------|
| 00000 | success | 成功 |
| 10000 | 系统错误 | 服务器内部错误 |
| 10001 | 参数错误 | 请求参数格式不正确 |
| 10002 | 实体不存在 | 请求的资源不存在 |
| 10004 | 未登录 | 用户未登录或 Session 过期 |
| 20004 | 权限不足 | 角色不符或越权 |

### 知识点派生错误码 (5xxxx)

| code | message | 说明 |
|------|---------|------|
| 50007 | 派生观测不存在 | 挂起确认的目标 id 不存在或已处理 |

> 解析失败不报错：`resolve` 返回 `status=PENDING`（200），避免前端把"未解析"当接口错误。

---

## 前端调用注意事项

### 1. Session 管理

- 所有接口请求必须携带 `credentials: 'include'`。
- 学生端 `mastery` 的路径 `studentId` 必须取当前登录会话的 userId，否则返回 20004。

### 2. 图谱点亮匹配规则

- 图谱节点 `node.id` == `mastery.items[].kpKey`(URI) 即匹配。
- 档位：`masteryLevel ≥ 75` 绿（掌握）/ `= 50` 黄（练习中）/ `≤ 25` 红（薄弱）。
- `status=PENDING` 或低置信 → 疑似态（虚线 + 「待确认」角标），**不按确认薄弱渲染**。
- 无掌握度数据节点保持中性灰。

### 3. resolve 的 status 语义

- `RESOLVED`：可直接用于展示/点亮。
- `PENDING`：进入挂起队列，前端可提示"待确认"，**不应**写入掌握度。
