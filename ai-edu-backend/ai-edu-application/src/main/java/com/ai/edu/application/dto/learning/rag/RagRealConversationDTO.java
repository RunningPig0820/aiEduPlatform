package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 真实对话质量统计（并入 /eval/report 的 realConversation 区段，前端契约，camelCase）。
 *
 * <p>每轮真实问答生成完后 Java 后台异步 LLM 打分（0-5），累计进 Redis（TTL 24h）；
 * 报告读取聚合，与离线 benchmark 并存展示。打分失败/无生成轮不入统计。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagRealConversationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 已评分真实对话轮数 */
    private Integer count;

    /** 平均 LLM 质量分（0-5） */
    private Double avgQuality;

    /** 引用有效比例（quotedKeys 非空轮数 / 总评分轮数） */
    private Double quotedRatio;

    /** 平均单轮耗时（ms） */
    private Long avgLatencyMs;
}
