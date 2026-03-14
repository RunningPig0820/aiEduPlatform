# 认证模块 API 接口文档

> 基础路径: `/api/auth`
>
> 更新日期: 2026-03-14

---

## 目录

- [通用响应结构](#通用响应结构)
- [1. 用户注册](#1-用户注册)
- [2. 用户登录](#2-用户登录)
- [3. 演示账号快捷登录](#3-演示账号快捷登录)
- [4. 发送验证码](#4-发送验证码)
- [5. 重置密码](#5-重置密码)
- [6. 修改密码](#6-修改密码)
- [7. 登出](#7-登出)
- [8. 获取当前登录用户](#8-获取当前登录用户)
- [9. 获取用户信息](#9-获取用户信息)
- [错误码说明](#错误码说明)
- [前端调用注意事项](#前端调用注意事项)

---

## 通用响应结构

所有接口均返回统一的 JSON 格式：

```json
{
  "code": "00000",
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | String | 状态码，`00000` 表示成功，其他为错误码 |
| message | String | 提示信息 |
| data | Object | 业务数据，可能为 null |

---

## 1. 用户注册

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/auth/register` |
| Content-Type | `application/json` |

### 请求参数 (RequestBody)

```json
{
  "username": "testuser",
  "password": "123456",
  "realName": "测试用户",
  "phone": "13800138000",
  "code": "123456",
  "role": "STUDENT"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| username | String | 是 | 长度 3-50，以字母开头，仅含字母、数字、下划线 | 用户名 |
| password | String | 是 | 长度 6-100 | 密码 |
| realName | String | 否 | 最大长度 50 | 真实姓名 |
| phone | String | 是 | 11 位手机号格式 | 手机号 |
| code | String | 是 | 6 位数字 | 验证码（需先调用发送验证码接口，scene=REGISTER） |
| role | String | 否 | 枚举值 | 角色：`STUDENT`(学生)、`TEACHER`(教师)、`PARENT`(家长)，默认 STUDENT |

### 响应参数

成功时 `data` 返回 `UserResponse` 对象：

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "realName": "测试用户",
    "phone": "13800138000",
    "email": null,
    "role": "STUDENT",
    "enabled": true
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户 ID |
| username | String | 用户名 |
| realName | String | 真实姓名 |
| phone | String | 手机号 |
| email | String | 邮箱（可能为 null） |
| role | String | 角色 |
| enabled | Boolean | 是否启用 |

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "realName": "测试用户",
    "phone": "13800138000",
    "code": "123456",
    "role": "STUDENT"
  }'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'testuser',
    password: '123456',
    realName: '测试用户',
    phone: '13800138000',
    code: '123456',
    role: 'STUDENT'
  })
});
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 20002 | 用户已存在 | 用户名已被注册 |
| 20009 | 手机号已注册 | 手机号已被使用 |
| 20005 | 验证码无效 | 验证码不正确或已过期 |

---

## 2. 用户登录

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/auth/login` |
| Content-Type | `application/json` |

### 请求参数 (RequestBody)

支持三种登录方式，通过 `loginType` 区分：

#### 方式一：用户名+密码登录

```json
{
  "loginType": "USERNAME_PASSWORD",
  "username": "testuser",
  "password": "123456"
}
```

#### 方式二：手机号+密码登录

```json
{
  "loginType": "PHONE_PASSWORD",
  "phone": "13800138000",
  "password": "123456"
}
```

#### 方式三：手机号+验证码登录

```json
{
  "loginType": "PHONE_CODE",
  "phone": "13800138000",
  "code": "123456"
}
```

### 字段说明

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| loginType | String | 是 | 枚举值 | 登录类型，见下表 |
| username | String | 条件必填 | 长度 3-50 | 用户名，loginType=USERNAME_PASSWORD 时必填 |
| phone | String | 条件必填 | 11 位手机号格式 | 手机号，loginType=PHONE_PASSWORD 或 PHONE_CODE 时必填 |
| password | String | 条件必填 | 长度 6-100 | 密码，loginType=USERNAME_PASSWORD 或 PHONE_PASSWORD 时必填 |
| code | String | 条件必填 | 6 位数字 | 验证码，loginType=PHONE_CODE 时必填 |

### loginType 枚举值

| 值 | 说明 |
|----|------|
| `USERNAME_PASSWORD` | 用户名+密码登录 |
| `PHONE_PASSWORD` | 手机号+密码登录 |
| `PHONE_CODE` | 手机号+验证码登录 |

### 响应参数

成功时 `data` 返回 `UserResponse` 对象：

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "realName": "测试用户",
    "phone": "13800138000",
    "email": null,
    "role": "STUDENT",
    "enabled": true
  }
}
```

> **注意**: 登录成功后，服务端会在 Session 中存储用户信息，前端需要确保 Cookie 中携带 JSESSIONID。

### 请求示例

**cURL (用户名密码登录):**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "loginType": "USERNAME_PASSWORD",
    "username": "testuser",
    "password": "123456"
  }'
```

**cURL (手机号验证码登录):**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "loginType": "PHONE_CODE",
    "phone": "13800138000",
    "code": "123456"
  }'
```

**JavaScript (fetch):**
```javascript
// 用户名密码登录
const response = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include', // 重要：携带 Cookie
  body: JSON.stringify({
    loginType: 'USERNAME_PASSWORD',
    username: 'testuser',
    password: '123456'
  })
});
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 20003 | 用户名或密码错误 | 凭据无效 |
| 20001 | 用户不存在 | 用户未注册 |
| 20005 | 验证码无效 | 验证码不正确或已过期 |
| 20008 | 手机号未注册 | 手机号登录时手机号未注册 |

---

## 3. 演示账号快捷登录

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/auth/demo-login` |
| Content-Type | `application/json` |

### 请求参数 (RequestBody)

```json
{
  "role": "STUDENT"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| role | String | 是 | 枚举值 | 角色：`STUDENT`、`TEACHER`、`PARENT` |

### 可用演示账号

| role | 用户名 | 密码 | 真实姓名 |
|------|--------|------|----------|
| STUDENT | student | 123456 | Demo Student |
| TEACHER | teacher | 123456 | Demo Teacher |
| PARENT | parent | 123456 | Demo Parent |

### 响应参数

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "id": 1,
    "username": "student",
    "realName": "Demo Student",
    "phone": null,
    "email": null,
    "role": "STUDENT",
    "enabled": true
  }
}
```

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/demo-login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{"role": "STUDENT"}'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/auth/demo-login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({ role: 'STUDENT' })
});
```

---

## 4. 发送验证码

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/auth/send-code` |
| Content-Type | `application/json` |

### 请求参数 (RequestBody)

```json
{
  "phone": "13800138000",
  "scene": "REGISTER"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| phone | String | 是 | 11 位手机号格式 | 目标手机号 |
| scene | String | 是 | 枚举值 | 验证码场景，见下表 |

### scene 枚举值

| 值 | 说明 | 用途 |
|----|------|------|
| `REGISTER` | 注册 | 新用户注册时验证手机号 |
| `LOGIN` | 登录 | 手机号验证码登录 |
| `RESET_PASSWORD` | 重置密码 | 忘记密码时重置 |

### 响应参数

```json
{
  "code": "00000",
  "message": "success",
  "data": null
}
```

> **注意**: 验证码有效期通常为 5 分钟，发送后需在有效期内使用。

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/send-code \
  -H "Content-Type: application/json" \
  -d '{"phone": "13800138000", "scene": "REGISTER"}'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/auth/send-code', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    phone: '13800138000',
    scene: 'REGISTER'
  })
});
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 20007 | 验证码发送过于频繁 | 请稍后重试 |

---

## 5. 重置密码

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/auth/reset-password` |
| Content-Type | `application/json` |

### 请求参数 (RequestBody)

```json
{
  "phone": "13800138000",
  "code": "123456",
  "newPassword": "newpassword123"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| phone | String | 是 | 11 位手机号格式 | 手机号 |
| code | String | 是 | 6 位数字 | 验证码（需先调用发送验证码接口，scene=RESET_PASSWORD） |
| newPassword | String | 是 | 长度 6-100 | 新密码 |

### 响应参数

```json
{
  "code": "00000",
  "message": "success",
  "data": null
}
```

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "13800138000",
    "code": "123456",
    "newPassword": "newpassword123"
  }'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/auth/reset-password', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    phone: '13800138000',
    code: '123456',
    newPassword: 'newpassword123'
  })
});
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 20008 | 手机号未注册 | 该手机号未绑定任何账号 |
| 20005 | 验证码无效 | 验证码不正确或已过期 |

---

## 6. 修改密码

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `PUT` |
| 接口路径 | `/api/auth/password` |
| Content-Type | `application/json` |
| 需要登录 | 是 |

### 请求参数 (RequestBody)

```json
{
  "oldPassword": "123456",
  "newPassword": "newpassword123"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| oldPassword | String | 是 | - | 原密码 |
| newPassword | String | 是 | 长度 6-100 | 新密码 |

### 响应参数

```json
{
  "code": "00000",
  "message": "success",
  "data": null
}
```

### 请求示例

**cURL:**
```bash
curl -X PUT http://localhost:8080/api/auth/password \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "oldPassword": "123456",
    "newPassword": "newpassword123"
  }'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/auth/password', {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    oldPassword: '123456',
    newPassword: 'newpassword123'
  })
});
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | 需要先登录 |
| 20011 | 原密码错误 | oldPassword 不正确 |
| 20010 | 新密码不能与原密码相同 | 新旧密码一致 |

---

## 7. 登出

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/auth/logout` |
| 需要登录 | 是 |

### 请求参数

无

### 响应参数

```json
{
  "code": "00000",
  "message": "success",
  "data": null
}
```

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/auth/logout', {
  method: 'POST',
  credentials: 'include'
});
```

---

## 8. 获取当前登录用户

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/auth/current-user` |
| 需要登录 | 是 |

### 请求参数

无

### 响应参数

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "realName": "测试用户",
    "phone": "13800138000",
    "email": null,
    "role": "STUDENT",
    "enabled": true
  }
}
```

### 请求示例

**cURL:**
```bash
curl -X GET http://localhost:8080/api/auth/current-user \
  -b cookies.txt
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/auth/current-user', {
  credentials: 'include'
});
const result = await response.json();
if (result.code === '00000') {
  console.log('当前用户:', result.data);
}
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10004 | 未登录 | Session 已过期或不存在 |

---

## 9. 获取用户信息

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/auth/user/{id}` |

### 请求参数 (Path)

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户 ID |

### 响应参数

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "realName": "测试用户",
    "phone": "13800138000",
    "email": null,
    "role": "STUDENT",
    "enabled": true
  }
}
```

### 请求示例

**cURL:**
```bash
curl -X GET http://localhost:8080/api/auth/user/1
```

**JavaScript (fetch):**
```javascript
const userId = 1;
const response = await fetch(`/api/auth/user/${userId}`);
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 20001 | 用户不存在 | 指定 ID 的用户不存在 |

---

## 错误码说明

### 通用错误码 (1xxxx)

| code | message | 说明 |
|------|---------|------|
| 00000 | success | 成功 |
| 10000 | 系统错误 | 服务器内部错误 |
| 10001 | 参数错误 | 请求参数格式不正确 |
| 10002 | 实体不存在 | 请求的资源不存在 |
| 10003 | 参数无效 | 参数校验失败 |
| 10004 | 未登录 | 用户未登录或 Session 过期 |

### 用户模块错误码 (2xxxx)

| code | message | 说明 |
|------|---------|------|
| 20001 | 用户不存在 | 指定用户不存在 |
| 20002 | 用户已存在 | 用户名已被注册 |
| 20003 | 用户名或密码错误 | 登录凭据无效 |
| 20004 | 权限不足 | 无操作权限 |
| 20005 | 验证码无效 | 验证码不正确 |
| 20006 | 验证码已过期 | 验证码已超过有效期 |
| 20007 | 验证码发送过于频繁 | 请稍后重试 |
| 20008 | 手机号未注册 | 该手机号未绑定账号 |
| 20009 | 手机号已注册 | 该手机号已被使用 |
| 20010 | 新密码不能与原密码相同 | 修改密码时新旧密码一致 |
| 20011 | 原密码错误 | 修改密码时原密码不正确 |

---

## 前端调用注意事项

### 1. Session 管理

本系统使用 Spring Session + Redis 管理 Session，前端需要：

- **携带 Cookie**: 所有需要登录的接口，请求时必须携带 `credentials: 'include'`
- **跨域配置**: 开发环境需配置 CORS 允许携带凭证

```javascript
// fetch 请求示例
fetch('/api/auth/current-user', {
  credentials: 'include' // 重要：携带 Cookie
});
```

### 2. 参数校验

后端使用 Spring Validation 进行参数校验，校验失败会返回 `code: 10001` 或 `code: 10003`，`message` 中包含具体校验失败信息。

前端应在提交前进行基本校验：
- 用户名：3-50 字符，字母开头，仅含字母、数字、下划线
- 密码：6-100 字符
- 手机号：11 位，符合中国大陆手机号格式
- 验证码：6 位数字

### 3. 登录流程建议

1. 用户选择登录方式（用户名密码 / 手机号密码 / 手机号验证码）
2. 若选择验证码登录，先调用 `/api/auth/send-code` 发送验证码
3. 调用 `/api/auth/login` 完成登录
4. 登录成功后保存用户信息到本地状态
5. 后续请求携带 Cookie 即可

### 4. 注册流程建议

1. 用户填写注册信息
2. 调用 `/api/auth/send-code` 发送验证码（scene=REGISTER）
3. 用户输入验证码
4. 调用 `/api/auth/register` 完成注册
5. 注册成功后自动登录

### 5. 重置密码流程建议

1. 用户输入手机号
2. 调用 `/api/auth/send-code` 发送验证码（scene=RESET_PASSWORD）
3. 用户输入验证码和新密码
4. 调用 `/api/auth/reset-password` 完成重置

---

*文档生成时间: 2026-03-14*