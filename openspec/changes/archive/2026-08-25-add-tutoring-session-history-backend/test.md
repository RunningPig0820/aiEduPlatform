# AI 答疑会话历史 测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证会话历史能力：列表接口（全状态/按用户隔离/排除软删/倒序）、删除接口（软删 + Redis 清 + COS 保留 + 归属校验）、消息 meta 填充与 Redis/COS 序列化、会话标题生成。同时确认既有答疑行为（SSE 流/护栏/轮次/收尾）不回归。

### 1.2 测试方式
- **Controller 单测**（`TutoringControllerTest` 风格）：mock `TutoringAppService` + `MockHttpSession`，覆盖认证/越权/透传与 ApiResponse 包装。
- **AppService 单测**（`TutoringAppServiceTest` 风格）：mock 仓储/缓存，覆盖 meta 填充、列表/删除业务逻辑、title 生成、越权拒绝。
- **Mapper/Repository 集成测试**（H2，`@DS("learning")`，`application-integration.yml`）：列表查询（全状态/排除软删/倒序/按用户隔离）、软删级联。
- **序列化测试**：Redis（`TutoringSessionCacheImpl` 往返）与 COS（`TutoringTranscriptArchiver` JSON）携带 meta 字段。

### 1.3 测试环境配置
- Profile: `test` / `integration`（H2 in-memory，`logic-delete-field: deleted` 已配）
- Session：`MockHttpSession` 注入 `userId`（=studentId）+ `role=STUDENT`
- 常量：`STUDENT_A=501L`、`STUDENT_B=502L`、`SESSION_1=1001L`、`SESSION_2=1002L`

---

## 2. 测试数据

| 参数 | 值 | 说明 |
|-----|-----|------|
| STUDENT_A | 501L | 会话属主 |
| STUDENT_B | 502L | 越权访问者 |
| SESSION_ACTIVE | 1001L | ACTIVE 会话（A 的） |
| SESSION_ARCHIVED | 1002L | ARCHIVED 会话（A 的） |
| SESSION_OTHER | 2001L | B 的会话 |
| TITLE_TEXT | 「鸡兔同笼怎么做」 | 首条文字消息 |

---

## 3. 测试用例清单

### 3.1 列表接口（LIST）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| LIST-001 | 正常-全状态列表 | A 有 ACTIVE+ARCHIVED 各一 | GET /sessions（A 登录） | 返回 2 条，ARCHIVED 在列，updated_at 倒序 |
| LIST-002 | 正常-排除软删 | A 有一个 is_deleted=1 会话 | GET /sessions（A 登录） | 软删会话不在列表 |
| LIST-003 | 隔离-不含他人会话 | A、B 各有会话 | GET /sessions（A 登录） | 不含 B 的会话 |
| LIST-004 | 异常-未登录 | 无 Session | GET /sessions | code=10004 |
| LIST-005 | 异常-非 STUDENT | role=TEACHER | GET /sessions | code=20004 |
| LIST-006 | 边界-无会话 | A 无任何会话 | GET /sessions（A 登录） | data=[]（空数组，非 null） |

### 3.2 删除接口（DELETE）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| DEL-001 | 正常-软删 | A 的会话（Redis 有缓存） | DELETE /sessions/1001（A 登录） | 成功；session 行 is_deleted=1；Redis session+messages key 被清；COS 未调删除 |
| DEL-002 | 正常-删除后列表消失 | 已软删 1001 | GET /sessions | 1001 不在列表 |
| DEL-003 | 异常-越权删除 | B 登录 | DELETE /sessions/1001（A 的） | code=50002，行未被删 |
| DEL-004 | 异常-未登录 | 无 Session | DELETE /sessions/1001 | code=10004 |
| DEL-005 | 异常-会话不存在 | 无 9999 | DELETE /sessions/9999（A 登录） | code=50002 |
| DEL-006 | 边界-删除已归档会话 | A 的 ARCHIVED 会话 | DELETE /sessions/1002 | 成功（可删任意状态） |

### 3.3 消息 meta 填充（META）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| META-001 | 正常-AI 消息带完整 meta | 一轮问答完成（type=hint） | 触发 buildStream.doOnComplete | AI 消息 type/round/status 填充，question_kps/decide_reason/eval 与 action 一致 |
| META-002 | 降级-护栏拒绝 | reveal 被降级 approach | 学生要答案 | AI 消息 type=approach、denied=reveal |
| META-003 | 用户消息 meta 空 | start/sendMessage | append 用户消息 | 7 个 meta 字段全空 |
| META-004 | 序列化-Redis 往返 | AI 消息带 meta | append→listMessages | meta 字段不丢 |
| META-005 | 序列化-COS JSON | AI 消息带 meta | archiveTranscript | transcript messages[ai] 含 type/denied/decide_reason/round/question_kps/eval/status（snake_case） |
| META-006 | 思考- thinking 保留 | generate 有 thinking 分片 | buildStream doOnComplete | AI 消息 thinking 完整，与 meta 并存 |

### 3.4 会话标题（TITLE）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| TITLE-001 | 正常-文字首条生成标题 | 文字发起 | start(message=「鸡兔同笼怎么做」) | session.title=前 ~30 字，随会话落库 |
| TITLE-002 | 兜底-图片题无正文 | 纯图片发起 | start(imageData, content=null) | title 为兜底值（subject+questionType 或「图片题目」），不阻断 |
| TITLE-003 | 边界-超长消息截断 | message>30 字 | start(长消息) | title 截断至 ~30 字，无异常 |

### 3.5 回归（REG）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| REG-001 | SSE 流不受影响 | 正常一轮 | start/sendMessage | meta/token/done 序列不变，护栏/轮次逻辑不变 |
| REG-002 | getSession 不再下发签名 URL | 会话存在 | GET /sessions/{id} | 返回 recentMessages；**不含 transcriptUrl**（签名 URL 不下发浏览器） |
| REG-003 | Python 契约容忍 | AI 消息带 meta | 构造 DecideContext.history | Python 侧 Pydantic 忽略额外字段，decide 不中断（R1，跨仓验证） |

### 3.6 transcript 后端代理（TRAN）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| TRAN-001 | 正常-归属通过 | 会话存在（A 的，COS 有归档） | GET /sessions/1001/transcript（A 登录） | data.messages 完整（含 meta），结构与 COS 序列化一致 |
| TRAN-002 | 正常-COS 对象缺失 | 会话存在但未归档 | GET /sessions/1001/transcript | data.messages=[]（code 00000，**非 50002**） |
| TRAN-003 | 异常-越权 | B 登录访问 A 的会话 | GET /sessions/1001/transcript | code=50002，不读 COS |
| TRAN-004 | 异常-会话不存在/已软删 | 无 9999 | GET /sessions/9999/transcript | code=50002 |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功 |
| 10004 | UNAUTHORIZED | 未登录 |
| 20004 | PERMISSION_DENIED | 仅学生可访问 |
| 50002 | TUTORING_SESSION_NOT_FOUND | 会话不存在 / 非本人 / 已删除 |

---

## 5. 测试用例统计

| 模块 | 用例数量 |
|-----|---------|
| 列表接口 | 6 |
| 删除接口 | 6 |
| 消息 meta | 6 |
| 会话标题 | 3 |
| 回归 | 3 |
| transcript 后端代理 | 4 |
| **总计** | **28** |

---

## 6. 测试执行顺序

```
100-106  : LIST 列表接口
200-206  : DEL  删除接口
300-306  : META 消息 meta
400-403  : TITLE 会话标题
500-503  : REG  回归（含 Python 契约验证）
600-603  : TRAN transcript 后端代理
```

---

## 7. 辅助方法

### 7.1 创建登录会话
```java
private MockHttpSession loginSession(Long studentId) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", studentId);
    session.setAttribute("role", "STUDENT");
    return session;
}
```

### 7.2 构造带 meta 的 AI 消息
```java
private TutoringChatMessage aiMessageWithMeta() {
    TutoringChatMessage m = TutoringChatMessage.ai("先假设全是鸡", "设 x 只兔…");
    m.setType("approach");
    m.setDenied("reveal");
    m.setDecideReason("学生第一次要思路");
    m.setRound(1);
    m.setQuestionKps(List.of("鸡兔同笼"));
    m.setEval(EvalInfo.builder().correct(false).emotion("CONFUSED").build());
    m.setStatus("ACTIVE");
    return m;
}
```

---

## 8. 运行测试

```bash
# Controller 单测
cd ai-edu-backend && mvn test -pl ai-edu-interface -Dtest=TutoringControllerTest

# AppService 单测（含 meta 填充/列表/删除）
mvn test -pl ai-edu-application -Dtest=TutoringAppServiceTest

# Mapper/Repository 集成测试（H2）
mvn test -pl ai-edu-infrastructure -Dtest=TutoringSessionRepositoryImplTest
```
