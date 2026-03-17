# Agent 与 Skill 使用规范

> 更新日期：2026-03-17
> 核心原则：**限制即自由**
> **完整流程图：** 见 [AGENT-FLOW-CHART.md](./AGENT-FLOW-CHART.md)

---

## 一、核心理念

### 为什么限制主 Agent？

```
❌ 功能太全的主 Agent = 不确定的输出质量

✅ 受限的主 Agent + 明确的 Subagent 调用 = 工程化输出
```

| 问题 | 不限制 | 限制后 |
|------|--------|--------|
| 谁在调用？ | 不确定 | 不重要，流程强制统一 |
| 编码规范？ | 可能绕过 | 必须遵循 |
| TDD？ | 可能跳过 | 强制执行 |
| 先文档后开发？ | 可能忘记 | 流程强制 |

### 核心原则

1. **主 Agent 只做统筹** - 不直接执行编码操作
2. **所有编码交给 Subagent** - 强制规范化
3. **Skill 归属明确** - 编码类 skill 只有 subagent 能用
4. **错误必须报告** - 发现问题立即上报

---

## 二、目录结构

```
.claude/
├── AGENT-SKILL-SPEC.md          # 本规范文档
│
├── skills/                       # 用户可调用 Skill
│   ├── brainstorming/           # 需求引导
│   ├── main-agent-guard/        # 主 Agent 行为限制
│   ├── test-driven-development/ # TDD 开发流程（含 Java/Maven 特定）
│   ├── verification-before-completion/ # 完成前验证（含 Java/Maven 特定）
│   └── ...
│
└── agents/
    ├── agent-skills/            # Subagent 内部规范
    │   ├── dispatching-parallel-agents/ # 并行派发
    │   ├── error-reporting.md   # 错误报告
    │   ├── executing-plans/
    │   ├── finishing-a-development-branch/
    │   ├── subagent-driven-development/
    │   └── systematic-debugging/
    │
    ├── ai-edu-architect-design.md      # 设计角色
    ├── ai-edu-architect-coordinator.md # 协调角色
    ├── ai-edu-coder-user.md            # 用户领域开发
    ├── ai-edu-coder-homework.md        # 作业领域开发
    ├── ai-edu-coder-question.md        # 题库领域开发
    ├── ai-edu-coder-learning.md        # 学习领域开发
    └── ai-edu-coder-organization.md    # 组织领域开发
```

---

## 三、角色定义

### 主 Agent（统筹层）

**允许：**
- ✅ 对话、解释、回答问题
- ✅ 调度 subagent
- ✅ 审核结果
- ✅ 使用统筹类 skill（brainstorming）

**禁止：**
- ❌ 直接编写代码
- ❌ 直接运行测试
- ❌ 直接修改文件（除文档外）
- ❌ 绕过流程直接开发

### Subagent（执行层）

**职责：**
- 设计文档输出
- 代码实现
- 测试编写
- 验证执行

**要求：**
- 必须阅读 agent-skills 目录下的 skill
- 完成后输出验证报告
- 发现问题立即报告

---

## 四、强制流程

### 新功能开发

```
用户请求
    │
    ▼
┌─────────────────────────────────────┐
│ 主 Agent 检查请求类型                │
│                                     │
│ 检测到设计请求？                     │
│ → 引导使用 brainstorming skill      │
└─────────────────────────────────────┘
    │
    ▼
brainstorming skill（需求引导）
    │
    ▼
┌─────────────────────────────────────┐
│ ai-edu-architect-design (Subagent)  │
│                                     │
│ 输出：设计文档、API文档、测试文档     │
└─────────────────────────────────────┘
    │
    ▼
【人工审核】✅ 必须等待
    │
    ▼
┌─────────────────────────────────────┐
│ ai-edu-architect-coordinator        │
│                                     │
│ 调用：verification-before-completion│
│ 阅读：dispatching-parallel-agents   │
│ 阅读：error-reporting.md            │
│                                     │
│ 执行：任务派发、验收                 │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ ai-edu-coder-* (Subagent)           │
│                                     │
│ 调用：test-driven-development       │
│ 调用：verification-before-completion│
│ 阅读：error-reporting.md            │
│                                     │
│ 执行：TDD 开发、验证、报告           │
└─────────────────────────────────────┘
```

### Bug 修复

```
用户报告 Bug
    │
    ▼
┌─────────────────────────────────────┐
│ 主 Agent 检查请求类型                │
│                                     │
│ 检测到 Bug 修复请求？                │
│ → 引导使用对应 subagent             │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ ai-edu-coder-* (Subagent)           │
│                                     │
│ TDD 修复流程：                       │
│ 1. 写测试重现 Bug                   │
│ 2. 修复代码                         │
│ 3. 验证通过                         │
└─────────────────────────────────────┘
```

---

## 五、Skill 分类

### 用户可调用 Skill（skills/）

| Skill | 位置 | 用途 |
|-------|------|------|
| `main-agent-guard` | skills/ | 请求类型检查、流程强制 |
| `brainstorming` | skills/ | 需求引导 |
| `writing-plans` | skills/ | 编写计划 |
| `writing-skills` | skills/ | 编写 Skill |
| `test-driven-development` | skills/ | TDD 开发流程（含 Java/Maven） |
| `verification-before-completion` | skills/ | 完成前验证（含 Java/Maven） |

### Subagent 内部规范（agents/agent-skills/）

| 规范 | 位置 | 使用者 |
|-------|------|--------|
| `dispatching-parallel-agents` | agents/agent-skills/ | ai-edu-architect-coordinator |
| `error-reporting` | agents/agent-skills/ | 所有 subagent |
| `subagent-driven-development` | agents/agent-skills/ | 通用执行模式 |
| `systematic-debugging` | agents/agent-skills/ | 调试流程 |

---

## 六、Subagent 映射表

| 领域 | Subagent | 说明 |
|------|----------|------|
| 公共组件 | `ai-edu-coder-common` | 跨领域基础设施、工具类、通用服务（Redis、MinIO 等） |
| 用户、权限 | `ai-edu-coder-user` | 用户认证、角色权限 |
| 题库、知识点 | `ai-edu-coder-question` | 题目管理、知识点管理 |
| 作业、批改 | `ai-edu-coder-homework` | 作业布置、批改评分 |
| 错题本、掌握度 | `ai-edu-coder-learning` | 学习追踪、掌握度分析 |
| 学校、班级 | `ai-edu-coder-organization` | 组织架构管理 |

---

## 七、检查清单

### 主 Agent 自检

```
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

### Subagent 自检

```
□ 我是否调用了 test-driven-development skill？
□ 我是否调用了 verification-before-completion skill？
□ 我是否阅读了 error-reporting.md 内部规范？
□ 我是否按照 skill 流程执行？
□ 遇到配置问题时是否先询问用户再假设代码问题？
□ 我是否在完成前进行了验证？
□ 我是否输出了验证报告？
□ 发现问题时是否报告了？
```

---

## 八、更新记录

| 日期 | 修改内容 | 修改者 |
|------|----------|--------|
| 2026-03-17 | 初版创建 | Claude |
| 2026-03-17 | 添加 agent-skills 目录，限制主 Agent | Claude |
| 2026-03-17 | 合并重复 skill，优化目录结构 | Claude |
| 2026-03-18 | 新增 ai-edu-coder-common 公共组件 Agent | Claude |