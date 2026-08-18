# question-type-mastery-backend 测试文档

## 测试概述

- **测试目标**：验证掌握度数据底盘——题目采集 + 掌握信号映射 + 累计平均聚合 + 题型名向量归一 + `getMastery` 契约变更 + 按题型查题目。
- **测试方式**：JUnit 5 + Mockito 单测（domain/application 层为主），controller 用 MockMvc；向量库/embedding 用桩（spike 阶段真模型对比单独验证）。
- **测试环境**：`mvn test`（6 模块），`mvn test -Dtest=ClassNameTest#methodName` 跑单方法。

## 测试数据

| 常量 | 值 | 说明 |
|------|-----|------|
| studentId | 1001L | 学生 ID |
| 直接答对 | hint_count=0, answer_request_count=0, correct | score=1.0 |
| 引导后答对 | hint_count=1, correct | score=0.5 |
| 答错 | correct=false / 未完成 | score=0.0 |
| 打折系数 | 第1题 0.7 / 第2题 0.8 / 第3题起 1.0 | per-题型，可配置 |
| 向量阈值 | 0.95（保守，spike 后定） | 归一并阈值 |
| 近义对 | 「一元二次方程」/「解一元二次方程」 | 应归一同一 canonical |
| 语义对 | 「相遇问题」/「行程问题」 | 阈值判是否合并（spike 标定） |

## 测试用例清单

### 模块 SIG（掌握信号映射，对应 tasks 7.1）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| SIG-001 | 直接答对 score=1.0 | 会话有 roundCount=0 | 学生直接答对 | 题目记录 score=1.0 |
| SIG-002 | 引导后答对 score=0.5 | 会话有 roundCount=1 | 引导后答对 | 题目记录 score=0.5 |
| SIG-003 | 答错 score=0.0 | 作答答错 | eval.correct=false | 题目记录 score=0.0 |
| SIG-004 | 首题打折 70% | 该题型 train_count=0 | 第 1 题直接答对 | 生效分值 1.0×0.7=0.7 |
| SIG-005 | 第 2 题打折 80% | train_count=1 | 第 2 题直接答对 | 生效分值 0.8 |
| SIG-006 | PENDING 信号不丢 | 题型未识别 | PENDING 题答对 | 信号落题目表，题型待归属 |
| SIG-007 | 题型分析页不产生信号 | analyze-question 消费题目 | 贴题 | 题目落库 source=ai，无 score |

### 模块 AGG（累计平均聚合，对应 tasks 7.2）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| AGG-001 | 累计平均 60→64% | mastery=60, train_count=9 | 新答对 score=1.0 | new=60×9/10+1×1/10≈55，train_count=10（正确率视角） |
| AGG-002 | 重复作答计两次 | 同一题做过一次 | 再做一次 | train_count=2，两次都计入 |
| AGG-003 | PENDING 归属后聚合 | 题信号在题目表，题型 PENDING | 题型归一为 canonical | 信号聚合进对应 canonical 掌握表行 |

### 模块 NOR（题型动态聚集，零锚点，对应 tasks 7.3/7.4）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| NOR-001 | 字符规则「解X→X」 | 规则库含解X | 「解一元二次方程」 | 归一「一元二次方程」，零 embedding 调用 |
| NOR-002 | 题目向量命中归并 | 向量库已有「鸡兔同笼」题 | 连发鸡兔变体（题型名不同） | 归并 canonical + 写别名表 |
| NOR-003 | 题型名向量命中归并 | 题面不同但题型名近义 | 汽车轮子版鸡兔同笼 | 归一到该 canonical |
| NOR-004 | 相似度不足不聚集 | 最近邻相似度 < 阈值 | 「相遇问题」查询 | 不归并，走建新 |
| NOR-005 | 首题建锚（零锚点） | 向量桶空 | 学生第 1 条题 | 建新 canonical + 题目/题型名向量入库 |
| NOR-006 | 失败兜底 | 向量库不可用 | 「解一元二次方程」 | 回退规则 + 原样落库，不阻塞 |
| NOR-007 | 落库前聚集不裂行 | decide 输出「解一元二次方程」 | 聚集 canonical「一元二次方程」 | 与「一元二次方程」共享同一掌握表行 |
| NOR-008 | 批量聚集散名归并 | 历史有近义散名 | 鸡兔同笼/鸡兔同笼问题/假设法 | 并到同一 canonical，重算掌握表 |

### 模块 MST（getMastery，对应 tasks 7.5）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| MST-001 | 连续百分比 + source + trainCount | 某题型练 10 题 | GET /students/1001/mastery | items[].masteryLevel 连续值、source=ai、trainCount=10 |
| MST-002 | 未开始题型不出现 | 无掌握记录 | GET /students/1001/mastery | 该题型不在 items[] |
| MST-003 | PENDING 项 | 有 obs 未确认 | GET /students/1001/mastery | masteryLevel=0、status=PENDING |
| MST-004 | 越权拦截 | 会话 userId≠studentId | GET /students/2002/mastery | 10005 无权访问 |
| MST-005 | 未登录 | 无会话 | GET /students/1001/mastery | 10004 未登录 |

### 模块 QST（按题型查题目，对应 tasks 7.6）

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| QST-001 | 查该题型题目列表 | 该题型有 3 道题记录 | GET /students/1001/topics/鸡兔同笼/questions | 返回 3 条（内容/score/时间） |
| QST-002 | 空态 | 无题目证据 | GET /students/1001/topics/X/questions | 返回空数组，code=00000 |
| QST-003 | 越权拦截 | 会话 userId≠studentId | GET /students/2002/topics/.../questions | 10005 |

## 错误码对照表

| code | message | 常量 | 说明 |
|------|---------|------|------|
| 10004 | 未登录 | `UNAUTHORIZED` | 无会话/过期 |
| 10005 | 无权访问 | `PERMISSION_DENIED` | 路径 studentId ≠ 会话 userId |

## 测试用例统计

| 模块 | 用例数 |
|------|--------|
| SIG（信号映射） | 7 |
| AGG（累计平均） | 3 |
| NOR（动态聚集） | 8 |
| MST（getMastery） | 5 |
| QST（按题型查题目） | 3 |
| **合计** | **26** |

> 另含：spike 脚本（embedding 模型对比，非单测，见 tasks 1.2）+ 全量回归（tasks 7.7，AI 答疑主流程/题型库/analyze-question 契约不变）。

## 测试执行顺序

1. 单元测试（domain/application，SIG/AGG/NOR 为主）——不依赖外部：
   - 信号映射 → 累计平均 → 动态聚集（字符规则 → 向量桩 → 双信号编排 → 批量聚集）→ 掌握表实体
2. 应用层集成（controller，MockMvc，MST/QST）——`getMastery` 契约、越权/未登录。
3. 回归（全量 `mvn test`）——主流程不回归。
4. spike（独立脚本，非 `mvn test`）——embedding 模型 + 阈值对比。
