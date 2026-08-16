# 知识点匹配与点亮 测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证解析管线（resolve）、题型掌握度翻转（mastery 题型粒度 + kp-coverage 知识点派生覆盖度）、挂起审核（pending/confirm）及派生层聚合/维护的业务场景，覆盖正常、边界与异常路径，并断言**权威图谱零写入**。

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
| RES-013 | 冷启动 LLM 生成候选名 + 镜像校验 | 题型库空，镜像名 LIKE 无召回，LLM 生成「二元一次方程组」「假设法」，「二元一次方程组」镜像命中、「假设法」未命中 | `{label: LABEL_JT}` | 保留「二元一次方程组」候选；单候选 RESOLVED 标 WEAK |
| RES-014 | LLM 候选名全未命中镜像 | LLM 生成候选名镜像校验全不命中 | `{label: LABEL_JT}` | `status=PENDING`，candidates=[]，不返回镜像不存在 kp |

### 3.2 题型掌握度 `GET /api/students/{id}/mastery`

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| MAS-001 | 正常返回题型掌握度 | 学生有题型掌握度记录 | studentId=STUDENT_ID，Session=STUDENT_ID | `items[]` 含 `topicKey`、`topicLabel`、`masteryLevel`、`status`、`confidence` |
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
| AGG-004 | LLM 自动关联建候选 | 题型「鸡兔同笼」累积 ≥N 名学生 obs 共现（二元一次方程组、假设法） | 离线任务调用 LLM | LLM 输出归一化 kp 分布，建/更新 CANDIDATE + 分布桶 |
| AGG-005 | LLM 关联不直接稳定 | LLM 关联仅单来源（无第二独立信号） | 离线任务 | 保持 CANDIDATE，不升 STABLE 进先验 |
| MAI-001 | 冲突观测自动重判 | decide 诊断 vs 观测冲突（CONFLICTED） | 运行维护任务 | 高置信则更新 obs + 题型库统计回流 |
| MAI-002 | 低置信进人工 | 重判仍低置信/无年级锚 | 运行维护任务 | status=HUMAN_REVIEW |
| MAI-003 | 权威图零写入 | 执行聚合 + 维护后 | — | kg-sync 镜像行数不变、Neo4j 无写调用 |

### 3.6 全量知识点分页 `POST /api/kg/knowledge-points`

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| OVW-001 | 按学段分页返回 | kg 镜像有初中教材知识点 | `{stage: middle, page: 1, size: 20}` | items 含 kpUri/kpLabel/stage/chapterLabel/sectionLabel，total=总数 |
| OVW-002 | 无知识点返回空 | 某学段无教材 | `{stage: high}` | items=[]，total=0 |
| OVW-003 | 分页越界 | 初中知识点 10 条 | `{stage: middle, page: 99, size: 20}` | items=[]，不报错 |
| OVW-004 | 参数错误 | — | `{stage: "invalid"}` | 抛出 INVALID_PARAMS |
| OVW-005 | 未登录 | 无 Session | `{stage: middle}` | 抛出 UNAUTHORIZED |

### 3.7 题型库分页 + 关联知识点

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| QTP-001 | 分页列题型 | 题型库有 STABLE/CANDIDATE 条目 | `GET /api/kp/question-types?page=1&size=20` | items 含 id/topicLabel/status/hitCount，total=总数 |
| QTP-002 | 空题型库 | 无题型条目 | 同上 | items=[]，total=0 |
| QTP-003 | 题型关联知识点 | 题型 id=1 有分布桶 | `GET /api/kp/question-types/1/knowledge-points` | 返回 kpUri/kpLabel/gradeRange/ratio/hitCount，kpLabel 反查自镜像 |
| QTP-004 | 题型不存在 | id 不存在 | `GET /api/kp/question-types/999/knowledge-points` | 抛出 10002（实体不存在） |
| QTP-005 | 未登录 | 无 Session | 任一 | 抛出 UNAUTHORIZED |

### 3.8 掌握度主体翻转（服务层 + 覆盖度接口）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| NORM-001 | 题型归一化 | — | normalize("鸡兔同笼问题") vs normalize("鸡兔同笼") | 同一 topic_key |
| NORM-002 | 归一化空白/全角半角 | — | normalize("鸡 兔 同 笼") / 全角写法 | 归一化到规范 topic_key |
| TPM-001 | 题型掌握度落库 | 学生产生「鸡兔同笼」mastered 信号 | `applyMasteryAndErrors` | `t_student_topic_mastery` 落 topic_key + 75，RESOLVED |
| TPM-002 | 同题型取 max 单调不减 | 该生已有鸡兔同笼 75 | 再遇 practicing 信号 | 保持 75 不降 |
| COV-001 | 聚合题型按 ratio 派生 | 学生鸡兔同笼掌握 75，题型库鸡兔同笼→二元一次方程组 ratio 0.8 | `kp-coverage` | coverage=60，masteryLevel=50 |
| COV-002 | 未聚合题型按单观测派生 | 学生相遇问题掌握 50，obs 单观测相遇问题→kp | `kp-coverage` | coverage=50 |
| COV-003 | 无映射回退旧 KP 掌握度 | 某 kp 仅旧 `t_student_kp_mastery` 记录 | `kp-coverage` | coverage=旧 masteryLevel |
| COV-004 | 携带学段字段 | kp 归属初中教材 | `kp-coverage` | `items[]` 含 stage=middle、chapterLabel、sectionLabel |
| COV-005 | 无归属 stage 为空 | kp 未挂小节/章节 | `kp-coverage` | stage=null，kpUri/coverage 仍正常返回 |
| COV-006 | 越权查询 | Session=STUDENT_ID | studentId=ADMIN_ID | 抛出 PERMISSION_DENIED |
| COV-007 | 未登录 | 无 Session | studentId=STUDENT_ID | 抛出 UNAUTHORIZED |

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
| RES（解析） | 8 | 3 | 3 | 14 |
| MAS（题型掌握度） | 1 | 1 | 2 | 4 |
| 翻转（NORM/TPM/COV） | 8 | 1 | 2 | 11 |
| PEN（挂起清单） | 2 | 0 | 2 | 4 |
| CFM（挂起确认） | 2 | 0 | 3 | 5 |
| AGG/MAI（聚合维护） | 5 | 1 | 2 | 8 |
| OVW（全量知识点分页） | 1 | 2 | 2 | 5 |
| QTP（题型库分页） | 2 | 1 | 2 | 5 |
| **合计** | **29** | **9** | **18** | **56** |

---

## 6. 测试执行顺序

`@Order` 按依赖执行：

| 顺序 | 类 | 说明 |
|------|-----|------|
| 1 | `KpResolutionResolverTest` | 解析管线（镜像/题型库/LLM/挂起/去重） |
| 2 | `KpAggregationMaintenanceTest` | 聚合阈值 + 维护重判 + 零写入断言 |
| 3 | `KpResolutionControllerTest` | resolve 接口（含权限/参数） |
| 4 | `TopicKeyNormalizerTest` | 题型归一化（空白/全角半角/去末尾语气词） |
| 5 | `StudentTopicMasteryTest` | 题型掌握度落库 + 取 max 单调不减 |
| 6 | `StudentMasteryLightupTest` | mastery 题型掌握度接口 + 越权 |
| 7 | `KpCoverageAppServiceTest` | 知识点派生覆盖度（ratio/单观测/回退旧表） |
| 8 | `KpCoverageControllerTest` | kp-coverage 接口（stage/越权） |
| 9 | `KpAliasReviewControllerTest` | pending / confirm 接口 |
| 10 | `KgKnowledgeOverviewControllerTest` | 全量知识点分页 + 权限 |
| 11 | `KpQuestionTypeControllerTest` | 题型库分页 + 关联知识点 + kpLabel 反查 |

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
mvn test -Dtest='KpResolutionResolverTest,KpAggregationMaintenanceTest,KpResolutionControllerTest,TopicKeyNormalizerTest,StudentTopicMasteryTest,StudentMasteryLightupTest,KpCoverageAppServiceTest,KpCoverageControllerTest,KpAliasReviewControllerTest,KgKnowledgeOverviewControllerTest,KpQuestionTypeControllerTest'
```
