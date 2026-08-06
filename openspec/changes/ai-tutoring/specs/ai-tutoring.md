# ai-tutoring Specification

## Purpose

AI 答疑（能力受限 agent + 工具护栏）：学生提交题目后，Java 网关编排——安全过滤 → 组装上下文 → 调 Python `decide`（非流式，出动作元数据）→ **Java 护栏校验动作** → 调 Python `generate`（流式，出正文）SSE 透传前端。答疑中渐进确认学生知识点掌握度，按知识图谱 TextbookKP URI 为 key 落库，供图谱叠加展示。

会话仅保留生命周期 3 状态（ACTIVE / ARCHIVED / TERMINATED）+ 护栏计数器（round_count ≤ 20、answer_request_count），不随题目/对话/换题增长。本期仅支持数学。MVP 为 L0 单次调用（LangChain 结构化输出），无 agent 循环。

## Requirements

### Requirement: 学生可以拍照识别题目（OCR 前置）

系统 SHALL 支持学生上传题目照片，通过 OCR 识别为题目文本，供学生确认/修改后作为当前题目进入答疑。OCR 是答疑前的独立预处理，不进 decide/generate 契约。

#### Scenario: 拍照识别成功
- **WHEN** 学生上传一张数学题目照片到 `POST /api/tutoring/ocr`
- **THEN** Java 认证后代理调 Python `POST /api/ocr/recognize`，返回 `{text, confidence}`
- **THEN** 前端展示识别文本供学生确认/修改

#### Scenario: 确认后进入答疑
- **WHEN** 学生确认（或修改后确认）识别出的题目文本
- **THEN** 该文本作为**首条学生消息**发起答疑会话，进入正常答疑流程

#### Scenario: OCR 识别质量差
- **WHEN** OCR 识别结果可能错误（公式/上下标易错）
- **THEN** 识别结果必须经学生确认/修改后才能进答疑，未确认不进入引导

### Requirement: 学生可以发起答疑会话

系统 SHALL 允许学生携带题目文本发起答疑会话，返回会话 ID 与首条引导（类型先行流式）。

#### Scenario: 正常发起学科问题
- **WHEN** 已登录学生发送 POST `/api/tutoring/sessions`，请求体含 `message`（如"鸡兔同笼，笼子里有 35 个头、94 只脚，问各几只？"），`student_id` 取自 `HttpSession.getAttribute("userId")`（STUDENT 角色校验）
- **THEN** 系统安全预检 → 组装上下文 → 调 Python `decide`，返回 action 元数据（type=hint、ACADEMIC、subject=math）
- **THEN** Java 护栏校验通过 → 创建 `t_tutoring_session`（status=ACTIVE）→ 调 Python `generate`（type=hint）流式生成首条引导
- **THEN** 首条引导只给提示，不含完整答案，SSE 返回 meta + token + done 事件

#### Scenario: 会话创建频率过高
- **WHEN** 同一学生 5 分钟内创建会话超过 3 个
- **THEN** 系统拒绝新会话，提示"请先完成当前答疑"

### Requirement: 无关内容获得学习范围提示并终止

系统 SHALL 将完全与学习无关的输入识别并终止，不进入解题流程。

#### Scenario: 闲聊类输入
- **WHEN** 学生发起会话，消息为"今天天气怎么样"
- **THEN** `decide` 判定无关（或输出相应 type），Java 回复"我主要解答学科问题，请提出学习相关的内容"，会话置 TERMINATED

#### Scenario: 非数学学科
- **WHEN** 学生提交一道英语题
- **THEN** `decide` 判定 subject≠math
- **THEN** 系统回复"当前仅支持数学答疑"并终止会话

### Requirement: 苏格拉底式分步引导

系统 SHALL 在引导中每次仅输出一条提示/反问，逐步推进，严禁直接输出完整答案（除非答案护栏放行）。

#### Scenario: 分步推进
- **WHEN** 会话 ACTIVE 且学生正常回答
- **THEN** Java 组装上下文（历史 + counters + 掌握度快照；**不含当前题目**，后端不记录）→ 调 `decide` 输出 type=hint 及 eval/mastery_signals
- **THEN** Java 护栏通过 → 落库副作用（掌握度/错误/情绪）→ `generate`（type=hint）流式返回一条引导
- **THEN** 引导类动作 round_count+1；达 20 → 强制收尾

### Requirement: 学生回答获得智能反馈

系统 SHALL 评估学生每轮回答的正确性，对错误给出引导性反问而非直接评判。

#### Scenario: 回答错误
- **WHEN** 学生回答错误
- **THEN** `decide` 返回 eval（correct=false、error_type、emotion）
- **THEN** Java 写 `t_tutoring_error_event`（含 emotion）、按 mastery_signals 更新掌握度
- **THEN** `generate`（type=hint）给出引导性反问（如"这一步的依据是什么？"），不直接评判

### Requirement: 答案护栏（第 1 次思路 / 第 2 次答案）

系统 SHALL 在学生请求答案时按答案护栏放行动作：第 1 次给思路，第 2 次给完整答案。

#### Scenario: 第 1 次请求答案
- **WHEN** 学生消息表达要答案（或调用 request-answer），`decide` 输出 type=reveal
- **THEN** Java 护栏检查 answer_request_count=0 < 1 → **拒绝 reveal**，重决策为 approach（或 Java 降级）→ count→1
- **THEN** `generate`（type=approach）返回解题思路（步骤提纲、关键公式），不含完整演算

#### Scenario: 第 2 次请求答案
- **WHEN** 学生再次要答案，`decide` 输出 type=reveal
- **THEN** Java 护栏检查 count=1 ≥ 1 → **放行** → count→2，标记已揭示
- **THEN** `generate`（type=reveal）返回完整答案与讲解，随后 Java 触发收尾（end_reason=ANSWER_REVEALED）

### Requirement: 会话轮次上限

系统 SHALL 限制单次会话最多 20 轮学生回答，达到上限后强制收尾。

#### Scenario: 达到 20 轮
- **WHEN** round_count 达到 20 且会话仍 ACTIVE
- **THEN** 引导类动作被护栏拒绝，Java 强制收尾（end_reason=ROUND_LIMIT）
- **THEN** 收尾后学生继续发送消息，系统提示"本轮答疑已结束，可发起新会话"

### Requirement: 知识点渐进确认与掌握度落库

系统 SHALL 按每轮 eval 结果渐进更新学生知识点掌握度，以 TextbookKP URI 为 key 幂等落库。

#### Scenario: 掌握度更新
- **WHEN** `decide` 返回 mastery_signals（如 `{kp_label:"鸡兔同笼", signal:"mastered"}`）
- **THEN** Java 通过 `TutoringKpResolver` 将 label 解析为 TextbookKP URI，UPSERT `t_student_kp_mastery`（student_id+kp_key 唯一），mastery_level 取 max(现值, signal分值)
- **THEN** 未命中的 label 记日志并标记"待收录"，不阻断答疑

#### Scenario: 看过答案不提升掌握度
- **WHEN** 会话经 ANSWER_REVEALED 收尾
- **THEN** Java 不提升涉及知识点掌握度（只保留过程中 eval 信号产生的值），记录错误事件

### Requirement: 换题与计数重置

系统 SHALL 在学生换题时重置护栏计数，按新题重新计；旧题知识点不按完成校正（不点亮）。**换题/当前题目判定链路在 Python decide**（从 history 语义判断）；后端不记录、不维护题目内容。

#### Scenario: 中途换题
- **WHEN** 学生贴出新题，`decide` 输出 type=switch + new_question
- **THEN** Java 仅 round_count / answer_request_count 归零（换题判定在 Python）；旧题知识点不校正（不点亮）
- **THEN** 会话保持 ACTIVE，按新题继续引导

### Requirement: 会话收尾与归档

系统 SHALL 在会话结束时按退出路径校正掌握度，生成总结并将对话终态写 COS。

#### Scenario: 正常收尾
- **WHEN** 会话收尾（独立解出 / 看过答案 / 20 轮到顶 / 主动结束）
- **THEN** Java 按 end_reason 校正掌握度（COMPLETED → 提升到 75+；其余不提升）
- **THEN** 生成总结（涉及知识点/薄弱点）→ 对话终态写 COS（`tutoring/transcripts/{sessionId}.json`，脱敏、私有读；对话全程已每轮实时整写，此处为终态）→ 置 ARCHIVED

#### Scenario: 断点恢复
- **WHEN** 学生通过 GET `/api/tutoring/sessions/{id}` 查询进行中的会话
- **THEN** 系统返回会话状态、计数、最近消息（来自 Redis；当前题目后端不记录，前端从最近消息推断），供前端续聊
- **THEN** 续聊后新消息在原会话继续推进，不新建会话

### Requirement: 掌握度查询供图谱叠加

系统 SHALL 提供按学生查询知识点掌握度的接口，供知识图谱前端叠加渲染。

#### Scenario: 查询学生掌握度
- **WHEN** 前端 GET `/api/students/{studentId}/mastery`
- **THEN** 系统返回 `[{kp_key(uri), kp_label, mastery_level, updated_at}]`，前端按 URI 叠加到图谱节点

### Requirement: 类型先行流式（护栏安全）

系统 SHALL 在流式回复前先放行动作类型，保证任何内容流入学生前已完成护栏校验。

#### Scenario: 流式回复
- **WHEN** 学生发送回答，Java 完成 decide 与护栏校验
- **THEN** SSE 先发 `meta` 事件（type、round_count）→ 再发 `token` 事件（正文流）→ `done` 事件（状态、eval）
- **THEN** 若护栏拒绝（如 reveal 未授权），**不产生任何 token 事件**，直接返回降级后的 meta 与思路

### Requirement: 安全过滤

系统 SHALL 过滤自伤、暴力等高危内容，命中即终止 AI 应答。

#### Scenario: 高危内容
- **WHEN** 学生消息命中本地关键词规则或 `decide` 返回 safety_flag=true
- **THEN** 系统终止会话并提示"该内容超出答疑范围，如需帮助请联系老师或监护人"，标记安全事件

## Python 端点契约（Java 内部调用，L0 单次调用）

> 实现位于 `ai-edu-ai-service` 独立答疑模块；本仓库定义契约。Java 经内部 token 调用；Python 无状态、不碰数据源。`subject_hint` 恒传 `math`。

### `POST /api/tutoring/decide`（非流式）

请求：
```json
{
  "history": [{"role": "user", "content": "..."}, {"role": "ai", "content": "..."}],
  "round_count": 3,
  "answer_request_count": 0,
  "mastery_snapshot": [{"kp_key": "http://edukg.org/...", "label": "二元一次方程组", "mastery_level": 50}],
  "subject_hint": "math"
}
```
> **判定链路**：请求**不含 `current_question`**——换题 / 当前题目由 Python decide 从 `history` 语义判断（Java 不记录、不维护题目内容，记录易错）。Java 只认 `type=switch` 事件重置计数；`new_question` 为 Python 输出、Java 仅作展示可选、不落库。
响应（action 元数据）：
```json
{
  "type": "hint",
  "reason": "学生已列方程，下一步给一条引导性反问",
  "eval": {"correct": true, "error_type": null, "emotion": "NEUTRAL", "exercise_complete": false},
  "mastery_signals": [{"kp_label": "二元一次方程组", "signal": "practicing"}],
  "new_question": null,
  "end_reason": null,
  "summary": null,
  "safety_flag": false,
  "degraded": false
}
```
> `emotion` 为 **F7 七态**（NEUTRAL/CONFUSED/FRUSTRATED/ANXIOUS/CONFIDENT/INTERESTED/BORED，Python 输出方权威）。`reason` 为 Python 可选发送的调试字段，**Java 不建模**（Jackson 默认容忍未知字段 FAIL_ON_UNKNOWN_PROPERTIES=false）。`mastery_snapshot` 请求须带 `label`（Python 侧 label 接地）。结构化输出失败时 Python 兜底返回 **200 + ActionMeta(type=hint, degraded=true)**（不使用 503），Java 按普通 hint 放行 + 记日志。

### `POST /api/tutoring/generate`（流式 SSE）

请求：
```json
{
  "history": [...],
  "subject_hint": "math",
  "action_type": "hint",
  "action_meta": {"eval": {"correct": true, "emotion": "NEUTRAL"}}
}
```
响应：SSE 事件流，`event: token, data: {"content": "..."}` 直至 `event: done`。生成内容必须与已放行的 `action_type` 一致（approach 只给思路、reveal 才给完整答案）。

### `POST /api/ocr/recognize`（OCR 前置）

请求：`multipart file`（题目照片）；响应：`{text, confidence}`。Java 编排：前端 `POST /api/tutoring/ocr` 上传 → 代理本端点 → 识别文本供学生确认/修改 → 确认后作为**首条学生消息**进答疑。不进 decide/generate 契约。

> `decide` 建议用快模型（低成本、低延迟），`generate` 用强模型（流式质量）。两段式保证"类型先行"：任何内容流出前 type 已过 Java 护栏。
