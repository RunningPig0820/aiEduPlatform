# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

教育 AI 智能体平台 (Education AI Platform) - A DDD-architected educational AI platform with three client types: Teacher (老师端), Student (学生端), and Parent (家长端).

## Architecture

This project follows **Domain-Driven Design (DDD)** with a 4-layer architecture:

| Layer | Module | Description |
|-------|--------|-------------|
| Domain | `ai-edu-domain` | Core business logic: entities, value objects, aggregates, domain services, repository interfaces. No dependencies on other layers. |
| Application | `ai-edu-application` | Orchestrates domain services, handles use cases, DTO/aggregate conversion |
| Infrastructure | `ai-edu-infrastructure` | Technical implementations: persistence, MQ, file storage, cache, AI service clients |
| Interface | `ai-edu-interface` | User interaction: Thymeleaf controllers (SSR) and REST APIs |
| Common | `ai-edu-common` | Shared utilities, exceptions, constants |

### Bounded Contexts (Subdomains)

- **User**: User, Student, Teacher, Parent entities with role-based access
- **Question**: Question bank with knowledge point tagging, difficulty levels
- **Homework**: Homework submission, grading, scoring
- **Learning**: Error book (错题本), knowledge mastery tracking, emotion recognition

## Tech Stack

### Backend (Java)
- **Framework**: Spring Boot 3.2+ with Spring Modulith for modular DDD
- **ORM**: Spring Data JPA (aggregate persistence) + MyBatis-Plus (complex queries)
- **Database**: MySQL 8.0 with Flyway migrations
- **Cache**: Redis (Redisson client)
- **Auth**: Spring Security + Spring Session (Redis-backed)
- **MQ**: RabbitMQ for async AI task processing
- **Storage**: MinIO (S3-compatible)

### Frontend (Server-Side Rendering)
- **Template Engine**: Thymeleaf
- **CSS**: Tailwind CSS v3 + daisyUI
- **JS**: Alpine.js v3 (lightweight interactivity)
- **Charts**: ECharts
- **Rich Text**: TinyMCE

### AI Service (Python)
- **Framework**: FastAPI
- **OCR**: PaddleOCR (PP-StructureV2)
- **LLM**: Qwen2.5-Turbo API or local Qwen2.5-7B-Instruct
- **AI Orchestration**: LangChain
- **Vector DB**: Milvus

## Key Design Patterns

### Event-Driven Flow
1. Domain layer publishes domain events (e.g., `HomeworkSubmittedEvent`)
2. Application layer listens and triggers infrastructure (MQ, cache updates)
3. AI service consumes MQ messages, processes asynchronously, calls back
4. WebSocket pushes results to online users

### Repository Pattern
- Repository **interfaces** defined in Domain layer
- Repository **implementations** in Infrastructure layer using JPA + MyBatis-Plus

### Aggregate Pattern
- Aggregates ensure consistency boundaries (e.g., `HomeworkAggregate`, `UserAggregate`)
- External access only through aggregate roots

## Implementation Notes

- Use MapStruct for DTO ↔ Entity conversion
- Use Lombok to reduce boilerplate
- Domain layer must remain pure (no framework dependencies)
- Use value objects for domain concepts (UserId, QuestionId, Difficulty, etc.)
- Store session in Redis for multi-instance deployment