# Agent 与 Skill 流程全图

> 更新日期：2026-03-19
> 核心原则：**OpenSpec 主流程 + TDD 开发**

---

## 一、目录结构总览

```
.claude/
├── skills/                              # 用户可调用 Skill（通过 Skill tool）
│   ├── main-agent-guard/                # 主 Agent 行为限制
│   ├── test-driven-development/         # TDD 开发流程
│   ├── verification-before-completion/  # 完成前验证
│   ├── using-git-worktrees/             # Git worktrees（可选工具）
│   ├── openspec-explore/                # OpenSpec 探索
│   ├── openspec-propose/                # OpenSpec 提案
│   ├── openspec-apply-change/           # OpenSpec 实现
│   └── openspec-archive-change/         # OpenSpec 归档
│
└── agents/
    ├── agent-skills/                    # Subagent 内部规范
    │   ├── error-reporting.md           # 错误报告格式
    │   └── systematic-debugging/        # 系统化调试
    │
    ├── ai-edu-coder-common.md           # 公共组件开发
    ├── ai-edu-coder-user.md             # 用户领域开发
    ├── ai-edu-coder-homework.md         # 作业领域开发
    ├── ai-edu-coder-question.md         # 题库领域开发
    ├── ai-edu-coder-learning.md         # 学习领域开发
    └── ai-edu-coder-organization.md     # 组织领域开发
```

---

## 二、OpenSpec 主流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           用户请求                                        │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  主 Agent (main-agent-guard)                                             │
│                                                                          │
│  【允许操作】                                                             │
│  ✅ 对话、解释、回答问题                                                   │
│  ✅ 调度 openspec skills                                                 │
│  ✅ 审核 subagent 结果                                                    │
│                                                                          │
│  【禁止操作】                                                             │
│  ❌ 直接编写代码                                                          │
│  ❌ 直接运行测试                                                          │
│  ❌ 绕过流程直接开发                                                       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  openspec-explore                                                        │
│                                                                          │
│  【职责】                                                                 │
│  → 探索需求、澄清问题                                                     │
│  → 调查代码库、比较方案                                                   │
│  → 不编写代码                                                            │
│                                                                          │
│  【输出】                                                                 │
│  → 可选：创建/更新 OpenSpec artifacts                                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  openspec-propose                                                        │
│                                                                          │
│  【输出 Artifacts】                                                       │
│  → proposal.md  - 为什么做、做什么                                        │
│  → specs/**/*.md - 详细规格                                              │
│  → design.md    - 技术设计                                               │
│  → tasks.md     - 任务清单                                               │
│  → api.md       - API文档（前端联调）                                     │
│  → test.md      - 测试用例（自动化测试）                                   │
│                                                                          │
│  【保存路径】                                                             │
│  → openspec/changes/<change-name>/                                       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  openspec-apply-change                                                   │
│                                                                          │
│  【执行流程】                                                             │
│  1. 读取所有 artifacts                                                   │
│  2. 按 tasks.md 执行任务                                                 │
│  3. 派发 ai-edu-coder-* subagent                                         │
│  4. 使用 TDD 开发                                                        │
│  5. 标记任务完成                                                         │
│                                                                          │
│  【必须调用 Skill】                                                       │
│  → test-driven-development                                              │
│  → verification-before-completion                                       │
│                                                                          │
│  【必须读取内部规范】                                                      │
│  → agent-skills/error-reporting.md                                      │
│  → agent-skills/systematic-debugging/SKILL.md (如遇Bug)                 │
│                                                                          │
│  【派发目标】                                                             │
│  → ai-edu-coder-user      (用户域)                                       │
│  → ai-edu-coder-question  (题库域)                                       │
│  → ai-edu-coder-homework  (作业域)                                       │
│  → ai-edu-coder-learning  (学习域)                                       │
│  → ai-edu-coder-organization (组织域)                                    │
│  → ai-edu-coder-common    (公共组件)                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  openspec-archive-change                                                 │
│                                                                          │
│  【验证流程】                                                             │
│  → 检查 artifacts 完成状态                                               │
│  → 检查 tasks 完成状态                                                   │
│  → 同步 delta specs 到 main specs                                        │
│                                                                          │
│  【归档】                                                                 │
│  → 移动到 openspec/changes/archive/YYYY-MM-DD-<name>/                   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 三、Bounded Context 映射

| Context | 负责领域 | 对应 Subagent |
|---------|----------|---------------|
| **User** | 用户、权限、认证 | `ai-edu-coder-user` |
| **Question** | 题库、知识点 | `ai-edu-coder-question` |
| **Homework** | 作业、批改 | `ai-edu-coder-homework` |
| **Learning** | 错题本、掌握度 | `ai-edu-coder-learning` |
| **Organization** | 学校、班级 | `ai-edu-coder-organization` |
| **Common** | 跨领域基础设施 | `ai-edu-coder-common` |

---

## 四、OpenSpec Artifacts 说明

### ai-edu Schema Artifacts

| Artifact | 文件 | 说明 | 依赖 |
|----------|------|------|------|
| proposal | proposal.md | 为什么做、做什么 | 无 |
| specs | specs/**/*.md | 详细规格（每个能力一个） | proposal |
| design | design.md | 技术设计方案 | proposal |
| tasks | tasks.md | 实施任务清单 | specs, design |
| api | api.md | API文档（前端联调） | design, tasks |
| test | test.md | 测试用例（自动化测试） | api |

---

## 五、ai-edu-coder-* Subagent 职责

### 共同职责

所有 coder subagent 必须遵循：

```
【必须调用 Skill】
→ test-driven-development (TDD 开发)
→ verification-before-completion (完成前验证)

【必须读取内部规范】
→ agent-skills/error-reporting.md (错误报告格式)

【TDD 流程】
1. 写失败测试
2. 写最小代码通过
3. 重构
4. 验证覆盖率 ≥ 80%

【完成前验证】
→ mvn clean compile
→ mvn test
→ mvn jacoco:report
```

### 启动响应

```
[领域] Agent 已就绪
已调用：
- test-driven-development (Skill)
- verification-before-completion (Skill)
- error-reporting.md (内部规范)

准备开始 TDD 开发...
```

---

## 六、错误报告流程

所有 Subagent 发现问题时，必须按 `error-reporting.md` 格式报告：

```markdown
⚠️ 问题报告

**问题类型：** [设计问题/接口问题/依赖问题/实现问题/测试问题]

**问题描述：**
[清晰描述发现的问题]

**影响范围：**
[说明问题影响哪些功能或模块]

**建议方案：**
[提供可能的解决方案]

**是否需要人工介入：** [是/否]
```

---

## 七、关联矩阵

```
                     ┌────────────────────────────────────────────────────┐
                     │                    Skills                           │
                     ├─────────┬─────────┬─────────┬─────────┬────────────┤
                     │ main-   │ TDD     │ verify  │ git-    │ openspec   │
                     │ guard   │         │ -before │ worktree│            │
─────────────────────┼─────────┼─────────┼─────────┼─────────┼────────────┤
main-agent-guard     │    -    │    -    │    -    │    -    │     ✅     │
                     │         │         │         │ (可选)  │  (引导到)  │
─────────────────────┼─────────┼─────────┼─────────┼─────────┼────────────┤
ai-edu-coder-*       │    -    │    ✅   │    ✅   │    -    │     -     │
                     │         │ (必须)  │ (必须)  │         │            │
─────────────────────┼─────────┼─────────┼─────────┼─────────┼────────────┤
openspec-apply       │    -    │    ✅   │    ✅   │    ⚪   │     -     │
-change              │         │ (必须)  │ (必须)  │ (可选)  │            │
─────────────────────┼─────────┼─────────┼─────────┼─────────┼────────────┤
systematic-debugging │    -    │    ✅   │    ✅   │    -    │     -     │
                     │         │ (引用)  │ (引用)  │         │            │
└─────────────────────┴─────────┴─────────┴─────────┴─────────┴────────────┘

✅ = 必须调用
⚪ = 可选调用
```

---

## 八、验证检查清单

### 主 Agent 自检

```
□ 是否调用了 main-agent-guard？
□ 这个请求涉及编码吗？
  └─ 是 → 引导用户使用 openspec 流程
  └─ 否 → 继续
□ 我是否在直接编写代码？
  └─ 是 → 停止，重新引导
  └─ 否 → 继续
```

### ai-edu-coder-* 自检

```
□ 是否调用了 test-driven-development skill？
□ 是否调用了 verification-before-completion skill？
□ 是否显式读取了 error-reporting.md？
□ 是否按照 TDD 流程执行？
□ 是否在完成前进行了验证？
□ 是否输出了验证报告？
□ 发现问题时是否报告了？
```

---

## 九、典型场景示例

### 场景1：新功能开发

```
用户: "帮我实现用户登录功能"

主 Agent:
1. 调用 main-agent-guard skill
2. 检测到编码请求 → 引导使用 openspec 流程

openspec-explore:
1. 探索项目背景
2. 澄清需求细节
3. 建议创建提案

openspec-propose:
1. 创建 proposal.md
2. 创建 specs/user-auth/spec.md
3. 创建 design.md
4. 创建 tasks.md
5. 创建 api.md
6. 创建 test.md

openspec-apply-change:
1. 读取所有 artifacts
2. 派发 ai-edu-coder-user
3. 执行 TDD 开发
4. 验证完成

openspec-archive-change:
1. 验证所有任务完成
2. 同步 specs
3. 归档到 archive/
```

### 场景2：Bug 修复

```
用户: "登录接口报错了"

主 Agent:
1. 调用 main-agent-guard skill
2. 检测到 Bug 修复请求

主 Agent:
1. 建议使用 openspec-explore 调查问题
2. 或直接派发 ai-edu-coder-user

ai-edu-coder-user:
1. 调用 test-driven-development skill
2. 调用 systematic-debugging (如需深入调查)
3. 写测试重现 Bug
4. 修复代码
5. 调用 verification-before-completion 验证
```

---

## 十、更新记录

| 日期 | 修改内容 | 修改者 |
|------|----------|--------|
| 2026-03-17 | 初版创建，完整流程梳理 | Claude |
| 2026-03-18 | 新增 ai-edu-coder-common 公共组件 Agent | Claude |
| 2026-03-19 | 切换到 OpenSpec 主流程，删除冗余 skills/agents | Claude |
| 2026-03-19 | 更新关联矩阵，修复引用问题 | Claude |