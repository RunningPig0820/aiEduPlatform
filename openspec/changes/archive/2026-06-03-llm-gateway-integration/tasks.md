## 1. 依赖与配置

- [x] 1.1 在 ai-edu-infrastructure/pom.xml 添加 spring-boot-starter-webflux 依赖
- [x] 1.2 在 application.yml 添加 LLM Gateway 配置项 (base-url, internal-token, timeouts)
- [x] 1.3 创建 LlmGatewayProperties 配置类 (@ConfigurationProperties)

## 2. Domain 层 - 接口定义

- [x] 2.1 创建 LlmGateway 接口 (com.ai.edu.domain.shared.service)
- [x] 2.2 定义 chat() 方法签名: Mono<ChatResponse> chat(ChatRequest request)
- [x] 2.3 定义 chatStream() 方法签名: Flux<ServerSentEvent<String>> chatStream(ChatRequest request)
- [x] 2.4 定义 getAllowedModels() 方法签名: Mono<AllowedModelsResponse> getAllowedModels()

## 3. Infrastructure 层 - WebClient 实现

- [x] 3.1 创建 LlmGatewayImpl 实现类 (com.ai.edu.infrastructure.ai)
- [x] 3.2 配置 WebClient Bean (设置 base URL、超时、默认 headers)
- [x] 3.3 实现 chat() 方法 - POST /api/llm/chat
- [x] 3.4 实现 chatStream() 方法 - POST /api/llm/chat/stream (SSE)
- [x] 3.5 实现 getAllowedModels() 方法 - GET /api/llm/allowed-models
- [x] 3.6 实现 getModels() 方法 - GET /api/llm/models
- [x] 3.7 实现 getScenes() 方法 - GET /api/llm/scenes

## 4. Application 层 - DTO 与服务

- [x] 4.1 创建 ChatRequest DTO (message, userId, scene, provider, model, sessionId, pageCode, context)
- [x] 4.2 创建 ChatResponse DTO (response, sessionId, modelUsed, usage, toolCalls)
- [x] 4.3 创建 ModelInfo DTO (provider, model, fullName, displayName, free, supportsTools, supportsVision, description)
- [x] 4.4 创建 AllowedModelsResponse DTO
- [x] 4.5 创建 ModelsResponse DTO
- [x] 4.6 创建 ScenesResponse DTO
- [x] 4.7 创建 LlmAppService 应用服务

## 5. Interface 层 - REST 控制器

- [x] 5.1 创建 LlmApiController (com.ai.edu.interface_.api)
- [x] 5.2 实现 POST /api/llm/chat - 同步对话接口
- [x] 5.3 实现 POST /api/llm/chat/stream - 流式对话接口 (SSE)
- [x] 5.4 实现 GET /api/llm/allowed-models - 获取允许调用的模型列表
- [x] 5.5 实现 GET /api/llm/models - 获取所有模型列表
- [x] 5.6 实现 GET /api/llm/scenes - 获取场景列表
- [x] 5.7 配置 SecurityConfig 允许 /api/llm/** 路径访问

## 6. 错误处理

- [x] 6.1 在 ErrorCode.java 添加 LLM 相关错误码 (60001-60005)
- [x] 6.2 创建 LlmGatewayException 异常类
- [x] 6.3 在 GlobalExceptionHandler 添加 LLM 异常处理

## 7. 测试

- [x] 7.1 编写 LlmGatewayImplTest 单元测试
- [x] 7.2 编写 LlmAppServiceTest 单元测试
- [x] 7.3 编写 LlmApiControllerTest 集成测试