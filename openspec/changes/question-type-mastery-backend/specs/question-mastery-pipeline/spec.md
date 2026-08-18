# question-mastery-pipeline Spec

题目全量采集 → 掌握信号（直接答对/引导后答对/答错 + 打折）→ 累计平均聚合 → `getMastery` 连续百分比 + 按题型查题目。掌握度数据底盘。

## ADDED Requirements

### Requirement: 题目全量采集落库

系统 SHALL 将 AI 答疑、题型分析页的题目全量落库到题目记录表，作为掌握度的唯一证据源。题目分来源：学生作答（`ai`）与题库题目（`bank`，预留，当前题库无题不触发）。

#### Scenario: AI 答疑题目落库
- **WHEN** 学生在 AI 答疑中提交一道题目（含换题 `isNewQuestion` 触发）
- **THEN** 系统将题目写入题目记录表，`source=ai`，携带题型名、对错信号、引导轮数与 `session_id`（原题链接，可跳回 AI 答疑会话）

#### Scenario: 原题链接可回查
- **WHEN** 学生从掌握度页「查看题目」跳转某题型题目列表
- **THEN** 每道题携带 `session_id`，可跳回 AI 答疑会话查看原题；无会话链接的题目显示题目原文

#### Scenario: 题型分析页题目落库
- **WHEN** 学生通过题型分析页贴题/拍题提交一道题目
- **THEN** 系统将题目写入题目记录表，`source=ai`，**不产生掌握信号**（学生只贴题未作答）

#### Scenario: 题库题目来源预留
- **WHEN** 存在 `source=bank` 的题目读取路径且题库为空
- **THEN** 系统不触发 bank 采集，字段与路径就绪但不报错

### Requirement: 掌握信号跟题目走

系统 SHALL 在每道题上记录一次作答信号，信号跟题目走、不跟题型走——题型未识别（PENDING）的题照常记录对错信号，归属确定后再聚合进掌握表。

#### Scenario: 直接答对
- **WHEN** 学生作答且 `hint_count=0`、`answer_request_count=0`，题目答对
- **THEN** 系统记录该题 `score=1.0`

#### Scenario: 引导后答对
- **WHEN** 学生作答答对且 `hint_count≥1` 或 `answer_request_count≥1`
- **THEN** 系统记录该题 `score=0.5`

#### Scenario: 答错
- **WHEN** 学生作答答错或未完成
- **THEN** 系统记录该题 `score=0.0`

#### Scenario: PENDING 题型信号不丢
- **WHEN** 题目题型未识别（PENDING），学生已作答
- **THEN** 系统照常记录对错信号到题目表，等待题型归属确定后聚合

### Requirement: 掌握度累计平均聚合

系统 SHALL 按题型累计平均聚合掌握度：`new = old × n/(n+1) + score × 1/(n+1)`，`train_count += 1`。一次作答算一次，不做题目去重。掌握表记录来源（`source`）。

#### Scenario: 累计平均计算
- **WHEN** 某题型已有 `mastery=60`、`train_count=9`，新作答 `score=1.0`
- **THEN** 新掌握度 = 60×9/10 + 1.0×1/10 = 55 取整为 55（正确率视角），`train_count=10`

#### Scenario: 同一题重复作答
- **WHEN** 同一道题学生做两次
- **THEN** 两次都计入 `train_count`，反映真实练习量

#### Scenario: PENDING 题归属确定后聚合
- **WHEN** PENDING 题的题型归属确定（归一为 canonical）
- **THEN** 该题信号聚合进对应 canonical 题型的掌握表

### Requirement: 折扣与信号映射可配置

系统 SHALL 将 per-题型前几题打折系数（第1题70% / 第2题80% / 第3题起100%）与信号分值映射（直接答对 1.0 / 引导后答对 0.5 / 答错 0.0）做成可配置项，支持后续规则变更。

#### Scenario: 首题打折
- **WHEN** 某题型第 1 次作答且直接答对（`score=1.0`）
- **THEN** 该题生效分值为 `1.0 × 0.7 = 0.7`（打折作用于 score，不作用于结果）

#### Scenario: 配置变更重算
- **WHEN** 打折系数或信号映射配置变更
- **THEN** 系统可从题目表事实源重算聚合，题目证据不丢

### Requirement: getMastery 契约（连续百分比）

系统 SHALL 在 `GET /students/{id}/mastery` 返回题型掌握度，`masteryLevel` 为 0-100 连续百分比，`items[]` 携带 `source`（ai/bank）与 `trainCount`。

#### Scenario: 掌握度列表返回连续百分比
- **WHEN** 学生查询掌握度，某题型练过 10 题答对 6 题
- **THEN** 响应 `masteryLevel` 为该题连续百分比（约 64），`source=ai`，`trainCount=10`

#### Scenario: 未开始题型
- **WHEN** 学生查询掌握度，某题型无掌握记录
- **THEN** 该题型不出现在 `items[]`（前端引导去 AI 答疑做题）

#### Scenario: PENDING 题型项
- **WHEN** 题型有 obs 但未确认（PENDING）
- **THEN** 该项 `masteryLevel=0`、`status=PENDING`，与已确认项区分

### Requirement: 按题型查题目列表

系统 SHALL 提供按题型查该生题目列表的接口，返回题目内容、对错信号、作答时间，供掌握度页「查看题目」跳转。

#### Scenario: 查看某题型题目列表
- **WHEN** 学生查看某题型的题目列表
- **THEN** 系统返回该生该 canonical 题型下全部题目（内容、score、时间），空列表不报错
