# AI 答疑：答错必须引导、不直接结束 — 改造说明（交接 Python 侧）

> 交接对象：`aiEduPlatformModel` 仓 `ai-edu-ai-service` 答疑模块
> 交接日期：2026-08-13
> 现状：Java 侧（`aiEduPlatform`）已核查，确认改动归属 Python decide；Java 护栏与前端均无需改动。

---

## 一、问题背景

学生在数学答疑会话中作答。当前存在一种常见错误表现：

**场景**：学生回答的内容不属于这道题的解答（答错、答偏、跑题作答）时——

1. **直接结束对话**：会话被置为 TERMINATED / END，对话中断；
2. **有时候直接给答案**：结束语里直接给出答案或完整解答。

**期望行为**：学生作答后（无论对错），都应继续引导——答错给 `hint`/`approach` 引导学生自己再想一步；**只有学生明确表达结束**（"我不做了 / 结束 / 算了"）才真正结束对话。

---

## 二、为什么必须改（根因分析）

### 2.1 根因：decide 分类缺"答错 → 引导"这一档

当前 decide 系统提示词（`core/tutoring/prompts.py` 的 `_DECIDE_SYSTEM`）只有两档判断"无关 vs 模糊"：

- 完全与学习无关（闲聊/非数学）→ `end`
- 过简/模糊但相关（"我不会"）→ `concept`

**没有"学生作答但答错/答偏 → 引导（hint/approach）"这一档**。于是模型把"作答不属于题目答案"误判为"内容与学习无关"→ 输出 `type=end`。

### 2.2 为什么 Java 不能兜底

Java 侧（本仓）只有确定性护栏，**零题目状态**，无法语义区分"答错"和"真无关"：

- `type=end` 且 `end_reason` 为空 → Java `terminate()`（会话直接结束）
- 终止回复正文 = `action.summary`

所以"答错被当无关 → end"一旦在 Python 侧发生，Java 只能照单执行，无法纠正。**分类权必须在 Python decide**。

### 2.3 为什么要引导而不是直接给答案

产品定位是苏格拉底式引导（Socratic coaching），decide 角色设定即"引导学生自己解决问题，**不直接给答案**"。直接给答案违背产品定位，对学习无益，也会让学生失去主动思考的机会。

### 2.4 "有时候直接给答案"的第二个来源

学生答错时 decide 偶发输出 `reveal`（把"答错"误判为"学生要答案"）。Java 答案护栏虽能拦截第一次（answer_request_count 0→1 降级 approach），但第二次 reveal 会放行并给完整答案 + `ANSWER_REVEALED` 收尾——引导链被打断。故 `reveal` 触发条件也必须一并收紧。

---

## 三、改动方案（Python 侧）

### 规则 1：作答 ≠ 无关（新增档位）

学生**任何作答**（无论对错、是否跑偏）都属于"在解答题目"，不属于"无关内容"：

- 答错/答偏 → `type=hint`（只推一步）或 `type=approach`（学生卡住/求助时给思路大纲），`eval.correct=false`，可填 `error_type`
- 会话**保持 ACTIVE**，绝不 `end`、绝不 `reveal`

### 规则 2：`end` 收紧为四类（且必须带 `end_reason`）

| `end_reason` | 触发条件 |
|---|---|
| `COMPLETED` | 学生独立解出（`exercise_complete=true`） |
| `ABANDONED` | 学生**明确**表达放弃/结束（"我不做了""结束""算了""退出了"） |
| （空） | 与学习完全无关（闲聊/非数学内容）——**排除"答错"** |
| `safety_flag=true` | 安全内容，单独处理（Java 拦截） |

明确排除：**答错、答偏、求助都绝不归为 `end`**。

### 规则 3：终止不给答案

`end` 的 `summary` 只做原因说明/鼓励，**禁止写入完整解答或最终数值**。堵住"结束语直接给答案"的路径。

### 规则 4：`reveal` 门禁收紧

仅当历史中**学生明确表达要答案**（如"给答案""答案是多少"）才输出 `reveal`；答错、答偏绝不触发 `reveal`。

---

## 四、改动范围

| 项 | 内容 |
|---|---|
| 文件 | `ai-edu-ai-service/core/tutoring/prompts.py`（`_DECIDE_SYSTEM` 规则 1–4；`GENERATION_RULES` 中 `end` 生成规约对齐规则 3） |
| 测试 | `tests/tutoring/unit/test_decider.py` 补用例：答错→hint/approach 且保持 ACTIVE；明确放弃→end(ABANDONED)；无关→end；答错绝不 reveal |
| 契约 | 两端不变——`ActionMeta` schema 不新增字段，仅行为收紧 |

---

## 五、约束与风险

- 不改变 Java↔Python 契约字段（无 schema 变更），Java 侧无需改代码
- 不改变 Java 护栏职责（轮次 20 上限、答案计数硬拦首次 reveal 仍兜底）
- 换题短路（`is_new_question=true` → switch）不受影响
- 安全护栏（`safety_flag`）优先级最高，不受影响
- 前端无需改动（只渲染 SSE，决定权在服务端；前端"结束答疑"按钮即"用户明确结束"路径，保留）
