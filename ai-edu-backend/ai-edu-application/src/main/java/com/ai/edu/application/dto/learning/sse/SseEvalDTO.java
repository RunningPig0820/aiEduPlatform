package com.ai.edu.application.dto.learning.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE 事件 eval 段（前端 camelCase）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseEvalDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 回答是否正确 */
    private Boolean correct;

    /** 错误类型（可空） */
    private String errorType;

    /** 情绪（F7 七态） */
    private String emotion;

    /** 是否独立解出 */
    private Boolean exerciseComplete;
}
