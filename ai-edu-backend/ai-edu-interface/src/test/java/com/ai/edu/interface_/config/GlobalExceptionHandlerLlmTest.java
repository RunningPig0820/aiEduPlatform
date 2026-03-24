package com.ai.edu.interface_.config;

import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.LlmGatewayException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler LLM 异常处理测试
 *
 * 测试 LLM Gateway 异常的全局处理
 */
class GlobalExceptionHandlerLlmTest {

    @Test
    void shouldHandleLlmGatewayException() {
        // 准备
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        LlmGatewayException exception = new LlmGatewayException(
                ErrorCode.LLM_SERVICE_UNAVAILABLE,
                "LLM 服务不可用"
        );

        // 执行
        Map<String, Object> result = handler.handleLlmGatewayException(exception);

        // 验证
        assertEquals(ErrorCode.LLM_SERVICE_UNAVAILABLE, result.get("code"));
        assertEquals("LLM 服务不可用", result.get("message"));
    }

    @Test
    void shouldHandleLlmGatewayExceptionWithCause() {
        // 准备
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Throwable cause = new RuntimeException("Connection timeout");
        LlmGatewayException exception = new LlmGatewayException(
                ErrorCode.LLM_TIMEOUT,
                "LLM 调用超时",
                cause
        );

        // 执行
        Map<String, Object> result = handler.handleLlmGatewayException(exception);

        // 验证
        assertEquals(ErrorCode.LLM_TIMEOUT, result.get("code"));
        assertEquals("LLM 调用超时", result.get("message"));
    }

    @Test
    void shouldHandleAllLlmErrorCodes() {
        // 准备
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // 验证所有 LLM 错误码都能正确处理
        LlmGatewayException ex1 = new LlmGatewayException(
                ErrorCode.LLM_SERVICE_UNAVAILABLE, "服务不可用");
        Map<String, Object> result1 = handler.handleLlmGatewayException(ex1);
        assertEquals("60001", result1.get("code"));

        LlmGatewayException ex2 = new LlmGatewayException(
                ErrorCode.LLM_MODEL_NOT_ALLOWED, "模型不允许");
        Map<String, Object> result2 = handler.handleLlmGatewayException(ex2);
        assertEquals("60002", result2.get("code"));

        LlmGatewayException ex3 = new LlmGatewayException(
                ErrorCode.LLM_CALL_FAILED, "调用失败");
        Map<String, Object> result3 = handler.handleLlmGatewayException(ex3);
        assertEquals("60003", result3.get("code"));

        LlmGatewayException ex4 = new LlmGatewayException(
                ErrorCode.LLM_TIMEOUT, "超时");
        Map<String, Object> result4 = handler.handleLlmGatewayException(ex4);
        assertEquals("60004", result4.get("code"));

        LlmGatewayException ex5 = new LlmGatewayException(
                ErrorCode.LLM_INVALID_PARAMS, "参数无效");
        Map<String, Object> result5 = handler.handleLlmGatewayException(ex5);
        assertEquals("60005", result5.get("code"));
    }
}