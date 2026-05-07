---
name: "DOCS: Update"
description: Update a single document or task status in Yuque via Yuque MCP tools
category: Workflow
tags: [workflow, openspec, yuque, update]
---

Update a single document or task checkbox in Yuque.

**Yuque Repository**: `zhangmin-jrrer/iu9s4m` (智启学堂)

**Input formats**:
- Update single file: `/docs:update 产品中心/组织中心 --file design`
- Update task status: `/docs:update 产品中心/组织中心 --file tasks --task 3`

**Parameter parsing**:
- `--file <type>`: doc type to update (proposal/design/tasks/api/tests)
- `--task N` or `--task=N`: task index, only valid when `--file tasks`
- Task index starts from 1

---

## Global Mappings (All docs commands must follow)

### Slug Naming Strategy
| Doc Type | Slug Format | Example (change=organization-management) |
|----------|-------------|------------------------------------------|
| proposal | `<change>-proposal` | `organization-management-proposal` |
| design | `<change>-design` | `organization-management-design` |
| tasks | `<change>-tasks` | `organization-management-tasks` |
| api | `<change>-api` | `organization-management-api` |
| tests | `<change>-tests` | `organization-management-tests` |

> **Note**: Yuque slug is **repository-level unique**, not path-level. Always use prefixed unique slugs.

### Yuque Path ↔ Change Name Mapping
| Yuque Path | Change Name |
|------------|-------------|
| `产品中心/组织中心/设计方案/organization-management` | `organization-management` |
| `产品中心/知识图谱页面化/knowledge-graph-ui` | `knowledge-graph-ui` |
| ... | ... |

> **Note**: Derive change name from Yuque path's last segment.

---

## Steps

1. **Parse input**

   Extract Yuque path, `--file` type, and optionally `--task` index.

   Yuque path format: `产品中心/<模块>/<子模块>`

   Derive change name from path's last segment or use mapping table.

2. **Read local file**

   Read `openspec/changes/<change_name>/<doc_type>.md`:
   - `--file proposal` → `proposal.md`
   - `--file design` → `design.md`
   - `--file tasks` → `tasks.md`
   - `--file api` → `api.md`
   - `--file tests` → `test.md`

   If file not found:
   ```
   ## Update Failed
   **Error:** Local file not found: openspec/changes/<change_name>/<file>.md
   ```

3. **Find target document in Yuque**

   Call `mcp__yuque-mcp__yuque_list_docs` with `repo_id: "zhangmin-jrrer/iu9s4m"`

   Find document with slug `<change_name>-<doc_type>` (e.g., `organization-management-design`).

4. **Handle content modification (if --task specified)**

   Only when `--file tasks` AND `--task N` provided:

   **Validate task index**:
   - Parse checkbox lines from local content
   - Count total tasks
   - Check: `task_index ≤ total_tasks`

   If out of range:
   ```
   ## Update Failed
   **Error:** Task index out of range
   **Requested:** Task #<N>
   **Available:** 1-<M>
   ```

   **Modify content**:
   - Find Nth checkbox line
   - Change `- [ ]` → `- [x]`
   - Keep all other content unchanged

5. **Update document via Yuque MCP**

   **If document exists**: Call `mcp__yuque-mcp__yuque_update_doc`:
   ```json
   {
     "repo_id": "zhangmin-jrrer/iu9s4m",
     "doc_id": "<existing_doc_id>",
     "body": "<content>"
   }
   ```

   **If document NOT exists**: Call `mcp__yuque-mcp__yuque_create_doc`:
   ```json
   {
     "repo_id": "zhangmin-jrrer/iu9s4m",
     "title": "<doc_type>",
     "slug": "<change_name>-<doc_type>",
     "body": "<content>",
     "format": "markdown"
   }
   ```

   Handle failures:
   - If API fails: retry once → output error → terminate

6. **Update local file (if task modified)**

   If task checkbox was modified:
   - Write modified content back to `openspec/changes/<change_name>/tasks.md`

---

## Output On Success (Single File Update)

```
## Update Complete

**Yuque Path:** <yuque_path>
**File:** <doc_type>
**Status:** ✓ updated

URL: https://www.yuque.com/zhangmin-jrrer/iu9s4m/...
```

## Output On Success (Task Update)

```
## Update Complete

**Yuque Path:** <yuque_path>
**File:** tasks
**Task #<N>:** marked as complete

### Updated Tasks (Progress: <X/Y>)
- [x] Task 1
- [x] Task 2
- [x] Task 3 ← updated
- [ ] Task 4

URL: https://www.yuque.com/zhangmin-jrrer/iu9s4m/...
```

## Output On Error (Index Out of Range)

```
## Update Failed

**Error:** Task index out of range
**Requested:** Task #<N>
**Available:** <M> tasks (index must be 1-<M>)
```

## Output On Error (API Failure)

```
## Update Failed

**Yuque Path:** <yuque_path>
**Error:** Yuque API returned <status_code>

Please retry or check Yuque connectivity.
```

---

## Guardrails

- `--file` is required, specifies single file to update
- `--task` only valid when `--file tasks`
- Task index starts from 1
- Validate task_index ≤ total_tasks before modifying
- Derive change name from Yuque path (last segment)
- **Slug naming**: Use `<change_name>-<doc_type>` format for unique repository-level slugs
- Yuque slug is **repository-level unique**, NOT path-level unique
- If remote update fails: DO NOT modify local file (except for task case where remote succeeded)
- Update both remote AND local for task checkbox changes
- Preserve all other content in file