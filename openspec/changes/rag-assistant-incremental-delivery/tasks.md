# tasks

> 交付编排：M1→M7 逐个切片完成 + 前后端对接测试。每个里程碑完成后必须跑通其对接测试（见 test.md 对应分组）才能进入下一个。功能需求/事件契约/错误码语义查 `rag-project-intro-assistant` 变更（本变更只编排顺序与验收）。Python 契约任务在 Model 仓库对应变更实现，此处按里程碑拆分归属。

## M1 权限判断（角色门）

- [ ] 1.1 新增 `RagAssistantController` 骨架：`POST /api/rag/assistant/ask` 端点占位 + SSE 通道建立
- [ ] 1.2 角色硬门：从 `HttpSession.getAttribute("role")` 取角色，STUDENT 放行；非学生/缺失 → 固定 403 响应体；忽略 body 传 role；不调 LLM、不产生 trace
- [ ] 1.3 前端对接：非学生访问助手入口 → 固定 403 提示页；学生放行进入占位流程
- [ ] 1.4 对接测试：RAG-GATE-001~004 全绿（学生放行/教师 403/缺失 403/body role 忽略）

## M2 白盒骨架 + 意图分析 + Query 改写

- [ ] 2.1 SSE 事件契约冻结：时序 `permission → intent → (clarify|switch) → rewrite → rerank → (boundary) → token* → done`；事件 DTO（SsePermissionDTO/SseIntentDTO/SseRewriteDTO/SseRerankDTO/SseTokenDTO/SseDoneDTO，camelCase）
- [ ] 2.2 trace_id 生成（每轮 UUID）并透传 Python 贯穿日志
- [ ] 2.3 Python：intent LLM 结构化输出 `{anchor, category, switch_detected, ambiguous}`；失败回退 `_fallback_anchor`，degraded 走 200
- [ ] 2.4 Python：rewrite 事件透传 + generate 桩替（固定占位答案"（桩替）…"），整轮可通
- [ ] 2.5 Java 桥：`POST /api/rag/assistant/ask`（`x-internal-token`）SSE 消费并重建为 Java 事件；degraded 走 200 不 503
- [ ] 2.6 前端：阶段展示区（权限✓/意图分类标签/改写后问题）+ 桩替答案渲染
- [ ] 2.7 对接测试：RAG-SSE-001（桩）、RAG-CONTRACT-002/003、RAG-COST-003 全绿

## M3 多路召回 + remark 打分 + 边界拒答

- [ ] 3.1 Python：双路召回（向量 COS + BM25 本地 jsonl），单路 2s 硬超时 → 降级纯另一路（degraded 标记）
- [ ] 3.2 Python：RRF 融合（RRF_K=60）精排 Top-K（默认 3），精排块字段 blockId/title/summary/filePath/score
- [ ] 3.3 Python：范围门低置信过滤（索引层 0.75 / 源文档池 0.5）→ `boundary`（reason=low_confidence）固定话术，0 生成 token
- [ ] 3.4 Java：rerank/boundary 事件中继
- [ ] 3.5 前端：召回块面板（标题/摘要/file_path，点击查看原文）+ 边界拒答话术展示
- [ ] 3.6 对接测试：RAG-SSE-002/003、RAG-BRIDGE-001~003、RAG-COST-002 全绿

## M4 生成 + token 展示

- [ ] 4.1 Python：doubao 流式生成（`token*` 逐块）+ `include_usage` 取 usage；8s 硬超时 → 召回清单 + 固定话术（0 额外 token）；`is_disconnected()` 断连取消上游 doubao 流
- [ ] 4.2 Java：`token` 事件中继 + done 事件重建（answer/tokensUsage/traceId，不透传 Python 原始 meta/done）
- [ ] 4.3 cache_hit_tokens 估算：doubao 未返回 → tokenizer 估算并标注"估算"
- [ ] 4.4 移除 M2/M3 的 generate 桩替，接真实流式生成
- [ ] 4.5 前端：流式回答渲染 + 成本面板（prompt/completion/cache_hit/total 四字段）
- [ ] 4.6 对接测试：RAG-SSE-001（全量时序）、RAG-COST-001/007、RAG-ABORT-001 全绿

## M5 自我检查（is_quoted + 评估）

- [ ] 5.1 Python：is_quoted LCS 硬匹配纯函数（连续 8 中 / 12 英，窗口可调 `config/settings.py`），`done` 后补发 `quoted_keys`；全未命中 → answer 标注"引用未能精确匹配"
- [ ] 5.2 Python：评估集 VALID_TYPES 增加 `边界拒答` + `precision_at_k` 纯函数 + is_quoted 入评估（quoted_keys ⊆ 召回块，含"改写答案"用例）
- [ ] 5.3 Python：重跑 baseline 报告（hit@3/质量分/成本/耗时/版本）
- [ ] 5.4 Java：done 补 quotedKeys 字段 + `GET /api/rag/assistant/eval/report` 端点
- [ ] 5.5 前端：引用块高亮/未引用灰显折叠 + 评估报告一屏展示
- [ ] 5.6 对接测试：RAG-QUOTE-001~005、RAG-CONTRACT-001 全绿

## M6 问题提示（开始引导 + 结束建议 + clarify）

- [ ] 6.1 Python：结束建议（done 后）运行时 LLM 生成 1~3 条（向 ①项目介绍②操作③数据关联④难点），**必含 ≥1 条 RAG 方向**（RAG 始终带上，非并列模块）；失败 → 静态池兜底（对齐 Python 6 引导方向：定位/架构/数据流/防作弊/评测/坑）；completion_tokens 计入本轮 usage
- [ ] 6.2 Python：开始引导（会话入口）RAG 定向静态池（定位/架构/数据流/评测/坑）+ `GET /api/rag/assistant/guide` 接口，0 token、非 SSE、不占冻结时序
- [ ] 6.3 Python：clarify 澄清（`ambiguous=true` 且候选 ≥2 → 事件 + candidates + default，0 生成 token、不计答案轮次、最多一轮；default 绑定 `current_project` > 会话锚点；再模糊直接默认）
- [ ] 6.4 Java：clarify 事件中继 + done 补 suggestions 字段 + guide 接口中继
- [ ] 6.5 前端：开始引导 chips（定向 RAG，进入页面展示）+ 结束引导 chips（含 RAG，点击再问）+ clarify 澄清追问 UI（默认当前功能）
- [ ] 6.6 对接测试：RAG-SSE-004/005、SUGG-001~003 全绿

## M7 会话收尾（close + 累计 token + 补查）

- [ ] 7.1 Java：会话累计 token（每轮 done 后累加进 Redis `rag:assistant:session:{sessionId}:usage`，TTL 24h，含轮数计数）
- [ ] 7.2 Java：`POST /api/rag/assistant/sessions/{sessionId}/close`——中止在途流 + session 置 closed（Redis）+ 返回累计 usage/轮数；closed 后再 ask → 固定话术"本轮对话已结束，可开启新对话"0 token；close 幂等
- [ ] 7.3 Java：`GET /api/rag/assistant/turns/{traceId}` 补查端点（角色门同上，trace 不存在 → 10002）
- [ ] 7.4 前端：关闭对话按钮 + 结算面板（会话累计 token + 轮数）+ 断线重连凭 trace_id 补查
- [ ] 7.5 对接测试：RAG-CLOSE-001~006、RAG-COST-004~006 全绿

## 端到端收尾（全链路回归）

- [ ] 8.1 全量回归：M1-M7 对接测试全绿（36 条 RAG-* 用例）
- [ ] 8.2 契约冻结复核：SSE 时序未被下游里程碑重排/删字段，前端渲染层零返工
- [ ] 8.3 端到端冒烟：真实 Java↔Python 链路走通 权限→意图→改写→召回→重排→生成→done（含 clarify/switch/边界拒答/超时降级分支）
