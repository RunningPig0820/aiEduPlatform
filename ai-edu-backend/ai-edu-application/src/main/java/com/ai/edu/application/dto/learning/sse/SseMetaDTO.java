package com.ai.edu.application.dto.learning.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

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

    /** 学科（subject-classify 输出闭集：math/physics/chemistry/biology/other；正常答疑轮带，非数学跳过流不带；可空，前端缺失隐藏该行） */
    private String subject;

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

    /** Python 决策自由文本（解释为何选该 action，前端"为什么"hover 补充，可空） */
    private String decideReason;

    /** 题目涉及知识点（首轮 decide 读题分析，可空；完整读题分析功能后续替换数据源） */
    private List<String> questionKps;

    /** 掌握度信号（每轮 decide 输出，前端"知识点确认"阶段数据源；{kpLabel, signal} 数组） */
    private List<SseMasterySignalDTO> masterySignals;

    /** Python 结构化输出兜底标记（type=hint + degraded=true，监控用） */
    private Boolean degraded;

    /** 终止场景直接回复（无关/学习方法/非数学/安全），无 token 流 */
    private String reply;
}
