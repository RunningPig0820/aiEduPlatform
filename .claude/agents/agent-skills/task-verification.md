---
name: task-verification
description: ai-edu-coder-* 专属 - 任务完成前验证，证据先于声明
---

# 任务完成前验证

## 铁律

```
没有验证证据，就不声称完成
```

## 验证流程

### 1. 代码验证

```bash
# 编译检查
mvn clean compile

# 确认：BUILD SUCCESS
```

### 2. 测试验证

```bash
# 运行所有测试
mvn test

# 确认：Tests run: X, Failures: 0, Errors: 0
```

### 3. 覆盖率验证

```bash
# 生成覆盖率报告
mvn jacoco:report

# 确认：覆盖率 ≥ 80%
```

### 4. 集成验证

```bash
# 运行集成测试
mvn test -pl ai-edu-interface

# 确认：所有接口测试通过
```

## 验证输出模板

```markdown
## 验证报告

### 1. 编译验证
```bash
mvn clean compile
```
输出：BUILD SUCCESS

### 2. 测试验证
```bash
mvn test
```
输出：Tests run: 15, Failures: 0, Errors: 0

### 3. 覆盖率验证
- Line Coverage: 85%
- Branch Coverage: 82%

### 4. 功能验证
- [x] 接口契约符合
- [x] 测试用例通过
- [x] 代码规范符合

✅ 验证通过，任务完成
```

## 禁止事项

| 禁止 | 原因 |
|------|------|
| "应该通过" | 必须实际运行 |
| "看起来没问题" | 必须有证据 |
| "之前测试过" | 必须新鲜验证 |
| 跳过覆盖率检查 | 必须 ≥ 80% |

## 检查清单

- [ ] 编译成功（BUILD SUCCESS）
- [ ] 所有测试通过（0 Failures, 0 Errors）
- [ ] 覆盖率达标（≥ 80%）
- [ ] 接口契约符合设计文档
- [ ] 代码规范符合项目要求
- [ ] 输出验证报告