# RAG 项目介绍助手 测试用例设计（按里程碑交付）

## 1. 测试概述

### 1.1 测试目标

验证 RAG 助手按 **M1-M7 里程碑逐个切片交付**时的对接测试：每个里程碑完成 = 该里程碑的对接测试用例全绿 + 前端可见物验收。全程确保 RAG 功能不因切片缺失、SSE 契约不因下游里程碑破坏（M2 冻结时序不被重排/删字段）。

### 1.2 测试方式

- **里程碑门禁**：每个里程碑的测试用例全绿才进入下一里程碑（test.md 第 6 节执行顺序即构建顺序）。
- **集成测试**：注入 `RagAssistantController`，Python 引擎用 `@MockBean` 的 `RagAssistantPort` 模拟 SSE 事件流（按里程碑桩替/真实现）。
- **纯函数单测**：is_quoted LCS 匹配、契约 snake↔camel 映射（Java 侧）。
- **数据库回滚**：`@Transactional`，测试完成后自动回滚。
- **端到端**：真实 Java↔Python 联调单独跑（tasks 8.3），不纳入常规单元测试。
- 用例语义与既有 `rag-project-intro-assistant/test.md` 一致，本文件**按里程碑分组**并标注门禁（38 行：35 条 RAG-* + 3 条新增 SUGG 引导用例；RAG-SSE-001 / RAG-COST-007 在两个里程碑门禁复用）。

### 1.3 测试环境配置

- Profile: `test`
- 数据库：开发数据库，事务自动回滚
- Session：`MockHttpSession`（setAttribute userId / role）
- Python 引擎：`@MockBean` 的 `RagAssistantPort`，按里程碑返回桩替或真实事件流

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
| QUESTION_LOWCONF | 知识图谱模块怎么用？ | 无语料模块（低置信过滤） |
| QUESTION_AMBIGUOUS | 这个功能的流转是什么样的？ | 歧义问题 |
| BLOCK_ID_1 | block-01 | 精排块 id |

---

## 3. 里程碑门禁映射

| 里程碑 | 门禁用例（全绿才过） | 前端可见物验收 |
|--------|---------------------|---------------|
| M1 权限判断 | RAG-GATE-001~004 | 403 页 / 学生放行 |
| M2 意图+改写+骨架 | RAG-SSE-001（桩）、RAG-CONTRACT-002/003、RAG-COST-003 | 阶段展示区 + 桩替答案 |
| M3 召回+remark+边界 | RAG-SSE-002/003、RAG-BRIDGE-001~003、RAG-COST-002 | 召回块面板 + 边界话术 |
| M4 生成+token | RAG-SSE-001（全量）、RAG-COST-001/007、RAG-ABORT-001 | 流式回答 + 成本面板 |
| M5 自我检查 | RAG-QUOTE-001~005、RAG-CONTRACT-001 | 引用高亮 + 评估报告 |
| M6 问题提示 | RAG-SSE-004/005、SUGG-001~003 | 开始/结束引导 chips + 澄清 UI |
| M7 会话收尾 | RAG-CLOSE-001~006、RAG-COST-004~006 | 结算面板 + 断线补查 |

---

## 3A. M1 角色硬门（RAG-GATE）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-GATE-001 | 学生放行 | session 角色=STUDENT | 正常问答请求 | 进入 RAG 流程，SSE 首事件 `permission{allowed:true}` |
| RAG-GATE-002 | 非学生拒绝 | session 角色=TEACHER | 正常问答请求 | 固定 403"仅学生可访问此助手"，不产生 trace、不调 LLM |
| RAG-GATE-003 | 角色缺失 | 无有效 session | 正常问答请求 | 固定 403，不进 RAG 流程 |
| RAG-GATE-004 | body 传 role 被忽略 | session=TEACHER，body 带 role=STUDENT | 问答请求 | 仍按 session 角色拒绝（403），不信任前端传参 |

## 3B. M2 白盒骨架 + 意图 + 改写（RAG-SSE 桩 / RAG-CONTRACT / RAG-COST）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-SSE-001 | 正常时序（M2 桩替） | 桥 mock 桩替流 | QUESTION_OK | 顺序 permission→intent→rewrite→done，无重排丢失 |
| RAG-CONTRACT-002 | snake↔camel 映射 | Python 返回 snake_case | 桥 mock `switch_detected`/`quoted_keys` | Java DTO 映射 camelCase，SSE 输出 `switchDetected`/`quotedKeys` |
| RAG-CONTRACT-003 | 未知字段容忍 | Python 返回未知字段 | 桥 mock 额外字段 | 不报错（FAIL_ON_UNKNOWN_PROPERTIES=false），不影响解析 |
| RAG-COST-003 | trace 贯穿 | 正常流 | 无 | done.traceId = 请求入口生成值，非空 |

> M2 门禁后 SSE 契约冻结：后续里程碑只补字段不重排（回归断言见 8.2）。

## 3C. M3 多路召回 + remark 打分 + 边界拒答（RAG-SSE 边界 / RAG-BRIDGE / RAG-COST）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-SSE-002 | 无语料模块低置信过滤 | 桥 mock boundary 流 | QUESTION_LOWCONF | 四模块放行→正常召回→命中空→rerank 后 boundary(reason=low_confidence)，无 token，随后 done |
| RAG-SSE-003 | 语料未覆盖边界时序 | 桥 mock boundary 流 | 语料未覆盖问题 | rerank（可为空）后 boundary(reason=low_confidence)，无 token，随后 done |
| RAG-BRIDGE-001 | 正常消费 SSE | MockWebServer 返回预设 SSE | 正常请求 | 事件按序重建，顺序稳定 |
| RAG-BRIDGE-002 | Python 异常冒泡 | MockWebServer 返回 500 | 问答请求 | 抛 TutoringAgentException（或等价），网关降级处理 |
| RAG-BRIDGE-003 | degraded 200 | Python 返回 200+degraded | 问答请求 | 按普通结果处理，不 503 |
| RAG-COST-002 | 低置信过滤零生成 usage | 范围门 | QUESTION_LOWCONF | 无 generate，tokensUsage 为 0 或仅含实际消耗（boundary 路径 recall 不计入/单列） |

## 3D. M4 生成 + token 展示（RAG-SSE 全量 / RAG-COST / RAG-ABORT）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-SSE-001 | 正常时序（M4 全量） | 桥 mock 真流 | QUESTION_OK | 顺序 permission→intent→rewrite→rerank→token*→done，无重排丢失 |
| RAG-COST-001 | done 返回 usage | 正常流 | 桥 mock usage | tokensUsage 含 prompt/completion/cacheHit/total 四字段 |
| RAG-COST-007 | 会话累计累加 | 多轮 done 完成 | 桥 mock 多轮 usage | Redis 会话累计 = 各轮之和（M7 close 结算复用） |
| RAG-ABORT-001 | 前端断开中止 | 流式请求中关闭连接 | 桥 mock 长流 | Java 检测客户端断开，中止转发（透传取消语义到 Python） |

## 3E. M5 自我检查（RAG-QUOTE / RAG-CONTRACT done）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-QUOTE-001 | 命中引用 | 无 | answer 含某块连续 8 中文字符 | is_quoted=true，进入 quotedKeys |
| RAG-QUOTE-002 | 未命中 | 无 | answer 与块无 ≥8 中文字符连续命中 | is_quoted=false |
| RAG-QUOTE-003 | 英文 12 字符边界 | 无 | answer 含连续 12 英文字符 | is_quoted=true；11 字符命中=false |
| RAG-QUOTE-004 | 全部未命中 | 无 | 所有块均未命中 | quotedKeys 为空，answer 标注"引用未能精确匹配" |
| RAG-QUOTE-005 | 改写答案命中 | 无 | answer 改写用词（"类型先行流式"→"type先行"） | 记录引用命中与否，供 8 字符窗口漏判率评估 |
| RAG-CONTRACT-001 | done 完整字段 | 正常流完成 | 桥 mock done | done 含 answer/quotedKeys/tokensUsage/traceId/suggestions（suggestions M6 起） |

## 3F. M6 问题提示（RAG-SSE clarify/switch + SUGG 引导）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-SSE-004 | clarify 时序 | 桥 mock clarify 流 | QUESTION_AMBIGUOUS | intent 后 clarify(message/candidates/default)，无 recall/generate，随后 done |
| RAG-SSE-005 | switch 时序 | 桥 mock switch 流 | 切换功能问题 | switch(from/to) 后按新锚点 continue rewrite→recall→token→done |
| SUGG-001 | 开始引导定向 RAG | 学生进入助手页 | GET /guide | 返回 RAG 定向静态引导池（定位/架构/数据流/评测/坑），0 token、非 SSE、不占冻结时序 |
| SUGG-002 | 结束建议必含 RAG | 桥 mock done 带 suggestions | 学生问 AI答疑 完成一轮 | done.suggestions 1~3 条中至少 1 条指向 RAG 方向，前端渲染引导 chips |
| SUGG-003 | suggestions 静态池兜底 | suggestions LLM 失败 | 正常问答 | 返回静态池预写建议（含 RAG 方向，对齐 Python 6 引导方向），链路不中断 |

## 3G. M7 会话收尾（RAG-CLOSE / RAG-COST 补查）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RAG-CLOSE-001 | 关闭返回累计 token | 会话有 3 轮 | POST close/{SESSION_ID} | 返回 closed=true、rounds=3、sessionUsage 四字段累计值 |
| RAG-CLOSE-002 | 关闭后再问 | 会话已 closed | POST ask（同 session） | 固定话术"本轮对话已结束，可开启新对话"，tokensUsage=0 |
| RAG-CLOSE-003 | 关闭幂等 | 已 closed | POST close 同 session | 仍返回 closed=true + 当前累计值，不报错 |
| RAG-CLOSE-004 | 关闭不存在会话 | 无该 session | POST close/unknown | 10002 会话不存在 |
| RAG-CLOSE-005 | 关闭非学生 | session 角色=TEACHER | POST close/{SESSION_ID} | 403 固定响应 |
| RAG-CLOSE-006 | 关闭中止在途流 | 该 session 有在途生成流 | POST close | 中止上游 doubao 流，前端可关连接 |
| RAG-COST-004 | 断线补查成功 | 已有 trace | GET turns/{TRACE_ID} | 返回该轮完整结果（answer/quotedKeys/tokensUsage/suggestions） |
| RAG-COST-005 | 补查不存在 | 无该 trace | GET turns/unknown | 10002 trace 不存在 |
| RAG-COST-006 | 补查非学生 | session 角色=TEACHER | GET turns/{TRACE_ID} | 403 固定响应 |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功 |
| 10001 | INVALID_PARAMS | 参数无效（question 缺失/超长） |
| 10002 | NOT_FOUND | 实体不存在（trace 不存在 / 会话不存在 / 暂无评估报告） |
| 10004 | UNAUTHORIZED | 未登录 |
| 403 | ROLE_DENIED | 角色非 STUDENT（固定响应体，非标准错误码） |

---

## 5. 测试用例统计

| 里程碑 | 用例数量 |
|--------|---------|
| M1 角色硬门 RAG-GATE | 4 |
| M2 意图+改写 RAG-SSE(桩)/CONTRACT/COST | 4 |
| M3 召回+边界 RAG-SSE/BRIDGE/COST | 6 |
| M4 生成+token RAG-SSE/COST/ABORT | 4 |
| M5 自我检查 RAG-QUOTE/CONTRACT | 6 |
| M6 问题提示 RAG-SSE/SUGG | 5 |
| M7 会话收尾 RAG-CLOSE/COST | 9 |
| **总计** | **38** |

---

## 6. 测试执行顺序（即构建顺序门禁）

按里程碑分组执行，M(n) 全绿才进 M(n+1)：

```
100-103  : M1 角色硬门（RAG-GATE）
200-203  : M2 意图+改写（RAG-SSE 桩 / CONTRACT / COST）
300-305  : M3 召回+边界（RAG-SSE / BRIDGE / COST）
400-403  : M4 生成+token（RAG-SSE 全量 / COST / ABORT）
500-505  : M5 自我检查（RAG-QUOTE / CONTRACT）
600-604  : M6 问题提示（RAG-SSE / SUGG）
700-708  : M7 会话收尾（RAG-CLOSE / COST）
800      : 契约冻结回归（SSE 时序未被重排/删字段）
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

### 7.2 模拟 Python 桥返回 SSE 流（按里程碑桩替/真流）

```java
@MockBean
private RagAssistantPort ragAssistantPort;

// M2 桩替：permission → intent → rewrite → done
when(ragAssistantPort.ask(any(), any())).thenAnswer(inv -> {
    emitter.send(permissionEvent());
    emitter.send(intentEvent());
    emitter.send(rewriteEvent());
    emitter.send(doneStubEvent());   // 桩替占位答案
    emitter.complete();
    return null;
});

// M4 全量：permission → intent → rewrite → rerank → token* → done
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

### 7.3 契约冻结回归断言

```java
// M8：断言已冻结时序未被下游里程碑改变
List<String> frozenOrder = List.of("permission","intent","clarify","switch","rewrite","rerank","boundary","token","done");
// 实际流按序消费，任一事件顺序偏离即失败
```

---

## 8. 运行测试

```bash
# 运行单个测试类（接口层）
cd ai-edu-backend && mvn test -pl ai-edu-interface -Dtest=RagAssistantControllerTest

# 运行单个里程碑门禁（如 M1）
mvn test -pl ai-edu-interface -Dtest=RagAssistantControllerTest#gate_teacherRejected

# 运行桥测试（M3 门禁）
mvn test -pl ai-edu-infrastructure -Dtest=RagAssistantBridgeImplTest

# 运行 is_quoted 纯函数测试（M5 门禁）
mvn test -pl ai-edu-domain -Dtest=RagQuoteMatcherTest
```

> Python 侧 pytest 参考用例见 `rag-project-intro-assistant/test.md` 第 9 节（intent/召回/边界/clarify/超时/is_quoted/断连/评估），按里程碑归属执行。
