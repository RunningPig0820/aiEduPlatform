## ADDED Requirements

### Requirement: 用户可以发起同步对话

系统 SHALL 允许用户通过消息内容发起 LLM 同步对话，并返回 AI 响应。

#### Scenario: 基本对话成功
- **WHEN** 用户发送消息 "你好" 且指定 user_id
- **THEN** 系统返回 AI 响应内容，包含 response、session_id、model_used、usage 信息

#### Scenario: 通过场景选择模型
- **WHEN** 用户发送消息并指定 scene 为 "homework_grading"
- **THEN** 系统根据场景自动选择模型（如 deepseek-chat）并返回响应

#### Scenario: 指定特定模型
- **WHEN** 用户发送消息并指定 provider 为 "zhipu" 且 model 为 "glm-4-flash"
- **THEN** 系统使用指定模型返回响应

#### Scenario: 模型不在白名单
- **WHEN** 用户指定的模型不在允许调用列表中
- **THEN** 系统返回错误响应，提示模型不允许调用

---

### Requirement: 用户可以发起流式对话

系统 SHALL 允许用户发起流式对话，通过 SSE 返回实时响应片段。

#### Scenario: 流式响应成功
- **WHEN** 用户发起流式对话请求
- **THEN** 系统返回 SSE 事件流，包含 token 事件（内容片段）和 done 事件（完成信息）

#### Scenario: 流式响应错误
- **WHEN** 流式对话过程中发生错误
- **THEN** 系统发送 error 事件，包含错误码和错误信息

---

### Requirement: 用户可以获取允许调用的模型列表

系统 SHALL 返回当前允许调用的 LLM 模型列表。

#### Scenario: 获取模型列表成功
- **WHEN** 用户请求允许调用的模型列表
- **THEN** 系统返回模型列表，每个模型包含 provider、model、full_name、display_name、free、supports_tools、supports_vision、description

---

### Requirement: 用户可以获取所有模型列表

系统 SHALL 返回所有配置的 LLM 模型列表，按 Provider 分组。

#### Scenario: 获取所有模型成功
- **WHEN** 用户请求所有模型列表
- **THEN** 系统返回按 Provider 分组的模型列表

---

### Requirement: 用户可以获取场景列表

系统 SHALL 返回所有可用的场景配置列表。

#### Scenario: 获取场景列表成功
- **WHEN** 用户请求场景列表
- **THEN** 系统返回场景列表，每个场景包含 code、default_provider、default_model、description

---

### Requirement: LLM 调用需要用户认证

系统 SHALL 验证用户身份后才允许调用 LLM 接口。

#### Scenario: 未登录用户调用
- **WHEN** 未登录用户尝试调用 LLM 接口
- **THEN** 系统返回 401 未授权错误

#### Scenario: 已登录用户调用
- **WHEN** 已登录用户调用 LLM 接口
- **THEN** 系统正常处理请求，并将 user_id 传递给 Python 服务