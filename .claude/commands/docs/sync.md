---
name: "DOCS: Sync"
description: Sync OpenSpec artifacts to Yuque via OpenClaw session
category: Workflow
tags: [workflow, openspec, yuque, sync]
---

Sync OpenSpec change artifacts to Yuque via OpenClaw session message API.

**Gateway Configuration**
- URL: `http://114.132.222.92:22867`
- Session Key: `openclaw-control-ui`

**Input**: Change name (e.g., `/docs:sync organization-management`). If omitted, list available changes and prompt.

## Steps

1. **Select the change**

   If a name is provided, use it. Otherwise:
   - Run `openspec list --json` to get active changes
   - Use **AskUserQuestion tool** to let the user select

   Always announce: "Syncing change: <name>"

2. **Read local artifacts**

   Read all available artifacts from `openspec/changes/<name>/`:
   - `proposal.md`
   - `design.md`
   - `tasks.md`
   - `api.md`
   - `test.md`
   - `specs/*/spec.md` (if any)

3. **Determine Yuque path**

   Map the change name to a Yuque path. Infer from context:
   - `organization-management` → `产品中心/组织中心/organization-management`
   - `knowledge-graph-ui` → `产品中心/知识图谱页面化/knowledge-graph-ui`
   - `knowledge-graph-datasource` → `产品中心/知识图谱页面化/knowledge-graph-datasource`
   - `llm-gateway-integration` → `产品中心/llm对接/llm-gateway-integration`

   If uncertain, use **AskUserQuestion** to ask:
   > "这个变更属于哪个语雀目录？"

4. **Sync each artifact via session message**

   For each artifact, send a message to OpenClaw session:

   ```bash
   curl -s -X POST http://114.132.222.92:22867/api/sessions/send \
     -H "Authorization: Bearer a844da9d5750ef67f0f71612756ff900b21684e961f27bc8" \
     -H "Content-Type: application/json" \
     -d '{
       "sessionKey": "openclaw-control-ui",
       "message": "docs:sync <yuque_path> <doc_type>\n<markdown_content>"
     }'
   ```

   Map doc_type to artifact:
   - `proposal.md` → `proposal`
   - `design.md` → `design`
   - `tasks.md` → `tasks`
   - `api.md` → `api`
   - `test.md` → `tests`

   OpenClaw behavior:
   - If Yuque directory doesn't exist → auto-creates it
   - If document doesn't exist → auto-creates it
   - Returns Yuque URL

5. **Report results**

   For each synced doc, show the Yuque URL returned by the API.

## Output On Success

```
## Sync Complete

**Change:** <change-name>
**Yuque Path:** <yuque_path>
**Documents synced:**
- proposal: <yuque_url>
- design: <yuque_url>
- tasks: <yuque_url>
- api: <yuque_url>
- tests: <yuque_url>
```

## Output On Error

```
## Sync Failed

**Change:** <change-name>
**Error:** <error_message>
```

## Guardrails
- Read all local artifacts before syncing
- Skip artifacts that don't exist (don't error on missing files)
- Report each sync result individually
- Escape newlines in content properly for JSON payload
- Content must be raw markdown, not HTML
