---
name: requesting-code-review
description: 在完成任务、实现主要功能或合并之前使用，验证工作满足需求
---

# 请求代码审查

派发 superpowers:code-reviewer 子代理在问题级联前捕获它们。

**核心原则：** 早审查，勤审查。

## 何时请求审查

**强制：**
- 子代理驱动开发中每个任务后
- 完成主要功能后
- 合并到 main 之前

**可选但有价值：**
- 卡住时（新鲜视角）
- 重构前（基线检查）
- 修复复杂 bug 后

## 如何请求

**1. 获取 git SHA：**
```bash
BASE_SHA=$(git rev-parse HEAD~1)  # 或 origin/main
HEAD_SHA=$(git rev-parse HEAD)
```

**2. 派发 code-reviewer 子代理：**

使用 Task 工具和 superpowers:code-reviewer 类型，填写 `code-reviewer.md` 的模板

**占位符：**
- `{WHAT_WAS_IMPLEMENTED}` - 你刚构建的内容
- `{PLAN_OR_REQUIREMENTS}` - 它应该做什么
- `{BASE_SHA}` - 开始提交
- `{HEAD_SHA}` - 结束提交
- `{DESCRIPTION}` - 简要摘要

**3. 对反馈采取行动：**
- 立即修复关键问题
- 继续前修复重要问题
- 记录次要问题供以后
- 如果审查者错了就反驳（带理由）

## 示例

```
[刚完成任务 2：添加验证功能]

你：让我在继续前请求代码审查。

BASE_SHA=$(git log --oneline | grep "任务 1" | head -1 | awk '{print $1}')
HEAD_SHA=$(git rev-parse HEAD)

[派发 superpowers:code-reviewer 子代理]
  WHAT_WAS_IMPLEMENTED: 对话索引的验证和修复功能
  PLAN_OR_REQUIREMENTS: docs/plans/<topic>/<topic>-plan.md 中的任务 2
  BASE_SHA: a7981ec
  HEAD_SHA: 3df7661
  DESCRIPTION: 添加了 verifyIndex() 和 repairIndex()，有 4 种问题类型

[子代理返回]：
  优势：干净的架构，真实测试
  问题：
    重要：缺少进度指示器
    次要：报告间隔的魔术数字（100）
  评估：准备继续

你：[修复进度指示器]
[继续任务 3]
```

## 与工作流集成

**子代理驱动开发：**
- 每个任务后审查
- 在问题复合前捕获
- 移到下一个任务前修复

**执行计划：**
- 每批（3 个任务）后审查
- 获取反馈，应用，继续

**临时开发：**
- 合并前审查
- 卡住时审查

## 危险信号

**永远不要：**
- 因为"很简单"跳过审查
- 忽略关键问题
- 未修复重要问题继续
- 与有效技术反馈争论

**如果审查者错了：**
- 用技术理由反驳
- 展示证明有效的代码/测试
- 请求澄清

参见模板：requesting-code-review/code-reviewer.md