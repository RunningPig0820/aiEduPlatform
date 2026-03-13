# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目定位

本项目是**纯 Java DDD 后端**，仅提供 REST API。前端和 AI 服务已独立部署。

## Build Commands

```bash
# Build all modules
cd ai-edu-backend && mvn clean install -DskipTests

# Run the application (from interface module)
cd ai-edu-backend/ai-edu-interface && mvn spring-boot:run

# Run tests
mvn test

# Run single test class
mvn test -Dtest=ClassNameTest

# Run single test method
mvn test -Dtest=ClassNameTest#methodName
```

## Infrastructure

Start local infrastructure via Docker Compose:
```bash
cd deploy && docker-compose up -d
```
This starts MySQL (3306), Redis (6379), RabbitMQ (5672/15672), MinIO (9000/9001).

**Note**: Development mode currently disables Redis and RabbitMQ auto-configuration in `application.yml`.

## Architecture

This project follows **Domain-Driven Design (DDD)** with a 4-layer architecture:

| Layer | Module | Description |
|-------|--------|-------------|
| Domain | `ai-edu-domain` | Core business logic: entities, value objects, aggregates, domain services, repository interfaces. No dependencies on other layers. |
| Application | `ai-edu-application` | Orchestrates domain services, handles use cases, DTO/aggregate conversion |
| Infrastructure | `ai-edu-infrastructure` | Technical implementations: persistence, cache, file storage |
| Interface | `ai-edu-interface` | REST API controllers |
| Common | `ai-edu-common` | Shared utilities, exceptions, constants |

### Bounded Contexts (Subdomains)

| Context | Subdomain | Description |
|---------|-----------|-------------|
| User | 用户域 | User, Student, Teacher, Parent entities with role-based access |
| Organization | 组织域 | School, Class, Grade, StudentClass, TeacherClass |
| Question | 题库域 | Question bank with knowledge point tagging, difficulty levels |
| Homework | 作业域 | Homework submission, grading, scoring |
| Learning | 学习域 | Error book (错题本), knowledge mastery tracking |

### Package Structure Convention

Each bounded context follows this structure under `com.ai.edu.domain.{context}/`:
```
model/
  ├── entity/        # Entities (User, Question, Homework)
  ├── valueobject/   # Value objects (Role, Difficulty, Score)
  └── aggregate/     # Aggregate roots (UserAggregate, HomeworkAggregate)
repository/          # Repository interfaces
service/             # Domain services
event/               # Domain events (optional)
```

## Tech Stack

### Backend (Java 21)
- **Framework**: Spring Boot 3.2.5 + Spring Modulith 1.2.3
- **ORM**: Spring Data JPA (aggregate persistence) + MyBatis-Plus 3.5.5 (complex queries)
- **Database**: MySQL 8.0 with Flyway migrations
- **Cache**: Redis (Redisson 3.27.2 client)
- **Auth**: Spring Security + Spring Session (Redis-backed)
- **Storage**: MinIO (S3-compatible)

## Key Design Patterns

### Dependency Injection
Use `@Resource` for dependency injection (not constructor injection).

### Repository Pattern
- Repository **interfaces** defined in Domain layer (`com.ai.edu.domain.{context}.repository`)
- Repository **implementations** in Infrastructure layer (`com.ai.edu.infrastructure.persistence.repository`)
- JPA repositories scanned from `com.ai.edu.infrastructure.persistence.jpa`
- MyBatis mappers scanned from `com.ai.edu.infrastructure.persistence.mapper`

### Aggregate Pattern
- Aggregates ensure consistency boundaries (e.g., `HomeworkAggregate`, `UserAggregate`)
- External access only through aggregate roots

### Entity Design
- Use JPA annotations on domain entities
- Factory methods for entity creation (e.g., `User.create()`)
- Protected no-args constructor for JPA
- Lombok `@Getter` for field access

### REST API Design
- All controllers in `com.ai.edu.interface_.api`
- Use `ApiResponse<T>` as unified response wrapper
- Follow RESTful conventions

## Implementation Notes

- Use MapStruct for DTO ↔ Entity conversion
- Use Lombok to reduce boilerplate
- Domain layer must remain pure (no framework dependencies other than JPA annotations)
- Use value objects for domain concepts (UserId, QuestionId, Difficulty, etc.)
- Store session in Redis for multi-instance deployment