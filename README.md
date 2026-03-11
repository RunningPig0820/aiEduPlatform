# AI教育平台

基于 DDD 架构的教育 AI 智能体平台，支持老师、学生、家长三端。

## 项目结构

```
ai-edu-platform/
├── ai-edu-backend/          # Java后端 (Maven多模块)
│   ├── ai-edu-common/       # 公共模块
│   ├── ai-edu-domain/       # 领域层 (DDD核心)
│   ├── ai-edu-application/  # 应用层
│   ├── ai-edu-infrastructure/ # 基础设施层
│   └── ai-edu-interface/    # 接口层
├── ai-edu-ai-service/       # Python AI微服务
└── deploy/                  # 部署配置
```

## 技术栈

### 后端 (Java)
- Spring Boot 3.2+ / Spring Modulith
- Spring Data JPA + MyBatis-Plus
- MySQL 8.0 / Redis / RabbitMQ
- MinIO (对象存储)

### AI服务 (Python)
- FastAPI
- PaddleOCR / LangChain
- 通义千问 (Qwen)

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis 7.0
- RabbitMQ 3.x
- Python 3.10+ (AI服务)

### 启动基础设施

```bash
cd deploy
docker-compose up -d
```

### 启动Java后端

```bash
cd ai-edu-backend
./mvnw clean install -DskipTests
cd ai-edu-interface
../mvnw spring-boot:run
```

### 启动AI服务

```bash
cd ai-edu-ai-service
pip install -r requirements.txt
python main.py
```

## DDD架构说明

| 层级 | 模块 | 职责 |
|------|------|------|
| Domain | ai-edu-domain | 核心业务逻辑：实体、值对象、聚合根、领域服务、仓储接口 |
| Application | ai-edu-application | 编排领域服务，处理用例流转 |
| Infrastructure | ai-edu-infrastructure | 持久化、消息队列、文件存储、缓存、AI服务调用 |
| Interface | ai-edu-interface | Thymeleaf控制器、REST API、WebSocket |
| Common | ai-edu-common | 通用工具、异常、常量 |

## 子域划分

- **User**: 用户、学生、老师、家长
- **Question**: 题库、知识点
- **Homework**: 作业提交、批改
- **Learning**: 错题本、知识点掌握