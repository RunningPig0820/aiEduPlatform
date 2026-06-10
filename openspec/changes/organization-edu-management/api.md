# 行政班管理 API 接口文档

> 基础路径: `/api/admin-classes`
>
> 更新日期: 2026-06-10

---

## 目录

- [通用响应结构](#通用响应结构)
- [1. 创建行政班节点](#1-创建行政班节点)
- [2. 更新行政班节点](#2-更新行政班节点)
- [3. 查询行政班节点详情](#3-查询行政班节点详情)
- [4. 查询学校行政班树](#4-查询学校行政班树)
- [5. 删除行政班节点](#5-删除行政班节点)
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

## 1. 创建行政班节点

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `POST` |
| 接口路径 | `/api/admin-classes` |
| Content-Type | `application/json` |
| 需要登录 | 是 |

### 请求参数

**RequestBody**

```json
{
  "name": "小学部",
  "schoolId": 1,
  "parentId": null,
  "deptType": 3,
  "stageCode": "PRIMARY",
  "stageYearCode": "4",
  "gradeCode": "",
  "enrollmentYear": "",
  "sortOrder": 0,
  "description": "小学部学段节点"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| name | String | 是 | 1-100 字符 | 节点名称（如"小学部""2024级""1班"） |
| schoolId | Long | 是 | > 0 | 所属学校 ID |
| parentId | Long | 否 | — | 父节点 ID，不填则为根节点 |
| deptType | Integer | 是 | 3/4/5 | 节点类型：3-学段, 4-年级, 5-班级 |
| stageCode | String | 是 | PRIMARY/JUNIOR_HIGH/SENIOR_HIGH | 学段编码 |
| stageYearCode | String | 是 | 4/5/7 | 年制：4-小学六年制, 5-初中三年制, 7-高中三年制 |
| gradeCode | String | 条件必填 | 1-12 数字 | 年级编码，deptType=4 或 5 时必填 |
| enrollmentYear | String | 条件必填 | 如"2024" | 入学年份，deptType=4 或 5 时必填 |
| sortOrder | Integer | 否 | ≥ 0 | 排序序号，默认 0 |
| description | String | 否 | ≤ 500 字符 | 节点描述 |

### 响应参数

成功时 `data` 返回：

```json
{
  "deptId": 100,
  "name": "小学部",
  "schoolId": 1,
  "parentId": null,
  "departmentPath": "100",
  "departmentType": "ADMIN_CLASS",
  "deptType": 3,
  "stageCode": "PRIMARY",
  "stageYearCode": "4",
  "gradeCode": "",
  "enrollmentYear": "",
  "sortOrder": 0,
  "description": "小学部学段节点"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| deptId | Long | 部门 ID（对应 t_department.id） |
| name | String | 节点名称 |
| schoolId | Long | 所属学校 ID |
| parentId | Long | 父节点 ID，null 为根 |
| departmentPath | String | 部门路径（如 "1_10_100"） |
| departmentType | String | 固定为 ADMIN_CLASS |
| deptType | Integer | 节点类型：3/4/5 |
| stageCode | String | 学段编码 |
| stageYearCode | String | 年制编码 |
| gradeCode | String | 年级编码 |
| enrollmentYear | String | 入学年份 |
| sortOrder | Integer | 排序序号 |
| description | String | 描述 |

### 请求示例

**cURL:**
```bash
curl -X POST http://localhost:8080/api/admin-classes \
  -H "Content-Type: application/json" \
  -d '{
    "name": "小学部",
    "schoolId": 1,
    "deptType": 3,
    "stageCode": "PRIMARY",
    "stageYearCode": "4",
    "sortOrder": 0
  }'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/admin-classes', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    name: '小学部',
    schoolId: 1,
    deptType: 3,
    stageCode: 'PRIMARY',
    stageYearCode: '4',
    sortOrder: 0
  })
});
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10003 | deptType 必须为 3/4/5 | 节点类型参数无效 |
| 10003 | gradeCode 必填 | 年级/班级节点未填 gradeCode |
| 10003 | enrollmentYear 必填 | 年级/班级节点未填 enrollmentYear |
| 20001 | 父节点不存在 | 指定的 parentId 无效 |
| 20002 | 学段已存在 | 同一学校下已有同名同类型学段 |

---

## 2. 更新行政班节点

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `PUT` |
| 接口路径 | `/api/admin-classes/{id}` |
| Content-Type | `application/json` |
| 需要登录 | 是 |

### 请求参数

**Path**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 部门 ID（t_department.id） |

**RequestBody**

```json
{
  "name": "2024级",
  "gradeCode": "1",
  "enrollmentYear": "2024",
  "sortOrder": 1,
  "description": "2024年入学"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| name | String | 否 | 1-100 字符 | 节点名称 |
| stageCode | String | 否 | PRIMARY/JUNIOR_HIGH/SENIOR_HIGH | 学段编码 |
| stageYearCode | String | 否 | 4/5/7 | 年制 |
| gradeCode | String | 否 | 1-12 | 年级编码 |
| enrollmentYear | String | 否 | 如"2024" | 入学年份 |
| sortOrder | Integer | 否 | ≥ 0 | 排序序号 |
| description | String | 否 | ≤ 500 字符 | 描述 |

### 响应参数

同创建接口返回完整节点信息。

### 请求示例

**cURL:**
```bash
curl -X PUT http://localhost:8080/api/admin-classes/100 \
  -H "Content-Type: application/json" \
  -d '{"name": "2024级", "gradeCode": "1", "enrollmentYear": "2024"}'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/admin-classes/100', {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({ name: '2024级', gradeCode: '1', enrollmentYear: '2024' })
});
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10002 | 节点不存在 | 指定的 id 不存在或已删除 |
| 10003 | 参数无效 | 校验失败 |

---

## 3. 查询行政班节点详情

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/admin-classes/{id}` |
| Content-Type | — |
| 需要登录 | 是 |

### 请求参数

**Path**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 部门 ID |

### 响应参数

成功时返回完整节点信息（含 department 和 department_edu 字段），结构同创建响应。

### 请求示例

**cURL:**
```bash
curl http://localhost:8080/api/admin-classes/100
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/admin-classes/100', {
  credentials: 'include'
});
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10002 | 节点不存在 | 指定节点不存在或已删除 |

---

## 4. 查询学校行政班树

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `GET` |
| 接口路径 | `/api/admin-classes` |
| Content-Type | — |
| 需要登录 | 是 |

### 请求参数

**Query**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| schoolId | Long | 是 | 学校 ID |

### 响应参数

成功时 `data` 返回树形结构：

```json
[
  {
    "deptId": 100,
    "name": "小学部",
    "parentId": null,
    "deptType": 3,
    "stageCode": "PRIMARY",
    "stageYearCode": "4",
    "children": [
      {
        "deptId": 101,
        "name": "2024级",
        "parentId": 100,
        "deptType": 4,
        "gradeCode": "1",
        "enrollmentYear": "2024",
        "children": [
          {
            "deptId": 102,
            "name": "1班",
            "parentId": 101,
            "deptType": 5,
            "gradeCode": "1",
            "enrollmentYear": "2024"
          }
        ]
      }
    ]
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| deptId | Long | 节点 ID |
| name | String | 节点名称 |
| parentId | Long | 父节点 ID |
| deptType | Integer | 节点类型：3-学段, 4-年级, 5-班级 |
| stageCode | String | 学段编码 |
| stageYearCode | String | 年制 |
| gradeCode | String | 年级编码（年级和班级节点有值） |
| enrollmentYear | String | 入学年份（年级和班级节点有值） |
| children | Array | 子节点列表（递归结构） |

### 请求示例

**cURL:**
```bash
curl "http://localhost:8080/api/admin-classes?schoolId=1"
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/admin-classes?schoolId=1', {
  credentials: 'include'
});
const result = await response.json();
const tree = result.data;
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10003 | schoolId 必填 | 缺少 schoolId 参数 |

---

## 5. 删除行政班节点

### 基本信息

| 项目 | 值 |
|------|-----|
| HTTP 方法 | `DELETE` |
| 接口路径 | `/api/admin-classes/{id}` |
| Content-Type | — |
| 需要登录 | 是 |

### 请求参数

**Path**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 部门 ID |

### 响应参数

成功时 `data` 为 null，`code: "00000"`。

### 请求示例

**cURL:**
```bash
curl -X DELETE http://localhost:8080/api/admin-classes/100
```

**JavaScript (fetch):**
```javascript
const response = await fetch('/api/admin-classes/100', {
  method: 'DELETE',
  credentials: 'include'
});
const result = await response.json();
```

### 常见错误

| code | message | 说明 |
|------|---------|------|
| 10002 | 节点不存在 | 节点不存在或已删除 |
| 20003 | 存在子节点 | 删除前需先删除所有子节点 |

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

### 行政班错误码 (2xxxx)

| code | message | 说明 |
|------|---------|------|
| 20001 | 父节点不存在 | 指定的 parentId 无效 |
| 20002 | 学段已存在 | 同一学校下已有同名同类学段 |
| 20003 | 存在子节点 | 节点下存在子节点，无法删除 |
| 20004 | 创建行政班失败 | 事务创建 Department + DepartmentEdu 失败 |

---

## 前端调用注意事项

### 1. Session 管理

本系统使用 Spring Session + Redis 管理 Session，前端需要：

- **携带 Cookie**: 所有需要登录的接口，请求时必须携带 `credentials: 'include'`
- **跨域配置**: 开发环境需配置 CORS 允许携带凭证

```javascript
// fetch 请求示例
fetch('/api/admin-classes', {
  credentials: 'include' // 重要：携带 Cookie
});
```

### 2. 参数校验

- 创建节点时，`deptType` 决定哪些字段必填：3(学段)只需要 stageCode/stageYearCode，4(年级)和 5(班级)还需要 gradeCode/enrollmentYear
- `stageCode` 取值范围：PRIMARY, JUNIOR_HIGH, SENIOR_HIGH
- `stageYearCode` 取值范围：4, 5, 7
- `gradeCode` 为 1-12 数字字符串

### 3. 树结构构建

前端获取树形数据后可直接渲染。后端返回的 `children` 字段为递归结构，每个节点包含完整信息。

### 4. 删除限制

有子节点的节点不能直接删除，需先从叶子节点开始逐层删除（或前端提示用户先删除子节点）。

---

*文档生成时间: 2026-06-10*
