# AI 答疑 Agent 契约文档（Python 侧）

> 更新：2026-08-06
> 定位：`ai-edu-ai-service` 内独立答疑模块，**无状态、纯智能**，不碰 MySQL / KG / COS。
> Java 网关（认证/护栏/落库/COS）按服务边界调用，本模块只做决策与生成。
> 详细契约同步 `openspec/changes/ai-tutoring/api.md` + `design.md`。

---

## 1. 端点概览

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/tutoring/decide` | POST | 非流式，输出动作元数据 `ActionMeta`（Java 护栏据此审批） |
| `/api/tutoring/generate` | POST | 流式 SSE，按已放行 `action_type` 输出正文（类型先行） |
| `/api/ocr/recognize` | POST | 非流式，题目照片识别（image-first 后为兼容/降级路径，保留） |

认证：Java→Python 用 `x-internal-token`（复用 llm-gateway 模式）。Python 不自己做认证。

---

## 2. decide 契约（非流式）

### 请求 `DecideRequest`

```json
{
  "history": [
    {"role": "user", "content": "", "image_url": "https://cos/.../20260806-191428-614.png"},
    {"role": "ai", "content": "先找题目里的已知条件"}
  ],
  "round_count": 2,
  "answer_request_count": 0,
  "mastery_snapshot": [{"kp_key": "http://edukg.org/...", "label": "二元一次方程组", "mastery_level": 50}],
  "subject_hint": "math",
  "is_new_question": false
}
```

字段说明：

| 字段 | 类型 | 说明 |
|------|------|------|
| `history` | `ChatTurn[]` | 完整对话历史。`ChatTurn` = `{role, content, image_url?}`；题目图以 `image_url` 进历史，`content` 可为空串 |
| `round_count` | int | 轮次计数（护栏计数器） |
| `answer_request_count` | int | 已请求答案次数（护栏计数器） |
| `mastery_snapshot` | `KpSnapshot[]` | 学生已有掌握度，`label` 必带（label 接地，降低 label→URI 解析噪声） |
| `subject_hint` | str | 学科，本期恒 `math` |
| `is_new_question` | bool | **换题信号**：Java 检测到新题图 URL 首次出现 → `true`。**Python 短路**：见下 |

> **`is_new_question=true` 短路**：不调 LLM，直接返回 `type=switch`（确定性 100% 准、省调用）。
> 判定权在 Java（只有 Java 知道"本轮新上传了图"）；Python **不依赖 history 图片结构推断换题**
> （该做法有 bug：换题后每轮 history 都带旧图+新图，会被误判成连续换题）。

### 响应 `ActionMeta`

见 §4。结构化输出保障见 §6（四段降级，绝不返回畸形/抛异常）。

---

## 3. generate 契约（流式 SSE，类型先行）

### 请求 `GenerateRequest`

```json
{
  "history": [ {ChatTurn}... ],
  "subject_hint": "math",
  "action_type": "hint",
  "action_meta": { ActionMeta（Java 放行时附带的原决策元数据） }
}
```

> `action_type` 为**护栏已放行**的类型（可能异于 decide 原始输出，如 reveal 被拦→approach），
> Python 据此约束生成正文与 type 一致。`history` 同样携带 `image_url`（视觉模型可引用图中元素）。

### SSE 事件流

```
event: meta   data: {"action_type": "hint"}              # 类型先行，前端据此渲染 UI 骨架
event: token  data: {"content": "先看"}                   # 正文流（逐 token/逐 chunk）
event: token  data: {"content": "题目里的已知条件"}
event: done   data: {"model_used": "doubao/doubao-seed-2-0-lite"}
event: error  data: {"detail": "..."}                     # 异常时（HTTP 非 2xx 或中断）
```

Java 侧只透传 `token` 事件（其 `meta`/`done` 无正文，Java 自建 meta/done）。`token` 的 `content` 累积成完整 AI 回复，落库 Redis + COS。

---

## 4. ActionMeta schema（decide 输出，Java 护栏审批依据）

```json
{
  "type": "hint" | "approach" | "reveal" | "concept" | "switch" | "end",
  "reason": "string | null",
  "eval": {
    "correct": false,
    "error_type": "COMPUTATION | null",
    "emotion": "NEUTRAL|CONFUSED|FRUSTRATED|ANXIOUS|CONFIDENT|INTERESTED|BORED",
    "exercise_complete": false
  },
  "mastery_signals": [{"kp_label": "二元一次方程组", "signal": "mastered|practicing|struggling"}],
  "new_question": "string | null",
  "end_reason": "COMPLETED|ANSWER_REVEALED|ABANDONED|ROUND_LIMIT | null",
  "summary": "string | null",
  "safety_flag": false,
  "degraded": false
}
```

| 字段 | 说明 |
|------|------|
| `type` | 动作闭集，Java 护栏据此放行/拒绝 |
| `reason` | 决策理由（可选，调试用） |
| `eval` | 软信号：正误、错误类型、F7 情绪、是否独立解出 |
| `mastery_signals` | 掌握度信号，`kp_label` 优先复用 `mastery_snapshot` 候选 label |
| `new_question` | switch 时的新题文本（仅展示可选，不落库） |
| `end_reason` | type=end 时联动收尾原因 |
| `safety_flag` | 高危内容标记（拦截由 Java 执行） |
| `degraded` | 结构化输出兜底标记（四段管线全失败时 true，Java 监控降级频次） |

---

## 5. prompt 设计要点（苏格拉底式）

**角色**：苏格拉底式 AI 学习教练，引导学生自己解决问题，**不直接给答案**。

### 动作类型语义（decide 决策器）
| type | 含义 | 硬约束 |
|------|------|--------|
| `hint` | 一条引导性反问 | 零步骤、不给数值答案/解题步骤 |
| `approach` | 思路步骤大纲 | 步骤名+关键公式，不给完整演算与最终数值 |
| `reveal` | 完整解答 | 仅学生明确要答案时；放行由 Java 护栏决定（第 1 次思路 / 第 2 次答案） |
| `concept` | 澄清/追问 | 输入过简/模糊（"我不会"）时选，不终止会话 |
| `switch` | 换题 | `is_new_question=true` 短路，或学生贴出新题 |
| `end` | 收尾 | 独立解出/放弃/无关内容/轮次上限 |

### 关键规则
- **hint vs approach**：只推一步 → hint；给出完整解题路径骨架 → approach
- **exercise_complete 联动**：学生回答正确且独立解出 → type 必须 `end` + `end_reason=COMPLETED`
- **首条消息绝不判 switch**：历史只有一条、无老师回复 → 不能是 switch（无旧题可换），应 hint/approach/concept
- **终止型无关 vs 澄清型模糊**：完全与学习无关（闲聊/非数学）→ `end`；过简但相关（"我不会"）→ `concept`
- **安全**：自伤/暴力等 → `safety_flag=true`（拦截由 Java 执行）
- **当前题目**：由 Python 从 history 推断（Java 零题目状态）；视觉模型结合 `image_url` 看图，可引用图中元素（受力图/实例图）

### 生成器（generate）按类型约束
| action_type | 生成规约 |
|------|------|
| `hint` | 只给一条引导性反问，零步骤，无数值答案 |
| `approach` | 思路步骤大纲，不给完整演算/最终答案 |
| `reveal` | 完整解答与讲解，可给最终数值 |
| `concept` | 结合语境澄清，给引导或确认，不给答案 |
| `switch` | 确认换题，提示学生准备开始新题 |
| `end` | 按 end_reason 总结（COMPLETED 肯定+总结 / ANSWER_REVEALED 确认 / ABANDONED 鼓励 / ROUND_LIMIT 说明） |

---

## 6. 结构化输出保障（四段降级，安全关键）

保证 decide 绝不返回畸形 ActionMeta：

| 优先级 | 方式 | 说明 |
|--------|------|------|
| ① | `bind_tools` function calling | 原生 tool-calling，args 交 Pydantic 校验 |
| ② | JSON mode | `response_format=json_object` + 解析校验 + 有限纠错 |
| ③ | 正则提取 + Pydantic | 从混杂文本提取首个平衡 JSON 对象 |
| ④ | 兜底 | `ActionMeta(type=hint, degraded=true)`，绝不抛异常、绝不吐畸形 |

> 兜底按普通 hint 放行（200），**不用 503**（避免会话中断）。

---

## 7. 图像优先（视觉模型）

- 题目是图片（含受力分析图/实例图），答疑分析必须看到原图 → **答疑引擎为视觉模型**
- 模型：**豆包 `doubao-seed-2-0-lite`**（火山方舟，全模态：图/文/音统一理解）
- 图片 URL 在 `history` 消息的 `image_url`；decide/generate 均携带，模型看图作答
- 换题 = 学生发新图 → Java `is_new_question=true` → 短路 switch（见 §2）

---

## 8. 模型配对建议

| 角色 | 建议 | 原因 |
|------|------|------|
| decide（决策器）| **快模型**（判断密集、要稳、便宜）| 每轮必调、决定动作类型；温度低（0.3） |
| generate（生成器）| **强模型**（内容生成、要有层次）| 面向学生的正文质量最重要；温度高（0.7） |

> 当前实测用 `doubao-seed-2-0-lite` 兼顾两者（全模态视觉 + 数学推理 AIME 93）。升级路径：
> 强模型可用 `doubao-seed-2-0-pro`（AIME 98.3），弱模型可换 `deepseek-v4-flash`（省成本）。
> 视觉场景下 decide 与 generate 都必须是**视觉模型**（deepseek 纯文本看不到图，不适用）。
