# 知识点匹配与点亮 测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证解析管线（resolve）、掌握度增强（mastery）、挂起审核（pending/confirm）及派生层聚合/维护的业务场景，覆盖正常、边界与异常路径，并断言**权威图谱零写入**。

### 1.2 测试方式
- **集成测试**：直接注入 Controller / 应用服务，调用真实方法。
- **数据库回滚**：使用 `@Transactional`，测试完成后自动回滚。
- **无 Mock**：真实数据库操作（H2 共享库，learning 数据源），验证完整业务流程。
- **权威图断言**：通过 kg-sync 镜像 + Neo4j 仓储读取次数断言聚合/维护后无写入。

### 1.3 测试环境配置
- Profile: `test`（`application-test.yml` / `application-h2.yml`）
- 数据库：H2 共享库（learning 数据源），事务自动回滚
- Session：`MockHttpSession` 模拟（STUDENT/ADMIN/TEACHER 角色）
- 测试基类：`TestInfrastructureConfig`（现有基础设施测试配置）

---

## 2. 测试数据

| 参数 | 值 | 说明 |
|-----|-----|-----|
| STUDENT_ID | 101 | 学生（STUDENT 角色） |
| ADMIN_ID | 1 | 管理员（ADMIN 角色） |
| TEACHER_ID | 5 | 教师（TEACHER 角色） |
| LABEL_JT | 鸡兔同笼 | 题型 label（镜像未命中） |
| LABEL_FY | 二元一次方程组 | 题型 label（镜像精确命中） |
| URI_JSFA | `http://edukg.org/knowledge/3.1/kp/math#...jsfa` | 假设法 URI |
| URI_FY | `http://edukg.org/knowledge/3.1/kp/math#...fy` | 二元一次方程组 URI |
| GRADE_4 | 4 | 四年级 |
| GRADE_7 | 7 | 七年级 |
| CONF_THRESHOLD | 60 | 置信阈值（配置） |

---

## 3. 测试用例清单

### 3.1 解析接口 `POST /api/kp/resolve`

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RES-001 | 镜像精确命中 | 镜像存在「二元一次方程组」 | `{label: LABEL_FY, student_grade: 7}` | `status=RESOLVED`，uri=URI_FY，不调用 LLM |
| RES-002 | 题型库年级匹配 | 题型库 STABLE「鸡兔同笼」分布：假设法(4-6,38)、二元一次方程组(7-8,21) | `{label: LABEL_JT, student_grade: 7}` | `status=RESOLVED`，uri=URI_FY，confidence≈占比加权 |
| RES-003 | 年级锚跨年级归不同 kp | 同 RES-002 分布 | `{label: LABEL_JT, student_grade: 4}` | `status=RESOLVED`，uri=URI_JSFA |
| RES-004 | LLM 消歧低置信挂起 | 镜像/题型库均未命中，LLM 置信 40 | `{label: 牛吃草, student_grade: 7}` | `status=PENDING`，uri=null，落 obs(PENDING) |
| RES-005 | 年级缺失降级 | 学生无年级 | `{label: LABEL_JT}`（无 student_grade） | 走 LLM 消歧，不抛异常 |
| RES-006 | 参数错误-空 label | 无 | `{label: ""}` | 抛出 INVALID_PARAMS |
| RES-007 | 未登录 | 无 Session | `{label: LABEL_FY}` | 抛出 UNAUTHORIZED |
| RES-008 | 观测去重计数 | 学生 A 已存在 鸡兔同笼→URI_FY 观测 | 同生再次解析同题型 | 不新增行，`occurrence_count` +1 |
| RES-009 | 低置信学生澄清落票 | 镜像/题型库均未命中，LLM 置信 40，呈现「假设法/二元一次方程组/跳过」 | 学生选「假设法」 | 落 obs(source=student_vote)，不 PENDING，不暴露 kp_uri |
| RES-010 | 学生澄清跳过弃权 | 同上，呈现澄清选项 | 学生选「跳过」 | 不落 student_vote 观测，label 转 PENDING |
| RES-011 | 冷启动首条弱化 | 题型库空，镜像未命中，LLM 高置信(90)命中假设法 | 「鸡兔同笼」首条 | 落 obs(status=WEAK)，不点亮、不进题型库先验 |
| RES-012 | 票权重-LLM高置信覆盖学生票 | LLM 置信 90 命中 URI_FY | 学生想选假设法 | 直接 RESOLVED，不问学生，学生票不覆盖 |

### 3.2 学生掌握度 `GET /api/students/{id}/mastery`（增强）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| MAS-001 | 正常返回增强字段 | 学生有掌握度记录 | studentId=STUDENT_ID，Session=STUDENT_ID | `items[]` 含 `status`、`confidence` 字段 |
| MAS-002 | 越权查询 | Session=STUDENT_ID | studentId=ADMIN_ID | 抛出 PERMISSION_DENIED |
| MAS-003 | 无记录返回空列表 | 新学生 | studentId=新学生，Session=同 | `items=[]`，code 成功 |
| MAS-004 | 未登录 | 无 Session | studentId=STUDENT_ID | 抛出 UNAUTHORIZED |

### 3.3 挂起清单 `GET /api/kg/aliases/pending`

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| PEN-001 | 管理端正常列出 | 存在 PENDING/HUMAN_REVIEW 观测 | ADMIN Session | 返回 items，含 topicLabel/confidence/status |
| PEN-002 | 无挂起返回空 | 无挂起观测 | ADMIN Session | `items=[]` |
| PEN-003 | 权限不足-学生 | STUDENT Session | — | 抛出 PERMISSION_DENIED |
| PEN-004 | 未登录 | 无 Session | — | 抛出 UNAUTHORIZED |

### 3.4 挂起确认 `POST /api/kg/aliases/pending/{id}/confirm`

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| CFM-001 | 正常确认 | 存在挂起观测 id=1001，kp_uri 存在于镜像 | `{kp_uri: URI_JSFA}`，ADMIN Session | 观测转 RESOLVED，题型库假设法分布桶 hit 增加 |
| CFM-002 | 目标不存在 | id 不存在 | `{kp_uri: URI_JSFA}`，ADMIN Session | 抛出 50007（派生观测不存在） |
| CFM-003 | 参数错误-uri 无效 | kp_uri 不在镜像 | `{kp_uri: "invalid"}`，ADMIN Session | 抛出 INVALID_PARAMS |
| CFM-004 | 权限不足-教师允许 | TEACHER Session | 合法请求 | 返回成功（TEACHER 可访问） |
| CFM-005 | 权限不足-学生 | STUDENT Session | 合法请求 | 抛出 PERMISSION_DENIED |

### 3.5 聚合与维护（服务层）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| AGG-001 | 达阈值进候选 | 同一 label 3 名学生命中共 6 次 | 触发聚合 | 题型库建 CANDIDATE + 分布桶 |
| AGG-002 | 未达阈值不聚合 | 仅 1 名学生命中 1 次 | 触发聚合 | 题型库无新条目 |
| AGG-003 | 升级稳定 | CANDIDATE ≥10 学生 + 近 30 天增长 | 审核通过 | 升 STABLE，解析先验可用 |
| MAI-001 | 冲突观测自动重判 | decide 诊断 vs 观测冲突（CONFLICTED） | 运行维护任务 | 高置信则更新 obs + 题型库统计回流 |
| MAI-002 | 低置信进人工 | 重判仍低置信/无年级锚 | 运行维护任务 | status=HUMAN_REVIEW |
| MAI-003 | 权威图零写入 | 执行聚合 + 维护后 | — | kg-sync 镜像行数不变、Neo4j 无写调用 |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功 |
| 10001 | INVALID_PARAMS | 参数无效 |
| 10004 | UNAUTHORIZED | 未登录 |
| 20004 | PERMISSION_DENIED | 角色不符 / 越权 |
| 50007 | KP_OBS_NOT_FOUND | 派生观测不存在（confirm 目标缺失） |

> 解析失败不报错：`resolve` 返回 200 + `status=PENDING`。

---

## 5. 测试用例统计

| 模块 | 正常 | 边界 | 异常 | 合计 |
|------|------|------|------|------|
| RES（解析） | 6 | 3 | 3 | 12 |
| MAS（掌握度） | 2 | 1 | 1 | 4 |
| PEN（挂起清单） | 2 | 0 | 2 | 4 |
| CFM（挂起确认） | 2 | 0 | 3 | 5 |
| AGG/MAI（聚合维护） | 3 | 1 | 2 | 6 |
| **合计** | **15** | **5** | **11** | **31** |

---

## 6. 测试执行顺序

`@Order` 按依赖执行：

| 顺序 | 类 | 说明 |
|------|-----|------|
| 1 | `KpResolutionResolverTest` | 解析管线（镜像/题型库/LLM/挂起/去重） |
| 2 | `KpAggregationMaintenanceTest` | 聚合阈值 + 维护重判 + 零写入断言 |
| 3 | `KpResolutionControllerTest` | resolve 接口（含权限/参数） |
| 4 | `StudentMasteryLightupTest` | mastery 增强字段 + 越权 |
| 5 | `KpAliasReviewControllerTest` | pending / confirm 接口 |

---

## 7. 辅助方法

- `mockStudentSession()`：创建 STUDENT 角色 `MockHttpSession`（userId=STUDENT_ID）。
- `mockAdminSession()`：创建 ADMIN 角色 `MockHttpSession`（userId=ADMIN_ID）。
- `seedMirror(label, uri)`：向 kg-sync 镜像插入节点（H2）。
- `seedObs(studentId, topicLabel, kpUri, status, confidence)`：构造 `t_kp_derived_obs` 行。
- `seedQuestionType(topicLabel, kps...)`：构造题型库 + 年级分布桶。
- `assertMirrorRowCount(before)`：断言镜像行数与操作前一致（零写入）。

---

## 8. 运行测试

```bash
# 运行全部
cd ai-edu-backend && mvn test

# 运行本 change 相关
mvn test -Dtest='KpResolutionResolverTest,KpAggregationMaintenanceTest,KpResolutionControllerTest,StudentMasteryLightupTest,KpAliasReviewControllerTest'
```
