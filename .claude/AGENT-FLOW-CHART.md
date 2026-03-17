# Agent 与 Skill 流程全图

> 更新日期：2026-03-17
> 核心原则：**所有 agent-skill 必须显式调用**

---

## 一、目录结构总览

```
.claude/
├── skills/                              # 用户可调用 Skill（通过 Skill tool）
│   ├── brainstorming/                   # 需求引导
│   ├── main-agent-guard/                # 主 Agent 行为限制
│   ├── test-driven-development/         # TDD 开发流程（含 Java/Maven）
│   ├── verification-before-completion/  # 完成前验证（含 Java/Maven）
│   ├── writing-plans/                   # 编写计划
│   ├── writing-skills/                  # 编写 Skill
│   ├── using-superpowers/               # 技能使用指南
│   ├── using-git-worktrees/             # Git worktrees
│   ├── requesting-code-review/          # 请求代码审查
│   └── receiving-code-review/           # 接收代码审查
│
└── agents/
    ├── agent-skills/                    # Subagent 内部规范（显式调用）
    │   ├── error-reporting.md           # 错误报告格式
    │   ├── dispatching-parallel-agents/ # 并行派发流程
    │   ├── executing-plans/             # 执行计划
    │   ├── finishing-a-development-branch/ # 完成开发分支
    │   ├── subagent-driven-development/ # 子代理驱动开发
    │   └── systematic-debugging/        # 系统化调试
    │
    ├── ai-edu-architect-design.md       # 设计角色
    ├── ai-edu-architect-coordinator.md  # 协调角色
    ├── ai-edu-coder-user.md             # 用户领域开发
    ├── ai-edu-coder-homework.md         # 作业领域开发
    ├── ai-edu-coder-question.md         # 题库领域开发
    ├── ai-edu-coder-learning.md         # 学习领域开发
    └── ai-edu-coder-organization.md     # 组织领域开发
```

---

## 二、分类定义

### Skills（用户可调用）

| Skill | 用途 | 调用方式 |
|-------|------|----------|
| `brainstorming` | 需求引导 | `Skill tool` |
| `main-agent-guard` | 主 Agent 行为限制 | `Skill tool` |
| `test-driven-development` | TDD 开发流程 | `Skill tool` |
| `verification-before-completion` | 完成前验证 | `Skill tool` |
| `writing-plans` | 编写计划 | `Skill tool` |
| `writing-skills` | 编写 Skill | `Skill tool` |
| `using-superpowers` | 技能使用指南 | `Skill tool` |
| `using-git-worktrees` | Git worktrees | `Skill tool` |
| `requesting-code-review` | 请求代码审查 | `Skill tool` |
| `receiving-code-review` | 接收代码审查 | `Skill tool` |

### Agent-Skills（Subagent 内部规范）

| 规范 | 用途 | 调用方式 |
|------|------|----------|
| `error-reporting` | 错误报告格式 | `Read tool` |
| `dispatching-parallel-agents` | 并行派发流程 | `Read tool` |
| `executing-plans` | 执行计划流程 | `Read tool` |
| `finishing-a-development-branch` | 完成开发分支 | `Read tool` |
| `subagent-driven-development` | 子代理驱动开发 | `Read tool` |
| `systematic-debugging` | 系统化调试 | `Read tool` |

---

## 三、完整开发流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           用户请求                                        │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  主 Agent                                                                 │
│                                                                          │
│  【必须调用 Skill】                                                       │
│  → main-agent-guard（请求类型检查）                                       │
│                                                                          │
│  【允许操作】                                                             │
│  ✅ 对话、解释、回答问题                                                   │
│  ✅ 调度 subagent                                                         │
│  ✅ 审核结果                                                              │
│                                                                          │
│  【禁止操作】                                                             │
│  ❌ 直接编写代码                                                          │
│  ❌ 直接运行测试                                                          │
│  ❌ 直接修改文件（除文档外）                                               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          │                         │                         │
          ▼                         ▼                         ▼
    【设计请求】              【编码请求】              【Bug修复】
          │                         │                         │
          ▼                         ▼                         ▼
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│ brainstorming   │      │ 直接派发        │      │ 直接派发        │
│ (Skill tool)    │      │ ai-edu-coder-*  │      │ ai-edu-coder-*  │
└─────────────────┘      └─────────────────┘      └─────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  ai-edu-architect-design（设计 Agent）                                   │
│                                                                          │
│  【输出】                                                                 │
│  → 设计文档：docs/plans/<topic>/<topic>-design.md                        │
│  → API 文档：docs/plans/<topic>/<topic>-api.md                           │
│  → 测试文档：docs/plans/<topic>/<topic>-test.md                          │
│                                                                          │
│  【等待】人工审核 ✅                                                       │
└─────────────────────────────────────────────────────────────────────────┘
          │
          │ 审核通过
          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  ai-edu-architect-coordinator（协调 Agent）                              │
│                                                                          │
│  【必须显式调用】                                                         │
│  → Read: agent-skills/dispatching-parallel-agents/SKILL.md              │
│  → Read: agent-skills/error-reporting.md                                │
│  → Skill: verification-before-completion                                │
│                                                                          │
│  【职责】                                                                 │
│  → 读取设计文档                                                          │
│  → 制定执行计划                                                          │
│  → 任务分配给 coder subagent                                             │
│  → 监控进度                                                              │
│  → 验收与集成                                                            │
└─────────────────────────────────────────────────────────────────────────┘
          │
          │ Agent tool 派发
          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  ai-edu-coder-*（编码 Agent）                                            │
│                                                                          │
│  【必须显式调用】                                                         │
│  → Skill: test-driven-development                                       │
│  → Skill: verification-before-completion                                │
│  → Read: agent-skills/error-reporting.md                                │
│                                                                          │
│  【TDD 流程】                                                             │
│  1. 写失败测试                                                            │
│  2. 写最小代码通过                                                        │
│  3. 重构                                                                 │
│  4. 验证覆盖率 ≥ 80%                                                      │
│                                                                          │
│  【完成前验证】                                                           │
│  → mvn clean compile                                                     │
│  → mvn test                                                              │
│  → mvn jacoco:report                                                     │
│                                                                          │
│  【错误报告】                                                             │
│  → 发现问题时按 error-reporting.md 格式报告                               │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 四、Agent 调用矩阵

### ai-edu-architect-design

| 类型 | 调用项 | 方式 |
|------|--------|------|
| 无需调用 | - | 设计角色只负责输出文档 |

**启动响应：**
```
架构师设计已就绪，正在输出设计文档...
```

---

### ai-edu-architect-coordinator

| 类型 | 调用项 | 方式 | 用途 |
|------|--------|------|------|
| Skill | `verification-before-completion` | Skill tool | 验收阶段验证 |
| 内部规范 | `dispatching-parallel-agents` | Read tool | 并行派发流程 |
| 内部规范 | `error-reporting` | Read tool | 错误报告格式 |

**启动响应：**
```
架构师协调已就绪
已准备：
- dispatching-parallel-agents (内部规范)
- verification-before-completion (Skill)
- error-reporting.md (内部规范)

正在读取设计文档并制定执行计划...
```

---

### ai-edu-coder-*（所有编码 Agent）

| 类型 | 调用项 | 方式 | 用途 |
|------|--------|------|------|
| Skill | `test-driven-development` | Skill tool | TDD 开发流程 |
| Skill | `verification-before-completion` | Skill tool | 完成前验证 |
| 内部规范 | `error-reporting` | Read tool | 错误报告格式 |

**启动响应：**
```
[领域] Agent 已就绪
已调用：
- test-driven-development (Skill)
- verification-before-completion (Skill)
- error-reporting.md (内部规范)

准备开始 TDD 开发...
```

---

## 五、Skill 与 Agent-Skill 区分

### Skills（通过 Skill tool 调用）

**特点：**
- 用户可通过 `/skill-name` 直接调用
- 主 Agent 和 Subagent 都可以调用
- 是可复用的流程规范

**调用方式：**
```
Skill tool: "test-driven-development"
```

### Agent-Skills（通过 Read tool 调用）

**特点：**
- 仅 Subagent 内部使用
- 是具体的行为规范
- 必须显式 Read

**调用方式：**
```
Read tool: ".claude/agents/agent-skills/error-reporting.md"
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

## 七、验证检查清单

### 主 Agent 自检

```
□ 是否调用了 main-agent-guard？
□ 这个请求涉及编码吗？
  └─ 是 → 引导用户使用 subagent
  └─ 否 → 继续
□ 这个请求涉及设计吗？
  └─ 是 → 引导用户先 brainstorming
  └─ 否 → 继续
□ 我是否在直接编写代码？
  └─ 是 → 停止，重新引导
  └─ 否 → 继续
```

### ai-edu-architect-coordinator 自检

```
□ 是否显式读取了 dispatching-parallel-agents/SKILL.md？
□ 是否显式读取了 error-reporting.md？
□ 是否调用了 verification-before-completion skill？
□ 是否按设计文档任务拆分派发？
□ 是否监控了所有 subagent 执行状态？
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

## 八、典型场景示例

### 场景1：新功能开发

```
用户: "帮我实现用户登录功能"

主 Agent:
1. 调用 main-agent-guard skill
2. 检测到设计请求 → 引导使用 brainstorming

主 Agent:
1. 调用 brainstorming skill
2. 引导用户澄清需求
3. 调用 ai-edu-architect-design

ai-edu-architect-design:
1. 输出设计文档、API文档、测试文档
2. 等待人工审核

人工审核通过后:

主 Agent:
1. 调用 ai-edu-architect-coordinator

ai-edu-architect-coordinator:
1. Read: dispatching-parallel-agents/SKILL.md
2. Read: error-reporting.md
3. Skill: verification-before-completion
4. 派发任务给 ai-edu-coder-user

ai-edu-coder-user:
1. Skill: test-driven-development
2. Skill: verification-before-completion
3. Read: error-reporting.md
4. 执行 TDD 开发
5. 输出验证报告
```

### 场景2：Bug 修复

```
用户: "登录接口报错了"

主 Agent:
1. 调用 main-agent-guard skill
2. 检测到 Bug 修复请求 → 引导使用 ai-edu-coder-user

主 Agent:
1. 调用 ai-edu-coder-user

ai-edu-coder-user:
1. Skill: test-driven-development
2. Skill: verification-before-completion
3. Read: error-reporting.md
4. TDD 修复流程：
   - 写测试重现 Bug
   - 修复代码
   - 验证通过
5. 输出验证报告
```

### 场景3：多领域并行开发

```
设计文档任务拆分：
1. User - 用户认证接口（无依赖）
2. Question - 题目管理接口（无依赖）

ai-edu-architect-coordinator:
1. Read: dispatching-parallel-agents/SKILL.md
2. 识别可并行任务
3. 并行派发 ai-edu-coder-user 和 ai-edu-coder-question
4. 监控所有任务完成
5. Skill: verification-before-completion
6. 验收集成
```

---

## 九、更新记录

| 日期 | 修改内容 | 修改者 |
|------|----------|--------|
| 2026-03-17 | 初版创建，完整流程梳理 | Claude |