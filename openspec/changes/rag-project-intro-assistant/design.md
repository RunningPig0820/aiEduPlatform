## Context

面向学生的**RAG 项目介绍助手**（`rag-project-intro-assistant`）：学生可在项目页面内就本项目设计逻辑提问（项目介绍 / 操作流程 / 数据关联 / 难点），回答遵循 RAG 标准链路并把中间状态白盒透传前端。这不是纯 RAG——产品是"讲清项目"，RAG 是证明能力的引擎。

现状约束与可复用资产：
- **Python 侧已有单模块 RAG 链路**：`ai-edu-ai-service/core/rag/query.py` 已实现 `classify(LLM→关键词兜底)` → `retrieve_vector(COS)` + `retrieve_bm25(本地jsonl)` → `orchestrate(RRF×authority×锚定)` → `generate(doubao)`，含 `references`（file/file_path/anchor/authority/summary）、usage 统计、降级链（向量挂→纯BM25；doubao挂→返回召回块；置信度低→拒答）。语料 `scripts/rag/data/rag_slices.jsonl`（234 块）为 **AI答疑模块唯一已切片入库数据**。
- **评估链已存在**：`run_eval.py`（CLI/API，`--compare` 版本对比）→ `eval_agent.py`（`hit_at_k`/`judge_quality`/`calc_cost`/`aggregate`）→ `eval_dataset.py`（格式校验，5 类型闭集，每模块 ≥5 条）。已有 baseline 报告：hit@3=0.80、质量分=4.2/5、耗时≈5.6s、成本≈¥0.016。
- **tutoring 两段式可复用**：Java 网关编排（安全预检→组装上下文→Python decide 非流式→护栏→generate 流式 SSE 透传）已验证；`SseMetaDTO`/`SseMasterySignalDTO` 事件 DTO、snake↔camel 契约纪律（`@JsonProperty`、`FAIL_ON_UNKNOWN_PROPERTIES=false`、degraded 走 200 不走 503）均为既有约定。
- **前端**：学生已有 AI答疑页（AiQa.jsx）与相关 hooks，RAG 助手前端另立变更，本设计只定后端契约。

定位说明：本变更与 Model 仓库 08-21 `project-intro-rag` 设计**方向不同**——后者是面试官 demo、覆盖 4 业务页、`role` 走 body；本变更是**学生**、仅讲 RAG 项目自身（AI答疑模块有语料）、角色走可信 session。**实现上泛化已有 `/api/tutoring/rag/query`，不照搬 08-21 的双池 QA 设计**（保留其"范围门=检索置信度"与"预写答案兜底"思想）。

## Goals / Non-Goals

**Goals:**
- 白盒 RAG 链路：权限 → 意图 → 改写 → 多路召回 → RRF 重排 → 生成，全阶段 SSE 事件透传前端。
- 角色硬门：仅 STUDENT 放行，非学生/角色缺失 → 固定 403，不进 RAG 流程、0 token。
- 模块全放行 + 低置信度过滤：AI答疑/知识图谱/题型分析/RAG 四模块均可路由，无禁区硬拒答；查不到关联文档 → 范围门低置信度过滤（固定话术，付 recall 省 generate），唯一拒答路径为 `boundary`（reason=low_confidence）。
- clarify 澄清轮：歧义（多候选功能）→ 固定澄清话术 + 默认当前功能，最多一轮，不计答案轮次。
- 引用透明：仅回传 RRF 精排 Top-K 块（标题/摘要/file_path），`is_quoted` 用 LCS 硬匹配（8 中/12 英），非 LLM 自述，`done` 后补发。
- 健壮性：召回 2s / 生成 8s 分层超时，`is_disconnected()` 断连取消，超时降级话术写死（0 token）。
- 计费透明：tokens_usage `{prompt, completion, cache_hit, total}` + `trace_id`，供前端断线补查；会话累计 token（关闭对话时返回）。
- 显式关闭对话：学生可在对话中主动结束会话（中止在途流 + 会话置关闭 + 返回会话累计 token），区别于断连取消。
- 引导：完成后运行时 LLM 生成建议（1~3 条，向 ①项目介绍 ②操作 ③数据关联 ④难点 引导）。
- 评估复用：`run_eval.py` 链 + 新增 `边界拒答` 类型 + `precision_at_k` + is_quoted 校验 + baseline 报告白盒展示。

**Non-Goals:**
- **不做生成中切换**：切换只发生在下一轮 intent（`switch_detected`），生成中前端断开只走 `is_disconnected()` 取消，不做服务端主动掐流（半截 token 白烧 + 上游取消不可靠）。
- **不做教育内容检索**（知识点/题库/答疑学科题）——语料是本项目方案文档，不是教育数据。
- **不做图谱检索召回**（不接 Neo4j）——召回对象是文档（向量+BM25）。
- **不接真实权限体系扩展**——本期仅学生角色，非学生固定 403。
- **不实现 mermaid 动态生成**——本期不做流程图预置/渲染（前端另立变更，可后续补）。
- **不实现前端**——仅定后端契约与 SSE 事件格式。
- **不做生产级部署与鉴权扩展**——沿用 `x-internal-token` 内部调用。

## Decisions

### D1. 角色门在 Java（可信 session），不在 Python，禁信 body
学生登录后 session 含 `userId`+角色；Java 网关从 `HttpSession.getAttribute("role")`（或网关 Header）取角色，`STUDENT` 才放行，否则固定 403 响应体（非 RAG 流程、不调 LLM、不落任何 trace）。前端任何 body 传 role 一律忽略。
- **为什么**：与 tutoring 认证桥接（方案 A）一致——前端走 Java 网关，Python 不自己认证、不碰会话；严禁信任前端传参（spec 硬性要求）。
- **备选**：Python 自校验 → 破坏"Python 无状态"边界，弃。

### D2. 意图识别用 LLM 结构化输出 + 规则兜底，输出 `{anchor, category, switch_detected, ambiguous}`
intent 为每轮开头的**非流式**调用（快模型、0 温度、关思考），输出闭集元数据。失败/超时/非闭集 → 回退关键词锚定（复用 `_fallback_anchor` + `ANCHOR_RULES`），degraded 标记走 200。
- **为什么**：白盒展示"语义分析"必须真实发生；LLM 判意图类别（复用 `_CLASSIFY_SYSTEM` 的闭集分类）+ 关键词兜底 = 语义与成本平衡。接口返回结构固定（`{locked_sections, strategy}` → 扩展为 `{anchor, category, switch, ambiguous}`），检索/生成只消费结果。
- **备选**：纯规则 → 零成本但"语义分析"是假的，白盒露怯；纯 LLM 无兜底 → 挂了链路全断。

### D3. 切换判定收敛在下一轮 intent，服务端不做生成中切换
`switch_detected = (前端 current_project ≠ 会话已锚定 project) 或 (问题明确指向另一有语料模块)`。检测到 → 发 `switch` 事件 + 重置上下文（锚点/召回/轮次计数），走新锚点 rewrite→recall→generate。**不掐断任何在途流**——在途流要么完成、要么被 is_disconnected 取消。
- **为什么**：生成中切换 = ①中止上游 doubao HTTP 流（不可靠）②半截 token 已计费 ③前端打断渲染，三重代价，且学生真实动作只有"等完再问"或"关 fetch 再问"。tutoring 换题判定收敛 Java 的教训（换题判定在 Python decide、Java 只认 switch 事件）同构。
- **备选**：生成中服务端掐流 → 复杂 + 烧钱 + 打断感，弃（用户确认）。

### D4. 模块全放行 + 范围门低置信度过滤（唯一拒答路径）
- **放行**：AI答疑/知识图谱/题型分析/RAG 四模块**全部可路由**，意图层**无禁区硬拒答**（用户确认："AI答疑、知识图谱、题型分析和RAG模块都放行，当查询不到关联文档就直接返回可信度低过滤"）。
- **范围门**（recall 后，唯一拒答机制）：RRF 精排 top-K 综合分低于阈值（索引层 0.75 / 源文档池 0.5，沿用）→ 固定话术"未找到关联文档，我尚未掌握"，事件 `event: boundary, reason: low_confidence`，付了 recall 省 generate。
- **硬路由**：涉及"系统架构/代码实现/部署流程/评测方案/接口设计" → 强制路由至 RAG 项目知识库。
- **为什么**：语料即边界，查不到=低置信过滤，无需维护禁区模块列表；未来模块入库切片即自动可答（数据驱动，无代码改动）。话术写死 0 token。
- **备选**：意图门硬拒答禁区 → 需维护禁区列表、与语料现状耦合，弃。

### D5. clarify 澄清轮：歧义才问，默认当前功能，最多一轮
`ambiguous=true` 且 `candidates ≥ 2`（多候选功能）→ 发 `event: clarify`（固定话术模板 + candidates + default），**0 token 生成、不计答案轮次、写 history**。学生下一条重跑 intent；仍模糊（"就那个嘛"）→ 不再 clarify，直接默认当前功能继续。`default` 绑定源优先级：前端 `current_project` > 会话最后成功锚定功能。
- **为什么**：低摩擦引导（单一候选直接走不问），防死循环（最多一轮），降本（写死话术）。spec 第 6 条"题型引导"的歧义场景正是"切换功能后问'这个功能怎么流转'"。
- **备选**：不问直接默认 → 答错功能体验更差；无限追问 → 死循环。

### D6. is_quoted 用 LCS 硬匹配，`done` 后补发，非 LLM 自述
生成完成后，遍历每个精排块的 `text`/`summary`，与最终 answer 做最长公共子串匹配，任意**连续 8 中文字符（或 12 英文字符）**命中 → `is_quoted=true`。前端 `rerank` 先发块（灰显），`done` 补 `quoted_keys`（高亮）。
- **为什么**：引用判定不依赖 LLM 主观自述（spec 硬性要求确定性），纯函数可单测可入评估。8 中文字符窗口对单 token chunk 不友好 → 生成完才匹配，故 `done` 后置补发。
- **风险（Python 侧校准）**：doubao 生成可能改写用词（如"类型先行流式"→"type先行"），导致 8 字符窗口**漏判**。→ 窗口大小可调（`config/settings.py`）；评估集加"改写答案"用例验证窗口够不够；漏判时块灰显但答案仍完整（非致命，前端无需报错）。
- **备选**：LLM 自报引用 → 不可靠；流中实时匹配 → chunk 粒度导致匹配窗口撕裂。

### D7. 分层超时 + 断连取消，降级话术写死
- 召回层：向量/Bm25 单路各 2s 硬超时，超时 → 降级为纯另一路（`{hits:[], confidence:0}` 冒泡捕获，复用 1.6C 语义）。
- 生成层：8s 硬超时 → **不走 LLM**，直接返回召回清单 + 固定话术"我找到了以下相关资料，但生成完整答案超时了，您可以直接点击查看原文：块1、块2、块3"。
- 断连：SSE 生成循环监听 `request.is_disconnected()`，断开 → 中止上游 doubao 流。
- **为什么**：分层超时是工程底线（spec 第 7 条）；超时降级话术写死成本 0，且用户拿到原始资料体验正向（非报错）。
- **备选**：统一 20s 超时 → 学生等待过久；生成超时也调 LLM 重试 → 重复花钱。

### D8. tokens_usage + trace_id
`done` 事件携带 `tokens_usage{prompt_tokens, completion_tokens, cache_hit_tokens, total_tokens}` + `trace_id`。usage 取流结束 ark 返回（`include_usage`）；cache_hit 取不到 → tokenizer 估算标注"估算"。`trace_id` 由 Java 生成透传 Python（同源贯穿日志），供前端 `GET /api/rag/assistant/turns/{trace_id}` 断线补查。
- **为什么**：spec 第 8 条透明计费；tutoring 已改 ark_stream 取 usage，复用。cache_hit 是 doubao prompt 缓存命中计数，用于成本叙事。
- **备选**：不补查接口 → trace_id 是死口（spec 要求"供断线后补查"）。

### D9. 模块可用性数据驱动：四模块放行，无语料自然低置信过滤
知识库按模块组织；`rag_slices.jsonl`（AI答疑）现状。其它模块语料不存在时，提问**正常进入召回**但命中为空/低置信 → 范围门低置信度过滤（固定话术），不是意图层拒答。未来某模块入库切片 → 自动可答（**无需改代码**）。
- **为什么**：用户确认"四个模块都放行，查不到关联文档直接返回可信度低过滤；语料后面会补充"。语料即边界，无禁区列表。
- **备选**：硬编码 4 模块白名单 / 意图门拒答 → 与语料现状耦合，弃。

### D10. 评估复用 run_eval 链 + 三处扩展
- `eval_dataset.py` `VALID_TYPES` 增加 `边界拒答`；`expected` 断言 = "必须触发固定话术且不产生 token 流"。
- `eval_agent.py` 新增 `precision_at_k`（召回 top-k 中相关块占比，纯函数）；`judge_quality` prompt 升级原子声明模式（RAGAS Faithfulness 思想）可选。
- 新增 is_quoted 纯函数 `lcs_quote_match` 单测 + 入评估（`quoted_keys ⊆ 召回块`）。
- baseline 报告经 `GET /api/rag/assistant/eval/report` 白盒展示（hit@k/质量分/成本/耗时）。
- **为什么**："证明有效"不能靠感觉（用户明确要求可量化/可复现/可追溯）；现有链完整可复用，只需扩展。
- **备选**：重新造评估轮 → 重复建设。

### D11. 问题提示 = 开始引导 + 结束引导，RAG 始终带上
**RAG 的特殊性**：AI答疑/知识图谱/题型分析是"展示页模块"（学生能导航到），RAG **不是展示页——它是始终在底层运行的引擎**，每轮答案都由它产出。所以问题提示不能把 RAG 当四个并列模块之一，**每次必须带上**（用户确认："每次问题提示的时候都需要把 RAG 带上"）。
- **开始引导**（会话入口，未提问前）：静态池定向 RAG（定位/架构/数据流/评测/坑），0 token；走非 SSE 接口 `GET /api/rag/assistant/guide`，前端进入页面拉取一次（**不占冻结的 SSE 时序**）。会话开始无上下文，LLM 无从生成，静态池即最优。
- **结束引导**（每轮 done 后）：运行时 LLM 生成 1~3 条建议（向 ①项目介绍 ②操作 ③数据关联 ④难点），**必含 ≥1 条 RAG 方向**（无论学生问哪个模块——AI答疑/知识图谱/题型分析——答案都是 RAG 引擎产出的，把话题带回 RAG）；`completion_tokens` 计入本轮 `tokens_usage`。
- **静态池兜底**：LLM 失败 → 静态池预写 2~3 条，对齐 Python 6 引导方向（定位/架构/数据流/防作弊/评测/坑），RAG 方向常驻、兜底不跑偏。
- **为什么**：用户明确"运行时 LLM 生成"（可针对当前回答上下文）+ "每次问题提示都要把 RAG 带上"（RAG 是始终在用的引擎，非展示页，学生问完任何模块都应被带回 RAG）。
- **备选**：suggestions 把 RAG 当并列模块随机抽 → 学生问完 AI答疑 可能一条 RAG 都没有，白盒教学目的落空，弃。

### D12. 显式关闭对话（close）+ 会话累计 token
学生可在对话中主动"结束对话"：`POST /api/rag/assistant/sessions/{sessionId}/close`（角色门同上，仅 STUDENT）。close 语义：
- **中止在途流**：若该 session 当前有生成流，中止上游 doubao（同 is_disconnected 取消），前端可关连接。
- **结束会话**：session 状态置 closed（Redis），后续同 session_id 的 ask → 固定话术"本轮对话已结束，可开启新对话"，不进入 RAG 流程、0 token。
- **返回会话累计 token**：Java 每轮 `done` 后将 `tokens_usage` 累加进 Redis（`rag:assistant:session:{sessionId}:usage`，TTL 24h 对齐 tutoring）；close 时读回返回 `{prompt/completion/cache_hit/total}` 会话累计值 + 轮数。**这补上 spec 第 4 条"对话消耗总 token"的缺口**（原来只有每轮）。
- **为什么**：显式 close 与断连取消是两件事——断连是异常路径（仅中止流），close 是学生主动结束（结束会话 + 结算）。累计 token 放 Java（每轮都经过它，天然聚合点），Python 保持无状态。
- **备选**：close 仅前端清空 UI 不发后端 → 无法结算累计 token、session 状态残留；累计 token 放 Python → 破坏无状态边界。

## Risks / Trade-offs

- [intent LLM 偶发误判] → 规则兜底（`_fallback_anchor`）+ degraded 标记走 200；评估集 `边界拒答` 类型覆盖误判回归。
- [is_quoted 匹配 8 中字符过于严格/宽松] → 参数可调（`config/settings.py`）；入评估校验 quoted_keys ⊆ 召回块；前端灰显/高亮兜底。
- [cache_hit_tokens 拿不到] → tokenizer 估算 + 标注"估算"（08-21 已留口子）。
- [运行时 suggestions 增加成本] → 计入本轮 usage 展示；LLM 失败静态池兜底；可配置开关关闭。
- [多模块语料缺失导致可答面窄] → 数据驱动，先 AI答疑；未来入库即自动放行，验收按"链路真实完整"讲。
- [跨项目问题（AI答疑页问知识图谱）在无语料模块下低置信过滤] → 明确为预期行为（范围门 low_confidence），评估集覆盖。
- [上下文窗口截断丢上下文] → 保留最近 3 轮 + 锚点由 session 独立携带，前端可见截断提示（如需）。
- [SSE 事件时序被前端依赖] → 冻结契约：`permission → intent → (clarify|switch) → rewrite → rerank → token → done`，不得重排/丢失（沿用 tutoring 阶段二契约冻结纪律）。
- [流式 usage 只在结尾返回] → done 才更新成本展示（面试/汇报可讲这个坑）。

## Migration Plan

1. **Python 先行**（Model 仓库，对应其 `rag-project-intro-assistant-python` 变更）：泛化 `core/rag/query.py` 为白盒链路（intent/rewrite/recall/rerank/generate + clarify/is_quoted/分层超时/suggestions），新增 `/api/rag/assistant/ask` SSE 端点，扩评估集。**不影响既有 `/api/tutoring/rag/query`**（独立路由）。
2. **Java 网关**：新增 `RagAssistantController`（角色门 + SSE 中继 + trace_id），复用 `LlmGateway` internalToken 调用。回滚 = 摘除路由，不影响 tutoring。
3. **前端**（另立变更）：学生侧 RAG 助手页消费白盒事件。
4. **数据**：AI答疑语料保持现状；其它模块语料后续入库即自动放行，无迁移。

## Open Questions

- intent LLM 类别闭集与 `locked_sections` 的映射是否沿用现有 `CATEGORY_SECTIONS`（项目介绍/操作/难点/数据关联/最危险），还是针对学生场景重构（spec 提到 ①②③④ 四方向）——建议沿用闭集，前端引导语对应即可。
- `cache_hit_tokens` 是否真由 doubao/ark 返回——需实现期实测，取不到按"估算"。
- 会话：**不做断线恢复**（仅 trace_id 单轮补查，用户确认）；**不设轮数上限**（用户确认轮数无意义），改为上下文窗口保留**最近 3 轮**（默认，可配）。窗口大小的最终值待定。
