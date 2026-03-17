# 深度防御验证

## 概述

当你修复由无效数据引起的 bug 时，在一个地方添加验证感觉足够。但那个单一检查可能被不同的代码路径、重构或 mock 绕过。

**核心原则：** 在数据通过的每一层验证。使 bug 结构上不可能。

## 为什么多层

单一验证："我们修复了 bug"
多层："我们使 bug 不可能"

不同层捕获不同情况：
- 入口验证捕获大多数 bug
- 业务逻辑捕获边缘情况
- 环境守卫防止特定上下文危险
- 调试日志在其他层失败时有帮助

## 四层

### 第 1 层：入口点验证
**目的：** 在 API 边界拒绝明显无效输入

```typescript
function createProject(name: string, workingDirectory: string) {
  if (!workingDirectory || workingDirectory.trim() === '') {
    throw new Error('workingDirectory 不能为空');
  }
  if (!existsSync(workingDirectory)) {
    throw new Error(`workingDirectory 不存在: ${workingDirectory}`);
  }
  if (!statSync(workingDirectory).isDirectory()) {
    throw new Error(`workingDirectory 不是目录: ${workingDirectory}`);
  }
  // ... 继续
}
```

### 第 2 层：业务逻辑验证
**目的：** 确保数据对此操作有意义

```typescript
function initializeWorkspace(projectDir: string, sessionId: string) {
  if (!projectDir) {
    throw new Error('workspace 初始化需要 projectDir');
  }
  // ... 继续
}
```

### 第 3 层：环境守卫
**目的：** 在特定上下文中防止危险操作

```typescript
async function gitInit(directory: string) {
  // 在测试中，拒绝临时目录外的 git init
  if (process.env.NODE_ENV === 'test') {
    const normalized = normalize(resolve(directory));
    const tmpDir = normalize(resolve(tmpdir()));

    if (!normalized.startsWith(tmpDir)) {
      throw new Error(
        `测试期间拒绝临时目录外的 git init: ${directory}`
      );
    }
  }
  // ... 继续
}
```

### 第 4 层：调试工具
**目的：** 捕获取证上下文

```typescript
async function gitInit(directory: string) {
  const stack = new Error().stack;
  logger.debug('即将 git init', {
    directory,
    cwd: process.cwd(),
    stack,
  });
  // ... 继续
}
```

## 应用模式

当你发现 bug 时：

1. **追踪数据流** - 错误值从哪里来？在哪里使用？
2. **映射所有检查点** - 列出数据通过的每个点
3. **在每层添加验证** - 入口、业务、环境、调试
4. **测试每层** - 尝试绕过第 1 层，验证第 2 层捕获它

## 会话示例

Bug：空 `projectDir` 导致源代码中 `git init`

**数据流：**
1. 测试设置 → 空字符串
2. `Project.create(name, '')`
3. `WorkspaceManager.createWorkspace('')`
4. `git init` 在 `process.cwd()` 运行

**添加四层：**
- 第 1 层：`Project.create()` 验证非空/存在/可写
- 第 2 层：`WorkspaceManager` 验证 projectDir 非空
- 第 3 层：`WorktreeManager` 在测试中拒绝临时目录外 git init
- 第 4 层：git init 前堆栈跟踪日志

**结果：** 所有 1847 测试通过，bug 不可能重现

## 关键洞察

所有四层都是必要的。在测试期间，每层捕获了其他层遗漏的 bug：
- 不同代码路径绕过入口验证
- Mock 绕过业务逻辑检查
- 不同平台边缘情况需要环境守卫
- 调试日志识别结构误用

**不要在一个验证点停止。** 在每层添加检查。