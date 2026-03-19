# {模块名称} 测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证 `{ControllerName}` 的所有业务场景，确保功能的正确性和健壮性。

### 1.2 测试方式
- **集成测试**：直接注入 Controller，调用真实方法
- **数据库回滚**：使用 `@Transactional` 注解，测试完成后自动回滚
- **无 Mock**：真实数据库操作，验证完整业务流程

### 1.3 测试环境配置
- Profile: `test`
- 数据库：使用开发数据库，事务自动回滚
- Session：使用 `MockHttpSession` 模拟

---

## 2. 测试数据

| 参数 | 值 | 说明 |
|-----|-----|-----|
| TEST_ID | 1 | 测试ID |
| TEST_NAME | test001 | 测试名称 |
| TEST_PHONE | 13800138001 | 测试手机号 |
| TEST_PASSWORD | password123 | 测试密码 |

---

## 3. 测试用例清单

### 3.1 {模块名称}

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| {MODULE}-001 | 正常场景 | 数据存在 | 正确参数 | 返回成功 |
| {MODULE}-002 | 异常场景-参数错误 | 无 | 错误参数 | 抛出 INVALID_PARAMS 异常 |
| {MODULE}-003 | 异常场景-数据不存在 | 数据不存在 | 有效参数 | 抛出 NOT_FOUND 异常 |
| {MODULE}-004 | 异常场景-未授权 | 未登录 | 需要登录的接口 | 抛出 UNAUTHORIZED 异常 |
| {MODULE}-005 | 边界场景-空值 | 无 | 空参数 | 抛出 INVALID_PARAMS 异常 |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功 |
| 10001 | INVALID_PARAMS | 参数无效 |
| 10002 | NOT_FOUND | 实体不存在 |
| 10003 | INVALID_PARAMS | 参数校验失败 |
| 10004 | UNAUTHORIZED | 未授权 |

---

## 5. 测试用例统计

| 模块 | 用例数量 |
|-----|---------|
| {模块1} | {数量} |
| {模块2} | {数量} |
| **总计** | **{总数}** |

---

## 6. 测试执行顺序

测试按 `@Order` 注解指定的顺序执行：

```
100-103  : {模块1}测试
200-205  : {模块2}测试
300-303  : {模块3}测试
```

---

## 7. 辅助方法

### 7.1 创建测试数据
```java
private Entity createTestEntity(String name) {
    Entity entity = Entity.create(name);
    return repository.save(entity);
}
```

### 7.2 创建登录会话
```java
private HttpSession createLoginSession(User user) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", user.getId());
    session.setAttribute("username", user.getUsername());
    session.setAttribute("role", user.getRole());
    return session;
}
```

---

## 8. 运行测试

```bash
# 运行单个测试类
cd ai-edu-backend && mvn test -pl ai-edu-interface -Dtest={ControllerName}Test

# 运行单个测试方法
mvn test -pl ai-edu-interface -Dtest={ControllerName}Test#{methodName}
```