---
name: "DOCS: Status"
description: Check OpenSpec sync progress and task status from Yuque
category: Workflow
tags: [workflow, openspec, yuque, status]
---

Check the sync status and task progress for an OpenSpec change from Yuque.

**⚠️ Requires OpenClaw `docs:get` command — not yet implemented.**

**Gateway Configuration**
- URL: `http://114.132.222.92:22867`
- Session Key: `openclaw-control-ui`

**Input**: Change name or Yuque path (e.g., `/docs:status 产品中心/组织中心/organization-management`).

## Steps

1. **Determine Yuque path**

2. **Request tasks doc via session message**

   ```bash
   curl -s -X POST http://114.132.222.92:22867/api/sessions/send \
     -H "Authorization: Bearer <token>" \
     -d '{
       "sessionKey": "openclaw-control-ui",
       "message": "docs:get <yuque_path> tasks"
     }'
   ```

3. **Compare with local status**

   Run `openspec status --change "<name>" --json` to compare local vs remote.

4. **Parse tasks and show progress**

   Count `- [x]` vs `- [ ]` from remote tasks content.

## Output

```
## Status: <change-name>

**Progress:** <X/Y> tasks complete (<percentage>%)

### Remote Tasks (Yuque)
- [x] Task 1
- [ ] Task 2
```

## Guardrails
- Requires OpenClaw `docs:get` to be implemented
- Parse checkbox format carefully: `- [x]` = done, `- [ ]` = pending
