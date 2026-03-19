---
name: openspec-apply-change
description: Implement tasks from an OpenSpec change. Use when the user wants to start implementing, continue implementation, or work through tasks.
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "2.0"
  generatedBy: "1.2.0"
  features:
    - domain-based-dispatch: true
    - parallel-execution: true
---

Implement tasks from an OpenSpec change with **domain-based subagent dispatch**.

**Input**: Optionally specify a change name. If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

## Domain → Subagent Mapping

| Domain (领域) | Subagent Type | Keywords (关键词) |
|--------------|---------------|-------------------|
| user (用户域) | `ai-edu-coder-user` | User, 用户, 认证, 登录, 注册, 验证码, Student, Teacher, Parent, Role, Permission |
| organization (组织域) | `ai-edu-coder-organization` | Organization, 组织, School, Class, Grade, 班级, 学校, 年级, TeacherClass, StudentClass |
| question (题库域) | `ai-edu-coder-question` | Question, 题目, 题库, KnowledgePoint, 知识点, Difficulty, 难度, 题型 |
| homework (作业域) | `ai-edu-coder-homework` | Homework, 作业, Submission, 提交, Grading, 批改, Score, 评分, Assignment |
| learning (学习域) | `ai-edu-coder-learning` | Learning, 学习, ErrorBook, 错题本, Mastery, 掌握度, Progress, 进度 |
| common (公共组件) | `ai-edu-coder-common` | Common, 公共, Redis, Cache, 缓存, MinIO, 文件存储, RabbitMQ, 消息队列, Util, 工具类, Config, 配置, Exception, 异常, ApiResponse |

---

## Steps

### 1. Select the change

If a name is provided, use it. Otherwise:
- Infer from conversation context if the user mentioned a change
- Auto-select if only one active change exists
- If ambiguous, run `openspec list --json` to get available changes and use the **AskUserQuestion tool** to let the user select

Always announce: "Using change: <name>" and how to override (e.g., `/opsx:apply <other>`).

### 2. Check status to understand the schema

```bash
openspec status --change "<name>" --json
```

Parse the JSON to understand:
- `schemaName`: The workflow being used (e.g., "spec-driven")
- Which artifact contains the tasks (typically "tasks" for spec-driven, check status for others)

### 3. Get apply instructions

```bash
openspec instructions apply --change "<name>" --json
```

This returns:
- Context file paths (varies by schema)
- Progress (total, complete, remaining)
- Task list with status
- Dynamic instruction based on current state

**Handle states:**
- If `state: "blocked"` (missing artifacts): show message, suggest using openspec-continue-change
- If `state: "all_done"`: congratulate, suggest archive
- Otherwise: proceed to implementation

### 4. Read context files

Read the files listed in `contextFiles` from the apply instructions output.

### 5. Show current progress and analyze tasks

Display:
- Schema being used
- Progress: "N/M tasks complete"
- Remaining tasks overview

Then **analyze each pending task** to determine its domain.

### 6. Dispatch tasks by domain (NEW)

For each pending task:

**a) Identify Domain**

Use the keyword mapping above to analyze task description and determine which subagent should handle it:

```
Task: "实现用户登录接口，包含验证码校验"
→ Keywords: 用户, 登录, 验证码
→ Domain: user
→ Subagent: ai-edu-coder-user
```

**b) Group tasks by domain**

Group tasks that can be executed in parallel (same domain tasks can be batched):

```python
# Example grouping
{
  "ai-edu-coder-user": [task1, task3],
  "ai-edu-coder-common": [task2],
  "ai-edu-coder-homework": [task4, task5, task6]
}
```

**c) Dispatch to subagents**

For each domain group, use the **Agent tool** to spawn the appropriate subagent:

```
Agent(
  subagent_type="ai-edu-coder-{domain}",
  prompt="""
  ## Task Context
  - Change: {change_name}
  - Tasks assigned to {domain} domain:
    {task_list}

  ## Context Files
  {relevant_context}

  ## Instructions
  1. Implement each task following TDD practices
  2. Update task checkboxes: `- [ ]` → `- [x]` after completion
  3. Report any blockers or design issues immediately
  """,
  description="Implement {domain} tasks"
)
```

**d) Collect results**

- Wait for subagent completion
- Review changes made
- Update task status in tasks file
- Handle any errors or blockers reported

### 7. Handle cross-domain dependencies

If a task depends on another domain's task:
- Ensure dependent task completes first
- Use `blockedBy` in task metadata if available
- Or dispatch sequentially with proper handoff

### 8. On completion or pause, show status

Display:
- Tasks completed this session (grouped by domain)
- Overall progress: "N/M tasks complete"
- If all done: suggest archive
- If paused: explain why and wait for guidance

---

## Domain Detection Logic

When analyzing a task, check in this order:

1. **Explicit domain tag** (if task has `[domain:user]` or similar)
2. **Keyword matching** (use the keyword table above)
3. **File path hints** (`com.ai.edu.domain.user.*` → user domain)
4. **Default to `common`** if no specific domain detected

### Priority for overlapping keywords

Some keywords may match multiple domains:
- "Student" → user (Student entity is in user domain)
- "Class" → organization (班级 in organization domain)
- "Score" → homework (评分 context) or learning (掌握度 context)

Use task context to disambiguate.

---

## Parallel Execution Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    Task Analysis Phase                       │
│  Parse all pending tasks, identify domains, detect deps     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Parallel Dispatch Phase                   │
│                                                              │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐      │
│  │  user   │   │  common │   │ homework│   │question│      │
│  │ tasks   │   │ tasks   │   │ tasks   │   │ tasks   │      │
│  │ 1,3     │   │ 2       │   │ 4,5,6   │   │ 7       │      │
│  └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘      │
│       │             │             │             │            │
│       ↓             ↓             ↓             ↓            │
│  [subagent]   [subagent]   [subagent]   [subagent]          │
│                                                              │
│  (Parallel execution via Agent tool with run_in_background)  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Result Collection Phase                   │
│  Wait for all subagents, aggregate results, update status   │
└─────────────────────────────────────────────────────────────┘
```

---

## Output Format

### During Implementation

```
## Implementing: <change-name> (schema: <schema-name>)

### Domain Analysis
| Task | Domain | Subagent |
|------|--------|----------|
| #1   | user   | ai-edu-coder-user |
| #2   | common | ai-edu-coder-common |
| #3   | user   | ai-edu-coder-user |

### Dispatching to Subagents...

#### [user] Tasks #1, #3
→ Spawning ai-edu-coder-user...
✓ Task #1 complete
✓ Task #3 complete

#### [common] Task #2
→ Spawning ai-edu-coder-common...
✓ Task #2 complete

Progress: 3/7 tasks complete
```

### On Completion

```
## Implementation Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 7/7 tasks complete ✓

### Completed by Domain
| Domain | Tasks | Status |
|--------|-------|--------|
| user | #1, #3 | ✓ |
| common | #2 | ✓ |
| homework | #4, #5, #6 | ✓ |
| question | #7 | ✓ |

All tasks complete! Ready to archive this change.
```

---

## Guardrails

- Always analyze domain before dispatching
- Group independent tasks for parallel execution
- Respect task dependencies (use blockedBy if available)
- Fallback to main agent execution if domain is ambiguous
- Keep code changes minimal and scoped to each task
- Update task checkbox immediately after completing each task
- Use contextFiles from CLI output, don't assume specific file names
- If subagent reports blocker, pause and inform user

## Fluid Workflow Integration

This skill supports the "actions on a change" model:

- **Can be invoked anytime**: Before all artifacts are done (if tasks exist), after partial implementation, interleaved with other actions
- **Allows artifact updates**: If implementation reveals design issues, suggest updating artifacts - not phase-locked, work fluidly
- **Domain-aware dispatch**: Automatically routes tasks to specialized subagents for efficient parallel execution