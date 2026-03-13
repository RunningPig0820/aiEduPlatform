---
name: workflow-orchestration
description: "任务编排总入口 - 根据任务类型自动路由到正确的执行方式"
---

# 工作流编排

## 概述

根据任务涉及的领域和依赖关系，自动路由到正确的执行方式。

**核心原则：** 先分析，再路由，确保每个任务由正确的 agent 以正确的方式执行。

## 项目定位

本项目是**纯 Java DDD 后端**，仅提供 REST API。

## 总体流程图

```
┌─────────────────────────────────────────────────────────────────────┐
│                         用户提出需求                                  │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Step 1: brainstorming (头脑风暴)                        │
│  • 梳理需求                                                          │
│  • 识别涉及的 Bounded Context                                        │
│  • 判断任务类型                                                      │
│  • 输出设计文档 + 任务清单                                           │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                                  ▼
                         ┌────────────────┐
                         │  任务类型判定   │
                         └───────┬────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
         ▼                       ▼                       ▼
   ┌───────────┐          ┌───────────┐          ┌───────────┐
   │  类型 A   │          │  类型 B   │          │  类型 C   │
   │  单领域   │          │多领域独立 │          │多领域依赖 │
   └─────┬─────┘          └─────┬─────┘          └─────┬─────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Step 2a:        │    │ Step 2b:        │    │ Step 2c:        │
│ 直接派发        │    │ 并行派发        │    │ 架构师协调      │
│                 │    │                 │    │                 │
│ 派发给对应的    │    │ dispatching-    │    │ ai-edu-architect│
│ subagent        │    │ parallel-agents │    │ -coordinator    │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Step 3: verification-before-completion                  │
│  • 运行测试                                                          │
│  • 验证功能                                                          │
│  • 代码审查                                                          │
└─────────────────────────────────────────────────────────────────────┘
```

## 任务类型路由表

| 类型 | 判定条件 | 执行方式 | 涉及 Agent |
|------|----------|----------|------------|
| **A: 单领域** | 仅涉及1个 Bounded Context | 直接派发 | 1个 coder agent |
| **B: 多领域独立** | 涉及多个 Context，任务无依赖 | 并行派发 | 多个 coder agent 并行 |
| **C: 多领域有依赖** | 涉及多个 Context，任务有依赖 | 架构师协调 | architect + 多个 coder agent |

## Agent 路由映射

| Bounded Context | Subagent | 职责 |
|-----------------|----------|------|
| User | `ai-edu-coder-user` | 用户、权限、认证 |
| Question | `ai-edu-coder-question` | 题库、知识点 |
| Homework | `ai-edu-coder-homework` | 作业、批改、评分 |
| Learning | `ai-edu-coder-learning` | 错题本、知识掌握度 |
| Organization | `ai-edu-coder-organization` | 学校、班级、年级 |
| Testing | `ai-edu-tester` | 测试 |
| **协调** | `ai-edu-architect-coordinator` | 架构、接口契约、协调 |

## 执行流程

### 类型 A：单领域任务

```
1. 从设计文档提取任务描述
2. 调用对应的 subagent
3. 提供：设计文档 + 接口契约（如有）
4. Subagent 遵循 TDD 完成开发
5. ai-edu-tester 进行测试验证
```

**示例命令：**
```
请 ai-edu-coder-user 根据设计文档 docs/plans/2024-01-15-user-avatar-design.md
实现用户头像上传功能，遵循 TDD 流程。
```

### 类型 B：多领域独立任务

```
1. 从设计文档提取多个独立任务
2. 调用 dispatching-parallel-agents skill
3. 并行派发多个 subagent
4. 各 subagent 独立完成
5. 汇总结果，统一测试
```

**示例命令：**
```
使用 dispatching-parallel-agents 并行执行：
- ai-edu-coder-question: 实现题库导入功能
- ai-edu-coder-user: 实现用户注册功能

两个任务独立，无依赖关系。
```

### 类型 C：多领域有依赖任务

```
1. 调用 ai-edu-architect-coordinator
2. 架构师分析依赖关系
3. 输出接口契约
4. 按依赖顺序派发任务
5. 逐阶段验收集成
```

**示例命令：**
```
请 ai-edu-architect-coordinator 协调以下有依赖的任务：
- Homework Context 依赖 User Context
- Learning Context 依赖 Homework Context

设计文档：docs/plans/2024-01-15-homework-grading-design.md
```

## 工作流程检查清单

### 开始前
- [ ] 是否已完成 brainstorming？
- [ ] 是否有设计文档？
- [ ] 是否识别了涉及的 Bounded Context？

### 执行中
- [ ] 是否选择了正确的任务类型？
- [ ] 是否派发给了正确的 subagent？
- [ ] Subagent 是否遵循 TDD？

### 完成后
- [ ] 测试是否通过？
- [ ] 代码是否审查？
- [ ] 是否符合接口契约？

## 常见错误

| 错误 | 后果 | 正确做法 |
|------|------|----------|
| 跳过 brainstorming | 需求不清，返工 | 必须先完成设计 |
| 类型判断错误 | 执行方式错误 | 仔细分析依赖关系 |
| 派发错误的 agent | 代码混乱 | 按路由表派发 |
| 忽略接口契约 | 集成失败 | 架构师先定义契约 |

## 与 Skills 的集成

本 workflow 依赖以下 skills：

| Skill | 使用时机 |
|-------|----------|
| `brainstorming` | 第一步，梳理需求 |
| `test-driven-development` | 各 subagent 内部遵循 |
| `dispatching-parallel-agents` | 类型 B 任务 |
| `verification-before-completion` | 完成前验证 |
| `systematic-debugging` | 遇到 bug 时 |

## 快速参考

```
新需求 → brainstorming → 设计文档
                           │
                           ▼
                    识别 Context
                           │
            ┌──────────────┼──────────────┐
            │              │              │
         单Context     多Context      多Context
                       (独立)        (有依赖)
            │              │              │
            ▼              ▼              ▼
      直接派发        并行派发       架构师协调
            │              │              │
            └──────────────┼──────────────┘
                           │
                           ▼
                      测试验证
                           │
                           ▼
                        完成
```