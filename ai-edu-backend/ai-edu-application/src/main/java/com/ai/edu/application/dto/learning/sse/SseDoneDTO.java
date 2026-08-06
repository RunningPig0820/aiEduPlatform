package com.ai.edu.application.dto.learning.sse;

import com.ai.edu.application.dto.learning.SummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE {@code done} 事件（前端 camelCase）——正文流结束后的终态（状态 + eval + summary）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseDoneDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 ID */
    private Long sessionId;

    /** 终态（ACTIVE / ARCHIVED / TERMINATED） */
    private String status;

    /** 轮次计数 */
    private Integer roundCount;

    /** 学生回答评估（可空） */
    private SseEvalDTO eval;

    /** 收尾总结（可空） */
    private SummaryDTO summary;

    /** 收尾原因（COMPLETED / ANSWER_REVEALED / ABANDONED / ROUND_LIMIT，可空） */
    private String endReason;
}
