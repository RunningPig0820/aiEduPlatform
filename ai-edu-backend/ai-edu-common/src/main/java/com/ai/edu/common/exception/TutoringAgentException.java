package com.ai.edu.common.exception;

import com.ai.edu.common.constant.ErrorCode;

/**
 * 答疑 Python agent 调用异常（decide/generate/OCR 重试后仍失败）。
 *
 * <p>错误码 40004（TUTORING_AGENT_FAILED）。流式路径由编排服务捕获后降级为
 * "网络波动，请重试"SSE 消息（会话保持 ACTIVE 不断开）；非流式路径由接口层映射 40004。
 */
public class TutoringAgentException extends BusinessException {

    public TutoringAgentException(String message) {
        super(ErrorCode.TUTORING_AGENT_FAILED, message);
    }

    public TutoringAgentException(String message, Throwable cause) {
        super(ErrorCode.TUTORING_AGENT_FAILED, message, cause);
    }
}
