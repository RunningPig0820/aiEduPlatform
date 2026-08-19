# tutoring-subject-gate 测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证 AI 答疑学科门：decide 之前判定学科（subject-classify）、非数学题跳过（不建/不续会话、不记录）、数学题放行、失败降级、文本与图片双通道、幂等无副作用。

### 1.2 测试方式
- **Python 单元测试**：subject-classify 端点——文本/图片分类、失败空结果、模型参数（aiEduPlatformModel）。
- **Java 单元测试**：`TutoringAppService` 编排层——mock subject-classify 返回不同 subject，断言分流（不建会话/不记录/返回提示）。
- **Java 集成测试**：`TutoringControllerTest`——MockMvc + MockHttpSession，SSE 事件序列 + 数据库零写入。
- **契约测试**：subject-classify 请求/响应序列化。

### 1.3 测试环境配置
- Profile: `test`，事务回滚，MockHttpSession 模拟登录。

---

## 2. 测试数据

| 参数 | 值 | 说明 |
|-----|-----|-----|
| STUDENT_ID | 1001 | 学生 |
| SUBJECT_MATH | math | 数学题 |
| SUBJECT_PHYSICS | physics | 物理题（非数学） |
| OUT_OF_SCOPE_MSG | 目前仅支持数学答疑，换一道数学题试试吧。 | 提示语 |

---

## 3. 测试用例清单

### 3.1 Python subject-classify（PSC）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| PSC-001 | 文本物理题 | 纯文字 | content="自由落体运动…" | subject=physics |
| PSC-002 | 文本数学题 | 纯文字 | content="鸡兔同笼…" | subject=math |
| PSC-003 | 图片题目 | 图片 URL | image_url=受力分析图 | 多模态分类，返回学科 |
| PSC-004 | LLM 异常 | 注入抛错 llm | 任意输入 | 返回空 subject，不抛异常 |
| PSC-005 | 模型参数统一 | - | 调用 | 使用 doubao-seed-2-0-mini-260428 / temp 0.3 |

### 3.2 学科分流（GATE：TutoringAppService 编排层）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| GATE-001 | 拍题物理题不建会话 | classify 返回 physics | 发起会话+物理题 | 不建会话、不调 decide/generate、零写入、返回提示 |
| GATE-002 | 拍题数学题正常建会话 | classify 返回 math | 发起会话+数学题 | 建会话(subject=math) + decide + 落库 |
| GATE-003 | 换题非数学跳过 | 数学会话中传化学题图 | classify 返回 chemistry | 该新题不结算/不记录，返回提示，原会话不受影响 |
| GATE-004 | classify 失败降级 | classify 异常/超时 | 返回空 | 按 math 放行，正常答疑 |
| GATE-005 | 重复发物理题幂等 | 同一物理题多次 | 重复请求 | 每次均跳过，零记录，不建会话 |
| GATE-006 | 会话 subject 记录真实值 | classify 返回 math | 正常建会话 | 会话 subject=math（非硬编码默认值） |

### 3.3 契约（CON）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| CON-001 | subject-classify 请求 snake_case | Java→Python | `{content, image_url}` | 字段名与 Python 对齐 |
| CON-002 | 响应反序列化 | Python 返回 | `{"subject":"physics"}` | 映射到 DTO |
| CON-003 | 响应 subject 缺失容忍 | Python 空结果 | `{}` | subject=null，不抛错（Java 降级） |

### 3.4 接口层（API：SSE 事件序列）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| API-001 | 拍题非数学 SSE | classify=physics | 发起会话 | meta(sessionId=null,type=hint) → token(提示语) → done |
| API-002 | 非数学不报错 | 同上 | 发起会话 | HTTP 200，正常 SSE 流 |
| API-003 | 未登录 | 无 Session | 请求 | 10004 未登录 |
| API-004 | 越权 | 学生 A 用学生 B 会话 | 请求 | 10005 无权访问 |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功（含非数学题提示流，非错误） |
| 10004 | UNAUTHORIZED | 未登录 |
| 10005 | FORBIDDEN | 无权访问 |

---

## 5. 测试用例统计

| 模块 | 用例数量 |
|-----|---------|
| Python subject-classify（PSC） | 5 |
| 学科分流（GATE） | 6 |
| 契约（CON） | 3 |
| 接口层（API） | 4 |
| **总计** | **18** |

---

## 6. 测试执行顺序

```
100-104  : PSC Python 分类（文本/图片/失败/模型）
200-205  : GATE 学科分流（拍题/换题/降级/幂等）
300-302  : CON 契约
400-403  : API SSE 序列
```

---

## 7. 辅助方法

### 7.1 mock subject-classify
```java
private void mockClassify(String subject) {
    when(classifyPort.classify(any())).thenReturn(new SubjectClassifyResult(subject));
}
```

### 7.2 断言不建会话 / 零写入
```java
private void assertNoWrite() {
    assertThat(tutoringSessionRepository.count()).isZero();
    assertThat(questionRecordRepository.count()).isZero();
    assertThat(topicMasteryRepository.count()).isZero();
}
```

---

## 8. 运行测试

```bash
# Java 编排层测试
cd ai-edu-backend && mvn test -pl ai-edu-application -Dtest=TutoringAppServiceSubjectGateTest

# Java 接口测试
mvn test -pl ai-edu-interface -Dtest=TutoringControllerTest

# Python 分类测试（aiEduPlatformModel）
cd ../aiEduPlatformModel && python -m pytest ai-edu-ai-service/tests/tutoring -k subject
```
