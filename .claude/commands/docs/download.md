---
name: "DOCS: Download"
description: Download OpenSpec artifacts from Yuque via OpenClaw session
category: Workflow
tags: [workflow, openspec, yuque, download]
---

Download OpenSpec artifacts from Yuque via OpenClaw session.

**⚠️ Requires OpenClaw `docs:get` command — not yet implemented.**

**Gateway Configuration**
- URL: `http://114.132.222.92:22867`
- Session Key: `openclaw-control-ui`

**Input**: Yuque path and optionally doc_type (e.g., `/docs:download 产品中心/组织中心/organization-management tasks`).

## Steps

1. **Determine Yuque path and doc_type**

   If doc_type specified, download only that type. Otherwise download all:
   `proposal | design | tasks | api | tests`

2. **Request download via session message**

   ```bash
   curl -s -X POST http://114.132.222.92:22867/api/sessions/send \
     -H "Authorization: Bearer <token>" \
     -d '{
       "sessionKey": "openclaw-control-ui",
       "message": "docs:get <yuque_path> <doc_type>"
     }'
   ```

3. **Display content**

   OpenClaw returns the markdown content. Show it.

## Guardrails
- This command requires OpenClaw to implement `docs:get`
- Until implemented, inform user the method is unavailable
