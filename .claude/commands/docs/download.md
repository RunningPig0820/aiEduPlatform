---
name: "DOCS: Download"
description: Download OpenSpec artifacts from Yuque via Yuque MCP tools and save locally
category: Workflow
tags: [workflow, openspec, yuque, download]
---

Download OpenSpec artifacts from Yuque and save to local directory.

**Yuque Repository**: `zhangmin-jrrer/iu9s4m` (智启学堂)

**Input**: Yuque path and optionally doc_type.
Formats:
- `/docs:download 产品中心/组织中心` (download all types)
- `/docs:download 产品中心/组织中心 tasks` (download single type)

---

## Global Mappings (Reference only - derive from input)

### Slug Naming Strategy
| Doc Type | Slug Format | Example (change=organization-management) |
|----------|-------------|------------------------------------------|
| proposal | `<change>-proposal` | `organization-management-proposal` |
| design | `<change>-design` | `organization-management-design` |
| tasks | `<change>-tasks` | `organization-management-tasks` |
| api | `<change>-api` | `organization-management-api` |
| tests | `<change>-tests` | `organization-management-tests` |

> **Note**: Yuque slug is **repository-level unique**, not path-level. Always use prefixed unique slugs.

### Doc Type ↔ Local File Mapping
| Doc Type | Local File |
|----------|------------|
| proposal | `proposal.md` |
| design | `design.md` |
| tasks | `tasks.md` |
| api | `api.md` |
| tests | `test.md` |

> **Note**: Change name derived from Yuque path's last segment.

---

## Steps

1. **Parse input**

   Extract Yuque path and optional doc_type.

   Yuque path: full path like `产品中心/组织中心/organization-center`
   - Last segment → change name
   - Rest → Yuque directory context

   If doc_type specified: download only that type.
   If doc_type omitted: download all (proposal, design, tasks, api, tests).

2. **Derive change name from path**

   Extract last segment from Yuque path as change name.

   Example: `产品中心/组织中心/organization-center` → change name = `organization-center`

3. **Find documents in Yuque**

   Call `mcp__yuque-mcp__yuque_list_docs` with `repo_id: "zhangmin-jrrer/iu9s4m"`

   Find documents with slug `<change_name>-<doc_type>`:
   - Match by slug (e.g., `organization-management-proposal`)
   - Store doc_ids for download

4. **Download via Yuque MCP**

   For each doc_type to download:

   Call `mcp__yuque-mcp__yuque_get_doc`:
   ```json
   {
     "repo_id": "zhangmin-jrrer/iu9s4m",
     "doc_id": "<doc_id_or_slug>",
     "include_lake": false
   }
   ```

   Handle failures:
   - 404: record as "not found"
   - 5xx: retry once → record error if fails

5. **Save to local directory**

   For each successfully downloaded document:

   Write to `openspec/changes/<change_name>/<local_file>`:
   - Use doc_type → local file mapping
   - Note: `tests` → `test.md`

   Create directory `openspec/changes/<change_name>/` if not exists.

---

## Output

```
## Download Complete

**Yuque Path:** <yuque_path>
**Change Name:** <change_name>
**Documents downloaded:** <N> of <M>

| Type | Status | Local Path |
|------|--------|------------|
| proposal | ✓ saved | openspec/changes/<name>/proposal.md |
| design | ✓ saved | openspec/changes/<name>/design.md |
| tasks | ✓ saved | openspec/changes/<name>/tasks.md |
| api | ✗ not found | - |
| tests | ✗ API error 500 | - |
```

## Output On All Failed

```
## Download Failed

**Yuque Path:** <yuque_path>
**Error:** No documents could be downloaded

Details:
- proposal: 404 not found
- design: 404 not found
...
```

---

## Guardrails

- Derive change name from Yuque path (last segment)
- **Slug naming**: Use `<change_name>-<doc_type>` format for unique repository-level slugs
- Yuque slug is **repository-level unique**, NOT path-level unique
- Doc type `tests` → local file `test.md`
- If document not found, report as "not found", don't error
- If downloading all and some fail: report partial success
- Save to local file (not just display)
- Create local directory if needed
- API failure: retry once → record → continue