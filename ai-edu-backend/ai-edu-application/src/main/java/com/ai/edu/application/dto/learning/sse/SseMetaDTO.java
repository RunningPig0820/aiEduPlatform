package com.ai.edu.application.dto.learning.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE {@code meta} 事件（前端 camelCase）——护栏已放行的 type 先行到达。
 *
 * <p>护栏拒绝时（DENY）带 {@code denied}（原始请求 type）+ {@code reason}，无 token 流；
 * 终止场景（无关/学习方法/非数学/安全）带 {@code reply} 直接回复，无 token 流。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseMetaDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 ID */
    private Long sessionId;

    /** 会话状态（ACTIVE / ARCHIVED / TERMINATED） */
    private String status;

    /** 已放行 type（hint/approach/reveal/concept/switch/end） */
    private String type;

    /** 轮次计数 */
    private Integer roundCount;

    /** 要答案次数 */
    private Integer answerRequestCount;

    /** 学生回答评估（可空） */
    private SseEvalDTO eval;

    /** 换题时的展示新题（可选，不落库） */
    private String newQuestion;

    /** 护栏拒绝：原始请求 type（如 reveal 被拦） */
    private String denied;

    /** 护栏拒绝原因（answerCountInsufficient / roundLimitExceeded / safetyFlagHit） */
    private String reason;

    /** Python 结构化输出兜底标记（type=hint + degraded=true，监控用） */
    private Boolean degraded;

    /** 终止场景直接回复（无关/学习方法/非数学/安全），无 token 流 */
    private String reply;
}
