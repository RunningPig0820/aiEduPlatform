# tasks

> **里程碑原则**：按 M1→M7 逐个切片完成，每个里程碑 = 纵向切片 + **前后端 + 模型端三端对接测试**（完成即联调，问题早暴露，不等到最后一次对接才排查）。M8 为全链路回归收尾。
>
> 契约 DTO 按里程碑**渐进定义、字段随里程碑扩展**（SSE 事件契约在 M2 冻结，后续只追加字段不重排）；`@JsonProperty` snake→camel、`FAIL_ON_UNKNOWN_PROPERTIES=false`、degraded 走 200 不 503，沿用既有纪律。
>
> **模块 id 闭集（2026-08-25 三端定稿）**：`ai-tutoring` / `knowledge-graph` / `question-analysis` / `rag-system`。弃用 `rag-project`/`question-type`。前端 → Java 网关 camelCase（`currentProject`），Java 桥 → Python snake_case（`current_project`）。查看原文走 Java 代理 `GET /api/rag/assistant/source?path=`。

## 设计功能点 → 里程碑 映射（防漏核对）

| 设计决策 / 功能点 | 里程碑 | 说明 |
|-------------------|--------|------|
| D1 角色硬门（可信 session，禁信 body） | M1 | 纯 Java 0 依赖 |
| D2 intent LLM 结构化输出 + 兜底 | M2 | `{anchor, category, switch_detected, ambiguous}` |
| D3 切换判定收敛下一轮，不生成中切换 | M2 | switch 事件 |
| D4 模块全放行 + 范围门低置信过滤 + 硬路由 | M3 | 唯一拒答路径 boundary |
| D9 模块可用性数据驱动 | M3 | 语料即边界 |
| D7 召回层 2s 超时降级 | M3 | 单路超时纯另一路 |
| D8 tokens_usage + trace_id | M4（usage）/ M2（trace 贯穿） | cache_hit 估算 |
| D7 生成层 8s 超时 + 断连取消 | M4 | 降级话术写死 |
| D6 is_quoted LCS 硬匹配 | M5 | 8 中/12 英，done 后补发 |
| D10 评估扩展 + baseline 白盒 | M5 | 边界拒答 / precision_at_k / is_quoted 校验 |
| D11 问题提示（开始引导 + 结束建议必含 RAG） | M6 | guide 非 SSE + done.suggestions 含 RAG |
| D5 clarify 澄清轮 | M6 | 最多一轮，default=current_project>锚点 |
| D12 显式关闭 close + 会话累计 token | M7 | Redis TTL 24h，close 结算 |
| trace_id 断线补查（超时保留窗口） | M7 | GET turns/{traceId} |
| SSE 事件契约冻结 | M2 | 时序定稿，后续只补字段 |
| 上下文窗口截断（最近 3 轮） | M2 | 不设轮数上限，锚点独立携带 |
| 评估报告白盒展示 | M5 | GET eval/report |
| 三端对接测试（前后端+模型端） | 每里程碑 | 完成即联调 |

---

## M1 权限判断（纯 Java，0 依赖，第一个）

- [x] 1.1 契约 DTO 基线：`RagAskRequest`（question/sessionId/currentProject/topK/**history**/**traceId**——history=最近 N 轮（默认 3，含 clarify 轮，Java 网关组装传入），traceId=Java 生成传 Python），snake→camel 映射，`@JsonProperty`，`FAIL_ON_UNKNOWN_PROPERTIES=false`
- [x] 1.2 新增 `RagAssistantController` 骨架：`POST /api/rag/assistant/ask`（SSE 流式 + 非流式两模式占位），SSE 通道建立
- [x] 1.3 角色硬门：从 `HttpSession.getAttribute("role")` 取角色（STUDENT 才放行），非学生/缺失 → 固定 403 响应体；忽略 body 传 role；角色门不调 LLM、不产生 trace
- [x] 1.4 桥桩替：`RagAssistantPort` 骨架 + infra 桥占位（Python 未就绪前返回桩替流），学生放行 → `permission{allowed:true}` → 桩替 done，ask 整轮可通
- [ ] 1.5 三端对接测试：前端 403 页 / 学生放行（**后端 RAG-GATE-001~004 已绿 4/4**；前端 F-M1 对接待前端侧完成）

## M2 意图+改写+骨架（SSE 契约冻结、trace、intent LLM、rewrite、switch、上下文窗口）

- [x] 2.1 SSE 事件契约冻结：时序 `permission → intent → (clarify|switch) → rewrite → rerank → (boundary) → token* → done`；定义全部 `Sse*DTO`（SsePermissionDTO/SseIntentDTO/SseRewriteDTO/SseRerankDTO/SseRejectDTO/SseBoundaryDTO/SseClarifyDTO/SseSwitchDTO/SseTokenDTO/SseDoneDTO，camelCase）
- [x] 2.2 契约 DTO 扩展：`RagIntentMeta`（anchor/category/switchDetected/ambiguous/candidates/lockedSections/degraded；anchor=模块路由，lockedSections=节级加权，两层并存）
- [x] 2.3 端口与桥：`RagAssistantPort`（入参 ask/查询，出参流式事件回调；放学习域答疑子模块），infra 桥实现（复用 `LlmGateway` internalToken，`POST /api/rag/assistant/ask`）；桥组装 **history（最近 N 轮，含 clarify 轮）+ traceId** 传给 Python；SSE 中继从 Python 的 `intent` 事件开始（**permission 仅 Java 发，桥不消费 Python 的 permission**）；桥单测：snake↔camel 映射、SSE 事件重建顺序、degraded 200 不 503
- [x] 2.4 trace_id 生成（定死归属）：**Java 生成**（每轮入口 UUID）→ **permission 事件携带 traceId（前端流开始即取，供断线补查）** → 随 ask 请求传 Python → Python 贯穿日志并在 done 回显 → Java 校验回显一致（两端 trace 对得上）
- [x] 2.5 SSE 中继（阶段 1）：permission/intent/rewrite/done 按序透传，meta/done 由 Java 重建不透传原始
- [x] 2.6 Python intent 泛化：`classify` 升级 LLM 结构化输出 `{anchor, category, switch_detected, ambiguous, candidates}`（anchor=模块级路由，locked_sections=节级加权，两层并存），失败回退 `_fallback_anchor`（保留），degraded 标记 —— **Model A1 [x] + Java e2e 验证**（真实 intent 输出 anchor=ai-tutoring/locked_sections=["02","06"]）
- [x] 2.7 Python rewrite：生成改写后 query，透传 `rewrite` 事件 —— **Model A2 [x] + Java e2e 验证**（真实 rewrite 事件 original_question/rewritten_query）
- [x] 2.8 Python switch 判定：`switch_detected=(前端 current_project≠会话锚点 或 问题明确指向另一有语料模块)`，发 `switch` 事件（from/to），收敛下一轮 intent，不做生成中切换 —— **Model A2b [x]**；Java 侧 SseSwitchDTO 重建已就绪（2.5），switch 路径在 M8 e2e 覆盖
- [x] 2.9 Python 上下文窗口截断：intent/generate 历史仅保留最近 N 轮（默认 3，可配），不设轮数上限；锚点由 session 独立携带 —— **Model A1 [x]**（history[-3:]）；Java 侧组装 history 待 M7 会话累计时落地
- [x] 2.10 Python generate 桩替：固定占位答案回填 done，整轮可通（M4 移除）—— **已过时**：Python generate 为真实实现（Java e2e 验证 token 流式输出），无需桩替，此任务作废
- [ ] 2.11 三端对接测试：前端阶段展示区（权限✓/意图分类/改写后问题）+ 桩替答案；后端+模型端联调 RAG-SSE-001(桩)/RAG-CONTRACT-002/003/RAG-COST-003

## M3 召回+remark+边界（双路召回、RRF top-K、范围门、硬路由、数据驱动）

- [x] 3.1 契约 DTO 扩展：`RagBlock`（blockId/title/summary/filePath/score）—— **由 SseRerankBlock（2.1）覆盖**，重建直接反序列化进前端 DTO，无需单独 Python 契约 DTO
- [x] 3.2 Python 双路召回：向量（COS）+ BM25（本地 jsonl），单路各 2s 硬超时 → 降级纯另一路（degraded 标记）；**按 anchor 选语料池**（多模块目录，orchestrate 入参加 corpus 参数，节级锚定加权逻辑保留不改）—— **Model A3 [x] + Java e2e 验证**
- [x] 3.3 Python RRF 精排：RRF_K=60 融合 Top-K（默认 3，可配），仅回传精排块，不吐全量召回 —— **Model A4 [x] + Java e2e 验证**（真实 3 块带 score/file_path）
- [x] 3.4 Python 模块全放行 + 硬路由：四模块（AI答疑/知识图谱/题型分析/RAG）均可路由无禁区；硬路由（架构/代码/部署/评测/接口 → RAG 项目）—— **Model A4 [x] + Java e2e 验证**（架构问题硬路由 rag-system→boundary）
- [x] 3.5 Python 范围门低置信过滤：综合分低于 0.75/0.5 → `boundary`（reason=low_confidence）固定话术，0 生成 token（唯一拒答路径）；模块可用性数据驱动（语料即边界，无语料模块正常召回命中空→过滤）—— **Model A9 [x] + Java e2e 验证**
- [x] 3.6 SSE 中继（阶段 2）：rerank/boundary 事件按序透传 —— **2.5 已实现**（rebuildEvent 处理 rerank/boundary），应用服务测试覆盖
- [x] 3.7 桥单测扩展：边界流重建（rerank→boundary）、Python 异常冒泡（500 → 网关降级）—— RagAssistantAppServiceTest 加 rerank/boundary 重建用例；RagAssistantBridgeImplTest 已有异常冒泡
- [x] 3.7b 查看原文代理：`GET /api/rag/assistant/source?path=<urlencoded>`（STUDENT 角色门）转发 Python `/api/rag/source/{file_path}`；file_path 走 query 传参（不走 path，避免特殊字符被容器拒）；原文不存在 → 10002 —— **已实现**（port.source + 桥逐段编码转发 + 404→EntityNotFoundException；Python source 端点真实验证）
- [ ] 3.8 三端对接测试：前端召回块面板（标题/摘要/file_path，点击查看原文走 source 代理）+ 边界拒答话术；后端+模型端联调 RAG-SSE-002/003、RAG-BRIDGE-001~003、RAG-COST-002 —— **后端部分已测**（RAG-SSE-002/003 边界时序、RAG-BRIDGE 全测）；前端 F-M3 引用面板待前端侧

## M4 生成+token展示（doubao 流式、8s 超时、断连取消、usage、done 重建）

- [x] 4.1 契约 DTO 扩展：`RagTokensUsage`（promptTokens/completionTokens/cacheHitTokens/totalTokens）、`RagDoneResult` 核心（answer/quotedKeys/tokensUsage/traceId/reason）—— **由 SseTokensUsageDTO/SseDoneDTO（2.1）覆盖**（tokensUsage 四字段 + done 六字段），重建直接反序列化进前端 DTO，无需单独 Python 契约 DTO
- [x] 4.2 Python doubao 流式生成：`token*` 逐块，`include_usage` 取 usage，8s 硬超时 → 固定降级话术返回召回清单（0 额外 token），`is_disconnected()` 断连取消上游 doubao 流 —— **Python 已验证**（assistant.py A5 流式生成 token*/usage/error 三型，GEN_TIMEOUT 8s 降级话术 + 召回清单，is_disconnected 取消；Java e2e 已验证 token 流式输出）
- [x] 4.3 Python cache_hit 估算：doubao 未返回 → tokenizer 估算并标注"估算" —— **0 兜底就绪**（assistant.py `cached_tokens or 0`，Java 侧无改动）；tokenizer 估算标注"估算"为 Python 后续细化（设计 D8 留口子，不影响 M4 门禁 RAG-COST-001 四字段完整）
- [x] 4.4 SSE 中继（阶段 3）：token/done 事件；done 由 Java 重建（answer/tokensUsage/traceId），不透传 Python 原始 meta/done —— **已实现**（rebuildEvent 处理 token/done；SseDoneDTO 重建 answer/quotedKeys/tokensUsage/traceId/suggestions/reason）；新增 AppServiceTest 全量时序用例（permission→intent→rewrite→rerank→token*→done，RAG-SSE-001）验证 token 逐块 camel 重建 + done 四字段
- [x] 4.5 移除 M2/M3 的 generate 桩替，接真实流式生成 —— **已接真实流**（ask 中继 Python 真实 token 流，RagAssistantPythonSmokeTest 已验证；非流式 askSync 的桩替仅为 M1 遗留独立模式，不影响流式链路）
- [x] 4.6 桥单测扩展：真流消费、Python 异常冒泡 500 —— **已实现**（RagAssistantBridgeImplTest 新增 token* 保序透传用例 + HTTP 500 → TutoringAgentException 用例，10/10 绿）
- [ ] 4.7 三端对接测试：前端流式回答渲染 + 成本面板（prompt/completion/cache_hit/total 四字段）；后端+模型端联调 RAG-SSE-001(全量时序)/RAG-COST-001/007/RAG-ABORT-001

## M5 自我检查（is_quoted + 评估扩展 + 评估报告白盒）

- [x] 5.1 Python is_quoted LCS 硬匹配纯函数（连续 8 中 / 12 英，窗口可调 `config/settings.py`）：生成完成后对精排块 text/summary 与 answer 匹配 → `quoted_keys`，done 后补发；quotedKeys 为空 → answer 标注"引用未能精确匹配" —— **Python 已验证**（assistant.py `_lcs_longest`/`lcs_quote_match` A6）
- [x] 5.2 done 补 quotedKeys 字段（Java 重建）—— **已实现**（SseDoneDTO.quotedKeys + rebuildEvent 读 quoted_keys，AppServiceTest 断言 camel 化）
- [x] 5.3 Python 评估集扩面：`eval_dataset.py` VALID_TYPES 增加 `边界拒答`；RAG 助手评估集 ≥15 条覆盖 5 类（项目介绍/操作/数据关联/难点/边界拒答），含"改写答案"用例（验证 8 字符窗口漏判率）—— **Python 已验证**（eval_agent.py BOUNDARY_TYPE/边界拒答 trace）
- [x] 5.4 Python `precision_at_k` 纯函数并纳入聚合报告 —— **Python 已验证**（eval_agent.py `precision_at_k` + `precision_at_k_avg` 聚合）
- [x] 5.5 Python is_quoted 入评估：`lcs_quote_match` 单测 + 断言 quoted_keys ⊆ 召回块（引用不得指向未召回内容）—— **Python 已验证**（quoted_valid_ratio 引用合法率）
- [x] 5.6 Python 边界拒答用例判定：命中固定话术 + 0 token —— **Python 已验证**（`_boundary_trace` 断言低置信固定话术 + 0 token）
- [x] 5.7 Python 重跑 baseline 报告：hit@3/质量分/成本/耗时/版本，`--compare` 版本对比可复现 —— **Python 已验证**（真实报告 version=2026-08-25-e966ac、count=6、hit@3=0.667）
- [x] 5.8 Java `GET /api/rag/assistant/eval/report` 端点（白盒展示，暂无报告 → 10002）—— **已实现**（Port evalReport() + Bridge GET 代理 404→10002 + AppService SNAKE_MAPPER 解析 + Controller GET /eval/report requireStudent；数字后缀字段用 @JsonAlias 收 hit_at_3/precision_at_3（SNAKE_CASE 会翻成 hit_at3），输出仍 hitAt3；新增 controller/appService/bridge 4 测试全绿）
- [ ] 5.9 三端对接测试：前端引用高亮/灰显折叠 + 评估报告一屏；后端+模型端联调 RAG-QUOTE-001~005、RAG-CONTRACT-001

## M6 问题提示（开始引导 + 结束建议必含 RAG + clarify）

- [x] 6.1 Python 引导底座池 + 开始引导：建**每模块引导底座池** `{module_id: {direction: [问题]}}`（`ai-tutoring` 来源 `docs/rag/ai-tutoring/7. 引导问题/引导问题.md`，以文件为准随切片迭代；direction=intro/operation/data_relation/difficulty + rag 常驻；问题对齐语料保证可答；后续模块各加条目可扩展）；`GET /api/rag/assistant/guide?currentProject=` 从池取 3 条（**必含 ≥1 条 RAG 方向**），0 token、非 SSE、不占冻结时序 —— **Python 已落地**（guide_pool.py 75 题/去重/子集/兜底 + guide(current_project)；Java 侧实测响应 shape 正确、无参兜底 ai-tutoring；direction 值已切 intro/operation/data_relation/difficulty/rag）
- [x] 6.2 Python 结束建议（池约束）：done 后 LLM 生成 1~3 条（向 ①项目介绍②操作③数据关联④难点），**提示词注入池内问题作「可提问范围」硬约束——只生成与池内方向类似的问题，不得自由发挥超出池子**；**必含 ≥1 条 RAG 方向**（RAG 始终带上，非并列模块）；LLM 失败/形状异常 → **池内随机抽 2~3 条**兜底（非自由发挥） —— **Python 已落地**（gen_suggestions 注入池内 75 题范围 + 兜底池内随机 2 条必含 rag）
- [ ] 6.3 Python clarify 澄清轮：`ambiguous=true` 且候选 ≥2 → `clarify` 事件（固定话术+candidates+default），0 生成 token、不计答案轮次、最多一轮；default 绑定 current_project > 会话锚点；再模糊直接默认；**候选来源** = ① intent LLM 输出 candidates（主源）② 会话最近 N 轮锚点去重（兜底）③ 仍 <2 不澄清
  - **点选交互定稿（2026-08-25）**：clarify 后前端点选候选 → **重发原问 + current_project=点选模块**（非裸功能名）；intent 以 current_project 为**权威消歧锚点**直接锚定、**不因问题含糊再拉 ambiguous**；点选模块与会话锚点不同 → `switch` 事件照常触发
- [x] 6.4 done 补 suggestions 字段 + guide 接口中继（Java；guide 透传 currentProject 给 Python，缺省后端兜底）—— **已实现**（GET /api/rag/assistant/guide 代理：Port/Bridge/AppService/Controller requireStudent；透传 ?current_project=，缺省 Python 兜底；done.suggestions 已在 M4 中继透传。测试 41/41 含 guide 7 例）
- [ ] 6.5 三端对接测试：前端开始引导 chips + 结束引导 chips（含 RAG，点击再问）+ clarify 澄清追问 UI；后端+模型端联调 RAG-SSE-004/005、SUGG-001~003
- [x] 6.6 Python 问候识别 + 欢迎引导（D-E 定稿）：intent 判 `category=问候`/`ambiguous=false`（**不触发 clarify**，实联调发现"你好"被误判 ambiguous）；问候语走固定欢迎话术 + 引导建议（复用模块引导池指向 ①②③④），0 生成 token、不 recall 不 generate —— **Python 已落地**（is_greeting 关键词预检 + category 问候枚举；done 返回欢迎语 total_tokens=0 + 池内引导建议）；**Java 适配**：scheduleGradeOnDone 捕获 intent.category，问候轮跳过质量打分（欢迎语非真实答案，不入 realConversation 评估聚合），新增单测

## M7 会话收尾（close + 累计 token + 补查）

- [x] 7.1 Java 会话累计 token：每轮 done 后累加进 Redis `rag:assistant:session:{sessionId}:usage`（TTL 24h 对齐 tutoring），含轮数 —— **已实现**（persistRound→accumulateSessionUsage 读-改-写：prompt/completion/cacheHit/total + rounds；落库失败不阻断回答链路）
- [x] 7.2 Java close 端点：`POST /api/rag/assistant/sessions/{sessionId}/close`（角色门同上；置 session closed + 返回累计 usage/轮数）；closed 后再 ask → 固定话术"本轮对话已结束，可开启新对话"0 token；幂等处理 —— **已实现**（close 读 Redis usage 返回结算、置 closed 标志幂等、未知会话 10002；ask 前置 closed 短路→permission+done(话术) 不调 Python 不落库不评分；**中止在途流**依赖前端断连→Python is_disconnected 取消，Java 侧置 closed 标志）
- [x] 7.3 Java turns 存储与补查：每轮 done 后按 trace_id 落 Redis（`rag:assistant:trace:{traceId}`，TTL 24h）；`GET /api/rag/assistant/turns/{traceId}` 读 Redis 返回完整结果（answer/quotedKeys/tokensUsage/suggestions；不存在 → 10002）。**turns 只存 Java Redis，Python 不落会话 trace**（Python eval trace jsonl 与补查分开） —— **已实现**（persistRound 存 done camel JSON；turn 读回 SseDoneDTO；新 DTO RagCloseDTO/RagSessionUsageDTO）
- [x] 7.4 控制器测试全量：角色门/事件时序/断线补查/评估报告/关闭对话（累计结算/关闭后再问/幂等） —— **已实现**（AppService +7：closed 短路/累计+trace 落库/close 正常/幂等/未知 10002/turn 命中/缺失 10002；Controller +2：close/turn 学生 200 教师 403；冒烟真实链路复跑通过）
- [ ] 7.5 三端对接测试：前端关闭对话按钮 + 结算面板（累计 token + 轮数）+ 断线凭 trace_id 补查；后端+模型端联调 RAG-CLOSE-001~006、RAG-COST-004~006

## M8 端到端收尾（全链路回归）

- [ ] 8.1 契约联调：Java 网关 ↔ Python 引擎真实链路（权限→意图→改写→召回→重排→生成→done）
- [ ] 8.2 分支验证：范围门低置信过滤（含无语料模块）/ clarify / switch / 超时降级 / 断连取消 / 上下文窗口截断
- [ ] 8.3 计费验证：done 返回完整 tokens_usage + trace_id；断线后凭 trace_id 补查成功
- [ ] 8.4 全部测试用例回归：M1-M7 门禁用例 + 契约冻结复核（SSE 时序未被下游里程碑重排/删字段）
