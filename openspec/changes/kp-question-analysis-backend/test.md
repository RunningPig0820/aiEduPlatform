# kp-question-analysis-backend 测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证三件事：① 题目理解（题目文本 → 题型名，LLM 参考词表注入）；② `analyze-question` 端点全分支（题型库命中全分布 / resolve 单点 / PENDING 候选 / 纯分析不写 obs / 越权）；③ 题型库别名合并（kp 重叠收敛、阈值不再被变体稀释）。

### 1.2 测试方式
- **单测**：domain/infra 纯逻辑（题目理解、别名合并算法）用 Mockito mock LLM/仓储。
- **接口测试**：`MockMvc` + `MockHttpSession`，mock `KpQuestionAnalysisAppService` 或真实仓储（复用现有 `KpResolutionController` 测试模式）。
- **数据库**：不依赖真实 DB，别名合并用内存仓储 stub 验证算法。

### 1.3 测试环境配置
- Profile: `test`
- LLM：`mock(LlmGateway.class)`，返回预设 `AiEduChatResponse`（复用 `KpLlmDisambiguatorTest` 的 `setField` 注入模式）
- Session：`MockHttpSession`（STUDENT 角色）

## 2. 测试数据

| 参数 | 值 | 说明 |
|-----|-----|-----|
| QUESTION_TEXT | 「笼子里有鸡和兔共 35 个头，94 只脚，鸡和兔各有多少只？」 | 鸡兔同笼应用题 |
| TOPIC_VARIANT_A | 鸡兔同笼 | 变体 A |
| TOPIC_VARIANT_B | 鸡兔同笼问题 | 变体 B |
| KP_URI_A | math#textbook-middle-00085 | 鸡兔同笼知识点 URI |
| KP_URI_B | math#textbook-middle-00090 | 二元一次方程组 URI |
| STUDENT_ID | 1001L | 测试学生 |
| SESSION_STUDENT | MockHttpSession(studentId=1001, role=STUDENT) | 学生会话 |

## 3. 测试用例清单

### 3.1 题目理解（KpQuestionAnalyzerTest，infra）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| UND-001 | 参考词表注入 prompt | `findTopTopicLabels(20)` 返回「鸡兔同笼」「相遇问题」 | 题目文本 | `llmGateway.chat` 收到的 prompt 含「鸡兔同笼」词表；返回候选含「鸡兔同笼」 |
| UND-002 | 多候选解析去编号 | LLM 返回「1. 鸡兔同笼\n2. 假设法」 | 题目文本 | 返回 `[鸡兔同笼, 假设法]`，编号/bullet 被剥离 |
| UND-003 | LLM 失败返回空 | `chat` 抛异常 / 返回空响应 | 题目文本 | 返回空列表，不抛异常（调用方降级 PENDING） |

### 3.2 analyze-question（KpQuestionAnalysisAppServiceTest + Controller 测试）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| ANQ-001 | 题型库命中返回全分布 | `findByTopicLabelOrAlias` 命中 canonical，该题型 2 个 kp 桶 | QUESTION_TEXT | `status=RESOLVED`，`knowledgePoints` 含 2 条（kpLabel 已反查、gradeRange/ratio 透传） |
| ANQ-002 | 题型库未命中走 resolve 单点 | 题型库 miss，只读 resolve 命中某 kp | QUESTION_TEXT | `status=RESOLVED`，`knowledgePoints` 单条 `ratio=1.0` |
| ANQ-003 | 多候选歧义 PENDING | 首个候选题型库 miss，resolve 返回 PENDING 带候选 | QUESTION_TEXT | `status=PENDING`，`knowledgePoints=[]`，`candidates` 非空 |
| ANQ-004 | 题目理解失败降级 | `understand` 返回空 | QUESTION_TEXT | `status=PENDING`，`knowledgePoints=[]`，`candidates=[]`，code=00000 不报错 |
| ANQ-005 | 纯分析不写 obs | 调用 analyze（mock `derivedKpObsRepository`） | QUESTION_TEXT | `upsert` 未被调用（浏览不产生学习观测） |
| ANQ-006 | 参数校验 text 为空 | 无 | `{ "text": "" }` | 抛出 INVALID_PARAMS（10001） |
| ANQ-007 | 越权/未登录 | 无 Session | QUESTION_TEXT | 抛出 UNAUTHORIZED（10004） |

### 3.3 别名合并（KpQuestionTypeAggregationService 别名单测）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| ALI-001 | kp 重叠合并 + 统计折叠 | 现有 canonical「鸡兔同笼问题」kp={A,B}；新桶「鸡兔同笼」kp={A,B}，各 2 学生 | 聚合「鸡兔同笼」桶 | 不新建 CANDIDATE；插别名「鸡兔同笼」→ canonical；canonical hit 统计合并（4 学生） |
| ALI-002 | 无相似新建 | 现有题型 kp 集合与新桶重叠 <70% | 聚合新题型桶 | 新建独立 CANDIDATE |
| ALI-003 | 别名命中后续聚合 | canonical 已有别名「鸡兔同笼」 | 聚合「鸡兔同笼」新 obs | `findByTopicLabelOrAlias` 命中 canonical，更新统计，不新建条目 |
| ALI-004 | 变体不稀释阈值 | 变体 A/B 各 2 学生（合并后 4 ≥ 3 阈值） | 聚合两桶 | 合并后按 4 学生判定，达 CANDIDATE 阈值；若各自 2 学生则均不达标 |
| ALI-005 | 解析②/vote 别名命中 | canonical「鸡兔同笼问题」，别名「鸡兔同笼」 | `resolve("鸡兔同笼")` / `vote` | 按 canonical 年级分布返回先验 / vote 落观测成功 |

### 3.4 回归（resolve/vote/题型库接口）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| REG-001 | 既有接口契约不变 | 现有测试基线 | resolve/vote/题型库分页/关联知识点 | `kp-matching-lightup` 相关测试全绿，契约无破坏 |

### 3.5 联调修复与存疑挂起闭环（2026-08-17 新增）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| FIX-001 | WEAK（LLM 冷启动）→ PENDING 候选待确认 | resolveReadOnly 返回 resolvedWeak | 题目文本 | `status=PENDING`，knowledgePoints 空，candidates 含该 kpLabel（不再权威 RESOLVED） |
| FIX-002 | 遍历候选顺序无关 | 候选1 PENDING、候选2 权威命中 | 多候选 | RESOLVED（取候选2），不依赖 LLM 候选顺序 |
| FIX-003 | 存疑挂起落 PENDING obs | analyze PENDING | 题目文本 | `upsertPendingIfAbsent` 被调（进待确认清单，不丢） |
| FIX-004 | 非镜像候选丢弃 | PENDING 候选「方程组问题」不在镜像 | 题目文本 | candidates 空（vote 不 10003） |
| FIX-005 | resolveReadOnly WEAK 标记 + 只读 | LLM 消歧冷启动命中 | 题型 label | `isWeak()=true`，obs 不写 |
| FIX-006 | vote 转正 PENDING | 该生该题型有 PENDING obs | topicLabel+selectedLabel | `resolvePendingByStudentTopic` 更新转正，不新建；无 PENDING 才新建 |
| FIX-007 | 聚合排除 WEAK | obs 含 WEAK（LLM 幻觉） | 聚合 | WEAK 不进题型库（防「对数方程求解」式污染），第二信号转正才入 |
| FIX-008 | 维护重判 PENDING → WEAK | PENDING obs + LLM 高置信 | maintain() | `resolveWeakByMaintenance` 转 WEAK（待共现转正，不直接 RESOLVED） |

### 3.6 封闭域约束选择：学段知识点池（D8，2026-08-17 第二轮）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| POOL-001 | LLM 从池里选 top-N | 学段池含「二元一次方程组」「鸡兔同笼」；题目鸡兔同笼 | 题目+grade+池 | 返回池内 label，无池外 |
| POOL-002 | LLM 失败 → 回退池前 N（恒非空） | LLM chat 抛异常 | 题目+grade+池 | 返回池前 N 个 label，非空 |
| POOL-003 | LLM 输出池外 → 过滤丢弃 | LLM 返回池外「对数方程求解」 | 题目+grade+池 | 结果仅池内 label（跨学段错误被挡） |
| POOL-004 | 子池粗筛：关键词召回 | 池 500 个，题目含「鸡兔」 | 题目+grade+池 | 子池经 name-LIKE 缩到 <200 |
| POOL-005 | 子池空 → 回退全池截断 | 关键词无命中 | 题目+grade+池 | 回退全池截断（MAX=200），LLM 仍能选 |
| POOL-006 | analyze 恒非空（冷启动） | 题型库空 + 学段池有数据 | 题目文本 | `candidates` 非空（池内最相近），不返回空态 |
| POOL-007 | keyword 搜索 | 知识点接口 | `{stage, keyword:"二元一次"}` | label LIKE `%二元一次%` 过滤，分页正常 |

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功 |
| 10001 | PARAM_ERROR | 参数错误（analyze-question text 为空） |
| 10002 | ENTITY_NOT_FOUND | 实体不存在 |
| 10003 | INVALID_PARAMS | 参数无效（vote 候选不存在） |
| 10004 | UNAUTHORIZED | 未登录 |

## 5. 测试用例统计

| 模块 | 用例数量 |
|-----|---------|
| 题目理解（UND） | 3 |
| analyze-question（ANQ） | 7 |
| 别名合并（ALI） | 5 |
| 联调修复与挂起闭环（FIX） | 8 |
| 封闭域约束选择（POOL） | 7 |
| 回归（REG） | 1 |
| **总计** | **31** |

## 6. 测试执行顺序

```
UND-001~003 : 题目理解单测（先于应用层，底座能力）
POOL-001~007 : 封闭域约束选择（池选择/恒非空/池外过滤/子池粗筛/keyword）
ANQ-001~007 : analyze-question 应用 + 接口
FIX-001~008 : 联调修复 + 存疑挂起闭环（WEAK 降级/遍历/挂起/镜像校验/vote 转正/聚合排 WEAK/维护重判）
ALI-001~005 : 别名合并单测
REG-001     : 回归（最后，全量）
```

## 7. 辅助方法

### 7.1 注入私有字段（复用现有模式）
```java
private void setField(Object target, String fieldName, Object value) {
    try {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

### 7.2 构造题型库 stub
```java
private QuestionType questionType(String label, Long id) {
    return QuestionType.create(label, QuestionTypeStatus.CANDIDATE, 1001L) /* +setId(id) */;
}
```

### 7.3 创建学生会话
```java
private HttpSession createStudentSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 1001L);
    session.setAttribute("role", "STUDENT");
    return session;
}
```

## 8. 运行测试

```bash
cd ai-edu-backend

# 题目理解单测（infra）
mvn test -pl ai-edu-infrastructure -Dtest=KpQuestionAnalyzerTest

# analyze-question 应用 + 别名合并单测（application）
mvn test -pl ai-edu-application -Dtest=KpQuestionAnalysisAppServiceTest,KpQuestionTypeAggregationServiceTest

# analyze-question 接口测试（interface）
mvn test -pl ai-edu-interface -Dtest=KpQuestionAnalysisControllerTest

# 全量
mvn clean install -DskipTests && mvn test
```
