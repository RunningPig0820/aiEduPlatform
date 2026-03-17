# AI Edu Platform 开发记忆

> 更新日期：2026-03-18

## 核心原则

### 主 Agent 行为限制

```
主 Agent 永远不直接执行以下操作：
1. 编写生产代码
2. 运行测试
3. 修改文件内容（除文档外）
4. 绕过流程直接开发
```

所有编码工作必须通过 Subagent 完成。

### Subagent 映射

| 领域 | Subagent | 说明 |
|------|----------|------|
| 公共组件 | `ai-edu-coder-common` | 跨领域基础设施、工具类、通用服务 |
| 用户域 | `ai-edu-coder-user` | 用户、权限相关 |
| 题库域 | `ai-edu-coder-question` | 题目、知识点相关 |
| 作业域 | `ai-edu-coder-homework` | 作业、批改相关 |
| 学习域 | `ai-edu-coder-learning` | 错题本、掌握度相关 |
| 组织域 | `ai-edu-coder-organization` | 学校、班级相关 |

---

## 经验教训

### 2026-03-18: 环境配置问题处理

**问题：** Redis 连接失败时，尝试了多种代码绕过方案（嵌入式 Redis、Mock），而不是先询问用户配置。

**教训：** 遇到基础设施相关问题时，应先询问用户确认配置，而不是假设代码问题。

**正确流程：**
1. 检测到配置问题（如 Redis 连接失败）
2. 询问用户服务状态和配置
3. 根据用户反馈决定方案

**已在 TDD Skill 中添加环境配置排查清单。**

### 2026-03-18: 跨领域组件开发

**问题：** 开发 RedisService 时没有意识到这是公共组件，应由专门的 Agent 处理。

**教训：** 跨领域共享的基础设施应使用 `ai-edu-coder-common` Agent 开发。

**已创建 `ai-edu-coder-common.md` Agent 定义。**

---

## DDD 架构规范

### 验证码模块结构（正确示例）

```
domain/user/
├── model/valueobject/VerificationCode.java  # 值对象
├── repository/VerificationCodeRepository.java  # 仓储接口
└── service/VerificationCodeService.java  # 领域服务

infrastructure/
└── repository/VerificationCodeRepositoryImpl.java  # 仓储实现（Redis）
```

### RedisService 职责边界

| RedisService（公共层） | 领域仓储（领域层） |
|------------------------|-------------------|
| set/get/delete（通用操作） | save/findByPhone（业务操作） |
| expire/hasKey（工具方法） | verifyAndDelete（业务逻辑） |
| 不包含业务语义 | 包含业务语义 |

---

## 技术栈

- Java 21 + Spring Boot 3.2.5
- MyBatis-Plus 3.5.5（替代 JPA）
- Redis（Redisson 3.27.2）
- H2 内存数据库（测试）
- DDD 四层架构