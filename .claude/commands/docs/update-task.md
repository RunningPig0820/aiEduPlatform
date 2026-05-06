---
name: "DOCS: Update-Task"
description: Update a single task status in Yuque via OpenClaw session
category: Workflow
tags: [workflow, openspec, yuque, update]
---

Update a single task checkbox in Yuque when a task is completed locally.

**⚠️ Requires OpenClaw `docs:update-task` command — not yet implemented.**

**Gateway Configuration**
- URL: `http://114.132.222.92:22867`
- Session Key: `openclaw-control-ui`

**Input**: Yuque path and task identifier (e.g., `/docs:update-task 产品中心/组织中心/organization-management --task 3`).

## Steps

1. **Fetch current tasks** via `docs:get <yuque_path> tasks`

2. **Identify the task** by index or title

3. **Send update via session message**

   ```bash
   curl -s -X POST http://114.132.222.92:22867/api/sessions/send \
     -H "Authorization: Bearer <token>" \
     -d '{
       "sessionKey": "openclaw-control-ui",
       "message": "docs:update-task <yuque_path> <task_index> done"
     }'
   ```

4. **Update local tasks.md**

   Change `- [ ]` to `- [x]` for the matching task.

## Guardrails
- Requires OpenClaw `docs:update-task` to be implemented
- Update both remote and local to keep in sync
- If update fails, do not modify local file
