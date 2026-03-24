## Why

平台需要对接已部署的 Python LLM Gateway 服务（端口 9527），为教育平台提供 AI 能力支持，包括页面助手、作业批改、FAQ 问答、图片分析、内容生成、数学辅导等场景。当前 Java 后端缺乏与 LLM 服务的集成能力。

## What Changes

- 新增 LLM Gateway 客户端，使用 WebClient 实现 HTTP 调用
- 新增同步对话接口 `/api/llm/chat`
- 新增流式对话接口 `/api/llm/chat/stream`（SSE）
- 新增模型查询接口 `/api/llm/models`、`/api/llm/allowed-models`、`/api/llm/scenes`
- 支持前端通过 `scene`（场景）或 `provider + model` 指定模型
- 添加 `spring-boot-starter-webflux` 依赖

## Capabilities

### New Capabilities

- `llm-gateway`: LLM Gateway 集成能力，提供与 Python LLM 服务的通信接口，支持同步对话、流式对话（SSE）、模型查询等功能

### Modified Capabilities

(无现有能力需要修改)

## Impact

**新增依赖**:
- `ai-edu-infrastructure`: 添加 `spring-boot-starter-webflux`

**新增代码**:
- `ai-edu-domain/shared/service/LlmGateway.java` - 接口定义
- `ai-edu-infrastructure/ai/LlmGatewayImpl.java` - WebClient 实现
- `ai-edu-infrastructure/ai/LlmGatewayProperties.java` - 配置属性
- `ai-edu-application/dto/ChatRequest.java` - 请求 DTO
- `ai-edu-application/dto/ChatResponse.java` - 响应 DTO
- `ai-edu-application/dto/ModelInfo.java` - 模型信息 DTO
- `ai-edu-application/service/LlmAppService.java` - 应用服务
- `ai-edu-interface/api/LlmApiController.java` - REST 控制器

**配置变更**:
- `application.yml`: 新增 `ai-edu.llm.gateway.*` 配置项

**错误码**:
- `ErrorCode.java`: 新增 LLM 相关错误码（6xxxx）