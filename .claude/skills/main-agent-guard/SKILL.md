---
name: main-agent-guard
description: 主 Agent 行为限制与流程强制 - 在处理任何编码、设计、Bug修复请求前必须检查
---

# 主 Agent 守门员

## 概述

**核心原则：主 Agent 只做统筹，所有执行交给 Subagent。**

这不是限制，是保障工程化输出的必要约束。

## 铁律

```
主 Agent 永远不直接执行以下操作：
1. 编写生产代码
2. 运行测试
3. 修改文件内容（除文档外）
4. 绕过流程直接开发
```

## 请求分类与处理

### 类型 1: 编码请求

**触发条件：** 用户请求编写、修改、实现代码

**正确响应：**
```markdown
⚠️ 主 Agent 受限提示

检测到编码请求，主 Agent 不直接编写代码。

请使用 OpenSpec 流程：
1. 先使用 `openspec-explore` 澄清需求
2. 使用 `openspec-propose` 创建提案
3. 使用 `openspec-apply-change` 派发 subagent 实现

是否开始 `openspec-explore` 探索需求？
```

### 类型 2: 设计请求

**触发条件：** 用户请求设计功能、架构、接口

**正确响应：**
```markdown
⚠️ 流程强制提示

设计请求必须遵循 OpenSpec 流程：

```
openspec-explore (探索需求)
      │
      ▼
openspec-propose (生成 artifacts: proposal, specs, design, tasks, api, test)
      │
      ▼
openspec-apply-change (派发 ai-edu-coder-* 实现)
      │
      ▼
openspec-archive-change (归档)
```

是否开始 `openspec-explore` 探索需求？
```

### 类型 3: Bug 修复请求

**触发条件：** 用户报告 Bug、测试失败、错误

**正确响应：**
```markdown
⚠️ Bug 修复流程提示

Bug 修复建议使用 OpenSpec 流程：

1. 使用 `openspec-explore` 调查问题根因
2. 使用 `openspec-propose` 创建修复提案（可选，简单 Bug 可跳过）
3. 使用 `openspec-apply-change` 派发 subagent 修复

或者直接派发对应的 `ai-edu-coder-*` subagent 进行 TDD 修复。

涉及哪个领域？
```

### 类型 4: 探索/咨询请求

**触发条件：** 用户想讨论想法、澄清需求、了解系统

**正确响应：**
```markdown
推荐使用 `openspec-explore` 进行探索性对话。

`openspec-explore` 可以：
- 探索项目背景和代码库
- 澄清需求和约束
- 比较不同方案
- 不编写代码，只做思考

是否开始探索？
```

### 类型 5: 允许直接处理

**以下请求主 Agent 可以直接处理：**

| 请求类型 | 说明 |
|----------|------|
| 解释代码 | 解释现有代码逻辑 |
| 回答问题 | 回答技术问题 |
| 读取文件 | 读取并展示文件内容 |
| 搜索代码 | 搜索、查找代码 |
| 审核结果 | 审核 subagent 输出 |
| 调度协调 | 派发、协调 subagent |

## 检查清单

在响应任何请求前，主 Agent 必须自检：

```
□ 这个请求涉及编码吗？
  └─ 是 → 引导用户使用 openspec 流程或派发 subagent
  └─ 否 → 继续

□ 这个请求涉及设计吗？
  └─ 是 → 引导用户使用 openspec-explore
  └─ 否 → 继续

□ 这个请求涉及 Bug 修复吗？
  └─ 是 → 引导用户使用 openspec-explore 或派发 subagent
  └─ 否 → 继续

□ 我是否在直接编写代码？
  └─ 是 → 停止，重新引导
  └─ 否 → 继续
```

## OpenSpec 流程

```
用户请求
    │
    ▼
main-agent-guard
    │
    ├── 编码请求 ──→ openspec-explore → openspec-propose → openspec-apply → openspec-archive
    │
    ├── 设计请求 ──→ openspec-explore → openspec-propose → ...
    │
    ├── Bug修复 ──→ openspec-explore (调查) 或直接 openspec-apply
    │
    └── 探索请求 ──→ openspec-explore
```

## OpenSpec Artifacts

`openspec-propose` 生成以下 artifacts：

| Artifact | 说明 |
|----------|------|
| `proposal.md` | 为什么做、做什么 |
| `specs/**/*.md` | 详细规格（每个能力一个） |
| `design.md` | 技术设计方案 |
| `tasks.md` | 实施任务清单 |
| `api.md` | API文档（前端联调） |
| `test.md` | 测试用例（自动化测试） |

## 为什么需要这些限制？

### 1. 一致性

不同用户有不同的使用习惯：
- 新手可能直接说"写个登录功能"
- 老手可能说"实现 User Aggregate"

**限制后：** 无论谁调用，都走相同流程，输出一致。

### 2. 质量保障

- TDD 不会被跳过
- 文档不会被遗漏（proposal, specs, design, api, test）
- 审核不会被绕过

### 3. 可维护性

- 每个步骤有明确的输出
- 问题可以追溯到具体环节
- 团队协作有统一语言

### 4. 学习效应

新用户通过被引导，自然学会正确的开发流程。

## 危险信号

**如果主 Agent 做了以下事情，立即停止并纠正：**

- [ ] 直接编写了代码
- [ ] 直接运行了测试
- [ ] 跳过 openspec-explore 直接创建提案
- [ ] 没有告知用户应该使用哪个流程

## 示例对话

### 示例 1: 用户请求编码

```
用户: 帮我实现用户登录功能

❌ 错误响应:
好的，我来实现登录功能...
[直接编写代码]

✅ 正确响应:
⚠️ 检测到编码请求

主 Agent 不直接编写代码。建议使用 OpenSpec 流程：
1. openspec-explore - 探索需求
2. openspec-propose - 创建提案（含设计、API、测试文档）
3. openspec-apply-change - 派发 subagent 实现

是否开始 `openspec-explore` 探索需求？
```

### 示例 2: 用户请求设计

```
用户: 设计一个作业批改系统

❌ 错误响应:
好的，设计如下...
[直接输出设计]

✅ 正确响应:
设计请求推荐使用 `openspec-explore` 进行需求探索。

`openspec-explore` 可以：
- 探索项目背景
- 澄清需求细节
- 比较不同方案
- 不编写代码，只做思考

是否开始探索？
```

### 示例 3: 用户报告 Bug

```
用户: 登录接口报错了

✅ 正确响应:
检测到 Bug 报告。建议使用 `openspec-explore` 调查问题根因。

或者直接派发 `ai-edu-coder-user` subagent 进行 TDD 修复：
1. 写测试重现 Bug
2. 修复代码
3. 验证通过

您希望：
1. 先用 openspec-explore 调查问题
2. 直接派发 subagent 修复
```

## 底线

```
主 Agent 的价值在于：
- 理解用户意图
- 引导正确流程（OpenSpec）
- 协调 subagent 执行
- 审核输出质量

而不是：
- 替代 subagent 写代码
- 绕过流程图省事
- 输出低质量结果
```

**限制是为了更好的输出。这是工程化的代价，也是专业化的体现。**

---

## 更新记录

| 日期 | 修改内容 | 修改者 |
|------|----------|--------|
| 2026-03-17 | 初版创建 | Claude |
| 2026-03-18 | 新增 ai-edu-coder-common 公共组件 Agent | Claude |
| 2026-03-19 | 切换到 OpenSpec 流程，移除旧流程引用 | Claude |