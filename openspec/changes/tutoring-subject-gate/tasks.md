# tutoring-subject-gate 实施任务

## 1. Python：subject-classify 端点

- [ ] 1.1 新增 `subject-classify` 端点（stateless）：请求 `{content, image_url}`（至少一个非空）、响应 `{subject}`（闭集 math/physics/chemistry/biology/other）
- [ ] 1.2 学科无关提示词：只判学科不做解题；"图片无法辨认/拿不准 → other/不误拦"
- [ ] 1.3 文本 + 图片双通道：无图纯文本 HumanMessage，有图多模态（复用 decide 看图路径）
- [ ] 1.4 模型统一：`doubao-seed-2-0-mini-260428`，temp 0.3（与 decide/understand 同款）
- [ ] 1.5 绝不抛异常：失败/超时 → 空结果（Java 按 math 放行）
- [ ] 1.6 TDD：文本物理题→physics、图片题→学科、纯文本数学题→math、失败→空结果

## 2. Java：学科分流（decide 之前）

- [x] 2.1 新增 subject-classify 契约 DTO + Python 桥（对齐 understand/vector 桥模式）
- [x] 2.2 拍题（建会话）：先 subject-classify → math 才建会话（subject 传真实值）；非 math 不建，返回「仅支持数学」
- [x] 2.3 换题（消息带新图）：新图先 subject-classify → 非 math 跳过该题（不结算/不记录）返回提示；math 正常走 switch 结算
- [x] 2.4 失败降级：classify 异常/超时 → 按 math 放行，不阻断答疑
- [x] 2.5 会话 `TutoringSession.start(studentId, subject)` 传 classify 结果
- [x] 2.6 TDD：拍题物理跳过（不建会话/不记录/返回提示）、换题非数学跳过、数学放行、失败降级放行、重复发送幂等

## 3. 联调与回归

- [ ] 3.1 端到端联调：物理题（文本/图片）→ 「仅支持数学」；数学题全流程回归（落库/掌握度正常）
- [ ] 3.2 全量测试全绿
