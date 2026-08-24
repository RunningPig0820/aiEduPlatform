# AI 答疑测试计划

## 测试策略

- **护栏单测（Java，确定性，重点）**：答案护栏、轮次护栏、换题、收尾、降级逻辑——纯规则，逐条覆盖
- **领域单测**：TutoringSession 生命周期与计数器、StudentKpMastery 掌握度规则
- **编排服务测试**（Mock `TutoringLlmClient`）：start/sendMessage 的 decide→guard→generate 编排、SSE 事件序列、落库副作用
- **契约测试**：`TutoringLlmClient` 对 Python decide/generate 响应的解析、SSE 解析
- **集成测试**：真实 MySQL（Flyway）、Redis 缓存一致性、COS 归档（Mock FileStorageService）
- **端到端（真实）**：真实 Java↔Python 全链路（Python 服务 + Java 后端 + MySQL/Redis/COS）。**答题功能优先**（T1 解题流 / T2 护栏 / T3 换题 + E 系列健壮性），**知识点点亮延后**（T4，答题链路稳定后阶段 2 再联调）

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
- [ ] 对话每轮实时整写 COS（Mock 验证 upload 幂等），收尾终态写 + transcript_url 回填 + Redis 清理

## 领域单测

- [ ] TutoringSession：`start()` → ACTIVE；`terminate()` → TERMINATED；`complete()` → ARCHIVED+endReason；`switchQuestion()` 重置计数；`recordRound()` 达 20 抛异常
- [ ] StudentKpMastery：signal 分值映射、取 max 单调不减、显式纠正例外下调、`raiseByCorrection()` 上限 100
- [ ] KpKey 拒绝空/非法 URI；ActionType/EndReason 取值封闭

## 编排服务测试（Mock TutoringLlmClient）

### start()
- [ ] ACADEMIC → decide 返回 type=hint → 护栏通过 → 建会话 → generate 流式返回首条引导
- [ ] 无关 / 学习方法 / 非数学 / 安全 → 直接 TERMINATED，无 generate 调用
- [ ] 5 分钟内创建 >3 会话 → 50004
- [ ] decide 失败重试 1 次后仍失败 → 50005，不建会话

### sendMessage()（SSE 事件序列）
- [ ] 正常一轮：meta（type=hint）→ token 流 → done；round_count 落库、掌握度/错误按 signal 更新
- [ ] 护栏拒绝（reveal 未授权）：**无 token 流**，meta 带 denied + fallback 思路
- [ ] 学生换题：decide 返回 switch → 归档旧题 + 计数重置 → 新题继续
- [ ] eval.correct=false → 写 t_tutoring_error_event（含 emotion）+ last_emotion 落库
- [ ] 会话 ARCHIVED 后再发消息 → 50003
- [ ] mastery_signals 为空 → 跳过掌握度更新，不报错
- [ ] decide 输出非法 type → 走默认（type=hint），记日志，不阻断

### requestAnswer()
- [ ] 第 1 次 → approach（count=1）；第 2 次 → reveal（count=2）
- [ ] 会话已结束 → 50003，不计数

### OCR 前置
- [ ] `POST /api/tutoring/ocr`：图片上传 → 代理 Python `/api/ocr/recognize` → 返回 {text, confidence}
- [ ] Python 识别失败重试 1 次后仍失败 → 50005；无效图片 → 50006
- [ ] 识别文本经确认/修改后作为首条学生消息发起会话

### 降级与契约
- [ ] Python decide 结构化输出兜底：返回 **200 + ActionMeta(type=hint, degraded=true)** → Java 按普通 hint 放行 + 记日志，不拦护栏（不使用 503）
- [ ] decide 输出非法 type → 走默认 type=hint，记日志，不阻断（degraded 场景被此逻辑覆盖）
- [ ] `TutoringContextAssembler`：mastery_snapshot 带 kp_label 传入 decide（Python label 接地）
- [ ] emotion 落库为 F7 七态之一（t_tutoring_error_event.emotion / t_tutoring_session.last_emotion）

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
- [ ] `TutoringTranscriptArchiver`：每轮实时整写 COS（Mock 验证 upload）、幂等重写、脱敏、首次写回填 transcript_url、收尾终态写

## 端到端（真实 Java↔Python 全链路，非 Mock）

> **执行前提**：Python 服务起（`main.py`，端口 9527，LLM key 已配）+ Java 后端起 + `ai_edu_learning` 建表（V9-V11 手动执行，flyway 关闭）+ Redis/COS 可达 + STUDENT 账号登录（session 取 userId=studentId）。
> **优先级**：答题功能（T1-T3 + E 系列）优先；**知识点点亮（T4）延后阶段 2**——答题链路稳定后再联调。
> **判定标准**：SSE 事件序列 `meta → token → done` 正确；护栏拒绝**无 token 流**；错误码 5xxxx 符合 api.md。

### P0 答题功能（✅ 2026-08-05 真实 E2E 全部验证通过）

#### T1 完整解题流（数学题目能否完成解答）✅
- [x] 发起鸡兔同笼 → SSE `meta` → `token`（引导，不含答案）→ `done`，roundCount 递增——**实测通过**
- [x] 学生逐步回答 → 引导推进（hint/approach），token 流不含完整答案——**实测通过**
- [x] 学生解出 → `done(ARCHIVED, endReason=COMPLETED)`——**实测通过**
- [x] SSE 类型先行：meta 先到、token 流、done 收尾——**实测通过**

#### T2 问题控制（护栏）✅
- [x] T2.1 答案护栏：第 1 次 request-answer → `approach + denied=reveal + answerCountInsufficient`，无完整答案——**实测通过**
- [x] T2.2 答案护栏：第 2 次 request-answer → `reveal` + 收尾 `ARCHIVED/ANSWER_REVEALED`（**B2 已修**）——**实测通过**
- [x] T2.3 轮次上限：config `round-limit` 临时调 2 → round≥2 后 `end(ROUND_LIMIT)` + ARCHIVED——**实测通过**
- [x] T2.4 无关内容 → 终止（⚠️ 实测 status=ARCHIVED 而非 TERMINATED，见 **B4**）
- [x] T2.5 非数学 → 终止（⚠️ 同 B4，ARCHIVED）
- [x] T2.6 安全内容（自伤）→ `TERMINATED` + "检测到学生有危险情绪"（Python `safety_flag` 拦截）——**实测通过**

#### T3 换题 ✅
- [x] 对话中贴新题 → `switch` + round/answer 计数归零 + `newQuestion`——**实测通过**
- [ ] 换题后旧题知识点不新增（点亮验证延后 T4 阶段 2）

### P1 补充测试点（答题链路健壮性）
- [x] E1 降级：停 Python → "网络波动"流 + 会话保持 ACTIVE——**实测通过**
- [ ] E2 degraded 兜底：Python 结构化输出四段全失败（难人为触发；单测覆盖 degraded=true 放行）
- [x] E3 SSE 过滤：Python 发的 `meta/done` 不泄漏为假 token——**实测通过**
- [ ] E4 OCR 前置：需真实题目照片 + 百度 OCR key（未测）
- [x] E4b OCR 开关：`GET /api/tutoring/config` → `{ocrEnabled:true}`——**实测通过**；`ocr.enabled=false` → 50006（单测覆盖）
- [ ] E5 断点恢复：Redis 过期后从 MySQL 查（需清 Redis 或等 24h TTL）
- [x] E6 创建频率：5 分钟 >3 个 → 50004——**实测通过**
- [ ] E7 并发：同会话双发 → 锁拒绝 10000（单测覆盖，未真实双发）
- [x] E8 越权：TEACHER 访问学生会话 → 20004（同步+SSE）——**实测通过**（真跨学生需两个学生账号）
- [x] E9 已归档后 `requestAnswer` / `messages` → 50003——**实测通过**
- [x] E10 输入校验：空消息 start→10001 / sendMessage→SSE 10003——**实测通过**
- [x] E11 transcript 签名 URL：COS presigned URL（30 分钟有效）——**实测通过**

### P2 知识点点亮（延后，阶段 2）
- [ ] T4.1-T4.5（**B5**：实测模型输出 mastery_signals 但 kg 镜像缺 label → "待收录"不点亮，kg 数据依赖）

### 已知缺口
- [x] **G2 reveal 后收尾 ANSWER_REVEALED**——**已修复（B2）+ 实测验证**（第 2 次 reveal → ARCHIVED/ANSWER_REVEALED）
- [ ] **G1 安全预检关键词拦截**——未实现；当前靠 Python `safety_flag`（实测 T2.6 有效，但内容仍先发 LLM）
- [ ] **G3 answer-request-limit 强制**——G2 修复后 reveal 即收尾，天然无第 3 次；配置项仍未接入护栏

### 实测发现（2026-08-05，真实 LLM）
- [x] **B1 首问误判 switch**——**已修复**（Python decide prompt 增"首条消息绝不能是 switch"规则）+ 验证：真实模型首问测试通过、Java E2E 首问返回 approach 引导
- [ ] **B4 无关/非数学 status=ARCHIVED 而非 TERMINATED**：Python decide 对无关内容返回带 end_reason 的 end → Java 走收尾路径——需 Python 侧判定调整（安全内容 T2.6 正确 TERMINATED）
- [ ] **B5 知识点不点亮**：模型输出 mastery_signals 但 kg 镜像缺 label → "待收录"不点亮——kg 数据依赖
- [x] **B2/B3 已修复**：reveal 收尾 + 错误事件门控（decide 原 type 为 hint/approach 且 error_type 非空才写）
- [x] **AI 回复落库已修复**：buildStream 流结束后把 AI 回复（拼接 token）追加到 Redis + 重新整写 COS——实测 transcript 已含 user/ai 完整对话
