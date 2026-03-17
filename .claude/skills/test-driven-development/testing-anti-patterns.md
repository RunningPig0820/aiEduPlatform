# 测试反模式

**何时加载此参考：** 编写或更改测试、添加 mock 或想在生产代码中添加仅测试方法时。

## 概述

测试必须验证真实行为，不是 mock 行为。Mock 是隔离手段，不是被测试的东西。

**核心原则：** 测试代码做什么，不是 mock 做什么。

**遵循严格 TDD 防止这些反模式。**

## 铁律

```
1. 永远不要测试 mock 行为
2. 永远不要向生产类添加仅测试方法
3. 永远不要在不理解依赖的情况下 mock
```

## 反模式 1：测试 Mock 行为

**违规：**
```typescript
// ❌ 坏：测试 mock 存在
test('renders sidebar', () => {
  render(<Page />);
  expect(screen.getByTestId('sidebar-mock')).toBeInTheDocument();
});
```

**为什么错误：**
- 你在验证 mock 有效，不是组件有效
- mock 存在时测试通过，不存在时失败
- 对真实行为没有任何说明

**你的合作伙伴的纠正：** "我们在测试 mock 的行为吗？"

**修复：**
```typescript
// ✅ 好：测试真实组件或不要 mock 它
test('renders sidebar', () => {
  render(<Page />);  // 不要 mock sidebar
  expect(screen.getByRole('navigation')).toBeInTheDocument();
});

// 或者如果 sidebar 必须 mock 来隔离：
// 不要断言 mock - 测试 Page 在 sidebar 存在时的行为
```

### 门控功能

```
在对任何 mock 元素断言之前：
  问："我在测试真实组件行为还是只是 mock 存在？"

  如果测试 mock 存在：
    停止 - 删除断言或取消 mock 组件

  改为测试真实行为
```

## 反模式 2：生产中的仅测试方法

**违规：**
```typescript
// ❌ 坏：destroy() 只在测试中使用
class Session {
  async destroy() {  // 看起来像生产 API！
    await this._workspaceManager?.destroyWorkspace(this.id);
    // ... 清理
  }
}

// 在测试中
afterEach(() => session.destroy());
```

**为什么错误：**
- 生产类被仅测试代码污染
- 如果在生产中意外调用很危险
- 违反 YAGNI 和关注点分离
- 混淆对象生命周期和实体生命周期

**修复：**
```typescript
// ✅ 好：测试工具处理测试清理
// Session 没有 destroy() - 在生产中是无状态的

// 在 test-utils/
export async function cleanupSession(session: Session) {
  const workspace = session.getWorkspaceInfo();
  if (workspace) {
    await workspaceManager.destroyWorkspace(workspace.id);
  }
}

// 在测试中
afterEach(() => cleanupSession(session));
```

### 门控功能

```
在向生产类添加任何方法之前：
  问："这只在测试中使用吗？"

  如果是：
    停止 - 不要添加它
    放在测试工具中代替

  问："这个类拥有这个资源的生命周期吗？"

  如果否：
    停止 - 这个方法在错误的类中
```

## 反模式 3：不理解就 Mock

**违规：**
```typescript
// ❌ 坏：Mock 破坏测试逻辑
test('detects duplicate server', () => {
  // Mock 阻止测试依赖的配置写入！
  vi.mock('ToolCatalog', () => ({
    discoverAndCacheTools: vi.fn().mockResolvedValue(undefined)
  }));

  await addServer(config);
  await addServer(config);  // 应该抛出 - 但不会！
});
```

**为什么错误：**
- Mock 的方法有测试依赖的副作用（写配置）
- 过度 mock"为了安全"破坏实际行为
- 测试因错误原因通过或神秘失败

**修复：**
```typescript
// ✅ 好：在正确层级 Mock
test('detects duplicate server', () => {
  // Mock 慢的部分，保留测试需要的行为
  vi.mock('MCPServerManager'); // 只 mock 慢的服务器启动

  await addServer(config);  // 配置已写入
  await addServer(config);  // 检测到重复 ✓
});
```

### 门控功能

```
在 mock 任何方法之前：
  停止 - 还不要 mock

  1. 问："真实方法有什么副作用？"
  2. 问："这个测试依赖这些副作用中的任何一个吗？"
  3. 问："我完全理解这个测试需要什么吗？"

  如果依赖副作用：
    在更低层级 mock（实际的慢/外部操作）
    或使用保留必要行为的测试替身
    不是测试依赖的高级方法

  如果不确定测试依赖什么：
    先用真实实现运行测试
    观察实际需要发生什么
    然后在正确层级添加最小 mock

  危险信号：
    - "为了安全我会 mock 这个"
    - "这可能很慢，最好 mock 它"
    - 不理解依赖链就 mock
```

## 反模式 4：不完整的 Mock

**违规：**
```typescript
// ❌ 坏：部分 mock - 只有你认为需要的字段
const mockResponse = {
  status: 'success',
  data: { userId: '123', name: 'Alice' }
  // 缺失：下游代码使用的元数据
};

// 稍后：当代码访问 response.metadata.requestId 时中断
```

**为什么错误：**
- **部分 mock 隐藏结构假设** - 你只 mock 了你知道的字段
- **下游代码可能依赖你未包含的字段** - 静默失败
- **测试通过但集成失败** - Mock 不完整，真实 API 完整
- **虚假信心** - 测试对真实行为证明不了什么

**铁律：** Mock 完整的数据结构，如现实中存在的那样，不只是你即时测试使用的字段。

**修复：**
```typescript
// ✅ 好：镜像真实 API 完整性
const mockResponse = {
  status: 'success',
  data: { userId: '123', name: 'Alice' },
  metadata: { requestId: 'req-789', timestamp: 1234567890 }
  // 真实 API 返回的所有字段
};
```

### 门控功能

```
在创建 mock 响应之前：
  检查："真实 API 响应包含什么字段？"

  动作：
    1. 检查文档/示例中的实际 API 响应
    2. 包含系统下游可能消费的所有字段
    3. 验证 mock 完全匹配真实响应 schema

  关键：
    如果你创建 mock，你必须理解整个结构
    部分 mock 在代码依赖省略字段时静默失败

  如果不确定：包含所有文档化字段
```

## 反模式 5：集成测试作为事后想法

**违规：**
```
✅ 实现完成
❌ 没有编写测试
"准备测试"
```

**为什么错误：**
- 测试是实现的一部分，不是可选的后续
- TDD 会捕获这个
- 没有测试不能声称完成

**修复：**
```
TDD 循环：
1. 写失败测试
2. 实现通过
3. 重构
4. 然后声称完成
```

## 当 Mock 变得太复杂

**警告信号：**
- Mock 设置比测试逻辑长
- Mock 一切让测试通过
- Mock 缺少真实组件有的方法
- Mock 更改时测试中断

**你的合作伙伴的问题：** "我们这里需要使用 mock 吗？"

**考虑：** 用真实组件的集成测试通常比复杂 mock 更简单

## TDD 防止这些反模式

**为什么 TDD 有帮助：**
1. **先写测试** → 强迫你思考你实际在测试什么
2. **看它失败** → 确认测试测试真实行为，不是 mock
3. **最小实现** → 没有仅测试方法悄悄进入
4. **真实依赖** → 你在 mock 前看到测试实际需要什么

**如果你在测试 mock 行为，你违反了 TDD** - 你添加了 mock 而没有先看测试对真实代码失败。

## 快速参考

| 反模式 | 修复 |
|--------------|-----|
| 断言 mock 元素 | 测试真实组件或取消 mock |
| 生产中的仅测试方法 | 移到测试工具 |
| 不理解就 mock | 先理解依赖，最小化 mock |
| 不完整 mock | 完全镜像真实 API |
| 测试作为事后想法 | TDD - 测试优先 |
| 过于复杂 mock | 考虑集成测试 |

## 危险信号

- 断言检查 `*-mock` 测试 ID
- 只在测试文件中调用的方法
- Mock 设置是测试的 >50%
| 删除 mock 时测试失败
- 无法解释为什么需要 mock
- Mock"为了安全"

## 底线

**Mock 是隔离工具，不是要测试的东西。**

如果 TDD 显示你在测试 mock 行为，你走错了。

修复：测试真实行为或质疑你为什么在 mock。