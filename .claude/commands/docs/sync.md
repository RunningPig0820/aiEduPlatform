---
name: "DOCS: Sync"
description: Sync OpenSpec artifacts to Yuque via Yuque MCP tools
category: Workflow
tags: [workflow, openspec, yuque, sync]
---

Sync OpenSpec change artifacts to Yuque directly using Yuque MCP tools.

**Yuque Repository**: `zhangmin-jrrer/iu9s4m` (智启学堂)

**Input**: Full Yuque path ending with change name.
Format: `/docs:sync 产品中心/组织中心/设计方案/organization-center`

**Path parsing rule**: Last segment = change name, all preceding segments = target Yuque directory.

---

## Global Mappings (Reference only - user path overrides)

### Slug Naming Strategy
| Doc Type | Slug Format | Example (change=organization-management) |
|----------|-------------|------------------------------------------|
| proposal | `<change>-proposal` | `organization-management-proposal` |
| design | `<change>-design` | `organization-management-design` |
| tasks | `<change>-tasks` | `organization-management-tasks` |
| api | `<change>-api` | `organization-management-api` |
| tests | `<change>-tests` | `organization-management-tests` |

> **Note**: Yuque slug is **repository-level unique**, not path-level. Using simple names like `proposal` will conflict with existing docs. Always use prefixed unique slugs.

### Change Name ↔ Yuque Path (Default mapping for convenience)
| Change Name | Default Yuque Path |
|-------------|-------------------|
| `knowledge-graph-ui` | `产品中心/知识图谱页面化/knowledge-graph-ui` |
| `knowledge-graph-datasource` | `产品中心/知识图谱页面化/knowledge-graph-datasource` |
| `organization-center` | `产品中心/组织中心` |
| ... | ... |

> **Note**: If user provides full path, it overrides default mapping.

---

## Steps

1. **Parse input path**

   Extract path segments from arguments.

   Example: `产品中心/组织中心/设计方案/organization-center`
   - **Target Yuque directory**: `产品中心/组织中心/设计方案`
   - **Change name**: `organization-center`

   If no path provided:
   - List files in `openspec/changes/` directory
   - Use **AskUserQuestion tool** to let user select change AND specify target path

   Always announce:
   ```
   Syncing change: <change_name>
   Target Yuque directory: <yuque_directory>
   ```

2. **Read local artifacts**

   Read all available artifacts from `openspec/changes/<change_name>/`:
   - `proposal.md`
   - `design.md`
   - `tasks.md`
   - `api.md`
   - `test.md`
   - `specs/*/spec.md` (if any)

   Report which files found.

3. **Get Yuque TOC and create directories if needed**

   Call `mcp__yuque-mcp__yuque_get_toc` with `repo_id: "zhangmin-jrrer/iu9s4m"`

   Navigate TOC to find/create target directory hierarchy:
   - Split target directory by `/` (e.g., `产品中心/组织中心/设计方案`)
   - For each level, check if node exists with matching `title`
   - If exists, use its `uuid` as parent for next level
   - If NOT exists, create via `mcp__yuque-mcp__yuque_update_toc`:

   ```json
   {
     "repo_id": "zhangmin-jrrer/iu9s4m",
     "toc_data": "{\"action\":\"appendNode\",\"action_mode\":\"child\",\"target_uuid\":\"<parent_uuid>\",\"type\":\"TITLE\",\"title\":\"<directory_name>\"}"
   }
   ```

   Repeat until full path hierarchy created/found.
   Store final directory `uuid` for document placement.

4. **Check existing documents**

   Call `mcp__yuque-mcp__yuque_list_docs` with `repo_id: "zhangmin-jrrer/iu9s4m"`

   Check if docs with target slugs exist under the target directory path.

5. **Sync each artifact**

   For each artifact file:

   Generate unique slug: `<change_name>-<doc_type>` (e.g., `organization-management-proposal`)

   **If document exists** (by slug): Call `mcp__yuque-mcp__yuque_update_doc`:
   ```json
   {
     "repo_id": "zhangmin-jrrer/iu9s4m",
     "doc_id": "<existing_doc_id_or_slug>",
     "body": "<markdown_content>"
   }
   ```

   **If document NOT exists**: Call `mcp__yuque-mcp__yuque_create_doc`:
   ```json
   {
     "repo_id": "zhangmin-jrrer/iu9s4m",
     "title": "<doc_type>",
     "slug": "<change_name>-<doc_type>",
     "body": "<markdown_content>",
     "format": "markdown"
   }
   ```

   Store the returned `doc_id` and get doc `uuid` from TOC for each created doc.

   Handle failures:
   - API error 4xx/5xx: retry once
   - If retry fails: record error, continue to next doc
   - Track success/failure status for each doc

6. **Organize docs under target directory (if new docs created)**

   For newly created docs, move them under target directory via `mcp__yuque-mcp__yuque_update_toc`:

   **Use `appendNode` action with `node_uuid` parameter** (NOT moveNode):
   ```json
   {
     "repo_id": "zhangmin-jrrer/iu9s4m",
     "toc_data": "{\"action\":\"appendNode\",\"action_mode\":\"child\",\"target_uuid\":\"<directory_uuid>\",\"node_uuid\":\"<doc_uuid>\"}"
   }
   ```

   > **Important**: Yuque does NOT have `moveNode` action. Use `appendNode` with `node_uuid` to move existing document nodes.

7. **Report results**

---

## Output On Success

```
## Sync Complete

**Change:** <change_name>
**Yuque Directory:** <yuque_directory>
**Documents synced:** <N> of <M>

| Type | Status | URL |
|------|--------|-----|
| proposal | ✓ created & moved | https://www.yuque.com/zhangmin-jrrer/iu9s4m/<change>-proposal |
| design | ✓ created & moved | https://www.yuque.com/zhangmin-jrrer/iu9s4m/<change>-design |
| tasks | ✓ created & moved | https://www.yuque.com/zhangmin-jrrer/iu9s4m/<change>-tasks |
| api | skipped (not found locally) | - |
| tests | ✗ failed: API error 500 | - |
```

## Output On Error

```
## Sync Failed

**Change:** <change_name>
**Error:** <error_message>

Details:
- Failed to create directory: <reason>
- Or: All document syncs failed
```

---

## Guardrails

- Parse path: last segment = change name, rest = target directory
- User-provided path overrides default mapping table
- **Slug naming**: Use `<change_name>-<doc_type>` format for unique repository-level slugs
- Yuque slug is **repository-level unique**, NOT path-level unique
- Skip artifacts that don't exist locally (report as "skipped")
- Create directory hierarchy level-by-level if not exists
- **TOC move**: Use `appendNode` action with `node_uuid` parameter (NOT moveNode)
- Report each sync result individually
- Content must be raw markdown
- API failure: retry once → record error → continue
- Preserve document history when updating