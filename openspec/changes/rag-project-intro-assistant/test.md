# RAG 项目介绍助手 测试用例设计

## 1. 测试概述

### 1.1 测试目标

验证 `RagAssistantController` 的所有业务场景：角色硬门、SSE 白盒事件时序、模块全放行 + 范围门低置信度过滤、clarify 澄清、switch 切换、上下文窗口截断、超时降级、断线取消、tokens_usage 计费、trace_id 断线补查。确保白盒链路正确性与健壮性。

### 1.2 测试方式

- **集成测试**：直接注入 Controller，调用真实方法（Python 引擎用 mock 桥，模拟 SSE 事件流）。
- **桥测试**：`RagAssistantBridgeImpl` 用 MockWebServer / 桩响应验证 SSE 消费与事件重建。
- **纯函数单测**：is_quoted LCS 匹配、契约字段 snake↔camel 映射（Java 侧）。
- **数据库回滚**：使用 `@Transactional`，测试完成后自动回滚。
- **无 Mock（端到端）**：真实 Java↔Python 联调单独跑（tasks 5.1），不纳入常规单元测试。

### 1.3 测试环境配置

- Profile: `test`
- 数据库：使用开发数据库，事务自动回滚
- Session：使用 `MockHttpSession` 模拟（setAttribute userId / role）
- Python 引擎：`@MockBean` 的 `RagAssistantPort`，按用例返回预设 SSE 事件序列

---

## 2. 测试数据

| 参数 | 值 | 说明 |
|-----|-----|------|
| STUDENT_ID | 1001 | 学生用户 id |
| TEACHER_ID | 2001 | 教师用户 id |
| STUDENT_ROLE | STUDENT | 学生角色 |
| TEACHER_ROLE | TEACHER | 教师角色 |
| TRACE_ID | trc-abc123 | 测试 trace_id |
| SESSION_ID | sess-001 | 测试会话 id |
| CURRENT_PROJECT | ai-tutoring | 页面锚定模块 |
| QUESTION_OK | 这个项目的整体架构是什么？ | 正常可答问题 |
| QUESTION_FORBIDDEN | 知识图谱模块怎么用？ | 指向无语料模块（四模块放行，低置信过滤） |
| QUESTION_AMBIGUOUS | 这个功能的流转是什么样的？ | 歧义问题 |
| BLOCK_ID_1 | block-01 | 精排块 id |

---

## 2A. 里程碑门禁映射（交付编排并入）

> 每个里程碑完成 = 该里程碑门禁用例全绿 + 前端可见物验收，才进入下一里程碑。SSE 契约在 M2 冻结，下游只补字段不重排。

| 里程碑 | 门禁用例（全绿才过） | 前端可见物验收 |
|--------|---------------------|---------------|
| M1 权限判断 | RAG-GATE-001~004 | 403 页 / 学生放行 |
| M2 意图+改写+骨架 | RAG-SSE-001（桩）、RAG-CONTRACT-002~004、RAG-COST-003 | 阶段展示区 + 桩替答案 |
| M3 召回+remark+边界 | RAG-SSE-002/003、RAG-BRIDGE-001~005、RAG-COST-002 | 召回块面板（含查看原文代理）+ 边界话术 |
| M4 生成+token | RAG-SSE-001（全量）、RAG-COST-001/007、RAG-ABORT-001 | 流式回答 + 成本面板 |
| M5 自我检查 | RAG-QUOTE-001~005、RAG-CONTRACT-001 | 引用高亮 + 评估报告 |
| M6 问题提示 | RAG-SSE-004/005/008/009、SUGG-001~003 | 开始/结束引导 chips + 澄清 UI + 问候欢迎 |
| M7 会话收尾 | RAG-CLOSE-001~006、RAG-COST-004~006 | 结算面板 + 断线补查 |
| M8 端到端回归 | 全量用例 + 契约冻结复核 | 全链路真实事件渲染 |

---

## 3. 测试用例清单

### 3.1 角色硬门（RAG-GATE）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-GATE-001 | 学生放行 | session 角色=STUDENT | 正常问答请求 | 进入 RAG 流程，SSE 首个事件为 `permission{allowed:true}` |
| RAG-GATE-002 | 非学生拒绝 | session 角色=TEACHER | 正常问答请求 | 返回固定 403"仅学生可访问此助手"，不产生 trace、不调 LLM |
| RAG-GATE-003 | 角色缺失 | 无有效 session | 正常问答请求 | 返回固定 403，不进入 RAG 流程 |
| RAG-GATE-004 | body 传 role 被忽略 | session 角色=TEACHER，body 带 role=STUDENT | 问答请求 | 仍按 session 角色拒绝（403），证明不信任前端传参 |

### 3.2 SSE 白盒事件时序（RAG-SSE）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-SSE-001 | 正常问答时序 | 学生已登录，桥 mock 正常流 | QUESTION_OK | 事件顺序 permission → intent → rewrite → rerank → token* → done，无重排丢失 |
| RAG-SSE-002 | 无语料模块低置信过滤时序 | 桥 mock boundary 流 | QUESTION_FORBIDDEN（指向知识图谱模块） | 四模块放行 → intent 路由后正常召回，命中空 → rerank 后 boundary（reason=low_confidence），无 token，随后 done |
| RAG-SSE-003 | 语料未覆盖边界时序 | 桥 mock boundary 流 | 语料未覆盖问题 | rerank（可为空）后 boundary（reason=low_confidence），无 token，随后 done |
| RAG-SSE-004 | clarify 时序 | 桥 mock clarify 流 | QUESTION_AMBIGUOUS | intent 后 clarify（message/candidates/default），无 recall/generate，随后 done |
| RAG-SSE-005 | switch 时序 | 桥 mock switch 流 | 切换功能问题 | switch 事件（from/to）后按新锚点 continue rewrite→recall→token→done |
| RAG-SSE-006 | meta/done 不透传原始 | 桥 mock 含 Python meta/done 事件 | 正常问答 | Java 重建为自定义事件，Python 原始 meta/done 不暴露给前端 |
| RAG-SSE-007 | 上下文窗口截断 | 会话第 4 轮发起（历史 >3 轮） | 正常问答 | intent/generate 上下文仅含最近 3 轮，第 1 轮截断；锚点由 session 独立携带不受影响 |
| RAG-SSE-008 | clarify 点选后重发 | 上一轮 clarify 发出，学生点选 [RAG项目] | 重发原问题 + currentProject=rag-system（含 clarify 轮 history） | intent 以 currentProject 为权威锚点直接锚定 anchor=rag-system，不再 ambiguous；锚点与会话不同则 switch 后正常 rewrite/recall/generate |
| RAG-SSE-009 | 问候语欢迎引导 | 学生发"你好" | 问候语 | intent 判 category=问候/ambiguous=false，**不触发 clarify**；直接 done（固定欢迎话术 + 引导建议指向 ①②③④），0 生成 token、无 rewrite/recall/token 流 |

### 3.3 事件契约字段（RAG-CONTRACT）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-CONTRACT-001 | done 完整字段 | 正常流完成 | 桥 mock done | done 含 answer/quotedKeys/tokensUsage/traceId/suggestions |
| RAG-CONTRACT-002 | snake↔camel 映射 | Python 返回 snake_case | 桥 mock `switch_detected`/`quoted_keys` | Java DTO 映射为 camelCase，SSE 输出 `switchDetected`/`quotedKeys` |
| RAG-CONTRACT-003 | 未知字段容忍 | Python 返回未知字段 | 桥 mock 额外字段 | 不报错（FAIL_ON_UNKNOWN_PROPERTIES=false），不影响解析 |
| RAG-CONTRACT-004 | permission 携带 traceId | 学生放行 | 正常问答 | permission 首事件含 traceId（=Java 入口生成值），前端流开始即可取，断线补查不依赖 done |

### 3.4 is_quoted（纯函数，RAG-QUOTE）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-QUOTE-001 | 命中引用 | 无 | answer 含某块连续 8 中文字符 | is_quoted=true，进入 quotedKeys |
| RAG-QUOTE-002 | 未命中 | 无 | answer 与块无 ≥8 中文字符连续命中 | is_quoted=false |
| RAG-QUOTE-003 | 英文 12 字符边界 | 无 | answer 含某块连续 12 英文字符 | is_quoted=true；11 字符命中 = false |
| RAG-QUOTE-004 | 全部未命中 | 无 | 所有块均未命中 | quotedKeys 为空，answer 标注"引用未能精确匹配" |
| RAG-QUOTE-005 | 改写答案命中（评估校准） | 无 | answer 改写用词（"类型先行流式"→"type先行"） | 记录该块引用命中与否，供 8 字符窗口漏判率评估 |

### 3.5 tokens_usage 与 trace_id（RAG-COST）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-COST-001 | done 返回 usage | 正常流 | 桥 mock usage | tokensUsage 含 prompt/completion/cacheHit/total 四字段 |
| RAG-COST-002 | 低置信过滤零生成 usage | 范围门 | QUESTION_FORBIDDEN | 无 generate，tokensUsage 为 0 或仅含实际消耗（boundary 路径 recall 不计入/单列） |
| RAG-COST-003 | trace 贯穿 | 正常流 | 无 | done.traceId = 请求入口生成值，非空 |
| RAG-COST-004 | 断线补查成功 | 已有 trace 落 Redis | GET turns/{TRACE_ID} | 从 Redis 读回该轮完整结果（answer/quotedKeys/tokensUsage/suggestions）；Python 无状态不落会话 trace |
| RAG-COST-005 | 补查不存在 | 无该 trace | GET turns/unknown | 10002 trace 不存在 |
| RAG-COST-006 | 补查非学生 | session 角色=TEACHER | GET turns/{TRACE_ID} | 403 固定响应 |
| RAG-COST-007 | 会话累计累加 | 多轮 done 完成 | 桥 mock 多轮 usage | Redis 会话累计 = 各轮之和（prompt/completion/cache_hit/total） |

### 3.6 关闭对话（RAG-CLOSE）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-CLOSE-001 | 关闭返回累计 token | 会话有 3 轮 | POST close/{SESSION_ID} | 返回 closed=true、rounds=3、sessionUsage 四字段累计值 |
| RAG-CLOSE-002 | 关闭后再问 | 会话已 closed | POST ask（同 session） | 固定话术"本轮对话已结束，可开启新对话"，tokensUsage=0 |
| RAG-CLOSE-003 | 关闭幂等 | 已 closed | POST close 同 session | 仍返回 closed=true + 当前累计值，不报错 |
| RAG-CLOSE-004 | 关闭不存在会话 | 无该 session | POST close/unknown | 10002 会话不存在 |
| RAG-CLOSE-005 | 关闭非学生 | session 角色=TEACHER | POST close/{SESSION_ID} | 403 固定响应 |
| RAG-CLOSE-006 | 关闭中止在途流 | 该 session 有在途生成流 | POST close | 中止上游 doubao 流，前端可关连接 |

### 3.7 桥实现（RAG-BRIDGE）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-BRIDGE-001 | 正常消费 SSE | MockWebServer 返回预设 SSE | 正常请求 | 事件按序重建，顺序稳定；从 intent 开始转发，不消费 Python 侧 permission |
| RAG-BRIDGE-002 | Python 异常冒泡 | MockWebServer 返回 500 | 问答请求 | 抛 TutoringAgentException（或等价异常），网关降级处理 |
| RAG-BRIDGE-003 | degraded 200 | Python 返回 200+degraded | 问答请求 | 按普通结果处理，不视为错误（不 503） |
| RAG-BRIDGE-004 | history/trace 组装透传 | 桥带最近 N 轮 + trace_id | 正常请求 | 请求含 history（最近 3 轮含 clarify 轮）+ trace_id（Java 生成）；done 回显 trace_id 与请求一致 |
| RAG-BRIDGE-005 | 查看原文代理 | rerank 块 filePath 存在 | GET source?path=<encoded> | Java 转发 Python source 端点返回原文；file_path 走 query 传参；原文不存在 → 10002 |

### 3.8 断连取消（RAG-ABORT）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-ABORT-001 | 前端断开中止 | 流式请求中关闭连接 | 桥 mock 长流 | Java 检测客户端断开，中止转发（透传取消语义到 Python） |

### 3.9 问题提示（开始引导 + 结束建议，RAG 常驻）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| SUGG-001 | 开始引导定向 RAG | 学生进入助手页 | GET /guide | 返回 RAG 定向静态引导池（定位/架构/数据流/评测/坑），0 token、非 SSE、不占冻结时序 |
| SUGG-002 | 结束建议必含 RAG | 桥 mock done 带 suggestions | 学生问 AI答疑 完成一轮 | done.suggestions 1~3 条中至少 1 条指向 RAG 方向（RAG 始终带上，非并列模块） |
| SUGG-003 | suggestions 静态池兜底 | suggestions LLM 失败 | 正常问答 | 返回静态池预写建议（含 RAG 方向，对齐 Python 6 引导方向），链路不中断 |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功 |
| 10001 | INVALID_PARAMS | 参数无效（question 缺失/超长） |
| 10002 | NOT_FOUND | 实体不存在（trace 不存在 / 暂无评估报告） |
| 10004 | UNAUTHORIZED | 未登录 |
| 403 | ROLE_DENIED | 角色非 STUDENT（固定响应，非标准错误码） |

---

## 5. 测试用例统计

| 模块 | 用例数量 |
|-----|---------|
| 角色硬门 RAG-GATE | 4 |
| SSE 事件时序 RAG-SSE | 9 |
| 事件契约字段 RAG-CONTRACT | 4 |
| is_quoted 纯函数 RAG-QUOTE | 5 |
| tokens_usage/trace RAG-COST | 7 |
| 关闭对话 RAG-CLOSE | 6 |
| 桥实现 RAG-BRIDGE | 5 |
| 断连取消 RAG-ABORT | 1 |
| 问题提示 SUGG（开始/结束引导，RAG 常驻） | 3 |
| **总计** | **43** |

---

## 6. 测试执行顺序

测试按 `@Order` 注解指定的顺序执行：

```
100-103  : 角色硬门测试（RAG-GATE）
200-207  : SSE 时序测试（RAG-SSE）
300-303  : 契约字段测试（RAG-CONTRACT）
400-403  : is_quoted 纯函数测试（RAG-QUOTE）
500-506  : usage/trace 测试（RAG-COST）
550-555  : 关闭对话测试（RAG-CLOSE）
600-604  : 桥实现测试（RAG-BRIDGE）
700      : 断连取消测试（RAG-ABORT）
800-802  : 问题提示测试（SUGG）
```

---

## 7. 辅助方法

### 7.1 创建登录会话

```java
private HttpSession createLoginSession(Long userId, String role) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", userId);
    session.setAttribute("role", role);
    return session;
}
```

### 7.2 模拟 Python 桥返回 SSE 流

```java
@MockBean
private RagAssistantPort ragAssistantPort;

// 正常流：permission → intent → rewrite → rerank → token → done
when(ragAssistantPort.ask(any(), any())).thenAnswer(inv -> {
    emitter.send(permissionEvent());
    emitter.send(intentEvent());
    emitter.send(rewriteEvent());
    emitter.send(rerankEvent());
    emitter.send(tokenEvent());
    emitter.send(doneEvent());
    emitter.complete();
    return null;
});
```

### 7.3 构建 done 事件数据

```java
private SseDoneDTO buildDoneEvent() {
    SseDoneDTO dto = new SseDoneDTO();
    dto.setAnswer("……");
    dto.setQuotedKeys(List.of("block-01"));
    dto.setTokensUsage(RagTokensUsage.of(320, 140, 0, 460));
    dto.setTraceId(TRACE_ID);
    dto.setSuggestions(List.of("想了解RAG的整体架构吗？"));
    dto.setReason(null);
    return dto;
}
```

---

## 8. 运行测试

```bash
# 运行单个测试类（接口层）
cd ai-edu-backend && mvn test -pl ai-edu-interface -Dtest=RagAssistantControllerTest

# 运行单个测试方法
mvn test -pl ai-edu-interface -Dtest=RagAssistantControllerTest#gate_teacherRejected

# 运行桥测试
mvn test -pl ai-edu-infrastructure -Dtest=RagAssistantBridgeImplTest

# 运行 is_quoted 纯函数测试
mvn test -pl ai-edu-domain -Dtest=RagQuoteMatcherTest
```

## 9. Python 引擎测试参考（aiEduPlatformModel 仓库）

Python 侧测试遵循其现有 pytest 约定（`tests/` 目录），关键用例：

| 用例 | 场景 | 预期 |
|-----|------|------|
| intent 正常分类 | LLM 返回闭集 | 输出 `{anchor, category, switch_detected, ambiguous}` |
| intent LLM 失败兜底 | mock 抛异常 | 回退 `_fallback_anchor`，degraded=true，不阻断 |
| 四模块放行 | 问题指向任一模块 | 正常路由进入召回，无禁区拒答 |
| 无语料模块低置信过滤 | 问题指向无语料模块 | 正常召回但命中空 → 固定低置信话术，无生成 token |
| 语料未覆盖低置信过滤 | 综合分低于阈值 | 返回固定低置信话术，无生成 token |
| clarify 触发 | ambiguous + 多候选 | 发 clarify，无 recall/generate |
| clarify 一次后仍模糊 | 下一条仍模糊 | 不再澄清，默认当前功能继续 |
| recall 单路超时 | 向量路超时 | 降级纯 BM25，degraded 标记 |
| 生成超时降级 | generate >8s | 返回召回清单 + 固定话术 |
| is_quoted LCS | 生成后匹配 | 连续 8 中/12 英命中 → quoted_keys |
| 断连取消 | is_disconnected() 为真 | 中止上游 doubao 流 |
| 边界拒答评估 | 评估集类型=边界拒答 | 断言固定话术 + 0 token |
| precision_at_k | 召回 top-k | 相关块占比 0~1 |

运行方式：`cd ai-edu-ai-service && venv/bin/python -m pytest tests/`（Python 侧另行维护）。
