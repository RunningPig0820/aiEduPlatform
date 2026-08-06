package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Python decide 输出动作元数据（Java↔Python 内部契约，snake_case）。
 *
 * <p>字段用 String 承载 Python 原始值（type="hint"、signal="practicing" 为小写），
 * 由护栏/应用层经容错 fromCode 转为领域枚举（非法 → 默认，不阻断）。
 * {@code reason} 是 Python 可选调试字段，Java 不建模（Jackson 默认容忍未知字段）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionMeta implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 动作类型（闭集字符串：hint/approach/reveal/concept/switch/end） */
    private String type;

    /** 学生回答评估 */
    private EvalInfo eval;

    /** 掌握度信号（kp_label + signal） */
    @JsonProperty("mastery_signals")
    private List<MasterySignalItem> masterySignals;

    /** switch 时的新题文本 */
    @JsonProperty("new_question")
    private String newQuestion;

    /** type=end 时的收尾原因（COMPLETED/ANSWER_REVEALED/ABANDONED/ROUND_LIMIT；null=终止类，见收尾规则） */
    @JsonProperty("end_reason")
    private String endReason;

    /** 收尾总结 / 终止场景的直接回复（Python 自由文本，可空） */
    private String summary;

    /** 高危内容标记（拦截由 Java 执行） */
    @JsonProperty("safety_flag")
    private Boolean safetyFlag = false;

    /** 结构化输出兜底标记：type=hint + degraded=true（监控用，Java 按普通 hint 放行） */
    private Boolean degraded = false;
}
