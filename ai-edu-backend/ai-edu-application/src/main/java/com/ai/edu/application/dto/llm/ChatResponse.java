package com.ai.edu.application.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM 对话响应 DTO
 *
 * @author AI Edu Platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * LLM 生成的响应内容
     */
    private String response;

    /**
     * 会话ID（用于后续多轮对话）
     */
    private String sessionId;

    /**
     * 实际使用的模型
     */
    private String modelUsed;

    /**
     * Token 使用量
     */
    private Usage usage;

    /**
     * 工具调用信息（如果模型调用了工具）
     */
    private List<ToolCall> toolCalls;

    /**
     * Token 使用量内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        /**
         * 输入 Token 数量
         */
        private Integer promptTokens;

        /**
         * 输出 Token 数量
         */
        private Integer completionTokens;

        /**
         * 总 Token 数量
         */
        private Integer totalTokens;
    }

    /**
     * 工具调用信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        /**
         * 工具调用ID
         */
        private String id;

        /**
         * 工具类型
         */
        private String type;

        /**
         * 工具名称
         */
        private String name;

        /**
         * 工具参数
         */
        private String arguments;
    }
}