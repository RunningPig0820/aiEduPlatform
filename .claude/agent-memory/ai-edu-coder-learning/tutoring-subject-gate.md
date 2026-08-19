---
name: tutoring-subject-gate
description: 答疑学科门（subject-classify）Java 侧已实现，失败放行 math、图片预检双上传、SSE meta.subject 契约收尾、Python 端点待联调
metadata:
  type: project
---

# tutoring-subject-gate Java 侧（tasks 2.1-2.6 + meta.subject 契约收尾）已完成

OpenSpec change `tutoring-subject-gate` 任务 2 已于 2026-08-19 落地（TDD，全量 852 测试绿）；2026-08-19 契约收尾补 `SseMetaDTO.subject`（全量 855 测试绿）。

**Why:** 答疑 decide 是数学专用提示词，物理/化学题需在 decide 之前判学科并跳过，避免数学人设污染 + 非数学题落库。前端「学科分析」行需要学科来源。

**How to apply:**

- **SSE meta.subject 契约**：正常答疑轮（拍题建会话/发消息/请求答案/换题走 switch）`buildMeta` 带 `.subject(session.getSubject())`（真实 classify 值，值域 math/physics/chemistry/biology/other）；非数学跳过流 `subjectHintStream` **不带**（subject=null，前端只展示提示语、隐藏学科行）；可空字段，缺失不抛异常。
- **失败哲学（fail-open to math）在两层实施**：`SubjectClassifyClient`（infra）catch 异常→空结果；`TutoringAppService.classifySafely` 再兜底 catch→null→`subjectAllowed` 放行。宁可漏拦非数学题，不误拦数学题。
- **拍题图片门控的「双上传」权衡**：sessionId 在建会话前不存在，拍题图片先传 `tutoring/questions/{studentId}/subject-check/` 供分类器看图；math 放行后正常流程再按会话路径传一次。非 math 时 subject-check 对象成为 COS 孤儿（预期副作用，未清理）。
- **`ensureCreateAllowed` 移到学科门之后**：非 math 跳过不消耗会话创建配额（避免连续发物理题触发 50004）。
- **端口可空**：`subjectClassifyPort == null` = 分类不可用 = math 放行，既有单测不 mock 该端口仍绿。
- **Python 端点未交付**：subject-classify 是 Python change 任务 1（另一 Agent 交付），Java 桥按契约实现 + mock 测试，联调在 change 任务 3。联调前 Java 端始终 fail-open。

相关：[[ai-tutoring-architecture]]
