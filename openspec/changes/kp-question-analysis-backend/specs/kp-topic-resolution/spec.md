# kp-topic-resolution 能力规格（delta）

解析管线新增题目理解前置（题目文本 → 题型名），题型库命中支持别名。既有解析管线行为（镜像→题型库→LLM 消歧→挂起）不变。

## ADDED Requirements

### Requirement: 题目理解前置（题目文本 → 题型名）

系统 SHALL 提供题目理解能力：输入题目文本 + 学生年级上下文，经 LLM 识别候选题型名（限 1~5 个）；prompt SHALL 注入当前题型库常用题型名作为参考词表，优先从词表选取以收敛命名。题目理解结果 SHALL 供 analyze-question 独立入口复用，SHALL NOT 直接写观测。

#### Scenario: 文本识别题型名
- **WHEN** 输入题目文本（如鸡兔同笼应用题）
- **THEN** 返回候选题型名列表（如「鸡兔同笼」），不调用解析管线、不写观测

#### Scenario: LLM 不可用降级
- **WHEN** LLM 题目理解失败
- **THEN** 返回空候选列表，调用方降级 PENDING

### Requirement: 题型库命中支持别名

解析管线②（题型库年级匹配）SHALL 支持 canonical 与别名命中：`findByTopicLabelOrAlias`，变体题型名命中 canonical 题型的分布先验，行为 SHALL 与 canonical 命中等价。

#### Scenario: 别名命中题型库先验
- **WHEN** 学生解析 label「鸡兔同笼」（别名），题型库 canonical「鸡兔同笼问题」有七年级分布桶
- **THEN** 按该 canonical 条目的七年级分布返回先验 kp
