# 认证模块测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证 `AuthApiController` 的所有业务场景，确保认证功能的正确性和健壮性。

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
| TEST_PHONE | 13800138001 | 主测试手机号 |
| TEST_PHONE_2 | 13800138002 | 备用测试手机号 |
| TEST_USERNAME | testuser001 | 主测试用户名 |
| TEST_PASSWORD | password123 | 测试密码 |
| WRONG_PASSWORD | wrongpassword | 错误密码 |
| NEW_PASSWORD | newpassword123 | 新密码 |
| WRONG_CODE | 654321 | 错误验证码 |

---

## 3. 测试用例清单

### 3.1 用户注册模块

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| REG-001 | 注册成功 | 手机号未注册，发送验证码成功 | 正确的用户名、密码、手机号、验证码 | 返回成功，创建用户 |
| REG-002 | 注册失败-验证码错误 | 无 | 错误的验证码 | 抛出 CODE_INVALID 异常 |
| REG-003 | 注册失败-用户名已存在 | 用户名已被注册 | 已存在的用户名 | 抛出 USER_ALREADY_EXISTS 异常 |
| REG-004 | 注册失败-手机号已注册 | 手机号已被注册 | 已注册的手机号 | 发送验证码时抛出 PHONE_ALREADY_REGISTERED 异常 |

### 3.2 用户登录模块

#### 3.2.1 用户名+密码登录

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| LOGIN-001 | 用户名密码登录成功 | 用户已注册 | 正确的用户名和密码 | 返回用户信息，Session 已设置 |
| LOGIN-002 | 登录失败-用户不存在 | 无 | 不存在的用户名 | 抛出 INVALID_CREDENTIALS 异常 |
| LOGIN-003 | 登录失败-密码错误 | 用户已注册 | 错误的密码 | 抛出 INVALID_CREDENTIALS 异常 |
| LOGIN-004 | 登录失败-用户名为空 | 无 | 用户名为 null | 抛出 INVALID_PARAMS 异常 |
| LOGIN-005 | 登录失败-密码为空 | 无 | 密码为 null | 抛出 INVALID_PARAMS 异常 |
| LOGIN-006 | 登录失败-账号被禁用 | 用户已禁用 | 正确的用户名和密码 | 抛出 PERMISSION_DENIED 异常 |

#### 3.2.2 手机号+密码登录

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| LOGIN-007 | 手机号密码登录成功 | 用户已注册 | 正确的手机号和密码 | 返回用户信息 |
| LOGIN-008 | 登录失败-手机号未注册 | 无 | 未注册的手机号 | 抛出 INVALID_CREDENTIALS 异常 |
| LOGIN-009 | 登录失败-密码错误 | 用户已注册 | 错误的密码 | 抛出 INVALID_CREDENTIALS 异常 |
| LOGIN-010 | 登录失败-手机号为空 | 无 | 手机号为 null | 抛出 INVALID_PARAMS 异常 |

#### 3.2.3 手机号+验证码登录

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| LOGIN-011 | 手机号验证码登录成功 | 用户已注册，验证码已发送 | 正确的手机号和验证码 | 返回用户信息 |
| LOGIN-012 | 登录失败-验证码错误 | 无 | 错误的验证码 | 抛出 CODE_INVALID 异常 |
| LOGIN-013 | 登录失败-手机号未注册 | 无 | 未注册的手机号 | 发送验证码时抛出 PHONE_NOT_REGISTERED 异常 |

#### 3.2.4 无效登录类型

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| LOGIN-014 | 登录失败-无效登录类型 | 无 | 无效的 loginType | 抛出 INVALID_PARAMS 异常 |

### 3.3 演示登录模块

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| DEMO-001 | 演示登录成功-STUDENT | 无 | role=STUDENT | 返回学生角色用户 |
| DEMO-002 | 演示登录成功-TEACHER | 无 | role=TEACHER | 返回教师角色用户 |
| DEMO-003 | 演示登录成功-PARENT | 无 | role=PARENT | 返回家长角色用户 |
| DEMO-004 | 演示登录失败-无效角色 | 无 | role=INVALID_ROLE | 抛出 INVALID_CREDENTIALS 异常 |

### 3.4 发送验证码模块

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| CODE-001 | 发送成功-注册场景 | 手机号未注册 | scene=REGISTER | 返回成功 |
| CODE-002 | 发送成功-登录场景 | 手机号已注册 | scene=LOGIN | 返回成功 |
| CODE-003 | 发送成功-重置密码场景 | 手机号已注册 | scene=RESET_PASSWORD | 返回成功 |
| CODE-004 | 发送失败-手机号格式错误 | 无 | 格式错误的手机号 | 抛出 INVALID_PARAMS 异常 |
| CODE-005 | 发送失败-注册场景手机号已存在 | 手机号已注册 | scene=REGISTER | 抛出 PHONE_ALREADY_REGISTERED 异常 |
| CODE-006 | 发送失败-登录场景手机号未注册 | 手机号未注册 | scene=LOGIN | 抛出 PHONE_NOT_REGISTERED 异常 |
| CODE-007 | 发送失败-无效场景 | 无 | scene=INVALID | 抛出 INVALID_PARAMS 异常 |

### 3.5 重置密码模块

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| RESET-001 | 重置密码成功 | 用户已注册，验证码已发送 | 正确的手机号、验证码、新密码 | 密码重置成功，可用新密码登录 |
| RESET-002 | 重置失败-验证码错误 | 无 | 错误的验证码 | 抛出 CODE_INVALID 异常 |

### 3.6 修改密码模块

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| PWD-001 | 修改密码成功 | 用户已登录 | 正确的原密码、新密码 | 密码修改成功，可用新密码登录 |
| PWD-002 | 修改失败-未登录 | 未登录 | 原密码、新密码 | 抛出 UNAUTHORIZED 异常 |
| PWD-003 | 修改失败-原密码错误 | 用户已登录 | 错误的原密码 | 抛出 OLD_PASSWORD_WRONG 异常 |
| PWD-004 | 修改失败-新密码与原密码相同 | 用户已登录 | 新密码=原密码 | 抛出 PASSWORD_SAME_AS_OLD 异常 |

### 3.7 登出模块

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| LOGOUT-001 | 登出成功 | 用户已登录 | Session | 返回成功 |

### 3.8 获取当前用户模块

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| USER-001 | 获取成功 | 用户已登录 | Session | 返回当前用户信息 |
| USER-002 | 获取失败-未登录 | 未登录 | Session（无 userId） | 抛出 UNAUTHORIZED 异常 |

### 3.9 获取用户信息模块

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| INFO-001 | 获取成功 | 用户存在 | 用户ID | 返回用户信息 |
| INFO-002 | 获取失败-用户不存在 | 无 | 不存在的用户ID | 抛出 USER_NOT_FOUND 异常 |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功 |
| 10003 | INVALID_PARAMS | 参数无效 |
| 10004 | UNAUTHORIZED | 未授权 |
| 20001 | USER_NOT_FOUND | 用户不存在 |
| 20002 | USER_ALREADY_EXISTS | 用户已存在 |
| 20003 | INVALID_CREDENTIALS | 凭证无效 |
| 20004 | PERMISSION_DENIED | 权限拒绝 |
| 20005 | CODE_INVALID | 验证码无效 |
| 20008 | PHONE_NOT_REGISTERED | 手机号未注册 |
| 20009 | PHONE_ALREADY_REGISTERED | 手机号已注册 |
| 20010 | PASSWORD_SAME_AS_OLD | 新密码与原密码相同 |
| 20011 | OLD_PASSWORD_WRONG | 原密码错误 |

---

## 5. 测试用例统计

| 模块 | 用例数量 |
|-----|---------|
| 用户注册 | 4 |
| 用户名+密码登录 | 6 |
| 手机号+密码登录 | 4 |
| 手机号+验证码登录 | 3 |
| 无效登录类型 | 1 |
| 演示登录 | 4 |
| 发送验证码 | 7 |
| 重置密码 | 2 |
| 修改密码 | 4 |
| 登出 | 1 |
| 获取当前用户 | 2 |
| 获取用户信息 | 2 |
| **总计** | **40** |

---

## 6. 测试执行顺序

测试按 `@Order` 注解指定的顺序执行：

```
100-103  : 用户注册测试
200-205  : 用户名+密码登录测试
300-303  : 手机号+密码登录测试
400-402  : 手机号+验证码登录测试
500-503  : 演示登录测试
600-606  : 发送验证码测试
700-701  : 重置密码测试
800-803  : 修改密码测试
900      : 登出测试
1000-1001: 获取当前用户测试
1100-1101: 获取用户信息测试
1200     : 无效登录类型测试
```

---

## 7. 辅助方法

### 7.1 创建测试用户
```java
private User createTestUser(String username, String phone) {
    User user = User.create(
            username,
            PasswordUtil.encode(TEST_PASSWORD),
            TEST_REAL_NAME,
            phone,
            "STUDENT"
    );
    return userRepository.save(user);
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
cd ai-edu-backend && mvn test -pl ai-edu-interface -Dtest=AuthApiControllerTest

# 运行单个测试方法
mvn test -pl ai-edu-interface -Dtest=AuthApiControllerTest#register_Success
```