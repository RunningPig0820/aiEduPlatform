package com.ai.edu.domain.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 对话请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户消息内容
     */
    private String message;

    /**
     * 用户 ID（必须是整数）
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 场景代码，用于自动选择模型
     */
    private String scene;

    /**
     * 指定 Provider，需配合 model 使用
     */
    private String provider;

    /**
     * 指定模型名称，需配合 provider 使用
     */
    private String model;

    /**
     * 会话 ID，用于多轮对话
     */
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * 当前页面编码
     */
    @JsonProperty("page_code")
    private String pageCode;

    /**
     * 额外上下文信息
     */
    private Object context;

    /**
     * 创建基本的对话请求
     */
    public static ChatRequest of(String message, Long userId) {
        return ChatRequest.builder()
                .message(message)
                .userId(userId)
                .build();
    }

    /**
     * 创建带场景的对话请求
     */
    public static ChatRequest of(String message, Long userId, String scene) {
        return ChatRequest.builder()
                .message(message)
                .userId(userId)
                .scene(scene)
                .build();
    }
}