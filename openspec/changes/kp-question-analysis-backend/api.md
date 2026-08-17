# 题型分析（kp-question-analysis）API 接口文档

> 基础路径: `/api/kp`
>
> 更新日期: 2026-08-17

---

## 目录

- [通用响应结构](#通用响应结构)
- [1. 单题分析 analyze-question](#1-单题分析-analyze-question)
- [2. 复用接口](#2-复用接口)
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

## 1. 单题分析 analyze-question

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/kp/analyze-question` |
| Content-Type | `application/json` |
| 需要登录 | 是（STUDENT） |

**用途**：学生贴题/拍题（OCR 后）→ 识别题型 → 返回该题型关联的知识点清单（「这道题考了哪些知识点」）。权威命中不产生学习观测；**存疑 PENDING 自动挂起一条 PENDING obs（进待确认清单，不丢）**；PENDING 不报错。**需登录（STUDENT）**。

### 请求参数

**RequestBody**

```json
{
  "text": "笼子里有鸡和兔共 35 个头，94 只脚，鸡和兔各有多少只？"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| text | String | 是 | 非空 | 题目文本（OCR 识别结果或手打） |

### 响应参数

成功时 `data` 返回：

```json
{
  "topicLabel": "鸡兔同笼",
  "status": "RESOLVED",
  "confidence": 85,
  "knowledgePoints": [
    {
      "kpUri": "math#textbook-middle-00085",
      "kpLabel": "鸡兔同笼",
      "gradeRange": "4-6",
      "ratio": 0.8
    }
  ],
  "candidates": []
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| topicLabel | String | 识别出的题型名；PENDING 时为首个候选题型名（可为 null） |
| status | String | `RESOLVED` / `PENDING`。RESOLVED=数据驱动权威命中（题型库/镜像）；PENDING=存疑/冷启动（含 **WEAK 降级**：LLM 冷启动猜测不再冒充权威 RESOLVED，只作候选待确认） |
| confidence | Integer | 置信度 0-100；PENDING 为 0 |
| knowledgePoints | Array | 关联知识点清单；题型库命中返回全分布，未命中走解析单点（单条 ratio=1），PENDING 为空数组 |
| knowledgePoints[].kpUri | String | 教材知识点 TextbookKP URI |
| knowledgePoints[].kpLabel | String | 知识点名（从 kg 镜像按 kpUri 反查） |
| knowledgePoints[].gradeRange | String | 年级分布段（如 `4-6` / `7`），可为 null |
| knowledgePoints[].ratio | Number | 该知识点在题型中的占比（分布桶归一化和=1；单点解析为 1.0） |
| candidates | Array[String] | PENDING 时的澄清候选（学科概念名，不暴露 kp_uri）；**已镜像校验——每个候选保证可 vote（不会 10003）**；RESOLVED 为空数组 |

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/kp/analyze-question \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSION=<your-session>" \
  -d '{"text": "笼子里有鸡和兔共 35 个头，94 只脚，鸡和兔各有多少只？"}'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/kp/analyze-question', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include', // 携带 Cookie
  body: JSON.stringify({ text: questionText })
});
const result = await response.json();
if (result.code === '00000') {
  const { topicLabel, status, knowledgePoints, candidates } = result.data;
  if (status === 'RESOLVED') {
    // 展示题型 + 关联知识点清单（kpLabel + gradeRange + ratio）
  } else {
    // PENDING：展示候选列表（candidates）供学生选择，或空态提示
  }
}
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10001 | text 不能为空 | 请求体缺失或 text 为空 |
| 10004 | 未登录 | 未登录或 Session 过期 |

> 注：题目理解/解析失败不会返回错误，统一降级 `status=PENDING`（前端渲染待确认态，不阻塞）。

---

## 2. 复用接口

以下接口已就绪（`kp-matching-lightup`），题型分析页直接复用：

| 接口 | 方法/路径 | 用途 |
|------|-----------|------|
| 题型库分页 | `GET /api/kp/question-types?page=&size=` | 题型库浏览（返回 id/topicLabel/status/hitCount + total） |
| 题型关联知识点 | `GET /api/kp/question-types/{id}/knowledge-points` | 展开某题型的关联知识点（kpUri/kpLabel/gradeRange/ratio/hitCount） |
| 学生确认关联 | `POST /api/kp/vote { topicLabel, selectedLabel }` | 学生点「确认关联」落 STUDENT_VOTE 观测（成功后提示「已记录，将参与题型整理」） |
| 拍题 OCR | `POST /api/tutoring/ocr`（multipart `file`） | 题目照片 → 文本，再触发 analyze-question |
| 知识点搜索兜底 | `POST /api/kg/knowledge-points { stage, keyword, page, size }` | 空候选手动搜教材知识点确认（**本轮新增 keyword**：`WHERE label LIKE '%kw%'`，stage 过滤内） |

### 2.1 知识点搜索兜底（keyword）

**用途**：analyze 返回 PENDING 且 candidates 为空（极端冷启动）时，学生用搜索选择器手动搜教材知识点确认。前端 `KpSearchSelector` 已就绪，发 `{ keyword }`。

**请求**：
```json
{ "stage": "primary", "keyword": "二元一次", "page": 1, "size": 20 }
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| stage | String | 是 | primary / middle / high |
| keyword | String | 否 | label LIKE `%keyword%`；缺省返回该 stage 全量 |
| page / size | Number | 否 | 分页（默认 1 / 20） |

**响应**：`{ items: [{kpUri, kpLabel, stage, chapterLabel, sectionLabel}], total, page, size }`（同原接口，仅新增 keyword 过滤）。

---

## 错误码说明

### 通用错误码 (1xxxx)

| code | message | 说明 |
|------|---------|------|
| 00000 | success | 成功 |
| 10001 | 参数错误 | 请求参数格式不正确 |
| 10002 | 实体不存在 | 请求的资源不存在 |
| 10003 | 参数无效 | 参数校验失败 |
| 10004 | 未登录 | 用户未登录或 Session 过期 |

### vote 相关错误码（复用，题型分析确认关联会用到）

| code | message | 说明 |
|------|---------|------|
| 10003 | 候选知识点不存在，无法投票 | vote 的 selectedLabel 精确/LIKE 均未命中镜像；前端需 toast 提示并复位可重试 |

---

## 前端调用注意事项

### 1. Session 管理

本系统使用 Spring Session + Redis 管理 Session，前端需要：

- **携带 Cookie**: `analyze-question`/`vote`/题型库接口均需登录，请求时必须 `credentials: 'include'`
- **跨域配置**: 开发环境需配置 CORS 允许携带凭证

```javascript
fetch('/api/kp/analyze-question', { credentials: 'include' });
```

### 2. PENDING 语义（含 WEAK 降级，常态路径）

`analyze-question` 返回 `status=PENDING` **不是错误**（code 仍为 `00000`）：
- **冷启动 LLM 猜测（WEAK）现在也返回 PENDING**——不再冒充权威 RESOLVED。PENDING 分支是**常态**。
- **恒非空（D8 约束选择后）**：candidates 从「学段知识点池」里选，**常态下永不空**（置信低也返回池内最相近）。仅极端冷启动（学段池空/粗筛全 miss）才可能空——此时用 keyword 搜索兜底（§2.1）。
- 不要因为 status 非 RESOLVED 就报错/阻塞。

### 3. 确认关联流程（vote 转正）

- 学生对某知识点/候选点「确认」→ `POST /api/kp/vote { topicLabel, selectedLabel }`（topicLabel=analyze 返回的 topicLabel，selectedLabel=知识点/候选 label）。
- vote 成功（code=00000）→ **该生该题型的 PENDING obs 转正为 RESOLVED**（待确认清单即时消失）+ 提示「已记录，将参与题型整理」。
- vote 失败（如 10003）→ toast 展示 message，状态复位可重试，不静默。**candidates 已镜像校验，正常不触发**。

### 4. 存疑挂起（待确认清单）

- analyze 存疑会自动落一条 PENDING obs（去重）→ 出现在 `GET /api/students/{id}/pending-kps`。
- 学生可**事后在待确认清单里补确认**（同一 vote 接口），或后端维护任务 LLM 重判补充。

### 5. 数据一致性

- 相似题型名变体（如「鸡兔同笼」/「鸡兔同笼问题」）由后端别名合并收敛到同一 canonical 题型，前端无需处理，题型库浏览只会看到 canonical 条目。
- 权威命中不产生学习观测；**存疑挂起 PENDING obs**；学生「确认」/维护任务重判后才进入聚合沉淀（跨学生达阈值生效，非即时）。
- **题型库空是冷启动预期**（聚合吃答疑/投票/确认产生的 RESOLVED obs），前端空态提示「随做题与确认逐步积累」即可。

### 6. 超时

- analyze-question 走 LLM 冷调用，慢则秒级——**前端 axios 超时建议 ≥30s**（同 OCR 先例）。
