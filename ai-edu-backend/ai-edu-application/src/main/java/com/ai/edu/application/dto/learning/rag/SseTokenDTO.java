package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE token 事件数据（前端契约，camelCase）。
 *
 * <p>生成正文增量（doubao 流式逐块透传）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseTokenDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 生成内容增量 */
    private String text;
}
