package com.ai.edu.common.exception;

import com.ai.edu.common.constant.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmGatewayException 异常类测试
 *
 * 测试 LLM Gateway 异常的创建和属性访问
 */
class LlmGatewayExceptionTest {

    @Test
    void shouldCreateExceptionWithCodeAndMessage() {
        // 验证使用 code 和 message 创建异常
        LlmGatewayException exception = new LlmGatewayException(
                ErrorCode.LLM_SERVICE_UNAVAILABLE,
                "LLM 服务不可用"
        );

        assertEquals(ErrorCode.LLM_SERVICE_UNAVAILABLE, exception.getCode());
        assertEquals("LLM 服务不可用", exception.getMessage());
    }

    @Test
    void shouldCreateExceptionWithCodeMessageAndCause() {
        // 验证使用 code、message 和 cause 创建异常
        Throwable cause = new RuntimeException("Connection refused");
        LlmGatewayException exception = new LlmGatewayException(
                ErrorCode.LLM_CALL_FAILED,
                "LLM 调用失败",
                cause
        );

        assertEquals(ErrorCode.LLM_CALL_FAILED, exception.getCode());
        assertEquals("LLM 调用失败", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void shouldExtendRuntimeException() {
        // 验证继承自 RuntimeException
        LlmGatewayException exception = new LlmGatewayException(
                ErrorCode.LLM_TIMEOUT,
                "LLM 调用超时"
        );

        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void shouldUseAllErrorCodes() {
        // 验证所有错误码都可以正确使用
        LlmGatewayException ex1 = new LlmGatewayException(
                ErrorCode.LLM_SERVICE_UNAVAILABLE, "服务不可用");
        assertEquals("60001", ex1.getCode());

        LlmGatewayException ex2 = new LlmGatewayException(
                ErrorCode.LLM_MODEL_NOT_ALLOWED, "模型不允许");
        assertEquals("60002", ex2.getCode());

        LlmGatewayException ex3 = new LlmGatewayException(
                ErrorCode.LLM_CALL_FAILED, "调用失败");
        assertEquals("60003", ex3.getCode());

        LlmGatewayException ex4 = new LlmGatewayException(
                ErrorCode.LLM_TIMEOUT, "超时");
        assertEquals("60004", ex4.getCode());

        LlmGatewayException ex5 = new LlmGatewayException(
                ErrorCode.LLM_INVALID_PARAMS, "参数无效");
        assertEquals("60005", ex5.getCode());
    }
}