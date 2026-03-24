package com.ai.edu.domain.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 对话响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * AI 响应内容
     */
    private String response;

    /**
     * 会话 ID
     */
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * 实际使用的模型，格式: {provider}/{model}
     */
    @JsonProperty("model_used")
    private String modelUsed;

    /**
     * Token 使用量统计
     */
    private Usage usage;

    /**
     * 工具调用列表
     */
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;
}