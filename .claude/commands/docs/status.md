---
name: "DOCS: Status"
description: Check task progress from Yuque tasks document
category: Workflow
tags: [workflow, openspec, yuque, status]
---

Check task progress for a change from Yuque tasks document.

**Yuque Repository**: `zhangmin-jrrer/iu9s4m` (智启学堂)

**Input**: Yuque path (e.g., `/docs:status 产品中心/组织中心`).

---

## Steps

1. **Parse Yuque path**

   Extract Yuque path from arguments.
   Derive change name from last segment.

2. **Find tasks document in Yuque**

   Call `mcp__yuque-mcp__yuque_list_docs` with `repo_id: "zhangmin-jrrer/iu9s4m"`

   Find document with slug `<change_name>-tasks` (e.g., `organization-management-tasks`).

3. **Get tasks document**

   Call `mcp__yuque-mcp__yuque_get_doc`:
   ```json
   {
     "repo_id": "zhangmin-jrrer/iu9s4m",
     "doc_id": "<doc_id_or_slug>",
     "include_lake": false
   }
   ```

   Handle failures:
   - 404: output "No tasks document found"
   - 5xx: retry once → output error

4. **Parse tasks and calculate progress**

   Parse checkbox formats:
   - Standard: `- [x]` = done, `- [ ]` = pending
   - Yuque Lake: `- [✓]` = done

   Count total and completed, calculate percentage.

5. **Handle edge cases**

   If no checkboxes: output "No tasks found"

---

## Output On Success

```
## Status: <yuque_path>

**Change Name:** <change_name>
**Progress:** <X/Y> tasks complete (<percentage>%)

### Tasks
- [x] Task 1: <description>
- [ ] Task 2: <description>
...
```

## Output On No Tasks

```
## Status: <yuque_path>

**Progress:** No tasks found
The tasks document exists but contains no checkboxes.
```

## Output On Error

```
## Status: <yuque_path>

**Error:** No tasks document found at this path
To sync tasks first, run: /docs:sync <path>
```

---

## Guardrails

- Find tasks document by slug `<change_name>-tasks` (e.g., `organization-management-tasks`)
- Yuque slug is **repository-level unique**, NOT path-level unique
- Parse both `- [x]` and `- [✓]` formats
- If no checkboxes: output "No tasks found"
- Derive change name from path's last segment