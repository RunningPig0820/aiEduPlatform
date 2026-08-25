package com.ai.edu.application.dto.learning.rag;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 助手评估报告（baseline 白盒，前端契约，camelCase）。
 *
 * <p>由 Python {@code GET /api/rag/assistant/eval/report} 返回的 snake_case 报告
 * 经 SNAKE_MAPPER 反序列化（hit_at_3 → hitAt3 等）。无报告 → 404（10002）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagEvalReportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 评估集版本 */
    private String version;

    /** 评估用例条数 */
    private Integer count;

    /** 召回 hit@3（平均）。SNAKE_CASE 会把 hitAt3 翻译成 hit_at3（数字前不加下划线），
     * 用 @JsonAlias 只影响反序列化收 Python 的 hit_at_3；序列化给前端仍是 hitAt3 */
    @JsonAlias("hit_at_3")
    private Double hitAt3;

    /** 回答质量分（平均） */
    private Double qualityAvg;

    /** 平均耗时（ms） */
    private Long avgLatencyMs;

    /** 平均成本（元） */
    private Double avgCostYuan;

    /** 可判定率 */
    private Double judgedRatio;

    /** precision@3（平均）。同上，@JsonAlias 收 Python 的 precision_at_3 */
    @JsonAlias("precision_at_3")
    private Double precisionAt3;

    /** 引用有效命中率 */
    private Double quotedValidRatio;

    /** 最近一次评测运行的 ISO 时间（诚实性：数字带"某次真实运行快照"语义） */
    private String evaluatedAt;

    /** 命中样本数（如 5/6） */
    private Integer hitCases;

    /** 平均每样本 token 消耗 */
    private Integer avgTokens;

    /** 本轮评测总成本（元） */
    private Double totalCostYuan;

    /** 评测是否进行中（异步 run 模型；前端轮询此字段到 false 再刷新） */
    private Boolean running;

    /** 真实对话质量统计（Java 侧每轮 LLM 打分累计；与离线 benchmark 并存） */
    private RagRealConversationDTO realConversation;
}
