package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 触发重评测的响应（前端契约，camelCase）。
 *
 * <p>异步模型：Python {@code POST /api/rag/assistant/eval/run} 后台跑一轮评估，
 * 立即返回 running 状态；前端轮询 GET /eval/report 的 running=false 后刷新。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagEvalRunDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 评测是否进行中 */
    private Boolean running;

    /** 是否已有一轮在跑（防重复触发；幂等返回，非错误） */
    private Boolean alreadyRunning;
}
