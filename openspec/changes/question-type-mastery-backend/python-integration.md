# 给 Python 端：题目掌握度方案——Python 需要改动的部分

> 更新：2026-08-18 | 方案：`question-type-mastery-backend`
>
> ✅ **Python 侧已交付（2026-08-18）**：2 个向量端点已在 `ai-edu-ai-service` 实现并提交（`b7159c5`），单测 241 全绿。本档现为**契约对齐 + Java 桥联调依据**。
>
> **一句话**：本期业务「题目 → 题型 → 掌握度」需要 Python 新增一个**向量服务**（dashscope embedding + COS 向量桶检索），用于题型聚集。**本期只存题型名向量，`vector_type` 每次必传（恒为 `"topic"`）；decide / generate / question-understand 全部不用动。**

---

## 一、业务背景（为什么需要 Python 出力）

学生 AI 答疑做题 → 后端记录题目 → **题型聚集**（把「一元二次方程/解一元二次方程」这类 LLM 猜的散题型名归并成 canonical）→ 掌握度累计平均。

**题型聚集需要向量相似度检索**，而腾讯 COS 向量桶（Vector Bucket）**只有 Python/Go SDK，没有 Java SDK** → 向量操作归 Python 侧做，Java 通过 HTTP 桥调用（复用现有 `TutoringLlmPort` 模式）。

## 二、Python 需要改动的（唯一：新增向量服务端点）

### 2.1 新增依赖 + 配置

| 项 | 说明 |
|----|------|
| `cos-python-sdk-v5` | `CosVectorsClient`（腾讯 COS 向量桶官方 SDK） |
| dashscope embedding | `text-embedding-v3`，**768 维**，中文（已确认：复用 `DASHSCOPE_API_KEY` + dashscope OpenAI 兼容 base，独立封装在 `vector_store.py`，不动 gateway factory） |
| 配置 | `COS_VECTORS_SECRET_ID/KEY`（子账号密钥）、桶名（`xxx-125xxxx`）、region（如 `ap-guangzhou`） |
| 索引 | 建索引：768 维、**cosine** 距离度量（控制台或 SDK `create_index`） |

### 2.2 端点 1：`POST /api/tutoring/vector/put`（存向量）

Java 发「题型名 + key + metadata + vector_type」→ Python 做 embedding → 存进向量桶。

```json
// 请求
{
  "key": "q_5001",
  "text": "鸡兔同笼",               // 本期只传题型名（题目向量不落库）
  "vector_type": "topic",          // ← 必填。本期唯一合法值 "topic"
  "metadata": {
    "student_id": "1001",
    "topic_label": "鸡兔同笼",      // LLM 原始题型名
    "canonical_label": "鸡兔同笼",  // 聚集后 canonical
    "timestamp": "2026-08-18T10:00:00"
  }
}
// 响应
{ "ok": true, "key": "q_5001" }
```

**Python 逻辑**：`text → dashscope embedding(768) → client.put_vectors(Bucket, Index, [{key, data:{float32}, metadata}])`。key 相同覆盖（upsert）。

> 说明：**本期只存题型名向量**，`vector_type` 恒为 `"topic"`（路由到题型名索引）。题目向量不落库（相似题功能预留 `question` 索引，后续启用）。

### 2.3 端点 2：`POST /api/tutoring/vector/query`（查最近邻）

Java 发「题型名 + top_k + vector_type」→ Python embedding → 查最相似 Top-K 返回。

```json
// 请求
{
  "text": "鸡兔同笼问题",          // 待归并题型名
  "top_k": 3,
  "vector_type": "topic"          // ← 必填。查哪个索引由后端显式声明，无缺省
}
// 响应
{
  "vectors": [
    {
      "key": "q_5001",
      "metadata": { "topic_label": "鸡兔同笼", "canonical_label": "鸡兔同笼" },
      "distance": 0.12             // 余弦距离，越小越相似
    }
  ]
}
```

**Python 逻辑**：`text → embedding(768) → client.query_vectors(Bucket, Index, QueryVector, TopK, ReturnMetaData=True, ReturnDistance=True)`。**distance 越小越相似**（cosine），self 命中 ≈ 0。

> ⚠️ **响应字段名是 `vectors`（不是 `hits`）**——对齐 COS `query_vectors` 返回结构，Python 端点已按 `vectors` 实现。

## 三、Java↔Python 契约约定

- 路径前缀：`/api/tutoring/vector/*`（与现有 `/api/tutoring/*` 一致）
- **snake_case**（与 decide/generate/question-understand 一致）
- **`vector_type` 必填路由键**：每次 put/query 后端显式声明写/查哪个索引——**无缺省、无跨索引查询**，Python 不做任何缺省猜测。本期唯一合法值 `"topic"`（题型名向量索引）。后端不感知 COS 索引名，路由全在 Python 内部。
- **未知 `vector_type` → Python 返回 400**：后端传了映射表里不存在的类型 → 这是**正常失败路径**，Java 桥收到后降级（回退字符规则 + 原样落库）。
- Java 侧经 `TopicVectorStore` 端口调这两个端点，**Java 不碰 embedding API / COS SDK**
- **失败语义**：端点异常 Java 会降级（回退字符规则 + 原样落库），不阻塞主链路——Python 正常返回错误码即可

## 四、Python 不需要改的（明确）

- ✅ **decide / generate / question-understand 全部不动**（掌握信号仍由 Java 从会话 `roundCount`/`answerRequestCount` 推断，不新增 decide 字段）
- ✅ 现有 gateway / LLM provider 配置不动（只新增 dashscope embedding 模型类）

## 五、Python 侧现状（2026-08-18 已交付，Java 桥联调依据）

1. ✅ **embedding**：`text-embedding-v3`，768 维（显式 `dimensions=768`），**独立封装在 `core/tutoring/vector_store.py`**——未动 `core/gateway/factory.py`、未新增模型类（embedding 与对话 ChatModel 语义不同，不塞进 LLMFactory）。复用 `DASHSCOPE_API_KEY` + dashscope OpenAI 兼容 base。
2. ✅ **CosVectorsClient**：`cos-python-sdk-v5`，签名已实测确认——`query_vectors(..., ReturnMetaData=..., ReturnDistance=...)`（**注意 `ReturnMetaData` 大写 M**），返回 `(resp, data)` 元组，命中在 `data["vectors"]`。
3. ✅ **索引**：`topic-index` 已建（`get_index` 实测 `dataType=float32, dimension=768, distanceMetric=cosine`）。
4. ✅ **权限**：子账号密钥已授权（`QcloudCOSFullAccess`），get/put/query/delete 全通。

## 六、spike 实测结论（2026-08-18，Java 桥实现必须知道）

| 结论 | 说明 | 对 Java 桥的影响 |
|------|------|------|
| **写入 ~10s 异步生效** | `put_vectors` 后索引异步构建，**立即 `query` 会 miss**（实测空 `vectors`，10s 后可见） | 首题建锚后**无需立查**（本来无近邻）；若「put 后查」路径要容忍延迟或留重试 |
| **distance 越小越相似** | cosine，self 命中 ≈ 0；同型 ~0.077（鸡兔同笼→鸡兔同笼问题） | 契约语义确认 |
| **阈值区间参考** | 异型实测：相遇→行程 0.332、鸡兔同笼 0.481 | 同型/异型在 **~0.3~0.48 有清晰间距**——归并阈值旋钮有可用区间，后端 spike 标定（默认偏保守：宁可拆不误并） |
| **query 响应字段 `vectors`** | 对齐 COS 返回，非 `hits` | Java 解析用 `data["vectors"][i].distance` |

## 七、后续 RAG 复用（为何现在打通）

这套向量服务（dashscope embedding + COS Vector Bucket + put/query 端点）就是 **RAG 的基础设施**——业务后续做文档问答/知识检索会复用同一套端点。本期打通即铺路，Python 端做一次，后面直接用。

---

**Python 端需要的全部改动 = 新增 2 个向量端点（embedding + 存取/检索）**。有问题随时对齐（契约字段、索引维度、距离语义都可以调）。
