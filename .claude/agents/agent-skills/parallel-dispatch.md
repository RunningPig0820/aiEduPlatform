---
name: parallel-dispatch
description: ai-edu-architect-coordinator 专属 - 并行派发多个独立 subagent
---

# 并行派发流程

## 使用场景

**类型 B 任务：** 多领域独立任务，可并行开发

```
示例：
├── [User] 用户认证接口 - ai-edu-coder-user
├── [Question] 题目管理接口 - ai-edu-coder-question
└── [Organization] 学校管理接口 - ai-edu-coder-organization

这三个任务相互独立，可以并行派发
```

## 派发流程

### Step 1: 识别独立任务

从设计文档读取任务拆分表：

```markdown
| 序号 | 任务 | Context | Agent | 依赖 |
|------|------|---------|-------|------|
| 1 | 用户认证接口 | User | ai-edu-coder-user | - |
| 2 | 题目管理接口 | Question | ai-edu-coder-question | - |
| 3 | 学校管理接口 | Organization | ai-edu-coder-organization | - |
```

**判断标准：** 依赖列为 `-` 的任务可以并行

### Step 2: 准备派发参数

为每个 subagent 准备：

```markdown
派发参数：
- 设计文档路径：docs/plans/<topic>/<topic>-design.md
- 具体任务描述：[从设计文档提取]
- API 文档路径：docs/plans/<topic>/<topic>-api.md
- 测试文档路径：docs/plans/<topic>/<topic>-test.md
```

### Step 3: 并行派发

使用 Agent tool 并行派发：

```
在一条消息中调用多个 Agent tool，实现并行派发
```

### Step 4: 等待结果

监控所有 subagent 执行状态

### Step 5: 验收集成

每个 subagent 完成后进行验收

## 禁止并行的情况

| 情况 | 说明 |
|------|------|
| 有依赖关系 | 必须按顺序执行 |
| 共享状态 | 可能产生冲突 |
| 编辑相同文件 | 会相互覆盖 |

## 示例

```
设计文档任务拆分：
1. User - 用户认证接口（无依赖）
2. Question - 题目管理接口（无依赖）

执行计划：
并行派发 ai-edu-coder-user 和 ai-edu-coder-question
```

## 检查清单

- [ ] 确认任务间无依赖
- [ ] 确认任务间无共享状态
- [ ] 为每个任务准备完整参数
- [ ] 并行派发（一条消息多个 Agent tool）
- [ ] 等待所有任务完成
- [ ] 验收每个任务输出