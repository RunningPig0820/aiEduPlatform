# AI 答疑测试计划

## 测试策略

- **护栏单测（Java，确定性，重点）**：答案护栏、轮次护栏、换题、收尾、降级逻辑——纯规则，逐条覆盖
- **领域单测**：TutoringSession 生命周期与计数器、StudentKpMastery 掌握度规则
- **编排服务测试**（Mock `TutoringLlmClient`）：start/sendMessage 的 decide→guard→generate 编排、SSE 事件序列、落库副作用
- **契约测试**：`TutoringLlmClient` 对 Python decide/generate 响应的解析、SSE 解析
- **集成测试**：真实 MySQL（Flyway）、Redis 缓存一致性、COS 归档（Mock FileStorageService）
- **端到端**：真实 Python 服务验证完整答疑、护栏拒绝、换题、20 轮、断点恢复

## 护栏单测（核心，确定性）

### 答案护栏
- [ ] `type=reveal` 且 `answer_request_count=0` → DENY + fallback=approach，count→1
- [ ] `type=reveal` 且 `answer_request_count=1` → ALLOW，count→2，标记已揭示
- [ ] `type=approach` 始终 ALLOW（第 1 次出口）
- [ ] 重决策仍 reveal 时 Java 直接降级固定思路话术 + count→1

### 轮次护栏
- [ ] `type=hint/approach` 且 `round_count=20` → DENY + fallback=end(ROUND_LIMIT)
- [ ] `type=concept` / `switch` 不消耗轮次
- [ ] round 达 20 后学生继续发言 → 强制收尾，提示"本轮已结束"

### 换题/收尾
- [ ] `type=switch` → 旧题 ABANDONED（不点亮）+ round/answer 计数归零
- [ ] `type=end` 按 endReason 校正掌握度：COMPLETED → 提升 75+；ANSWER_REVEALED / ABANDONED / ROUND_LIMIT → 不提升
- [ ] 收尾触发 COS 归档（验证 upload 参数、transcript_url 回填、Redis 清理）

## 领域单测

- [ ] TutoringSession：`start()` → ACTIVE；`terminate()` → TERMINATED；`complete()` → ARCHIVED+endReason；`switchQuestion()` 重置计数；`recordRound()` 达 20 抛异常
- [ ] StudentKpMastery：signal 分值映射、取 max 单调不减、显式纠正例外下调、`raiseByCorrection()` 上限 100
- [ ] KpKey 拒绝空/非法 URI；ActionType/EndReason 取值封闭

## 编排服务测试（Mock TutoringLlmClient）

### start()
- [ ] ACADEMIC → decide 返回 type=hint → 护栏通过 → 建会话 → generate 流式返回首条引导
- [ ] 无关 / 学习方法 / 非数学 / 安全 → 直接 TERMINATED，无 generate 调用
- [ ] 5 分钟内创建 >3 会话 → 40003
- [ ] decide 失败重试 1 次后仍失败 → 40004，不建会话

### sendMessage()（SSE 事件序列）
- [ ] 正常一轮：meta（type=hint）→ token 流 → done；round_count 落库、掌握度/错误按 signal 更新
- [ ] 护栏拒绝（reveal 未授权）：**无 token 流**，meta 带 denied + fallback 思路
- [ ] 学生换题：decide 返回 switch → 归档旧题 + 计数重置 → 新题继续
- [ ] eval.correct=false → 写 t_error_event（含 emotion）+ last_emotion 落库
- [ ] 会话 ARCHIVED 后再发消息 → 40002
- [ ] mastery_signals 为空 → 跳过掌握度更新，不报错
- [ ] decide 输出非法 type → 走默认（type=hint），记日志，不阻断

### requestAnswer()
- [ ] 第 1 次 → approach（count=1）；第 2 次 → reveal（count=2）
- [ ] 会话已结束 → 40002，不计数

### OCR 前置
- [ ] `POST /api/tutoring/ocr`：图片上传 → 代理 Python `/api/ocr/recognize` → 返回 {text, confidence}
- [ ] Python 识别失败重试 1 次后仍失败 → 40004；无效图片 → 40005
- [ ] 识别文本经确认/修改后作为 current_question 发起会话

### 降级与契约
- [ ] Python decide 结构化输出兜底：返回 **200 + ActionMeta(type=hint, degraded=true)** → Java 按普通 hint 放行 + 记日志，不拦护栏（不使用 503）
- [ ] decide 输出非法 type → 走默认 type=hint，记日志，不阻断（degraded 场景被此逻辑覆盖）
- [ ] `TutoringContextAssembler`：mastery_snapshot 带 kp_label 传入 decide（Python label 接地）
- [ ] emotion 落库为 F7 七态之一（t_error_event.emotion / t_tutoring_session.last_emotion）

## 契约测试（TutoringLlmClient）

- [ ] decide 响应解析：type/reason/eval/mastery_signals/new_question/end_reason/summary/safety_flag 各字段
- [ ] generate SSE 解析：token/done 事件序列正确映射
- [ ] OCR 响应解析：`/api/ocr/recognize` 返回 {text, confidence}
- [ ] 非法 JSON（缺字段/类型错）→ 降级（默认值 + 记日志），不抛未捕获异常

## 集成测试

- [ ] Flyway V9-V11 在干净库上可执行，表结构与索引正确
- [ ] `t_student_kp_mastery` UPSERT：同一 student+kp_key 只保留最新，mastery_level 单调更新
- [ ] `TutoringKpResolver`：精确/模糊命中 math 知识点返回 URI；未命中返回 null
- [ ] Redis 会话缓存含完整消息，断点恢复命中；归档后 Redis 消息被清理
- [ ] `TutoringTranscriptArchiver`：写 COS（Mock 验证 upload）、回填 transcript_url、幂等重写、脱敏

## 端到端（需真实 Python 服务）

- [ ] 完整答疑：发起 → 引导 → 回答 → 收尾 → 掌握度落库 → 图谱叠加接口返回
- [ ] 类型先行流式：meta 先到、token 流、done；护栏拒绝无 token
- [ ] 答案出口：2 次要答案后给出答案
- [ ] 换题：计数重置，旧题不点亮
- [ ] 20 轮到顶强制收尾
- [ ] 断点恢复：中途中断后 GET 会话续聊
