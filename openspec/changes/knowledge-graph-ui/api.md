# 知识图谱 API 接口文档

> 基础路径: `/api/kg`
>
> 更新日期: 2026-04-15

---

## 目录

- [通用响应结构](#通用响应结构)
- [1. 全量同步](#1-全量同步)
- [2. 查询同步状态](#2-查询同步状态)
- [3. 同步历史记录](#3-同步历史记录)
- [4. 获取教材列表](#4-获取教材列表)
- [5. 获取教材章节树](#5-获取教材章节树)
- [6. 获取小节知识点](#6-获取小节知识点)
- [7. 获取知识点详情](#7-获取知识点详情)
- [8. 获取年级知识体系](#8-获取年级知识体系)
- [9. 获取年级统计](#9-获取年级统计)
- [错误码说明](#错误码说明)
- [前端调用注意事项](#前端调用注意事项)

---

## 通用响应结构

```json
{
  "code": "00000",
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | String | 状态码，`00000` 表示成功 |
| message | String | 提示信息 |
| data | Object | 业务数据，可能为 null |

> **注**: 本系统中所有节点标识符均使用 `uri`（Neo4j 中的 URI），而非自增 ID。
> 例如：`uri: "http://edukg.org/knowledge/3.1/textbook/一年级上册"`

---

## 1. 全量同步

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/kg/sync/full` |
| 需要登录 | 当前阶段不限制，后续补充权限 |

### 请求参数

**Request Body（可选，不传则全量同步）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| subject | String | 否 | 学科过滤，如 `math` |
| phase | String | 否 | 学段过滤，如 `primary` |
| grade | String | 否 | 年级过滤，如 `一年级` |
| textbookUri | String | 否 | 指定教材 URI，精确同步某一本教材 |

### 响应参数

```json
{
  "syncId": 1,
  "status": "running",
  "message": "同步已开始"
}
```

### 请求示例

**全量同步:**
```bash
curl -X POST http://localhost:8080/api/kg/sync/full
```

**定向同步（仅同步一年级数学）:**
```bash
curl -X POST http://localhost:8080/api/kg/sync/full \
  -H "Content-Type: application/json" \
  -d '{"subject":"math","grade":"一年级"}'
```

---

## 2. 查询同步状态

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kg/sync/status` |
| 需要登录 | 否 |

### 响应参数

同步中：
```json
{
  "status": "running",
  "processedNodes": 3500,
  "startedAt": "2026-04-15T10:00:00"
}
```

空闲：
```json
{
  "status": "idle",
  "lastSync": {
    "status": "success",
    "insertedCount": 10,
    "updatedCount": 50,
    "statusChangedCount": 2,
    "reconciliationStatus": "matched",
    "scope": {"subject": "math"},
    "startedAt": "2026-04-15T10:00:00",
    "finishedAt": "2026-04-15T10:05:00"
  }
}
```

---

## 3. 同步历史记录

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kg/sync/records` |
| 需要登录 | 否 |

### 响应参数

```json
[
  {
    "id": 1,
    "syncType": "full",
    "scope": {"subject": "math", "grade": "一年级"},
    "status": "success",
    "insertedCount": 10,
    "updatedCount": 50,
    "statusChangedCount": 2,
    "reconciliationStatus": "matched",
    "startedAt": "2026-04-15T10:00:00",
    "finishedAt": "2026-04-15T10:05:00"
  }
]
```

---

## 4. 获取教材列表

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kg/textbooks` |
| 需要登录 | 否 |

### 请求参数

**Query 参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| subject | String | 否 | 学科过滤，默认 `math` |
| phase | String | 否 | 学段过滤：`primary`/`middle`/`high` |

### 响应参数

```json
[
  {
    "uri": "http://edukg.org/knowledge/3.1/textbook/一年级上册",
    "label": "一年级上册",
    "grade": "一年级",
    "phase": "primary",
    "subject": "math"
  }
]
```

---

## 5. 获取教材章节树

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kg/textbooks/{uri}/chapters` |
| 需要登录 | 否 |

> 注：`{uri}` 需要 URL 编码。

### 响应参数

```json
{
  "textbookUri": "http://edukg.org/knowledge/3.1/textbook/一年级上册",
  "textbookLabel": "一年级上册",
  "chapters": [
    {
      "uri": "http://edukg.org/knowledge/3.1/chapter/准备课",
      "label": "准备课",
      "topic": "数与代数",
      "orderIndex": 1,
      "sections": [
        {
          "uri": "http://edukg.org/knowledge/3.1/section/10以内数的认识",
          "label": "10以内数的认识",
          "orderIndex": 1,
          "knowledgePointCount": 5
        }
      ]
    }
  ]
}
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 70001 | 教材不存在 | 请求的教材不存在 |

---

## 6. 获取小节知识点

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kg/sections/{uri}/points` |
| 需要登录 | 否 |

> 注：`{uri}` 需要 URL 编码。

### 响应参数

```json
{
  "sectionUri": "http://edukg.org/knowledge/3.1/section/10以内数的认识",
  "sectionLabel": "10以内数的认识",
  "knowledgePoints": [
    {
      "uri": "http://edukg.org/knowledge/3.1/kp/10以内数的认识",
      "label": "10以内数的认识",
      "difficulty": "easy",
      "importance": "high",
      "cognitiveLevel": "记忆"
    }
  ]
}
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 70004 | 小节不存在 | 请求的小节不存在 |

---

## 7. 获取知识点详情

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kg/knowledge-points/{uri}` |
| 需要登录 | 否 |

> 注：`{uri}` 需要 URL 编码。

### 响应参数

```json
{
  "uri": "http://edukg.org/knowledge/3.1/kp/加法",
  "label": "加法",
  "difficulty": "easy",
  "importance": "high",
  "cognitiveLevel": "理解",
  "sectionUri": "http://edukg.org/knowledge/3.1/section/10以内数的认识",
  "sectionLabel": "10以内数的认识",
  "chapterUri": "http://edukg.org/knowledge/3.1/chapter/准备课",
  "chapterLabel": "准备课"
}
```

> 注：知识点详情仅返回 2 层父级（sectionLabel + chapterLabel），不返回教材/年级等更高层级。

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 70003 | 知识点不存在 | 请求的知识点不存在（含已软删除） |

---

## 8. 获取年级知识体系

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kg/system/grade/{grade}` |
| 需要登录 | 否 |

### 请求参数

**Path 参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| grade | String | 是 | 年级：`一年级` ~ `高三` |

**Query 参数**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| groupBy | String | 否 | `textbook` | 分组方式：`textbook` 或 `topic` |

### 响应参数

```json
{
  "grade": "一年级",
  "groupBy": "textbook",
  "groups": [
    {
      "groupName": "一年级上册",
      "groupType": "textbook",
      "textbookUri": "http://edukg.org/knowledge/3.1/textbook/一年级上册",
      "chapters": [
        {
          "uri": "http://edukg.org/knowledge/3.1/chapter/准备课",
          "label": "准备课",
          "topic": "数与代数",
          "sections": [
            {
              "uri": "http://edukg.org/knowledge/3.1/section/10以内数的认识",
              "label": "10以内数的认识",
              "knowledgePoints": [
                {
                  "uri": "http://edukg.org/knowledge/3.1/kp/10以内数的认识",
                  "label": "10以内数的认识",
                  "difficulty": "easy",
                  "importance": "high",
                  "cognitiveLevel": "记忆"
                }
              ]
            }
          ]
        }
      ]
    }
  ],
  "totalKnowledgePoints": 45
}
```

---

## 9. 获取年级统计

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kg/system/stats/{grade}` |
| 需要登录 | 否 |

### 响应参数

```json
{
  "grade": "一年级",
  "totalKnowledgePoints": 150,
  "difficultyDistribution": {
    "easy": 60,
    "medium": 70,
    "hard": 20
  },
  "importanceDistribution": {
    "high": 40,
    "medium": 80,
    "low": 30
  },
  "cognitiveLevelDistribution": {
    "记忆": 50,
    "理解": 40,
    "应用": 45,
    "分析": 15
  }
}
```

---

## 10. Neo4j 健康检查

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/kg/neo4j/health` |
| 需要登录 | 否 |

### 响应参数

Neo4j 可用：
```json
{
  "available": true,
  "responseTimeMs": 50
}
```

Neo4j 不可用：
```json
{
  "available": false,
  "error": "connection timeout"
}
```

---

## 11. 批量获取概念关联

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/kg/concepts/batch-relations` |
| 需要登录 | 否 |

### 请求参数

```json
{
  "uris": [
    "http://edukg.org/knowledge/0.1/instance/math#加法",
    "http://edukg.org/knowledge/0.1/instance/math#减法"
  ]
}
```

### 响应参数

```json
{
  "neo4jAvailable": true,
  "relations": {
    "http://edukg.org/knowledge/0.1/instance/math#加法": [
      {
        "type": "RELATED_TO",
        "targetUri": "http://edukg.org/knowledge/0.1/instance/math#加法定义",
        "targetLabel": "加法定义"
      }
    ]
  }
}
```

> **缓存策略**: 查询结果存入 Redis，TTL = 300s。Neo4j 不可用时返回 `neo4jAvailable: false`。

---

## 错误码说明

### 通用错误码

| code | message | 说明 |
|------|---------|------|
| 00000 | success | 成功 |
| 10000 | 系统错误 | 服务器内部错误 |
| 10001 | 参数错误 | 请求参数格式不正确 |

### 知识图谱错误码 (7xxxx)

| code | message | 说明 |
|------|---------|------|
| 70001 | 教材不存在 | 请求的教材不存在 |
| 70002 | 章节不存在 | 请求的章节不存在 |
| 70003 | 知识点不存在 | 请求的知识点不存在（含已软删除） |
| 70004 | 小节不存在 | 请求的小节不存在 |
| 70005 | Neo4j 查询失败 | 图数据库查询异常 |
| 70006 | 同步正在进行 | 已有同步任务在执行 |
| 70007 | 同步参数错误 | 同步请求参数格式不正确 |

---

## 前端调用注意事项

### 1. CORS 配置

前端独立部署，需在后端配置 CORS 允许跨域。

### 2. URI 编码

所有路径参数中的 `uri` 需要 URL 编码。例如：
```javascript
const uri = encodeURIComponent('http://edukg.org/knowledge/3.1/textbook/一年级上册');
fetch(`/api/kg/textbooks/${uri}/chapters`);
```

### 3. 权限说明

当前阶段同步接口不实现权限控制，后续组织结构/权限模块补充后限制为管理员角色。

---

*文档生成时间: 2026-04-15*
