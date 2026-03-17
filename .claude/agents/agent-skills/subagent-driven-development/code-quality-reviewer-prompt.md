# 代码质量审查者提示模板

派发代码质量审查者子代理时使用此模板。

**目的：** 验证实现构建良好（干净、已测试、可维护）

**只在规范符合性审查通过后派发。**

```
Task tool (superpowers:code-reviewer):
  Use template at requesting-code-review/code-reviewer.md

  WHAT_WAS_IMPLEMENTED: [来自实现者报告]
  PLAN_OR_REQUIREMENTS: [plan-file] 中的任务 N
  BASE_SHA: [任务前提交]
  HEAD_SHA: [当前提交]
  DESCRIPTION: [任务摘要]
```

**代码审查者返回：** 优势、问题（关键/重要/次要）、评估