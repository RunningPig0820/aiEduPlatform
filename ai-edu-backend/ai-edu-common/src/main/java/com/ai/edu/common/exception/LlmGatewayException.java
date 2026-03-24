package com.ai.edu.common.exception;

import lombok.Getter;

/**
 * LLM Gateway 异常
 */
@Getter
public class LlmGatewayException extends RuntimeException {

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 创建 LLM Gateway 异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public LlmGatewayException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 创建 LLM Gateway 异常（带原因）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     */
    public LlmGatewayException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}