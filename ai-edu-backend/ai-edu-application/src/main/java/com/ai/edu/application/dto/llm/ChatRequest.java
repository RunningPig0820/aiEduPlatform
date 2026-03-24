package com.ai.edu.application.dto.llm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * LLM 对话请求 DTO
 *
 * @author AI Edu Platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 10000, message = "消息内容不能超过10000字符")
    private String message;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 场景代码
     * 例如: homework_help, error_analysis, knowledge_qa
     */
    private String scene;

    /**
     * LLM 提供商
     * 例如: openai, anthropic, google
     */
    private String provider;

    /**
     * 模型名称
     * 例如: gpt-4, claude-3-opus
     */
    private String model;

    /**
     * 会话ID（用于多轮对话）
     */
    private String sessionId;

    /**
     * 页面代码（标识调用来源页面）
     */
    private String pageCode;

    /**
     * 上下文信息（额外参数）
     */
    private Map<String, Object> context;
}