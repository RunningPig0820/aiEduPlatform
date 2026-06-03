## Context

### 背景
教育平台需要对接已部署的 Python LLM Gateway 服务（端口 9527），提供 AI 对话能力。Python 服务已经实现了多 Provider 支持（智谱、DeepSeek、阿里通义等）、模型白名单、场景自动选择等功能。

### 约束
- Java 后端作为网关层，负责用户认证、请求转发、日志记录
- Python 服务负责 LLM 调用、模型管理、Token 验证
- 遵循 DDD 四层架构：Domain → Application → Infrastructure → Interface

### 外部服务接口
Python LLM Gateway 提供的 API：
- `POST /api/llm/chat` - 同步对话
- `POST /api/llm/chat/stream` - 流式对话（SSE）
- `GET /api/llm/allowed-models` - 允许调用的模型列表
- `GET /api/llm/models` - 所有模型列表
- `GET /api/llm/scenes` - 场景列表

## Goals / Non-Goals

**Goals:**
- 实现 LLM Gateway 客户端，支持同步和流式对话
- 提供 REST API 供前端调用
- 支持前端通过场景或模型参数选择 LLM
- 遵循 DDD 架构，接口定义在 Domain 层，实现在 Infrastructure 层

**Non-Goals:**
- 不实现 LLM 调用逻辑（由 Python 服务负责）
- 不实现模型管理和配置（由 Python 服务负责）
- 不实现用户配额或限流（后续迭代）

## Decisions

### 1. HTTP 客户端选择：WebClient

**选择**: Spring WebFlux WebClient

**理由**:
- 原生支持响应式编程（Mono/Flux）
- 原生支持 SSE 流式响应
- Spring Boot 3 推荐的 HTTP 客户端
- 与 Spring MVC 可以共存（仅使用 webflux 依赖，不启用 WebFlux Server）

**替代方案**:
- RestTemplate: Spring 6 标记为维护模式，不支持响应式
- OkHttp: 需要手动处理 SSE，增加复杂度

### 2. SSE 流式响应策略：透传

**选择**: Java 层透传 Python 服务的 SSE 事件

**理由**:
- 减少解析开销，降低延迟
- 前端可以直接处理原始事件
- 后续如需日志记录，可在透传前拦截

**实现**:
```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatRequest request) {
    return llmGateway.chatStream(request);
}
```

### 3. 模型选择策略

**选择**: 优先级规则

1. 如果指定 `provider + model`，直接使用（需在白名单）
2. 如果指定 `scene`，查询场景配置自动选择
3. 都不指定，使用默认模型 `zhipu/glm-4-flash`

**理由**: 前端灵活控制，后端兜底保护

### 4. 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│ Interface Layer                                              │
│ LlmApiController                                             │
│ - 接收 HTTP 请求                                             │
│ - 返回同步响应 / SSE 流                                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ Application Layer                                            │
│ LlmAppService                                                │
│ - 编排调用                                                   │
│ - DTO 转换                                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ Domain Layer                                                 │
│ LlmGateway (interface)                                       │
│ - 定义契约                                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ Infrastructure Layer                                         │
│ LlmGatewayImpl                                               │
│ - WebClient 实现                                             │
│ - 配置管理                                                   │
└─────────────────────────────────────────────────────────────┘
```

## Risks / Trade-offs

### 风险 1: Python 服务不可用
- **风险**: Python 服务宕机导致 LLM 功能完全不可用
- **缓解**: 实现优雅降级，返回友好错误提示；添加健康检查

### 风险 2: SSE 连接超时
- **风险**: 长时间 LLM 响应可能导致连接超时
- **缓解**: 配置合理的超时时间（默认 60s）；前端实现重连机制

### 风险 3: 并发压力
- **风险**: 大量并发请求可能压垮 Python 服务
- **缓解**: 后续可添加限流（本迭代不实现）