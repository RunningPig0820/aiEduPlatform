---
name: tdd-development
description: ai-edu-coder-* 专属 - TDD 开发流程，测试先于代码
---

# TDD 开发流程

## 铁律

```
没有先失败的测试，就不写生产代码
```

## 红绿重构循环

### 1. 红 - 写失败的测试

```java
@Test
void should_create_user_with_valid_username() {
    // given
    String username = "testuser";

    // when
    User user = User.create(username);

    // then
    assertThat(user.getUsername()).isEqualTo(username);
}
```

**验证红：** 运行测试，确认因功能缺失而失败

```bash
mvn test -Dtest=UserTest#should_create_user_with_valid_username
```

### 2. 绿 - 写最小代码通过

```java
public class User {
    private String username;

    public static User create(String username) {
        User user = new User();
        user.username = username;
        return user;
    }

    public String getUsername() {
        return username;
    }
}
```

**验证绿：** 运行测试，确认通过

### 3. 重构 - 清理

- 添加验证逻辑
- 改进命名
- 提取方法

**保持绿色：** 每次重构后运行测试

## Java DDD 项目结构

```
com.ai.edu.domain.{context}/
├── model/
│   ├── entity/           # 实体
│   ├── valueobject/      # 值对象
│   └── aggregate/        # 聚合根
├── repository/           # 仓储接口
└── service/              # 领域服务

com.ai.edu.application/
├── service/              # 应用服务
├── dto/                  # DTO
└── assembler/            # 转换器

com.ai.edu.interface_.api/
└── *Controller.java      # REST API
```

## 测试规范

### 测试命名

```java
// 格式: should_{expected_behavior}_when_{condition}
@Test
void should_throw_exception_when_username_is_empty() { }

@Test
void should_return_user_when_find_by_id() { }
```

### 测试结构

```java
@Test
void should_xxx() {
    // given - 准备测试数据

    // when - 执行被测试方法

    // then - 验证结果
}
```

### 集成测试

```java
@SpringBootTest
@Transactional
class UserControllerIntegrationTest {

    @Resource
    private UserController userController;

    @Test
    void should_register_user_successfully() {
        // given
        RegisterRequest request = new RegisterRequest("user", "password");

        // when
        ApiResponse<Long> response = userController.register(request);

        // then
        assertThat(response.getCode()).isEqualTo("00000");
        assertThat(response.getData()).isNotNull();
    }
}
```

## 运行测试

```bash
# 运行单个测试
mvn test -Dtest=ClassNameTest

# 运行单个方法
mvn test -Dtest=ClassNameTest#methodName

# 运行所有测试
mvn test

# 运行指定模块测试
mvn test -pl ai-edu-interface
```

## 完成前验证

在声称任务完成前，必须：

```bash
# 1. 运行测试确认通过
mvn test

# 2. 检查测试覆盖率
mvn jacoco:report

# 3. 运行构建确认成功
mvn clean install -DskipTests
```

## 检查清单

- [ ] 每个新方法都有对应测试
- [ ] 测试先于代码编写
- [ ] 看到测试失败后再写代码
- [ ] 所有测试通过
- [ ] 测试覆盖率 ≥ 80%
- [ ] 代码遵循 DDD 目录结构
- [ ] 使用 `@Resource` 注入依赖